package com.bottomlife.game;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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
  HeroView hero;
  TextView stats, notice;
  android.content.SharedPreferences prefs;
  Handler handler=new Handler();
  Random random=new Random();
  double money=0, lifetime=0, gems=0, ore=0, souls=0;
  int tapLevel=1, workers=0, business=0, drill=1, depth=1, prestige=0, soulPower=0, pick=1;
  int tab=0;
  long lastSave=0;
  String[] tabs={"거리","사업","광산","환생"};
  int totalTaps=0, bestDepth=1;
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
    LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(22,18,22,18);card.setBackground(shape(0xff23334b,24));card.setElevation(3);
    TextView a=label(title,17,WHITE);a.setTypeface(null,Typeface.BOLD);card.addView(a);
    if(subtitle!=null){TextView b=label(subtitle,13,0xffb5c5d8);b.setPadding(0,6,0,0);card.addView(b);}
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,12);box.addView(card,lp);
    card.setOnClickListener(v->{action.run();save();render();});
  }
  void build(){
    root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(BG);setContentView(root);
    header=new LinearLayout(this);header.setOrientation(1);header.setPadding(20,18,20,12);root.addView(header);
    TextView title=label("밑바닥 인생  ·  BOTTOM LIFE",21,GOLD);title.setTypeface(null,Typeface.BOLD);title.setLetterSpacing(.09f);header.addView(title);
    stats=label("",15,WHITE);stats.setPadding(0,14,0,10);header.addView(stats);
    notice=label("터치로 시작해 도시를 소유하자",12,GREEN);header.addView(notice);
    ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    body=new LinearLayout(this);body.setOrientation(1);body.setPadding(18,16,18,30);scroll.addView(body);
    LinearLayout nav=new LinearLayout(this);nav.setPadding(4,8,4,12);nav.setBackgroundColor(0xff172437);root.addView(nav);
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
    addText(body,"성장 단계  ·  "+rank(),15,GREEN);
    addText(body,"거리에서 시작하는 인생",23,GOLD);
    hero=new HeroView(this);body.addView(hero,new LinearLayout.LayoutParams(-1,410));
    button(body,"터치 수익  +"+fmt(tapIncome())+"원","위 캐릭터를 터치해서 돈을 벌어봐 · 누적 "+fmt(lifetime)+"원",()->{earn(tapIncome());totalTaps++;notice.setText("+"+fmt(tapIncome())+"원 획득!");});
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
    body.addView(new MineView(this),new LinearLayout.LayoutParams(-1,310));
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
    depth+=steps;bestDepth=Math.max(bestDepth,depth);ore+=steps*(1+depth/20.0);
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
    addText(body,"도전 기록",22,GOLD);
    addText(body,"총 터치 "+totalTaps+"회  ·  최고 광산 "+bestDepth+"층  ·  환생 "+prestige+"회",15,WHITE);
    achievement("첫걸음",totalTaps>=100,"터치 100회");
    achievement("사업가",owned[3]>0,"편의점 보유");
    achievement("지하 탐험가",bestDepth>=100,"광산 100층 도달");
    achievement("심연의 개척자",bestDepth>=1000,"광산 1,000층 도달");
    achievement("새로운 삶",prestige>=1,"첫 환생");
    achievement("환생의 달인",prestige>=10,"환생 10회");
    addText(body,"환생을 반복해 더 큰 사업을 사고, 더 깊은 광산을 개척해.",14,WHITE);
  }
  String rank(){if(prestige>=10)return "전설의 환생자";if(prestige>=1)return "다시 태어난 자";if(money>=1e9)return "재벌 회장";if(money>=1e6)return "사업가";if(money>=10000)return "자영업자";return "거리의 도전자";}
  void achievement(String name,boolean done,String goal){addText(body,(done?"[달성] ":"[진행] ")+name+"  ·  "+goal,14,done?GREEN:0xffaebbd0);}
  class MineView extends View {
    Paint p=new Paint(3);
    MineView(Context ctx){super(ctx);setOnClickListener(v->{dig();save();render();});}
    void box(Canvas c,int color,float l,float t,float r,float b){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(l,t,r,b,p);}
    void gem(Canvas c,float x,float y,float size,int color){
      android.graphics.Path path=new android.graphics.Path();path.moveTo(x,y-size);path.lineTo(x+size*.72f,y-size*.2f);path.lineTo(x+size*.4f,y+size);path.lineTo(x-size*.45f,y+size*.8f);path.lineTo(x-size*.8f,y-size*.2f);path.close();
      p.setColor(color);p.setStyle(Paint.Style.FILL);p.setShadowLayer(14,0,0,color);c.drawPath(path,p);p.clearShadowLayer();
      p.setColor(0x99ffffff);p.setStrokeWidth(2);c.drawLine(x,y-size,x,y+size*.7f,p);
    }
    protected void onDraw(Canvas canvas){
      float sx=getWidth()/340f,sy=getHeight()/310f;canvas.save();canvas.scale(sx,sy);
      p.setShader(new android.graphics.LinearGradient(0,0,0,310,0xff26374e,0xff080e20,android.graphics.Shader.TileMode.CLAMP));
      canvas.drawRoundRect(new RectF(0,0,340,310),22,22,p);p.setShader(null);
      for(int i=0;i<24;i++){int x=(i*73+19)%340,y=(i*97+31)%310;box(canvas,0xff31445d,x,y,x+17,y+9);}
      int[] colors={0xff40d9ff,0xff8a77ff,0xffffd76e,0xfff06ec5};
      for(int i=0;i<15;i++){float x=24+(i*67)%300,y=45+(i*79)%230;gem(canvas,x,y,9+(i%3)*5,colors[(i+depth/100)%4]);}
      // helmet and headlamp
      p.setColor(0xffb6c7d8);canvas.drawCircle(166,153,24,p);
      p.setColor(0xffe9b887);canvas.drawCircle(166,158,19,p);
      p.setColor(0xffe6ac45);canvas.drawArc(new RectF(143,132,190,156),180,180,false,p);
      p.setColor(0xfffff4ad);p.setShadowLayer(15,0,0,0xffffe399);canvas.drawCircle(183,140,7,p);p.clearShadowLayer();
      p.setColor(0xff5e8498);canvas.drawRoundRect(new RectF(142,182,191,240),12,12,p);
      p.setColor(0xff303d53);canvas.drawRoundRect(new RectF(145,234,160,263),5,5,p);canvas.drawRoundRect(new RectF(175,234,190,263),5,5,p);
      p.setColor(0xffc0cbd5);p.setStrokeWidth(9);p.setStrokeCap(Paint.Cap.ROUND);canvas.drawLine(192,198,225,169,p);canvas.drawLine(216,161,246,184,p);
      p.setColor(0xffffd166);p.setTextSize(18);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);canvas.drawText("UNDERGROUND  ·  "+depth+"F",170,30,p);
      p.setColor(0xffdcecff);p.setTextSize(14);canvas.drawText("터치해서 굴착",170,290,p);
      canvas.restore();
    }
  }
  Runnable ticker=new Runnable(){public void run(){
    long now=System.currentTimeMillis();double sec=Math.min(5,(now-lastTick)/1000.0);lastTick=now;
    if(sec>0){earn(passive()*sec);if(tab==2 && drill>1){int digs=(int)Math.min(20,Math.floor((drill-1)*sec));for(int i=0;i<digs;i++)dig();}}
    if(now-lastSave>1000){stats.setText("보유 "+fmt(money)+"원   |   초당 "+fmt(passive())+"원\n보석 "+fmt(gems)+"개   |   영혼 "+fmt(souls)+"개");lastSave=now;}
    if(now%5000<1100)save();
    handler.postDelayed(this,1000);
  }};
  class HeroView extends View {
    Paint p=new Paint(3);float pulse=0;
    HeroView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setOnClickListener(v->{earn(tapIncome());totalTaps++;pulse=1;notice.setText("+"+fmt(tapIncome())+"원");stats.setText("보유 "+fmt(money)+"원   |   초당 "+fmt(passive())+"원\n보석 "+fmt(gems)+"개   |   영혼 "+fmt(souls)+"개");invalidate();save();});}
    void rect(Canvas c,int color,float l,float t,float r,float b,float rad){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(l,t,r,b),rad,rad,p);}
    void circ(Canvas c,int color,float x,float y,float radius){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,radius,p);}
    void line(Canvas c,int color,float x,float y,float xx,float yy,float w){p.setColor(color);p.setStrokeWidth(w);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);}
    void txt(Canvas c,String s,int color,float x,float y,int size){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}
    protected void onDraw(Canvas original){
      super.onDraw(original);Canvas c=original;float sx=getWidth()/340f,sy=getHeight()/410f;c.save();c.scale(sx,sy);
      rect(c,0xff18273c,0,0,340,410,26);
      rect(c,0xff273950,12,12,328,398,22);
      p.setShader(new android.graphics.LinearGradient(0,0,0,410,0xff41516b,0xff142234,android.graphics.Shader.TileMode.CLAMP));c.drawRoundRect(new RectF(18,18,322,392),18,18,p);p.setShader(null);
      circ(c,0x55ffca7a,260,72,42);circ(c,0x33ffffff,265,70,65);
      rect(c,0xff1a2937,24,270,316,370,8);
      for(int i=0;i<10;i++){float x=30+i*31;rect(c,0xff32455a,x,220-(i%3)*24,x+24,302,2);rect(c,0xffe6b65b,x+7,236-(i%3)*24,x+12,243-(i%3)*24,1);}
      rect(c,0xff45515d,68,318,270,342,8);
      // legs and worn shoes
      line(c,0xff292c3a,155,271,134,317,22);line(c,0xff292c3a,185,271,211,317,22);
      rect(c,0xff141b27,105,313,148,326,7);rect(c,0xff141b27,201,313,243,326,7);
      // jacket and sleeves
      rect(c,0xff8b6955,119,190,220,284,26);
      line(c,0xff695449,126,215,94,275,24);line(c,0xff695449,210,215,245,273,24);
      line(c,0xffcfad88,96,272,87,289,14);line(c,0xffcfad88,244,270,250,290,14);
      // neck, face, ears
      rect(c,0xffd9a67d,156,178,187,208,8);circ(c,0xffe6b18a,170,143,48);circ(c,0xffe6b18a,124,151,10);circ(c,0xffe6b18a,215,151,10);
      // hair
      circ(c,0xff222333,136,110,27);circ(c,0xff222333,165,96,34);circ(c,0xff222333,196,108,28);
      rect(c,0xff222333,128,103,210,121,9);
      line(c,0xff232632,148,145,158,145,3);line(c,0xff232632,185,145,195,145,3);
      circ(c,0xff232632,155,146,2);circ(c,0xff232632,190,146,2);
      line(c,0xffa76e62,164,168,182,168,2);
      // cup
      rect(c,0xffaabac6,205,271,253,300,7);rect(c,0xffe6c878,207,269,250,277,4);
      txt(c,prestige>0?"환생자 · "+prestige+"회":depth>100?"광산 개척자":"거리의 도전자",0xffffdc89,170,48,17);
      txt(c,"TAP TO EARN",0xfff7f0de,170,378,17);
      if(pulse>0){txt(c,"+"+fmt(tapIncome()),0xffffd166,170,82,25);pulse=0;}
      c.restore();
    }
  }
  void save(){
    try{JSONObject j=new JSONObject();j.put("money",money);j.put("lifetime",lifetime);j.put("gems",gems);j.put("ore",ore);j.put("souls",souls);j.put("tapLevel",tapLevel);j.put("workers",workers);j.put("drill",drill);j.put("depth",depth);j.put("prestige",prestige);j.put("soulPower",soulPower);j.put("pick",pick);j.put("totalTaps",totalTaps);j.put("bestDepth",bestDepth);
      org.json.JSONArray a=new org.json.JSONArray();for(int v:owned)a.put(v);j.put("owned",a);prefs.edit().putString("state",j.toString()).putLong("savedAt",System.currentTimeMillis()).apply();
    }catch(Exception ignored){}
  }
  void load(){
    try{JSONObject j=new JSONObject(prefs.getString("state","{}"));money=j.optDouble("money",0);lifetime=j.optDouble("lifetime",0);gems=j.optDouble("gems",0);ore=j.optDouble("ore",0);souls=j.optDouble("souls",0);tapLevel=j.optInt("tapLevel",1);workers=j.optInt("workers",0);drill=j.optInt("drill",1);depth=j.optInt("depth",1);prestige=j.optInt("prestige",0);soulPower=j.optInt("soulPower",0);pick=j.optInt("pick",1);totalTaps=j.optInt("totalTaps",0);bestDepth=j.optInt("bestDepth",depth);
      org.json.JSONArray a=j.optJSONArray("owned");if(a!=null)for(int i=0;i<Math.min(a.length(),owned.length);i++)owned[i]=a.optInt(i);
      long elapsed=Math.max(0,Math.min(8*3600,(System.currentTimeMillis()-prefs.getLong("savedAt",System.currentTimeMillis()))/1000));earn(passive()*elapsed);
    }catch(Exception ignored){}
  }
  protected void onPause(){super.onPause();save();}
  protected void onDestroy(){handler.removeCallbacks(ticker);save();super.onDestroy();}
}
