package com.bottomlife.game;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
  final int BG=0xff101827, PANEL=0xff202d40, GOLD=0xffffd166, WHITE=0xfff4f6fc, GREEN=0xff64e6b0;
  LinearLayout root, body, header;
  TextView stats, notice;
  android.content.SharedPreferences prefs;
  Handler handler=new Handler();
  Random random=new Random();
  double money=0, lifetime=0, gems=0, ore=0, souls=0;
  int tapLevel=1, workers=0, business=0, drill=1, depth=1, prestige=0, soulPower=0, pick=1;
  int tab=0;
  long lastSave=0;
  String[] tabs={"거리","사업","광산","환생"};
  String[] businesses={"종이컵 구걸","폐지 수집소","붕어빵 노점","편의점","분식집","카페","프랜차이즈","공장","IT 기업","대기업","우주 산업"};
  double[] costs={50,500,5000,50000,500000,5000000,50000000,500000000,5e9,5e10,5e11};
  double[] yields={0.1,1,10,100,1000,10000,100000,1e6,1e7,1e8,1e9};
  int[] owned=new int[businesses.length];
  long lastTick=System.currentTimeMillis();

  public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);prefs=getSharedPreferences("save_v1",0);load();build();handler.post(ticker);}
  GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
  TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
  void addText(LinearLayout box,String s,int size,int color){TextView t=label(s,size,color);t.setPadding(10,12,10,12);box.addView(t);}
  void button(LinearLayout box,String title,String subtitle,Runnable action){
    LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(22,18,22,18);card.setBackground(shape(PANEL,20));
    TextView a=label(title,17,WHITE);a.setTypeface(null,Typeface.BOLD);card.addView(a);
    if(subtitle!=null){TextView b=label(subtitle,13,0xffb5c5d8);b.setPadding(0,6,0,0);card.addView(b);}
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,12);box.addView(card,lp);
    card.setOnClickListener(v->{action.run();save();render();});
  }
  void build(){
    root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(BG);setContentView(root);
    header=new LinearLayout(this);header.setOrientation(1);header.setPadding(20,18,20,12);root.addView(header);
    TextView title=label("밑바닥 인생  ·  BOTTOM LIFE",21,GOLD);title.setTypeface(null,Typeface.BOLD);header.addView(title);
    stats=label("",15,WHITE);stats.setPadding(0,10,0,6);header.addView(stats);
    notice=label("터치로 시작해 도시를 소유하자",12,GREEN);header.addView(notice);
    ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    body=new LinearLayout(this);body.setOrientation(1);body.setPadding(18,16,18,30);scroll.addView(body);
    LinearLayout nav=new LinearLayout(this);nav.setPadding(4,8,4,12);nav.setBackgroundColor(PANEL);root.addView(nav);
    for(int i=0;i<tabs.length;i++){final int n=i;TextView t=label(tabs[i],16,WHITE);t.setGravity(Gravity.CENTER);t.setTypeface(null,Typeface.BOLD);nav.addView(t,new LinearLayout.LayoutParams(0,52,1));t.setOnClickListener(v->{tab=n;render();scroll.scrollTo(0,0);});}
    render();
  }
  String fmt(double n){if(n<1000)return String.format(Locale.KOREA,"%,.0f",n);String[] u={"","천","만","억","조","경","해","자"};int k=0;while(n>=10000&&k<u.length-1){n/=10000;k++;}return String.format(Locale.KOREA,"%.2f%s",n,u[k]);}
  double multi(){return (1+soulPower*.25)*(1+prestige*.05);}
  double tapIncome(){return Math.pow(1.65,tapLevel-1)*multi();}
  double passive(){double p=workers*0.5;for(int i=0;i<owned.length;i++)p+=owned[i]*yields[i];return p*multi();}
  double tapCost(){return 10*Math.pow(1.6,tapLevel-1);}
  double workerCost(){return 100*Math.pow(1.22,workers);}
  double businessCost(int i){return costs[i]*Math.pow(1.18,owned[i]);}
  double pickCost(){return 30*Math.pow(1.75,pick-1);}
  double drillCost(){return 250*Math.pow(1.8,drill-1);}
  double digDamage(){return (pick*pick)*(1+drill*.15)*(1+soulPower*.1);}
  double digNeed(){return 10*Math.pow(1.095,Math.min(depth,2500));}
  void earn(double n){money+=n;lifetime+=n;}
  boolean spend(double n){if(money+1e-7<n){notice.setText("돈이 부족해!");return false;}money-=n;return true;}
  void render(){
    stats.setText("보유 "+fmt(money)+"원   |   초당 "+fmt(passive())+"원\n보석 "+fmt(gems)+"개   |   영혼 "+fmt(souls)+"개");
    body.removeAllViews();
    if(tab==0)street();else if(tab==1)business();else if(tab==2)mine();else rebirth();
  }
  void street(){
    addText(body,"거리에서 시작하는 인생",23,GOLD);
    addText(body,"     (•‿•)\n    /|   |\\\n     /   \\ \n\n빈털터리에서 재벌까지!",25,WHITE);
    button(body,"터치해서 돈 벌기  +"+fmt(tapIncome())+"원","무한 터치 가능 · 누적 수익 "+fmt(lifetime)+"원",()->{earn(tapIncome());notice.setText("+"+fmt(tapIncome())+"원 획득!");});
    button(body,"터치 강화 Lv."+tapLevel,"비용 "+fmt(tapCost())+"원 · 터치 수익 증가",()->{if(spend(tapCost()))tapLevel++;});
    button(body,"도우미 고용 "+workers+"명","비용 "+fmt(workerCost())+"원 · 초당 0.5원씩 추가",()->{if(spend(workerCost()))workers++;});
    button(body,"오늘의 행운","무료 보너스 · 30초에 한 번",()->{
      long now=System.currentTimeMillis(),last=prefs.getLong("luck",0);
      if(now-last<30000){notice.setText("행운은 "+((30000-(now-last))/1000)+"초 후 가능");return;}
      prefs.edit().putLong("luck",now).apply();
      double reward=Math.max(20,passive()*20+tapIncome()*10)*(1+random.nextInt(5));
      earn(reward);notice.setText("행운! "+fmt(reward)+"원 획득");
    });
    addText(body,"목표: 사업을 키우고, 광산을 파고, 환생해서 영구적으로 강해지자.",14,0xffb5c5d8);
  }
  void business(){
    addText(body,"사업 제국",23,GOLD);addText(body,"사업은 초당 자동 수익을 생산해. 많이 사면 구매 비용이 올라가.",14,WHITE);
    for(int i=0;i<businesses.length;i++){final int j=i;
      button(body,businesses[i]+"  ×"+owned[i],"구매 "+fmt(businessCost(i))+"원  |  개당 초당 "+fmt(yields[i]*multi())+"원",()->{if(spend(businessCost(j)))owned[j]++;});
    }
  }
  void mine(){
    addText(body,"뒷산의 무한 광산",23,GOLD);
    addText(body,"현재 지하 "+depth+"층 · 굴착력 "+fmt(digDamage())+"\n광석 "+fmt(ore)+"개 · 보석 "+fmt(gems)+"개",16,WHITE);
    button(body,"땅 파기  ⛏","굴착력 "+fmt(digDamage())+" / 필요 "+fmt(digNeed())+" · 층이 깊어질수록 보상 증가",()->dig());
    button(body,"광석 판매","광석 "+fmt(ore)+"개 → "+fmt(ore*5)+"원",()->{earn(ore*5);ore=0;notice.setText("광석 판매 완료!");});
    button(body,"곡괭이 강화 Lv."+pick,"비용 "+fmt(pickCost())+"원 · 굴착력 크게 증가",()->{if(spend(pickCost()))pick++;});
    button(body,"자동 드릴 Lv."+drill,"비용 "+fmt(drillCost())+"원 · 초당 자동 굴착",()->{if(spend(drillCost()))drill++;});
    button(body,"보석 감정","보석 10개를 5000원으로 교환",()->{if(gems>=10){gems-=10;earn(5000);notice.setText("보석 감정 성공");}else notice.setText("보석이 10개 필요해");});
    addText(body,"희귀 보석은 층이 깊어질수록 더 자주 발견돼. 드릴은 게임 실행 중 자동으로 광산을 파.",13,0xffb5c5d8);
  }
  void dig(){
    double damage=digDamage(),need=digNeed();
    int steps=Math.max(1,(int)Math.min(100,damage/need));
    depth+=steps;ore+=steps*(1+depth/20.0);
    if(random.nextDouble()<Math.min(.5,.05+depth/5000.0)){gems+=1+depth/250;notice.setText("희귀 보석 발견! 지하 "+depth+"층");}
    else notice.setText("지하 "+depth+"층 도달!");
  }
  double soulGain(){return Math.floor(Math.sqrt(lifetime/100000.0));}
  void rebirth(){
    addText(body,"환생의 제단",23,GOLD);
    addText(body,"환생 횟수 "+prestige+"회\n영혼 포인트 "+fmt(souls)+"개\n영구 강화 "+soulPower+"단계",17,WHITE);
    addText(body,"이번 환생에서 얻을 영혼: "+fmt(soulGain())+"개\n최소 누적 수익 10만원부터 환생 가능",15,GREEN);
    button(body,"환생하기","돈 · 사업 · 직원 · 터치 강화 초기화 / 광산 깊이·보석·영구 강화 유지",()->{
      if(soulGain()<1){notice.setText("누적 수익 10만원이 필요해");return;}
      new AlertDialog.Builder(this).setTitle("정말 환생할까?").setMessage("돈과 사업은 초기화돼. 영혼 포인트와 광산 진행도는 유지돼.")
        .setNegativeButton("취소",null).setPositiveButton("환생", (d,w)->{souls+=soulGain();prestige++;money=0;lifetime=0;tapLevel=1;workers=0;for(int i=0;i<owned.length;i++)owned[i]=0;notice.setText("환생 완료! 영구 성장 시작");save();render();}).show();
    });
    button(body,"영혼 강화 Lv."+soulPower,"영혼 "+(soulPower+1)+"개 소모 · 모든 돈 수익 영구 +25%",()->{if(souls>=soulPower+1){souls-=soulPower+1;soulPower++;}else notice.setText("영혼 포인트 부족");});
    addText(body,"환생을 반복해 더 큰 사업을 사고, 더 깊은 광산을 개척해. 목표는 끝없이 확장돼.",14,WHITE);
  }
  Runnable ticker=new Runnable(){public void run(){
    long now=System.currentTimeMillis();double sec=Math.min(5,(now-lastTick)/1000.0);lastTick=now;
    if(sec>0){earn(passive()*sec);if(tab==2 && drill>1){int digs=(int)Math.min(20,Math.floor((drill-1)*sec));for(int i=0;i<digs;i++)dig();}}
    if(now-lastSave>1000){stats.setText("보유 "+fmt(money)+"원   |   초당 "+fmt(passive())+"원\n보석 "+fmt(gems)+"개   |   영혼 "+fmt(souls)+"개");lastSave=now;}
    if(now%5000<1100)save();
    handler.postDelayed(this,1000);
  }};
  void save(){
    try{JSONObject j=new JSONObject();j.put("money",money);j.put("lifetime",lifetime);j.put("gems",gems);j.put("ore",ore);j.put("souls",souls);j.put("tapLevel",tapLevel);j.put("workers",workers);j.put("drill",drill);j.put("depth",depth);j.put("prestige",prestige);j.put("soulPower",soulPower);j.put("pick",pick);
      org.json.JSONArray a=new org.json.JSONArray();for(int v:owned)a.put(v);j.put("owned",a);prefs.edit().putString("state",j.toString()).putLong("savedAt",System.currentTimeMillis()).apply();
    }catch(Exception ignored){}
  }
  void load(){
    try{JSONObject j=new JSONObject(prefs.getString("state","{}"));money=j.optDouble("money",0);lifetime=j.optDouble("lifetime",0);gems=j.optDouble("gems",0);ore=j.optDouble("ore",0);souls=j.optDouble("souls",0);tapLevel=j.optInt("tapLevel",1);workers=j.optInt("workers",0);drill=j.optInt("drill",1);depth=j.optInt("depth",1);prestige=j.optInt("prestige",0);soulPower=j.optInt("soulPower",0);pick=j.optInt("pick",1);
      org.json.JSONArray a=j.optJSONArray("owned");if(a!=null)for(int i=0;i<Math.min(a.length(),owned.length);i++)owned[i]=a.optInt(i);
      long elapsed=Math.max(0,Math.min(8*3600,(System.currentTimeMillis()-prefs.getLong("savedAt",System.currentTimeMillis()))/1000));earn(passive()*elapsed);
    }catch(Exception ignored){}
  }
  protected void onPause(){super.onPause();save();}
  protected void onDestroy(){handler.removeCallbacks(ticker);save();super.onDestroy();}
}
