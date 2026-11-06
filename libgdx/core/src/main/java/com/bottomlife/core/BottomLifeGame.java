package com.bottomlife.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class BottomLifeGame extends ApplicationAdapter {
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private Texture koreanAtlas;
    private Texture streetSprite, mineSprite, shopSprite;
    private Texture[] actors=new Texture[4];
    private String koreanChars;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Preferences save;
    private final Vector3 touch = new Vector3();
    private double coins, gems, ore, souls;
    private int tapLevel, workerLevel, drillLevel, depth, rebirths;
    private float timer, saveTimer, flash, elapsed;
    private String notice = "터치해서 돈 획득!";
    private int combo, treasures, missions;
    private float comboTime;
    private float tapFloatTimer;
    private double lastTapEarned;
    private int totalTaps, achievementFlags;
    private float autoMineTimer;
    private int screen=0, career=0, claimed=0;
    private int questTier=0, questTapBase=0, questMineBase=0;
    private int mineActions=0;
    private final int[] shops=new int[5];
    private final String[] shopNames={"노점","분식점","편의점","피시방","카페"};
    private final double[] shopBase={100,2000,30000,400000,5000000};
    private static final float W=360, H=640;

    @Override public void create() {
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.35f);
        try {
            koreanAtlas = new Texture(Gdx.files.local("korean_atlas.png"));
            koreanAtlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            koreanChars = Gdx.files.local("korean_chars.txt").readString("UTF-8");
        } catch(Exception ex) { Gdx.app.error("BottomLife","Korean font atlas unavailable",ex); }
        streetSprite=makePixelScene(0);
        mineSprite=makePixelScene(1);
        shopSprite=makePixelScene(2);
        for(int i=0;i<4;i++)actors[i]=makeActor(i);
        camera = new OrthographicCamera();
        viewport = new FitViewport(W,H,camera);
        viewport.update(Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),true);
        save = Gdx.app.getPreferences("bottomlife_libgdx_v1");
        coins=Double.longBitsToDouble(save.getLong("coins",0L)); gems=Double.longBitsToDouble(save.getLong("gems",0L));
        ore=Double.longBitsToDouble(save.getLong("ore",0L)); souls=Double.longBitsToDouble(save.getLong("souls",0L));
        tapLevel=save.getInteger("tap",1); workerLevel=save.getInteger("workers",0);
        drillLevel=save.getInteger("drill",1); depth=save.getInteger("depth",1);
        rebirths=save.getInteger("rebirths",0);
        treasures=save.getInteger("treasures",0); missions=save.getInteger("missions",0);
        career=save.getInteger("career",0);claimed=save.getInteger("claimed",0);
        totalTaps=save.getInteger("totalTaps",0);
        achievementFlags=save.getInteger("achievementFlags",0);
        questTier=save.getInteger("questTier",0);questTapBase=save.getInteger("questTapBase",0);
        questMineBase=save.getInteger("questMineBase",0);mineActions=save.getInteger("mineActions",0);
        for(int i=0;i<shops.length;i++)shops[i]=save.getInteger("shop"+i,0);
        long last=save.getLong("last",System.currentTimeMillis());
        double seconds=Math.min(14400,Math.max(0,(System.currentTimeMillis()-last)/1000.0));
        coins+=seconds*income()*0.5;
        if(seconds>60) notice="오프라인 수익 획득!";
    }
    private double multiplier(){return 1+souls*0.15;}
    private double tapPower(){return Math.ceil((1+tapLevel*2)*multiplier()*(1+career*.1));}
    private double income(){
        double total=workerLevel*(1+workerLevel*0.45);
        for(int i=0;i<shops.length;i++)total+=shops[i]*Math.pow(2.8,i)*Math.pow(1.12,shops[i]);
        return total*multiplier()*(1+career*.05);
    }
    private double shopCost(int i){return Math.ceil(shopBase[i]*Math.pow(1.42,shops[i]));}
    private void purchaseShop(int i){
        double price=shopCost(i);
        if(coins<price){notice="돈 부족!";return;}
        coins-=price;shops[i]++;notice=shopNames[i]+" 사업 강화!";checkAchievements();persist();
    }
    private void collectReward(){
        long today=System.currentTimeMillis()/86400000L;
        if(claimed==(int)(today%2000000000L)){notice="오늘 보상 완료!";return;}
        double reward=Math.max(100, income()*120+tapPower()*50);
        coins+=reward;claimed=(int)(today%2000000000L);notice="출석 보상 +"+fmt(reward);persist();
    }
    private double tapCost(){return Math.ceil(20*Math.pow(1.22,tapLevel-1));}
    private double workerCost(){return Math.ceil(50*Math.pow(1.3,workerLevel));}
    private double drillCost(){return Math.ceil(30*Math.pow(1.25,drillLevel-1));}
    private double mineCost(){return Math.ceil(10*Math.pow(1.2,depth-1));}
    private void checkAchievements(){
        boolean[] completed={totalTaps>=100,depth>=10,shops[0]>=10,rebirths>=1,treasures>=10};
        for(int i=0;i<completed.length;i++){
            if(completed[i] && (achievementFlags&(1<<i))==0){
                achievementFlags|=1<<i;
                gems+=i+1;
                coins+=(i+1)*1000;
                notice="업적 달성! 보석 +"+(i+1);
            }
        }
    }
    private int questTapTarget(){return 25+questTier*15;}
    private int questMineTarget(){return 10+questTier*8;}
    private void claimQuest(){
        if(totalTaps-questTapBase<questTapTarget()||mineActions-questMineBase<questMineTarget()){
            notice="임무 목표 미달성!";return;
        }
        double reward=500*Math.pow(2,Math.min(40,questTier));
        coins+=reward;gems+=2+questTier/3;missions++;questTier++;
        questTapBase=totalTaps;questMineBase=mineActions;
        notice="임무 완료! 돈 +"+fmt(reward);persist();
    }
    private void mineOnce(boolean large){
        double amount=drillLevel*depth*(large?2:1);
        ore+=amount;mineActions++;flash=.16f;
        if(Math.random()<Math.min(.4,.04+depth*.004))gems++;
        if(Math.random()<.06){
            double bonus=100*depth*drillLevel;
            coins+=bonus;treasures++;
            notice="보물상자 발견! +"+fmt(bonus);
        }else notice="광물 +"+fmt(amount)+" 획득";
        checkAchievements();persist();
    }
    private void persist(){
        save.putLong("coins",Double.doubleToRawLongBits(coins)); save.putLong("gems",Double.doubleToRawLongBits(gems));
        save.putLong("ore",Double.doubleToRawLongBits(ore)); save.putLong("souls",Double.doubleToRawLongBits(souls));
        save.putInteger("tap",tapLevel); save.putInteger("workers",workerLevel);
        save.putInteger("drill",drillLevel); save.putInteger("depth",depth);
        save.putInteger("rebirths",rebirths);
        save.putInteger("treasures",treasures); save.putInteger("missions",missions);
        save.putInteger("career",career);save.putInteger("claimed",claimed);
        save.putInteger("totalTaps",totalTaps);
        save.putInteger("achievementFlags",achievementFlags);
        save.putInteger("questTier",questTier);save.putInteger("questTapBase",questTapBase);
        save.putInteger("questMineBase",questMineBase);save.putInteger("mineActions",mineActions);
        for(int i=0;i<shops.length;i++)save.putInteger("shop"+i,shops[i]);
        save.putLong("last",System.currentTimeMillis()); save.flush();
    }
    private String fmt(double v) {
        if(v>=1e12)return String.format(java.util.Locale.US,"%.2fT",v/1e12);
        if(v>=1e9)return String.format(java.util.Locale.US,"%.2fB",v/1e9);
        if(v>=1e6)return String.format(java.util.Locale.US,"%.2fM",v/1e6);
        if(v>=1e3)return String.format(java.util.Locale.US,"%.1fK",v/1e3);
        return String.valueOf((long)v);
    }
    private void buy(double price,int action){
        if(coins<price){notice="돈 부족!";return;}
        coins-=price;
        if(action==0)tapLevel++;
        if(action==1)workerLevel++;
        if(action==2)drillLevel++;
        if(action==3)depth++;
        notice="강화 성공!";
        checkAchievements();persist();
    }
    private void input(float x,float y){
        if(x<0||x>W||y<0||y>H)return;
        if(y>=588){screen=Math.min(3,(int)(x/90));notice=screen==0?"거리 노동":screen==1?"사업 운영":screen==2?"광산 탐험":"성장 임무";return;}
        if(screen==3){if(y>=295&&y<345)claimQuest();else if(y>=237&&y<287)collectReward();else if(y>=8&&y<55){screen=0;}return;}
        if(screen==1){
            int[] bottoms={295,237,179,121,63};
            for(int i=0;i<5;i++)if(y>=bottoms[i] && y<bottoms[i]+50){purchaseShop(i);return;}
            if(y>=8&&y<55){collectReward();return;}
            return;
        }
        if(screen==2){
            if(y>=358&&y<588){mineOnce(true);return;}
            if(y>=295&&y<345){buy(drillCost(),2);return;}
            if(y>=237&&y<287){buy(mineCost(),3);return;}
            if(y>=179&&y<229){double sold=ore*(2+depth);coins+=sold;ore=0;notice="광물 판매 +"+fmt(sold);persist();return;}
            if(y>=121&&y<171){if(gems>=10){gems-=10;coins+=1000*depth;notice="보석 교환 성공!";persist();}else notice="보석 10개 필요";return;}
            if(y>=63&&y<113){collectReward();return;}
            if(y>=8&&y<55){screen=0;return;}
            return;
        }
        if(y>=358 && y<588){
            combo=comboTime>0?Math.min(100,combo+1):1;comboTime=2.0f;
            double earned=tapPower()*(1+Math.min(2,combo/20.0));
            coins+=earned;flash=0.16f;tapFloatTimer=.6f;lastTapEarned=earned;totalTaps++;notice="+"+fmt(earned)+" 돈  콤보 "+combo;
            checkAchievements();
            if(Math.random()<0.005){gems++;notice="행운! 보석 +1";}
            return;
        }
        if(y>=295 && y<345)buy(tapCost(),0);
        else if(y>=237 && y<287)buy(workerCost(),1);
        else if(y>=179 && y<229){mineOnce(false);}
        else if(y>=121 && y<171)buy(drillCost(),2);
        else if(y>=63 && y<113)buy(mineCost(),3);
        else if(y>=8 && y<55){
            if(coins<100000){notice="환생 필요: 100K 돈";return;}
            double gain=Math.max(1,Math.floor(Math.sqrt(coins/100000)));
            souls+=gain;rebirths++;career=Math.max(career,rebirths);
            coins=0;ore=0;tapLevel=1;workerLevel=0;drillLevel=1;depth=1;
            notice="환생 성공! 영혼 +"+fmt(gain);checkAchievements();persist();
        }
    }

    private void pixel(Pixmap p,int x,int y,int w,int h,String color){
        p.setColor(Color.valueOf(color));p.fillRectangle(x,y,w,h);
    }
    private Texture makePixelScene(int mode){
        Pixmap p=new Pixmap(180,120,Pixmap.Format.RGBA8888);
        pixel(p,0,0,180,120,mode==1?"0b1426":mode==2?"192331":"27334a");
        java.util.Random random=new java.util.Random(3701+mode);
        if(mode==1){
            for(int i=0;i<15;i++){
                int x=random.nextInt(180), y=random.nextInt(110);
                pixel(p,x,y,4+random.nextInt(12),5+random.nextInt(18),"303449");
                pixel(p,x+2,y+2,3,9,"46435a");
            }
            for(int i=0;i<23;i++){
                int x=random.nextInt(174),y=20+random.nextInt(90);
                pixel(p,x,y,7,12,i%3==0?"e0a54c":"317fb9");
                pixel(p,x+2,y-5,3,8,i%3==0?"ffe08c":"7feaff");
                pixel(p,x+3,y+4,2,4,"d6f9ff");
            }
            pixel(p,0,106,180,14,"292838");
        } else {
            pixel(p,0,75,180,45,"34333c");
            pixel(p,0,101,180,19,"232631");
            for(int i=0;i<7;i++){
                int x=i*29-8, h=25+random.nextInt(40);
                pixel(p,x,75-h,27,h,mode==2?"38394a":"3a4056");
                for(int wy=78-h;wy<71;wy+=9)for(int wx=x+5;wx<x+24;wx+=9)
                    if(random.nextInt(4)>0)pixel(p,wx,wy,4,5,"e3a866");
            }
            pixel(p,8,51,53,47,mode==2?"6a4540":"594147");
            pixel(p,12,56,45,30,"182b38");
            pixel(p,8,47,53,9,"d47d42");
            for(int i=0;i<5;i++)pixel(p,13+i*9,48,5,6,"ffe4a3");
            pixel(p,126,54,45,44,"454455");
            pixel(p,131,59,35,29,"213748");
            pixel(p,0,102,180,2,"a99a76");
        }
        for(int i=0;i<18;i++){
            int sx=random.nextInt(176),sy=random.nextInt(90);
            if(sx<65||sx>118)pixel(p,sx,sy,2,2,mode==1?"6fd8ff":"b3a18a");
        }
        Texture t=new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        p.dispose();return t;
    }
    private Texture makeActor(int variant){
        Pixmap p=new Pixmap(40,64,Pixmap.Format.RGBA8888);
        p.setColor(0,0,0,0);p.fill();
        boolean rich=variant>=2, walk=variant%2==1;
        String coat=rich?"27394e":"657d7d";
        pixel(p,9,53,9,7,"1b2230");pixel(p,23,walk?51:53,9,7,"1b2230");
        pixel(p,10,42,8,12,"303447");pixel(p,22,42,8,12,"303447");
        pixel(p,9,25,23,21,"1b2634");pixel(p,11,27,19,18,coat);
        pixel(p,16,27,8,10,rich?"e8d6b3":"c9a88c");
        pixel(p,5,walk?31:29,6,16,coat);pixel(p,32,29,5,16,coat);
        pixel(p,7,walk?44:42,4,5,"dba47e");pixel(p,33,42,4,5,"dba47e");
        pixel(p,12,10,18,17,"b78362");pixel(p,13,11,16,15,"e3b18b");
        pixel(p,10,6,22,8,"25202b");pixel(p,10,10,5,9,"25202b");
        pixel(p,14,7,11,3,"38303a");pixel(p,16,18,3,3,"202b35");
        pixel(p,26,18,3,3,"202b35");pixel(p,21,23,4,2,"945d59");
        if(rich){
            pixel(p,17,27,6,17,"f0e6cf");pixel(p,19,30,3,13,"c6a44e");
            pixel(p,8,24,25,4,"d6ad54");
        }else{
            pixel(p,10,37,5,3,"9d8b76");pixel(p,29,34,4,5,"9b7965");
        }
        Texture t=new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        p.dispose();return t;
    }
    @Override public void resize(int width,int height){viewport.update(width,height,true);}
    private void rect(float x,float y,float w,float h,float r,float g,float b){
        shapes.setColor(r,g,b,1);shapes.rect(x,y,w,h);
    }
    private void label(String s,float x,float y){
        if(koreanAtlas==null || koreanChars==null){font.draw(batch,s,x,y);return;}
        float cursor=x;
        for(int i=0;i<s.length();i++){
            char ch=s.charAt(i);
            int index=koreanChars.indexOf(ch);
            if(index>=0){
                TextureRegion region=new TextureRegion(koreanAtlas,(index%16)*40,(index/16)*40,40,40);
                batch.draw(region,cursor,y-4,19,19);
            }
            cursor+=ch<128?10:18;
        }
    }
    @Override public void render(){
        float delta=Math.min(Gdx.graphics.getDeltaTime(),0.1f);
        timer+=delta;saveTimer+=delta;elapsed+=delta;flash=Math.max(0,flash-delta);comboTime=Math.max(0,comboTime-delta);tapFloatTimer=Math.max(0,tapFloatTimer-delta);
        if(timer>=0.1f){coins+=income()*timer;timer=0;}
        if(workerLevel>=5 && drillLevel>=3){autoMineTimer+=delta;if(autoMineTimer>=5){ore+=drillLevel*depth;autoMineTimer=0;}}
        if(saveTimer>=10){persist();saveTimer=0;}
        if(Gdx.input.justTouched()){
            touch.set(Gdx.input.getX(),Gdx.input.getY(),0);
            viewport.unproject(touch);input(touch.x,touch.y);
        }
        Gdx.gl.glClearColor(.045f,.065f,.11f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        rect(8,8,344,624,.08f,.11f,.18f);
        // Art is rendered in the sprite batch below at native pixel resolution.
        rect(12,360,336,152,.10f,.13f,.20f);
        if(flash>0)rect(17,364,326,144,.30f,.29f,.18f);
        // Raised pixel-style upgrade panels.
        float[][] buttons={{295,345},{237,287},{179,229},{121,171},{63,113},{8,55}};
        for(int i=0;i<buttons.length;i++){
            float y=buttons[i][0], h=buttons[i][1]-y;
            rect(16,y-3,328,h,.035f,.055f,.09f);
            rect(16,y,328,h,i==5?.41f:.16f,i==5?.26f:.23f,i==5?.22f:.34f);
            rect(16,y+h-5,328,5,i==5?.94f:.28f,i==5?.65f:.39f,i==5?.32f:.49f);
            rect(24,y+10,25,25,i==5?.96f:.83f,i==5?.68f:.61f,i==5?.35f:.33f);
            rect(29,y+15,15,15,.18f,.23f,.31f);
        }
        // Four navigation tabs with matching touch regions.
        for(int i=0;i<4;i++){
            float x=8+i*88;
            rect(x,590,84,36,i==screen?.46f:.13f,i==screen?.34f:.19f,i==screen?.20f:.28f);
            rect(x,590,84,3,i==screen?.97f:.30f,i==screen?.75f:.40f,i==screen?.39f:.51f);
        }
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Texture scene=screen==2?mineSprite:screen==1?shopSprite:streetSprite;
        batch.draw(scene,18,369,324,136);
        int actor=(rebirths>0?2:0)+((int)(elapsed*3)%2);
        batch.draw(actors[actor],149,389+((int)(elapsed*3)%2)*2,76,113);
        label("거리",18,613);
        label("사업",105,613);
        label("광산",193,613);
        label("임무",281,613);
        label("돈 "+fmt(coins)+"  초당 +"+fmt(income()),28,575);
        label("보석 "+fmt(gems)+" 광물 "+fmt(ore)+" 영혼 "+fmt(souls),28,545);
        label(screen==1?"사업 운영":screen==2?"광산 탐험":screen==3?"성장 임무":"터치해서 돈 획득 +"+fmt(tapPower()),52,501);
        label(screen==1?"사업 제국":screen==2?"지하 "+depth+"층":screen==3?"임무 보상":rebirths>0?"환생 성장":"거리 노동",125,405);
        if(screen==0){
            label("터치 강화 "+tapLevel+" 비용 "+fmt(tapCost()),58,325);
            label("노동자 고용 "+workerLevel+" 비용 "+fmt(workerCost()),58,267);
            label("광산 채굴 "+depth+"층 +"+fmt(drillLevel*depth),58,209);
            label("채굴 강화 "+drillLevel+" 비용 "+fmt(drillCost()),58,151);
            label("지하층 확장 비용 "+fmt(mineCost()),58,93);
            label("환생 100K  "+rebirths+"회",58,37);
        } else if(screen==1){
            for(int i=0;i<5;i++)label(shopNames[i]+" "+shops[i]+" 비용 "+fmt(shopCost(i)),58,325-i*58);
            label("출석 보상 받기",58,37);
        } else if(screen==3){
            label("임무 보상 받기 "+(questTier+1),58,325);
            label("출석 보상 받기",58,267);
            label("터치 "+Math.max(0,totalTaps-questTapBase)+"/"+questTapTarget(),58,209);
            label("채굴 "+Math.max(0,mineActions-questMineBase)+"/"+questMineTarget(),58,151);
            label("완료 "+missions+"  보석 "+fmt(gems),58,93);
            label("거리로 돌아가기",58,37);
        } else {
            label("곡괭이 강화 "+drillLevel+" "+fmt(drillCost()),58,325);
            label("더 깊이 "+depth+"층 "+fmt(mineCost()),58,267);
            label("광물 전부 판매",58,209);
            label("보석 10개 교환",58,151);
            label("출석 보상",58,93);
            label("거리로 돌아가기",58,37);
        }
        if(tapFloatTimer>0 && screen==0)label("+"+fmt(lastTapEarned),147,463+tapFloatTimer*32);
        label(notice,18,351);
        batch.end();
    }
    @Override public void pause(){persist();}
    @Override public void dispose(){persist();font.dispose();if(koreanAtlas!=null)koreanAtlas.dispose();streetSprite.dispose();mineSprite.dispose();shopSprite.dispose();for(Texture t:actors)t.dispose();batch.dispose();shapes.dispose();}
}
