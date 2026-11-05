package com.bottomlife.game;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Bitmap;
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
  float density(){return getResources().getDisplayMetrics().density;}
  int dp(float value){return Math.round(value*density());}
  TextView stats, notice;
  TextView[] navLabels=new TextView[4];
  android.content.SharedPreferences prefs;
  Handler handler=new Handler();
  Random random=new Random();
  double money=0, lifetime=0, gems=0, ore=0, souls=0;
  int tapLevel=1, workers=0, business=0, drill=1, depth=1, prestige=0, soulPower=0, pick=1;
  int tab=0;
  long lastSave=0;
  String[] tabs={"거리","사업","광산","환생"};
  int totalTaps=0, bestDepth=1, questClaims=0;
  int[] relics=new int[8];
  String[] relicNames={"녹슨 반지","고대 동전","푸른 수정","황금 해골","별빛 조각","심연의 나침반","왕의 인장","영원의 심장"};
  int[] relicFloors={1,30,100,250,500,1000,2000,5000};
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
    TextView title=label("밑바닥 인생  ·  BOTTOM LIFE",19,GOLD);title.setTypeface(null,Typeface.BOLD);title.setLetterSpacing(.09f);header.addView(title);
    stats=label("",15,WHITE);stats.setPadding(0,14,0,10);header.addView(stats);
    notice=label("터치로 시작해 도시를 소유하자",12,GREEN);header.addView(notice);
    ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    body=new LinearLayout(this);body.setOrientation(1);body.setPadding(18,16,18,30);scroll.addView(body);
    LinearLayout nav=new LinearLayout(this);nav.setPadding(4,8,4,12);nav.setBackgroundColor(0xff172437);root.addView(nav);
    String[] symbols={"♟","▣","◆","✦"};
    for(int i=0;i<tabs.length;i++){final int n=i;TextView t=label(symbols[i]+"\n"+tabs[i],13,i==tab?GOLD:0xffb2c2d6);
      t.setGravity(Gravity.CENTER);t.setTypeface(null,Typeface.BOLD);navLabels[i]=t;
      nav.addView(t,new LinearLayout.LayoutParams(0,dp(62),1));
      t.setOnClickListener(v->{tab=n;render();scroll.scrollTo(0,0);});}
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
    for(int i=0;i<navLabels.length;i++)if(navLabels[i]!=null)navLabels[i].setTextColor(i==tab?GOLD:0xffb2c2d6);
    if(tab==0)street();else if(tab==1)business();else if(tab==2)mine();else rebirth();
  }
  void street(){
    addText(body,"성장 단계  ·  "+rank(),15,GREEN);
    addText(body,"캐릭터 외형은 보유 자산과 환생에 따라 변해.",13,0xffb5c5d8);
    addText(body,"거리에서 시작하는 인생",23,GOLD);
    hero=new HeroView(this);body.addView(hero,new LinearLayout.LayoutParams(-1,dp(350)));
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
    addText(body,"사업 제국",23,GOLD);addText(body,"초당 수익 "+fmt(passive())+"원 · 사업 총 "+businessCount()+"개",15,GREEN);
    addText(body,"사업을 늘려 자동 수익을 키우고 더 높은 등급을 해금해.",14,WHITE);
    for(int i=0;i<businesses.length;i++){final int j=i;
      String symbol=i<2?"◈":i<5?"◆":i<8?"✦":"★";
      String state=money>=businessCost(i)?"구매 가능":"목표 "+fmt(businessCost(i))+"원";
      LinearLayout item=new LinearLayout(this);item.setGravity(Gravity.CENTER_VERTICAL);
      LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,dp(106));rowParams.setMargins(0,0,0,dp(10));
      item.setPadding(dp(10),dp(8),dp(10),dp(8));item.setBackground(shape(0xff23334b,dp(16)));body.addView(item,rowParams);
      item.addView(new BusinessArt(this,j),new LinearLayout.LayoutParams(dp(92),dp(88)));
      LinearLayout info=new LinearLayout(this);info.setOrientation(1);info.setPadding(dp(12),0,0,0);
      item.addView(info,new LinearLayout.LayoutParams(0,-2,1));
      TextView name=label(businesses[i]+"  Lv."+owned[i],15,GOLD);name.setTypeface(null,Typeface.BOLD);info.addView(name);
      TextView income=label("초당 +"+fmt(yields[i]*multi())+"원",13,GREEN);income.setPadding(0,dp(4),0,dp(4));info.addView(income);
      TextView price=label("구매 "+fmt(businessCost(i))+"원",12,0xffd8e2f0);info.addView(price);
      item.setOnClickListener(v->{if(spend(businessCost(j))){owned[j]++;notice.setText(businesses[j]+" 사업 확장!");}save();render();});


    }
  }
  void mine(){
    addText(body,"뒷산의 무한 광산",23,GOLD);
    body.addView(new MineView(this),new LinearLayout.LayoutParams(-1,dp(275)));
    addText(body,"현재 지하 "+depth+"층 · 굴착력 "+fmt(digDamage())+"\n광석 "+fmt(ore)+"개 · 보석 "+fmt(gems)+"개",16,WHITE);
    button(body,"땅 파기  ⛏","굴착력 "+fmt(digDamage())+" / 필요 "+fmt(digNeed())+" · 층이 깊어질수록 보상 증가",()->dig());
    button(body,"광석 판매","광석 "+fmt(ore)+"개 → "+fmt(ore*5)+"원",()->{earn(ore*5);ore=0;notice.setText("광석 판매 완료!");});
    button(body,"곡괭이 강화 Lv."+pick,"비용 "+fmt(pickCost())+"원 · 굴착력 크게 증가",()->{if(spend(pickCost()))pick++;});
    button(body,"자동 드릴 Lv."+drill,"비용 "+fmt(drillCost())+"원 · 초당 자동 굴착",()->{if(spend(drillCost()))drill++;});
    button(body,"보석 감정","보석 10개를 5000원으로 교환",()->{if(gems>=10){gems-=10;earn(5000);notice.setText("보석 감정 성공");}else notice.setText("보석이 10개 필요해");});
    addText(body,"광산 유물 도감",22,GOLD);
    for(int i=0;i<relics.length;i++){
      String found=relics[i]>0?"발견 · "+relics[i]+" ("+relics[i]+"개)":"미발견 · "+relicFloors[i]+"층 이상";
      addText(body,(relics[i]>0?"✦  ":"◇  ")+relicNames[i]+"   "+found,13,relics[i]>0?GOLD:0xffa8b8ca);
    }
    addText(body,"깊은 층에서 희귀 유물이 발견돼. 유물은 환생해도 보존돼.",13,0xffb5c5d8);
  }
  void dig(){
    double damage=digDamage(),need=digNeed();
    int steps=Math.max(1,(int)Math.min(100,damage/need));
    depth+=steps;bestDepth=Math.max(bestDepth,depth);ore+=steps*(1+depth/20.0);
    for(int i=0;i<relics.length;i++)if(depth>=relicFloors[i] && random.nextDouble()<0.004/(1+i*.45)){relics[i]++;notice.setText("희귀 유물 발견! "+relicNames[i]);}
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
    addText(body,"일일이 아닌 영구 도전",22,GOLD);
    int next=questClaims+1;
    int target=next*100;
    button(body,"도전 "+next+" · 광산 "+target+"층 돌파",
      "최고 기록 "+bestDepth+"층 · 보상 "+fmt(1000*Math.pow(2,Math.min(questClaims,30)))+"원",
      ()->{if(bestDepth>=target){earn(1000*Math.pow(2,Math.min(questClaims,30)));questClaims++;notice.setText("도전 보상 수령!");}else notice.setText("광산 "+target+"층에 먼저 도달해!");});
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
  int businessCount(){int n=0;for(int v:owned)n+=v;return n;}
  String rank(){if(prestige>=10)return "전설의 환생자";if(prestige>=1)return "다시 태어난 자";if(money>=1e9)return "재벌 회장";if(money>=1e6)return "사업가";if(money>=10000)return "자영업자";return "거리의 도전자";}
  void achievement(String name,boolean done,String goal){addText(body,(done?"[달성] ":"[진행] ")+name+"  ·  "+goal,14,done?GREEN:0xffaebbd0);}

  // Pixel-art renderer: draw on a small logical canvas and upscale with nearest-neighbor sampling.
  abstract class PixelView extends View {
    final Paint px=new Paint();final Paint blit=new Paint();
    Bitmap frame;Canvas low;int logicalW,logicalH;
    PixelView(Context ctx,int w,int h){super(ctx);logicalW=w;logicalH=h;px.setAntiAlias(false);px.setFilterBitmap(false);blit.setAntiAlias(false);blit.setFilterBitmap(false);}
    void block(int color,int x,int y,int w,int h){px.setColor(color);low.drawRect(x,y,x+w,y+h,px);}
    void textPixel(String text,int x,int y,int color,int size){
      px.setColor(color);px.setTypeface(Typeface.create("monospace",Typeface.BOLD));px.setTextSize(size);px.setTextAlign(Paint.Align.CENTER);px.setAntiAlias(false);low.drawText(text,x,y,px);
    }
    void frame(int color){block(color,0,0,logicalW,logicalH);block(0xffd6ad66,1,1,logicalW-2,1);block(0xffd6ad66,1,logicalH-2,logicalW-2,1);}
    protected abstract void paintPixel();
    protected void onDraw(Canvas canvas){
      super.onDraw(canvas);
      if(frame==null||frame.getWidth()!=logicalW||frame.getHeight()!=logicalH){frame=Bitmap.createBitmap(logicalW,logicalH,Bitmap.Config.ARGB_8888);low=new Canvas(frame);}
      frame.eraseColor(Color.TRANSPARENT);paintPixel();
      float s=Math.min(getWidth()/(float)logicalW,getHeight()/(float)logicalH);
      float left=(getWidth()-logicalW*s)/2f,top=(getHeight()-logicalH*s)/2f;
      canvas.drawBitmap(frame,null,new RectF(left,top,left+logicalW*s,top+logicalH*s),blit);
    }
    void sparkle(int x,int y,int color){block(color,x-1,y-4,3,9);block(color,x-4,y-1,9,3);block(Color.WHITE,x,y,1,1);}
  }
  class BusinessArt extends PixelView {
    int index;
    BusinessArt(Context ctx,int i){super(ctx,64,64);index=i;}
    protected void paintPixel(){
      frame(0xff152438);
      for(int y=8;y<50;y+=9)block(0xff263e57,4+(y*7)%51,y,6,2);
      block(0xffe3bb74,47,8,8,8);block(0xfff5dba0,49,10,4,4);
      block(0xff2c3644,3,52,58,9);
      if(index<2){
        block(0xffa27d56,9,36,46,18);block(0xffe8c48a,13,40,38,9);
        for(int i=0;i<3;i++)block(0xff6c5146,15+i*12,47,7,4);
      }else if(index<6){
        block(0xff141f2f,8,25,50,30);block(0xffd9b69b,11,27,44,25);
        block(index==2?0xffdb7453:0xffa9c2ca,7,23,52,9);
        for(int x=14;x<53;x+=13){block(0xff283f57,x,36,9,14);block(0xfff1c779,x+2,38,4,5);}
        block(0xffbd8652,29,45,7,8);
      }else{
        block(0xff0d1728,9,8,47,47);block(0xff788ea5,12,11,41,42);
        for(int y=14;y<49;y+=8)for(int x=16;x<50;x+=9)block((x+y)%3==0?0xffffd48a:0xff9dc5df,x,y,5,5);
        if(index>=9){block(0xfff4c974,27,3,10,6);block(0xffffe6a8,30,1,4,3);}
      }
      block(0xffd6ad66,4,56,56,2);
    }
  }
  class MineView extends PixelView {
    MineView(Context ctx){super(ctx,160,144);setOnClickListener(v->{dig();save();render();});}
    protected void paintPixel(){
      int zone=(depth/100)%5;
      int[] caves={0xff1b3047,0xff302342,0xff143b3c,0xff3e2a2a,0xff252044};
      int[] stones={0xff38536c,0xff604c75,0xff347068,0xff77503e,0xff524576};
      frame(caves[zone]);
      for(int i=0;i<46;i++){int x=(i*41+7)%154,y=(i*59+11)%136;block(stones[zone],x,y,3+i%5,2+i%3);}
      int[] gems={0xff50d8ff,0xffaa7bff,0xffffd76b,0xff64efbb};
      for(int i=0;i<13;i++){int x=8+(i*47)%143,y=23+(i*67)%104,col=gems[(i+zone)%4];block(col,x,y,5,10);block(col,x-3,y+3,11,4);block(Color.WHITE,x+1,y+1,2,3);}
      // Pixel miner with headlamp, helmet, overalls, arms and pickaxe.
      block(0xff0a1725,65,85,34,35);block(0xffe6b18a,72,55,20,20);
      block(0xffe9b94f,68,48,28,9);block(0xfff7d877,75,45,14,4);
      block(0xfffff5bb,92,50,6,6);block(0xff4d718d,67,78,31,30);
      block(0xff9dc6d5,70,81,24,5);block(0xff26394e,70,108,11,15);block(0xff26394e,87,108,11,15);
      block(0xffb78065,60,84,8,19);block(0xffb78065,98,82,8,16);
      block(0xffc8d7df,105,78,31,4);block(0xffc8d7df,128,68,4,24);
      block(0xffe5bb78,69,120,14,5);block(0xffe5bb78,85,120,14,5);
      sparkle(27,32,0xffb5eeff);sparkle(133,43,0xffffd66f);
      textPixel("DEPTH "+depth+"F",80,13,0xffffd78e,9);
      textPixel("TAP TO MINE",80,137,0xffe3eafa,9);
    }
  }
  class HeroView extends PixelView {
    long tapTime=0;
    HeroView(Context ctx){super(ctx,160,192);setOnClickListener(v->{
      earn(tapIncome());totalTaps++;tapTime=System.currentTimeMillis();
      notice.setText("+"+fmt(tapIncome())+"원");
      stats.setText("보유 "+fmt(money)+"원   |   초당 "+fmt(passive())+"원\\n보석 "+fmt(gems)+"개   |   영혼 "+fmt(souls)+"개");
      invalidate();save();
    });}
    protected void paintPixel(){
      frame(0xff25334c);
      block(0xfff3a76e,0,0,160,70);block(0xffca7d80,0,70,160,35);block(0xff36415b,0,105,160,87);
      block(0xffffd48a,122,24,19,19);block(0xffffe6a4,126,28,11,11);
      for(int i=0;i<10;i++){int x=i*17;int h=23+(i*19)%39;block(0xff273448,x,110-h,15,h+42);
        for(int y=113-h;y<106;y+=9)block(0xfff2c37c,x+4,y,3,4);}
      block(0xff27313d,0,151,160,41);block(0xff4e4c52,0,155,160,3);
      block(0xff8a7164,16,103,36,45);block(0xffc5a98c,20,108,28,24);
      int tier=prestige>0?4:money>=1e9?3:money>=1e6?2:money>=10000?1:0;
      int[] coats={0xff887061,0xff438777,0xff365a85,0xff27374b,0xff69439b};
      block(0xff1b2233,66,132,12,22);block(0xff1b2233,86,132,12,22);
      block(coats[tier],62,93,40,42);block(0xff263044,67,133,11,24);block(0xff263044,88,133,11,24);
      block(0xff171c2b,63,153,18,7);block(0xff171c2b,87,153,18,7);
      block(coats[tier],53,98,12,30);block(coats[tier],100,98,12,30);
      block(0xffe2ac85,54,124,10,9);block(0xffe2ac85,103,124,10,9);
      block(0xffd9a37c,66,67,33,31);block(0xffd9a37c,73,91,19,10);
      block(0xff23283b,64,62,37,14);block(0xff23283b,69,56,26,9);block(0xff23283b,61,71,10,14);
      block(0xff23283b,75,80,4,3);block(0xff23283b,90,80,4,3);block(0xffad716a,80,91,9,2);
      if(tier>=2){block(0xfff2d7a5,81,99,3,24);block(0xffd4ac62,78,105,9,7);}
      if(tier==4){sparkle(55,63,0xffd4a1ff);sparkle(108,70,0xffd4a1ff);}
      block(0xffaab8c4,107,129,24,13);block(0xfff2ce7b,109,127,20,4);
      textPixel(prestige>0?"REBORN HERO":tier>=3?"TYCOON":"BOTTOM LIFE",80,17,0xffffd888,10);
      textPixel("TAP TO EARN",80,180,0xfff5e7c8,10);
      if(System.currentTimeMillis()-tapTime<500){
        long age=System.currentTimeMillis()-tapTime;
        for(int i=0;i<7;i++){double angle=i*6.2831853/7;int radius=(int)(8+age*.045);int x=82+(int)(Math.cos(angle)*radius),y=106+(int)(Math.sin(angle)*radius);block(0xffffd166,x,y,4,4);}
        textPixel("+"+fmt(tapIncome()),80,42,0xfffff1a4,12);postInvalidateDelayed(32);
      }
    }
  }
  void save(){
    try{JSONObject j=new JSONObject();j.put("money",money);j.put("lifetime",lifetime);j.put("gems",gems);j.put("ore",ore);j.put("souls",souls);j.put("tapLevel",tapLevel);j.put("workers",workers);j.put("drill",drill);j.put("depth",depth);j.put("prestige",prestige);j.put("soulPower",soulPower);j.put("pick",pick);j.put("totalTaps",totalTaps);j.put("bestDepth",bestDepth);j.put("questClaims",questClaims);org.json.JSONArray relicArray=new org.json.JSONArray();for(int r:relics)relicArray.put(r);j.put("relics",relicArray);
      org.json.JSONArray a=new org.json.JSONArray();for(int v:owned)a.put(v);j.put("owned",a);prefs.edit().putString("state",j.toString()).putLong("savedAt",System.currentTimeMillis()).apply();
    }catch(Exception ignored){}
  }
  void load(){
    try{JSONObject j=new JSONObject(prefs.getString("state","{}"));money=j.optDouble("money",0);lifetime=j.optDouble("lifetime",0);gems=j.optDouble("gems",0);ore=j.optDouble("ore",0);souls=j.optDouble("souls",0);tapLevel=j.optInt("tapLevel",1);workers=j.optInt("workers",0);drill=j.optInt("drill",1);depth=j.optInt("depth",1);prestige=j.optInt("prestige",0);soulPower=j.optInt("soulPower",0);pick=j.optInt("pick",1);totalTaps=j.optInt("totalTaps",0);bestDepth=j.optInt("bestDepth",depth);questClaims=j.optInt("questClaims",0);org.json.JSONArray savedRelics=j.optJSONArray("relics");if(savedRelics!=null)for(int i=0;i<Math.min(relics.length,savedRelics.length());i++)relics[i]=savedRelics.optInt(i);
      org.json.JSONArray a=j.optJSONArray("owned");if(a!=null)for(int i=0;i<Math.min(a.length(),owned.length);i++)owned[i]=a.optInt(i);
      long elapsed=Math.max(0,Math.min(8*3600,(System.currentTimeMillis()-prefs.getLong("savedAt",System.currentTimeMillis()))/1000));earn(passive()*elapsed);
    }catch(Exception ignored){}
  }
  protected void onPause(){super.onPause();save();}
  protected void onDestroy(){handler.removeCallbacks(ticker);save();super.onDestroy();}
}
