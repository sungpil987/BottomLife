extends RefCounted
class_name Economy

const SAVE_PATH := "user://bottomlife_reborn.json"
const BUSINESS_NAMES := ["종이컵 구걸", "폐지 수집소", "붕어빵 노점", "편의점", "분식집", "카페", "프랜차이즈", "공장", "IT 기업", "대기업", "우주 산업"]
const COSTS := [50.0,500.0,5000.0,50000.0,500000.0,5000000.0,50000000.0,500000000.0,5000000000.0,50000000000.0,500000000000.0]
const YIELDS := [0.1,1.0,10.0,100.0,1000.0,10000.0,100000.0,1000000.0,10000000.0,100000000.0,1000000000.0]
var state: Dictionary = {}
func _init() -> void:
    state = {"money":0.0,"lifetime":0.0,"gems":0.0,"ore":0.0,"souls":0.0,"tapLevel":1,"workers":0,"drill":1,"depth":1,"prestige":0,"soulPower":0,"pick":1,"totalTaps":0,"bestDepth":1,"questClaims":0,"owned":[0,0,0,0,0,0,0,0,0,0,0],"relics":[0,0,0,0,0,0,0,0],"savedAt":0}
    load_game()
func val(key:String) -> float: return float(state.get(key,0))
func mul() -> float: return (1.0 + val("soulPower")*.25) * (1.0 + val("prestige")*.05)
func tap_value() -> float: return pow(1.65,val("tapLevel")-1.0)*mul()
func income() -> float:
    var v := val("workers")*.5
    for i in range(11): v += float(state["owned"][i])*YIELDS[i]
    return v*mul()
func earn(amount:float) -> void:
    state["money"] += amount
    state["lifetime"] += amount
func spend(amount:float) -> bool:
    if val("money") < amount: return false
    state["money"] -= amount
    return true
func tap() -> void:
    earn(tap_value())
    state["totalTaps"] += 1
func upgrade_tap() -> bool:
    if not spend(10.0*pow(1.6,val("tapLevel")-1.0)): return false
    state["tapLevel"] += 1
    return true
func hire() -> bool:
    if not spend(100.0*pow(1.22,val("workers"))): return false
    state["workers"] += 1
    return true
func buy_business(i:int) -> bool:
    var price := COSTS[i]*pow(1.18,float(state["owned"][i]))
    if not spend(price): return false
    state["owned"][i] += 1
    return true
func upgrade_pick() -> bool:
    if not spend(30.0*pow(1.75,val("pick")-1.0)): return false
    state["pick"] += 1
    return true
func upgrade_drill() -> bool:
    if not spend(250.0*pow(1.8,val("drill")-1.0)): return false
    state["drill"] += 1
    return true
func dig() -> void:
    var power := val("pick")*val("pick")*(1.0+val("drill")*.15)*(1.0+val("soulPower")*.1)
    var need := 10.0*pow(1.095,minf(val("depth"),2500.0))
    var steps:int = maxi(1,mini(100,int(power/need)))
    state["depth"] += steps
    state["bestDepth"] = maxi(int(state["bestDepth"]),int(state["depth"]))
    state["ore"] += float(steps)*(1.0+val("depth")/20.0)
    if randf() < minf(.5,.05+val("depth")/5000.0): state["gems"] += 1+int(val("depth")/250.0)
    var floors := [1,30,100,250,500,1000,2000,5000]
    for i in range(8):
        if val("depth") >= floors[i] and randf() < .004/(1.0+i*.45): state["relics"][i] += 1
func sell_ore() -> void:
    earn(val("ore")*5.0)
    state["ore"]=0.0
func prestige_gain() -> int: return int(floor(sqrt(val("lifetime")/100000.0)))
func rebirth() -> bool:
    var gain := prestige_gain()
    if gain < 1: return false
    state["souls"] += gain
    state["prestige"] += 1
    state["money"]=0.0
    state["lifetime"]=0.0
    state["tapLevel"]=1
    state["workers"]=0
    state["owned"]=[0,0,0,0,0,0,0,0,0,0,0]
    save_game()
    return true
func soul_upgrade() -> bool:
    var cost := val("soulPower")+1.0
    if val("souls") < cost: return false
    state["souls"] -= cost
    state["soulPower"] += 1
    return true
func tick(delta:float) -> void:
    earn(income()*delta)
    if val("drill")>1.0:
        for i in range(mini(20,int((val("drill")-1.0)*delta))): dig()
func save_game() -> void:
    state["savedAt"]=int(Time.get_unix_time_from_system())
    var f := FileAccess.open(SAVE_PATH,FileAccess.WRITE)
    if f: f.store_string(JSON.stringify(state))
func load_game() -> void:
    if not FileAccess.file_exists(SAVE_PATH): return
    var f := FileAccess.open(SAVE_PATH,FileAccess.READ)
    if f == null: return
    var parsed = JSON.parse_string(f.get_as_text())
    if not (parsed is Dictionary): return
    for key in state.keys():
        if parsed.has(key): state[key]=parsed[key]
    var offline := clampf(float(Time.get_unix_time_from_system())-val("savedAt"),0.0,28800.0)
    earn(income()*offline)
func money_text(v:float) -> String:
    if v<1000.0:return str(int(v))
    var units := ["","만","억","조","경","해","자"]
    var i:=0
    while v>=10000.0 and i<units.size()-1:
        v/=10000.0
        i+=1
    return "%.2f%s" % [v,units[i]]
