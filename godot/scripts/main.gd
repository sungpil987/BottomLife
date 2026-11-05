extends Control
const EconomyScript = preload("res://scripts/economy.gd")
var game = EconomyScript.new()
var page := 0
var cash:Label
var info:Label
var panel:VBoxContainer
var hero:Control
var tick_accum := 0.0
var refresh_accum := 0.0
var floating := 0.0
var dialog:ConfirmationDialog
const GOLD=Color("#f9d27a")
const INK=Color("#101a2c")
const SURFACE=Color("#22354d")
const CREAM=Color("#f2ecdf")

func _ready() -> void:
    set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    _make_ui()
    _render()
func _notification(what:int) -> void:
    if what == NOTIFICATION_APPLICATION_PAUSED or what == NOTIFICATION_WM_CLOSE_REQUEST:
        game.save_game()
func _exit_tree() -> void: game.save_game()
func _process(delta:float) -> void:
    tick_accum += delta
    refresh_accum += delta
    floating=maxf(0.0,floating-delta)
    if tick_accum >= 1.0:
        game.tick(tick_accum)
        tick_accum=0.0
    if refresh_accum >= 1.0:
        refresh_accum=0.0
        _update_stats()
        game.save_game()
    queue_redraw()
func _make_ui() -> void:
    var bg:=ColorRect.new()
    bg.color=INK
    bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    add_child(bg)
    var main:=VBoxContainer.new()
    main.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    main.add_theme_constant_override("separation",8)
    add_child(main)
    var heading:=Label.new()
    heading.text="✦ BOTTOM LIFE : REBORN ✦"
    heading.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
    heading.add_theme_color_override("font_color",GOLD)
    heading.add_theme_font_size_override("font_size",24)
    main.add_child(heading)
    cash=Label.new()
    cash.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
    cash.add_theme_font_size_override("font_size",19)
    cash.add_theme_color_override("font_color",CREAM)
    main.add_child(cash)
    info=Label.new()
    info.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
    info.add_theme_color_override("font_color",Color("#8fdfc2"))
    main.add_child(info)
    hero=Control.new()
    hero.custom_minimum_size=Vector2(0,350)
    hero.size_flags_vertical=Control.SIZE_EXPAND_FILL
    hero.draw.connect(_draw_world)
    hero.gui_input.connect(_tap_world)
    main.add_child(hero)
    var scroll:=ScrollContainer.new()
    scroll.size_flags_vertical=Control.SIZE_EXPAND_FILL
    scroll.custom_minimum_size=Vector2(0,200)
    main.add_child(scroll)
    panel=VBoxContainer.new()
    panel.size_flags_horizontal=Control.SIZE_EXPAND_FILL
    panel.add_theme_constant_override("separation",8)
    scroll.add_child(panel)
    var nav:=HBoxContainer.new()
    main.add_child(nav)
    for i in range(4):
        var index:=i
        var btn:=Button.new()
        btn.text=["거리","사업","광산","환생"][i]
        btn.size_flags_horizontal=Control.SIZE_EXPAND_FILL
        btn.custom_minimum_size=Vector2(0,65)
        btn.pressed.connect(func(): page=index;_render())
        nav.add_child(btn)
    dialog=ConfirmationDialog.new()
    dialog.title="환생"
    dialog.dialog_text="돈과 사업은 초기화돼. 광산과 영구 강화는 남아."
    dialog.confirmed.connect(func(): game.rebirth();_render())
    add_child(dialog)
func _update_stats() -> void:
    cash.text="₩ %s    ·    초당 %s" % [game.money_text(game.val("money")),game.money_text(game.income())]
    info.text="◆ 보석 %s    ✦ 영혼 %s    ⛏ %s층" % [game.money_text(game.val("gems")),game.money_text(game.val("souls")),game.money_text(game.val("depth"))]
func _render() -> void:
    _update_stats()
    for child in panel.get_children():child.queue_free()
    if page==0:
        _heading("거리에서 재벌까지")
        _button("터치 강화 Lv.%d" % int(game.val("tapLevel")),"수익 +%s  |  비용 %s" % [game.money_text(game.tap_value()),game.money_text(10.0*pow(1.6,game.val("tapLevel")-1.0))],func():game.upgrade_tap())
        _button("도우미 %d명" % int(game.val("workers")),"고용 비용 %s" % game.money_text(100.0*pow(1.22,game.val("workers"))),func():game.hire())
        _heading("캐릭터를 터치해서 돈을 벌어!")
    elif page==1:
        _heading("사업 제국")
        for i in range(11):
            var j:=i
            _button(EconomyScript.BUSINESS_NAMES[i]+"  Lv.%d" % int(game.state["owned"][i]),"초당 %s  ·  구매 %s" % [game.money_text(EconomyScript.YIELDS[i]*game.mul()),game.money_text(EconomyScript.COSTS[i]*pow(1.18,float(game.state["owned"][i])))],func():game.buy_business(j))
    elif page==2:
        _heading("지하 %d층  ·  광석 %s개" % [int(game.val("depth")),game.money_text(game.val("ore"))])
        _button("⛏ 땅 파기","터치로 지하 탐험",func():game.dig())
        _button("광석 판매","%s원 획득" % game.money_text(game.val("ore")*5.0),func():game.sell_ore())
        _button("곡괭이 Lv.%d" % int(game.val("pick")),"강화 %s원" % game.money_text(30.0*pow(1.75,game.val("pick")-1.0)),func():game.upgrade_pick())
        _button("자동 드릴 Lv.%d" % int(game.val("drill")),"강화 %s원" % game.money_text(250.0*pow(1.8,game.val("drill")-1.0)),func():game.upgrade_drill())
        _heading("발견한 유물")
        for i in range(8):
            _heading(["녹슨 반지","고대 동전","푸른 수정","황금 해골","별빛 조각","심연의 나침반","왕의 인장","영원의 심장"][i]+"  ×"+str(game.state["relics"][i]))
    else:
        _heading("환생 %d회 · 영구 성장" % int(game.val("prestige")))
        _button("환생하기","획득 영혼 %d개" % game.prestige_gain(),func():dialog.popup_centered())
        _button("영혼 강화 Lv.%d" % int(game.val("soulPower")),"비용 %d 영혼 · 수익 +25%%" % (int(game.val("soulPower"))+1),func():game.soul_upgrade())
        _heading("최고 광산 %d층" % int(game.val("bestDepth")))
    hero.queue_redraw()
func _heading(s:String) -> void:
    var label:=Label.new()
    label.text="  "+s
    label.add_theme_color_override("font_color",GOLD)
    label.add_theme_font_size_override("font_size",18)
    panel.add_child(label)
func _button(title:String,description:String,action:Callable) -> void:
    var b:=Button.new()
    b.text=title+"\n"+description
    b.custom_minimum_size=Vector2(0,78)
    b.add_theme_font_size_override("font_size",17)
    b.pressed.connect(func():action.call();game.save_game();_render())
    panel.add_child(b)
func _tap_world(event:InputEvent) -> void:
    if event is InputEventMouseButton and event.pressed:
        if page==0:
            game.tap()
            floating=.6
        elif page==2:
            game.dig()
            floating=.6
        _update_stats()
        hero.queue_redraw()
func _rect(x:float,y:float,w:float,h:float,color:Color) -> void:
    hero.draw_rect(Rect2(x,y,w,h),color)
func _draw_world() -> void:
    var size=hero.size
    var scale=minf(size.x/270.0,size.y/240.0)
    hero.draw_set_transform(Vector2((size.x-270.0*scale)/2.0,(size.y-240.0*scale)/2.0),0.0,Vector2(scale,scale))
    var mine=page==2
    var sky=Color("#1c2c47") if mine else Color("#e89d76")
    _rect(0,0,270,240,sky)
    if mine:
        _rect(0,0,270,240,Color("#18263b"))
        for i in range(75):
            var x=(i*53+11)%267
            var y=(i*79+23)%238
            _rect(x,y,4+i%6,2+i%3,Color("#31455d"))
        for i in range(17):
            var x=(i*47+9)%250+5
            var y=(i*73+35)%205+10
            var gem=Color("#64d9ff") if i%2==0 else Color("#d38bff")
            _rect(x,y,8,17,gem)
            _rect(x-4,y+5,16,6,gem.lightened(.3))
            _rect(x+2,y+2,3,4,Color.WHITE)
    else:
        _rect(0,0,270,125,Color("#d78880"))
        _rect(205,25,25,25,Color("#ffe0a0"))
        for i in range(13):
            var x=i*23
            var h=35+(i*17)%55
            _rect(x,150-h,21,h,Color("#39455c"))
            for j in range(3):_rect(x+5,155-h+j*11,5,5,Color("#e7bd84"))
        _rect(0,151,270,89,Color("#293747"))
        _rect(0,161,270,3,Color("#536078"))
    var tier=mini(4,int(game.val("prestige")) if game.val("prestige")>0 else (3 if game.val("money")>1000000000 else (2 if game.val("money")>1000000 else (1 if game.val("money")>10000 else 0))))
    var coats=[Color("#907263"),Color("#3f897a"),Color("#35618b"),Color("#253d59"),Color("#744ba8")]
    var bob=sin(Time.get_ticks_msec()/240.0)*2.0
    _rect(113,171+bob,16,38,Color("#222b40"))
    _rect(141,171+bob,16,38,Color("#222b40"))
    _rect(106,116+bob,58,63,coats[tier])
    _rect(96,123+bob,12,46,coats[tier].darkened(.2))
    _rect(164,123+bob,12,46,coats[tier].darkened(.2))
    _rect(107,160+bob,9,11,Color("#e9b58a"))
    _rect(164,160+bob,9,11,Color("#e9b58a"))
    _rect(113,82+bob,44,39,Color("#e9b58a"))
    _rect(109,75+bob,52,16,Color("#23293d"))
    _rect(115,66+bob,39,14,Color("#23293d"))
    _rect(122,101+bob,5,4,Color("#263044"))
    _rect(145,101+bob,5,4,Color("#263044"))
    if mine:
        _rect(108,74+bob,55,10,Color("#f2c56a"))
        _rect(150,72+bob,10,9,Color("#fff3b3"))
        _rect(176,125+bob,54,6,Color("#b9cad8"))
        _rect(218,111+bob,6,32,Color("#b9cad8"))
    if tier>=2:_rect(131,122+bob,6,35,Color("#f3dba8"))
    if floating>0.0:
        for i in range(9):
            var angle=i*TAU/9.0
            var r=(.6-floating)*100.0+12.0
            _rect(134+cos(angle)*r,120+sin(angle)*r,5,5,GOLD)
    hero.draw_set_transform(Vector2.ZERO)
