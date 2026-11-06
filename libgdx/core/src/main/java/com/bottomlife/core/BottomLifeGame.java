package com.bottomlife.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.InputAdapter;
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
    private Texture streetSprite, mineSprite, shopSprite, streetArt;
    private Texture[] actors=new Texture[8];
    private Texture streetForeground, mineForeground;
    private Texture pngActor;
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
    private float effectX=180,effectY=445,effectTimer;
    private float mineImpactTimer, mineRewardTimer;
    private float mineShakeTimer;
    private int mineCombo;
    private float mineComboTimer;
    private int mineRewardKind;
    private double mineRewardAmount;
    private int effectKind;
    private int screen=0, career=0, claimed=0;
    private int questTier=0, questTapBase=0, questMineBase=0;
    private int mineActions=0;
    private int mineBiomeCache=-1;
    private int growthMode=0;
    private int businessBuyCount=1;
    private final int[] growth=new int[3];
    private final int[] shops=new int[5];
    private final int[] managers=new int[5];
    private final String[] shopNames={"노점","분식점","편의점","피시방","카페"};
    private final double[] shopBase={100,2000,30000,400000,5000000};
    private static final float W=360, H=640;
    // Standalone authored 48x64 RGBA PNG sprite, embedded for reliable Android packaging.
    private static final String CHARACTER_PNG="iVBORw0KGgoAAAANSUhEUgAAADAAAABACAYAAABcIPRGAAABT0lEQVR42mNgGAWjYBQMacBIqgYNVYv/tHbUjdsniHYX01CPgVEPjHqAnh6gRwYejYFRD9AZsFDDkJ2dPlRzkHxQDeNoEhr1wFDLAwwMDAyOlVvg7P3tPlRTO5qEhl1/gIGBgeHhuhaaNSlGXDFKtG/tqpvhob7YmJFmDoo9i4jcQ621BC1ioZVDDq+7gVXcNkhjNBPTJQaoHdIk5QHk9I4N0CsPYAPo+YKiGLj0jZmqjtfj+kufPBB79j/BkKJF6I82JYZdKdR9/Q8K//X5G0TrFTXUGI2BUQ+MemDUA6MeGAL1AK6GGy2aEuTYOZqEhlWHhlrNA6p26nENoQxEHsA25EJ2DHBce0m7YDWWoF4MYANugTk0nSvbtX7K6ET3qAdGPTDqgVEPjHqAdhUZAwMDg5qeM1GV2a1LexnJUT+ahEY9MOqBUQ+MemAUEAMAGyZOkFrzLs0AAAAASUVORK5CYII=";

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
        try {streetArt=new Texture(Gdx.files.internal("street_art.png"));streetArt.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);}
        catch(Exception ex){Gdx.app.error("BottomLife","Street asset unavailable",ex);}
        mineSprite=makePixelScene(1);
        shopSprite=makePixelScene(2);
        streetForeground=makeForeground(0);
        mineForeground=makeForeground(1);
        for(int i=0;i<actors.length;i++)actors[i]=makeActor(i);
        try {
            byte[] bytes=java.util.Base64.getDecoder().decode(CHARACTER_PNG);
            Pixmap pix=new Pixmap(bytes,0,bytes.length);
            pngActor=new Texture(pix);
            pngActor.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
            pix.dispose();
        }catch(Exception e){Gdx.app.error("BottomLife","PNG sprite fallback",e);}
        camera = new OrthographicCamera();
        viewport = new FitViewport(W,H,camera);
        viewport.update(Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),true);
        Gdx.input.setInputProcessor(new InputAdapter(){
            @Override public boolean touchDown(int sx,int sy,int pointer,int button){
                if(pointer!=0)return false;
                touch.set(sx,sy,0);viewport.unproject(touch);input(touch.x,touch.y);
                return true;
            }
        });
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
        businessBuyCount=save.getInteger("businessBuyCount",1)==10?10:1;
        for(int i=0;i<shops.length;i++){shops[i]=save.getInteger("shop"+i,0);managers[i]=save.getInteger("manager"+i,0);}
        for(int i=0;i<growth.length;i++)growth[i]=save.getInteger("growth"+i,0);
        long last=save.getLong("last",System.currentTimeMillis());
        double seconds=Math.min(14400,Math.max(0,(System.currentTimeMillis()-last)/1000.0));
        coins+=seconds*income()*0.5;
        if(seconds>60) notice="오프라인 수익 획득!";
    }
    private double multiplier(){return (1+souls*0.15)*(1+growth[2]*.05);}
    private double rebirthGain(){return coins<100000?0:Math.max(1,Math.floor(Math.sqrt(coins/100000)));}
    private void performRebirth(){double gain=rebirthGain();if(gain<1){notice="환생 필요: 100K 돈";return;}souls+=gain;rebirths++;career=Math.max(career,rebirths);coins=0;ore=0;tapLevel=1;workerLevel=0;drillLevel=1;depth=1;screen=0;notice="환생 성공! 영혼 +"+fmt(gain);checkAchievements();persist();}
    private double growthCost(int i){return Math.ceil(new double[]{200,800,2500}[i]*Math.pow(1.65,growth[i]));}
    private void upgradeGrowth(int i){double cost=growthCost(i);if(coins<cost){notice="성장 비용 부족!";return;}coins-=cost;growth[i]++;notice="능력 성장 Lv."+growth[i];persist();}
    private double tapPower(){return Math.ceil((1+tapLevel*2)*(1+growth[0]*.12)*multiplier()*(1+career*.1));}
    private double income(){
        double total=workerLevel*(1+workerLevel*0.45)*multiplier()*(1+career*.05);
        for(int i=0;i<shops.length;i++)total+=shopIncome(i);
        return total*(1+growth[1]*.10);
    }
    private double shopCost(int i){return Math.ceil(shopBase[i]*Math.pow(1.42,shops[i]));}
    private int shopMilestone(int level){return level>=100?4:level>=50?3:level>=25?2:level>=10?1:0;}
    private int nextShopMilestone(int level){return level<10?10:level<25?25:level<50?50:level<100?100:0;}
    private int operatingShops(){int n=0;for(int level:shops)if(level>0)n++;return n;}
    private double businessSynergy(){int active=operatingShops();return active>=5?1.5:active>=3?1.2:1.0;}
    private double shopSpecial(int i){
        if(shops[i]<1)return 1;
        if(i==0)return 1+Math.min(2.0,totalTaps/500.0);
        if(i==1)return 1+Math.min(1.5,depth/50.0);
        if(i==2)return 1+Math.min(1.5,treasures/25.0);
        if(i==3)return 1+Math.min(2.0,rebirths*.15);
        return 1+Math.min(2.0,missions*.08);
    }
    private String shopSpecialName(int i){
        return new String[]{"손님 유입","광산 납품","보물 판매","환생 투자","임무 제휴"}[i];
    }
    private double shopIncome(int i){
        double managerBonus=managers[i]==0?1:managers[i]==1?1.5:managers[i]==2?2.25:3.5;
        return shops[i]*Math.pow(2.8,i)*Math.pow(1.12,shops[i])*Math.pow(2,shopMilestone(shops[i]))*managerBonus*shopSpecial(i)*businessSynergy()*multiplier()*(1+career*.05);
    }
    private double managerCost(int i){return Math.ceil(shopBase[i]*15*Math.pow(6,managers[i]));}
    private void hireManager(int i){
        if(shops[i]<1){notice="사업을 먼저 열어야 해!";return;}
        if(managers[i]>=3){notice="최고 관리자 고용 완료!";return;}
        double cost=managerCost(i);
        if(coins<cost){notice="관리자 고용 자금 부족!";return;}
        coins-=cost;managers[i]++;
        notice=shopNames[i]+" 관리자 Lv."+managers[i]+" 고용!";
        effectTimer=.5f;effectKind=0;effectX=180;effectY=450;
        persist();
    }
    private int businessLevels(){int n=0;for(int v:shops)n+=v;return n;}
    private double bulkShopCost(int i){
        double sum=0;
        for(int n=0;n<businessBuyCount;n++)sum+=Math.ceil(shopBase[i]*Math.pow(1.42,shops[i]+n));
        return sum;
    }
    private void purchaseShop(int i){
        double price=bulkShopCost(i);
        if(!Double.isFinite(price)||coins<price){notice="사업 구매 자금 부족!";return;}
        int previous=shops[i];
        coins-=price;shops[i]+=businessBuyCount;
        int gainedGems=0;
        for(int threshold:new int[]{10,25,50,100}){
            if(previous<threshold&&shops[i]>=threshold){
                gainedGems+=shopMilestone(threshold)*2;
            }
        }
        if(gainedGems>0){
            gems+=gainedGems;
            notice=shopNames[i]+" 돌파! 보석 +"+gainedGems;
            effectTimer=.5f;effectKind=0;effectX=180;effectY=449;
        }else notice=shopNames[i]+" Lv."+shops[i]+" 초당 +"+fmt(shopIncome(i));
        checkAchievements();persist();
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
    private int mineBiome(){return depth>=50?3:depth>=25?2:depth>=10?1:0;}
    private String biomeName(){String[] names={"암석 동굴","수정 광맥","용암 심층","고대 유적"};return names[mineBiome()];}
    private double biomeMultiplier(){return 1.0+mineBiome()*.35;}
    private void refreshMineBiome(){
        int biome=mineBiome();
        if(biome==mineBiomeCache)return;
        mineBiomeCache=biome;
        if(mineSprite!=null)mineSprite.dispose();
        if(mineForeground!=null)mineForeground.dispose();
        mineSprite=makePixelScene(1);
        mineForeground=makeForeground(1);
    }
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
        effectX=180;effectY=452;effectTimer=.5f;effectKind=1;
        mineImpactTimer=.34f;mineRewardTimer=1.3f;mineRewardKind=0;
        mineShakeTimer=.22f;
        mineCombo=mineComboTimer>0?Math.min(30,mineCombo+1):1;
        mineComboTimer=2.2f;
        double amount=drillLevel*depth*(large?2:1)*biomeMultiplier()*(1+Math.min(.5,(mineCombo-1)*.02));
        ore+=amount;mineActions++;flash=.16f;mineRewardAmount=amount;
        if(Math.random()<Math.min(.55,.04+depth*.004+mineBiome()*.025)){gems++;mineRewardKind=1;}
        if(Math.random()<.06+mineBiome()*.015){
            double bonus=100*depth*drillLevel;
            coins+=bonus;treasures++;mineRewardKind=2;mineRewardAmount=bonus;
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
        save.putInteger("businessBuyCount",businessBuyCount);
        for(int i=0;i<shops.length;i++){save.putInteger("shop"+i,shops[i]);save.putInteger("manager"+i,managers[i]);}
        for(int i=0;i<growth.length;i++)save.putInteger("growth"+i,growth[i]);
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
        if(action==3){depth++;refreshMineBiome();notice="새로운 깊이: "+biomeName();}
        if(action!=3)notice="강화 성공!";
        checkAchievements();persist();
    }
    private void input(float x,float y){
        effectX=x;effectY=y;
        if(x<0||x>W||y<0||y>H)return;
        if(screen==5){
            if(y>=365&&y<470){int col=x<180?0:1;int row=y>=420?0:1;screen=row==0?(col==0?1:2):(col==0?3:4);notice="콘텐츠 이동";return;}
            if(y>=290&&y<350){collectReward();return;}
            if(y>=18&&y<85){screen=0;notice="거리 노동";return;}
            return;
        }
        if(screen==4){
            if(y>=80&&y<=156){performRebirth();return;}
            if(y>=18&&y<=74){screen=0;notice="거리 노동";return;}
            return;
        }
        if(y>=588&&y<=632){screen=Math.min(3,(int)(x/90));notice=screen==0?"거리 노동":screen==1?"사업 운영":screen==2?"광산 탐험":"성장 임무";return;}
        if(screen==3){
            if(y>=358&&y<588){growthMode=1-growthMode;notice=growthMode==0?"성장 능력":"임무 보상";return;}
            if(growthMode==0){
                if(y>=295&&y<345)upgradeGrowth(0);
                else if(y>=237&&y<287)upgradeGrowth(1);
                else if(y>=179&&y<229)upgradeGrowth(2);
                else if(y>=121&&y<171){growthMode=1;notice="임무 화면";}
                else if(y>=63&&y<113)collectReward();
                else if(y>=8&&y<55)screen=0;
            }else{
                if(y>=295&&y<345)claimQuest();
                else if(y>=237&&y<287)collectReward();
                else if(y>=121&&y<171){growthMode=0;notice="성장 능력";}
                else if(y>=8&&y<55)screen=0;
            }
            return;
        }
        if(screen==1){
            if(y>=358&&y<588){businessBuyCount=businessBuyCount==1?10:1;notice="사업 일괄 구매 x"+businessBuyCount;persist();return;}
            int[] bottoms={295,237,179,121,63};
            for(int i=0;i<5;i++)if(y>=bottoms[i] && y<bottoms[i]+50){if(x>=231)hireManager(i);else purchaseShop(i);return;}
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
        if(screen==0&&x>=260&&y>=472&&y<515){screen=5;notice="콘텐츠 선택";return;}
        if(y>=358 && y<588){
            combo=comboTime>0?Math.min(100,combo+1):1;comboTime=2.0f;
            double earned=tapPower()*(1+Math.min(2,combo/20.0));
            effectTimer=.5f;effectKind=0;
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
        else if(y>=8 && y<55){screen=4;notice="환생 보상 확인";}
    }

    private void pixel(Pixmap p,int x,int y,int w,int h,String color){
        p.setColor(Color.valueOf(color));p.fillRectangle(x,y,w,h);
    }
    private Texture makePixelScene(int mode){
        Pixmap p=new Pixmap(180,120,Pixmap.Format.RGBA8888);
        pixel(p,0,0,180,120,mode==1?"0b1426":mode==2?"192331":"27334a");
        java.util.Random random=new java.util.Random(3701+mode);
        if(mode==1){
            int biome=mineBiome();
            String[] back={"080e1e","081c2a","30121a","19172e"};
            String[] stone={"22253e","1b3948","513038","37304e"};
            String[] gem={"3e9bd1","41d7dc","ed7543","b7a2ef"};
            pixel(p,0,0,180,120,back[biome]);
            pixel(p,0,0,180,18,"111b31");
            for(int layer=0;layer<4;layer++){
                int y=layer*23;
                for(int i=0;i<18;i++){
                    int x=i*13-8+(layer%2)*7;
                    pixel(p,x,y,12,15,layer%2==0?stone[biome]:"30324b");
                    pixel(p,x+2,y+2,7,2,"42435d");
                }
            }
            for(int i=0;i<15;i++){
                int x=i*13-4;
                pixel(p,x,0,8,10+random.nextInt(19),"34324b");
                pixel(p,x+2,12,4,6,"55506a");
                pixel(p,x+3,18,2,5,"77718a");
            }
            pixel(p,0,100,180,20,"1c2135");
            pixel(p,0,109,180,11,"11182a");
            for(int i=0;i<18;i++){
                int x=random.nextInt(174),y=30+random.nextInt(68);
                boolean blue=i%3!=0;
                pixel(p,x,y,9,16,blue?"255c91":"755c9d");
                pixel(p,x+2,y-8,5,11,gem[biome]);
                pixel(p,x+3,y-12,3,6,blue?"a0edff":"e1b4ff");
                pixel(p,x+3,y+3,2,8,blue?"76d8f7":"d4a5ee");
            }
            for(int i=0;i<22;i++){
                int x=random.nextInt(180),y=15+random.nextInt(90);
                pixel(p,x,y,2,2,i%2==0?"f0c981":"75d9fa");
            }
            pixel(p,12,91,43,4,"6a5147");
            pixel(p,16,86,5,9,"896a55");
            pixel(p,47,86,5,9,"896a55");
            pixel(p,18,82,31,5,"ad805c");
        } else if(mode==2){
            // Business avenue is its own scene, not a copy of the street screen.
            pixel(p,0,0,180,120,"171d30");
            pixel(p,0,0,180,39,"30364d");
            for(int i=0;i<9;i++){
                int x=i*22-5,h=23+random.nextInt(18);
                pixel(p,x,53-h,21,h+32,i%2==0?"3d4259":"333d54");
                for(int j=0;j<4;j++)for(int k=0;k<3;k++)
                    pixel(p,x+3+k*6,55-h+j*8,4,5,(i+j)%3==0?"f3b96b":"8da5ae");
            }
            pixel(p,0,93,180,27,"292c3c");
            pixel(p,0,98,180,3,"88745e");
            for(int i=0;i<5;i++){
                int x=3+i*36;
                pixel(p,x,52,33,42,"4c3942");
                pixel(p,x+2,56,29,34,"222f42");
                pixel(p,x+2,49,29,10,i%2==0?"a9554d":"4b7e82");
                pixel(p,x+5,51,23,5,"eac78a");
                pixel(p,x+4,65,25,20,"3d5760");
                pixel(p,x+7,68,19,14,"d49e67");
                for(int j=0;j<3;j++)pixel(p,x+8+j*6,71,4,7,j%2==0?"f0ce9b":"7dc6c6");
                pixel(p,x+3,87,27,4,"ab7961");
            }
            for(int i=0;i<10;i++){
                int x=i*19;
                pixel(p,x,106,12,2,"685a5b");
                pixel(p,x+4,112,8,1,"b48a62");
            }
        } else {
            // Four depth layers: moonlit skyline, lit windows, shops and wet road.
            pixel(p,0,0,180,120,"111b32");
            pixel(p,0,0,180,29,"1b2745");
            pixel(p,125,9,17,17,"b2b8d7");
            pixel(p,129,10,10,14,"e4ddce");
            for(int i=0;i<11;i++){
                int x=i*19-5,h=19+random.nextInt(27);
                pixel(p,x,65-h,18,h+20,i%2==0?"26304a":"303650");
                for(int yy=69-h;yy<66;yy+=7)for(int xx=x+3;xx<x+16;xx+=6)
                    if(random.nextInt(4)>0)pixel(p,xx,yy,3,4,i%3==0?"d5a66d":"b58a6a");
            }
            pixel(p,0,77,180,43,"302b3e");
            pixel(p,0,94,180,26,"202636");
            for(int i=0;i<18;i++){
                int x=i*11;
                pixel(p,x,96,8,1,"66536a");
                pixel(p,x+3,109,7,1,"4e4457");
                if(i%3==0)pixel(p,x+2,104,5,1,"ae7959");
            }
            // Left street food cart, striped canopy and warm interior.
            pixel(p,5,45,55,50,"3d3141");
            pixel(p,9,50,47,36,"604253");
            pixel(p,12,56,41,23,"172b3b");
            pixel(p,8,45,49,10,"bb654c");
            for(int i=0;i<6;i++)pixel(p,9+i*8,45,6,9,i%2==0?"e8b37c":"8f4551");
            pixel(p,10,83,45,5,"7b5155");
            pixel(p,11,88,5,10,"171e2c");
            pixel(p,49,88,5,10,"171e2c");
            pixel(p,18,63,25,2,"e6a868");
            pixel(p,18,65,4,11,"bd8659");
            // Right convenience kiosk, signage, shelves, door.
            pixel(p,127,43,49,54,"383349");
            pixel(p,131,50,42,38,"243448");
            pixel(p,128,41,47,10,"bb735a");
            pixel(p,133,45,37,4,"f4c47f");
            pixel(p,136,59,31,3,"987b73");
            pixel(p,136,72,31,3,"987b73");
            for(int i=0;i<5;i++){
                pixel(p,137+i*6,62,4,9,i%2==0?"6eb4b8":"e2a26c");
                pixel(p,137+i*6,76,4,7,i%2==0?"a6a3ce":"d1a875");
            }
            pixel(p,119,45,3,52,"786c68");
            pixel(p,116,48,9,5,"eac181");
            pixel(p,119,52,3,7,"c89e62");
            // Neon vending machine and sidewalk clutter.
            pixel(p,66,60,19,33,"2b5269");
            pixel(p,69,64,13,19,"96c3c5");
            for(int i=0;i<3;i++)for(int j=0;j<2;j++)
                pixel(p,71+i*4,67+j*7,3,5,(i+j)%2==0?"e3a2a2":"4f8ca6");
            pixel(p,66,89,19,3,"c9d4c2");
            pixel(p,90,94,13,5,"5a4449");
            pixel(p,92,90,8,4,"a47a5e");
            // Flowering tree canopy and drifting petals.
            pixel(p,2,34,4,60,"5e4351");
            pixel(p,4,40,23,3,"654354");
            for(int i=0;i<100;i++){
                int x=random.nextInt(69),y=random.nextInt(44);
                if(x<45||y<26)pixel(p,x,y,3+random.nextInt(4),2+random.nextInt(3),
                    i%3==0?"f5b6bf":i%3==1?"c87e9f":"e99cae");
            }
            for(int i=0;i<24;i++){
                int x=random.nextInt(180),y=32+random.nextInt(83);
                pixel(p,x,y,2,2,i%2==0?"e8a2b3":"dfb18d");
            }
            // Street lamps and reflections.
            for(int lamp=0;lamp<2;lamp++){
                int x=lamp==0?88:177;
                pixel(p,x,54,2,42,"3b3948");
                pixel(p,x-4,53,10,3,"b68d6a");
                pixel(p,x-2,56,6,5,"f8d58b");
                pixel(p,x-4,101,9,2,"ae825d");
                pixel(p,x-2,106,5,1,"8b735e");
            }
        }
        for(int i=0;i<18;i++){
            int sx=random.nextInt(176),sy=random.nextInt(90);
            if(sx<65||sx>118)pixel(p,sx,sy,2,2,mode==1?"6fd8ff":"b3a18a");
        }
        Texture t=new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        p.dispose();return t;
    }
    private Texture makeForeground(int mode){
        Pixmap p=new Pixmap(180,120,Pixmap.Format.RGBA8888);
        p.setColor(0,0,0,0);p.fill();
        if(mode==0){
            // Near-ground cobblestones, railings and street lamps, independent of the actor.
            pixel(p,0,103,180,17,"242c38");
            pixel(p,0,103,180,2,"62606a");
            for(int i=0;i<24;i++){
                int x=i*9-3;
                pixel(p,x,108+(i%2)*5,7,1,"565360");
                pixel(p,x+2,115,4,1,"393d4a");
            }
            for(int x:new int[]{5,171}){
                pixel(p,x,41,3,62,"3a3c48");
                pixel(p,x-5,40,13,4,"7c5d4b");
                pixel(p,x-3,43,9,5,"e9b66e");
                pixel(p,x-2,44,7,3,"fff0ae");
                pixel(p,x-4,102,12,3,"5b5360");
            }
            pixel(p,0,98,38,5,"343945");pixel(p,141,98,39,5,"343945");
            for(int x=3;x<38;x+=11)pixel(p,x,89,3,12,"55515b");
            for(int x=145;x<178;x+=11)pixel(p,x,89,3,12,"55515b");
        }else{
            // Crystal clusters and foreground rock ledges frame the miner.
            String[] floor={"101626","102f3d","391b20","29213d"};
            String[] crystal={"45acc8","51d8ce","f38b4b","b59cf0"};
            int biome=mineBiome();
            pixel(p,0,108,180,12,floor[biome]);
            pixel(p,0,108,180,3,"33384e");
            for(int i=0;i<16;i++){
                int x=i*13-5;
                pixel(p,x,112,11,8,i%3==0?"37334d":"252a41");
                pixel(p,x+2,111,6,2,"4c4c63");
            }
            for(int x:new int[]{7,149}){
                pixel(p,x,85,17,24,"26253d");
                pixel(p,x+3,76,9,24,"3a3652");
                pixel(p,x+6,67,4,22,crystal[biome]);
                pixel(p,x+8,73,2,13,"a5edfc");
                pixel(p,x+1,89,20,4,"454159");
            }
        }
        Texture t=new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        p.dispose();return t;
    }
    private Texture makeActor(int variant){
        Pixmap p=new Pixmap(48,72,Pixmap.Format.RGBA8888);
        p.setColor(0,0,0,0);p.fill();
        int tier=variant/2;
        boolean stride=(variant%2)==1;
        String coat=tier==0?"546a77":tier==1?"385e73":tier==2?"423e66":"6d4d49";
        String trim=tier==0?"8f9ca2":tier==1?"79c7cb":tier==2?"c2a16c":"f2d58e";
        String pants=tier<2?"303847":"26283e";
        // Offset feet and coat hem for a real two-frame walking silhouette.
        pixel(p,10,63,12,6,"171f2c");pixel(p,27,stride?61:63,12,6,"171f2c");
        pixel(p,12,48,9,16,pants);pixel(p,27,stride?46:48,9,16,pants);
        pixel(p,11,32,27,20,"172536");
        pixel(p,13,33,23,19,coat);
        pixel(p,12,45,25,4,trim);
        pixel(p,10,35,4,15,"263747");pixel(p,34,35,4,15,"263747");
        pixel(p,6,stride?39:36,7,18,coat);pixel(p,37,stride?36:39,6,18,coat);
        pixel(p,7,stride?53:50,6,6,"dca987");pixel(p,37,stride?50:53,6,6,"dca987");
        pixel(p,16,34,16,4,"1c2937");
        pixel(p,18,36,12,12,tier==0?"c0a38e":"efe1c5");
        if(tier>0){
            pixel(p,21,38,6,12,tier==3?"d5a94e":"b2d7df");
            pixel(p,22,42,4,7,tier==3?"f8e2a0":"5c8291");
        }else{
            pixel(p,14,39,5,5,"8b7470");pixel(p,30,39,4,5,"8b7470");
        }
        // Face: ears, neck, skin shadow, eyes and mouth.
        pixel(p,19,27,10,9,"ba8266");
        pixel(p,15,13,19,19,"9f6c58");
        pixel(p,16,13,17,17,"e4ad88");
        pixel(p,14,20,3,7,"b98066");pixel(p,33,20,3,7,"b98066");
        pixel(p,18,25,12,3,"efbc95");
        pixel(p,19,21,3,3,"25313b");pixel(p,28,21,3,3,"25313b");
        pixel(p,23,27,4,2,"a76765");
        pixel(p,16,11,17,6,"25232d");
        pixel(p,14,15,5,11,"25232d");
        pixel(p,30,13,5,9,"25232d");
        pixel(p,19,10,10,3,"49404a");
        if(tier>=2){
            // High-tier shoulder mantle and gold fasteners.
            pixel(p,8,31,8,6,tier==3?"b78a4e":"645e89");
            pixel(p,33,31,8,6,tier==3?"b78a4e":"645e89");
            pixel(p,12,36,5,5,"e2c07e");pixel(p,33,36,4,5,"e2c07e");
        }
        if(tier==3){
            pixel(p,13,9,22,4,"d2a95e");
            pixel(p,17,6,4,5,"f7df99");
            pixel(p,27,6,4,5,"f7df99");
            pixel(p,23,5,3,7,"f7df99");
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
                batch.draw(region,cursor,y-4,17,17);
            }
            cursor+=ch<128?9:16;
        }
    }
    @Override public void render(){
        float delta=Math.min(Gdx.graphics.getDeltaTime(),0.1f);
        timer+=delta;saveTimer+=delta;elapsed+=delta;mineShakeTimer=Math.max(0,mineShakeTimer-delta);mineComboTimer=Math.max(0,mineComboTimer-delta);if(mineComboTimer==0)mineCombo=0;mineImpactTimer=Math.max(0,mineImpactTimer-delta);mineRewardTimer=Math.max(0,mineRewardTimer-delta);effectTimer=Math.max(0,effectTimer-delta);flash=Math.max(0,flash-delta);comboTime=Math.max(0,comboTime-delta);tapFloatTimer=Math.max(0,tapFloatTimer-delta);
        if(timer>=0.1f){coins+=income()*timer;timer=0;}
        if(workerLevel>=5 && drillLevel>=3){autoMineTimer+=delta;if(autoMineTimer>=5){ore+=drillLevel*depth*biomeMultiplier();autoMineTimer-=5;}}
        if(saveTimer>=10){persist();saveTimer=0;}
        if(mineBiomeCache!=mineBiome())refreshMineBiome();
        Gdx.gl.glClearColor(.045f,.065f,.11f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if(screen==5){
            rect(8,8,344,624,.04f,.06f,.11f);
            // Layered skyline, light beams and drifting particles in the content hub.
            for(int i=0;i<15;i++){
                float x=i*25-5;
                float h=35+(i*37)%76;
                rect(x,502,22,h,.075f,.10f,.17f);
                for(int j=0;j<4;j++){
                    float wy=513+j*17;
                    if(wy<502+h)rect(x+5+(i%2)*4,wy,5,7,.52f,.39f,.22f);
                }
            }
            for(int i=0;i<22;i++){
                float x=(i*83+17)%330+15;
                float y=180+(i*47)%320+(float)Math.sin(elapsed*.7f+i)*4;
                rect(x,y,2+(i%3),2+(i%3),.45f,.54f,.70f);
            }
            rect(16,516,328,107,.11f,.16f,.26f);
            rect(20,521,320,97,.17f,.24f,.35f);
            for(int i=0;i<4;i++){
                int col=i%2,row=i/2;
                float x=18+col*165,y=420-row*55;
                rect(x,y,158,49,.08f,.12f,.20f);
                rect(x+3,y+4,152,41,.17f+i*.045f,.23f+i*.035f,.32f+i*.025f);
                rect(x+8,y+10,32,29,.28f+i*.10f,.48f,.61f-i*.07f);
                rect(x+13,y+15,22,19,.74f,.67f,.46f);
                rect(x+15,y+18,18,13,.24f,.33f,.41f);
                // Individual pixel emblems for business, mining, growth and rebirth.
                if(i==0){
                    rect(x+11,y+31,26,5,.92f,.49f,.30f);
                    rect(x+17,y+20,4,10,.98f,.83f,.58f);
                    rect(x+29,y+20,4,10,.98f,.83f,.58f);
                }else if(i==1){
                    rect(x+12,y+24,22,5,.81f,.88f,.95f);
                    rect(x+22,y+16,4,20,.61f,.41f,.27f);
                    rect(x+16,y+31,14,4,.36f,.77f,.95f);
                }else if(i==2){
                    rect(x+20,y+15,6,21,.98f,.82f,.39f);
                    rect(x+13,y+25,20,6,.98f,.82f,.39f);
                    rect(x+18,y+35,10,3,.99f,.91f,.62f);
                }else{
                    rect(x+18,y+16,10,22,.75f,.52f,.94f);
                    rect(x+12,y+22,22,10,.75f,.52f,.94f);
                    rect(x+21,y+26,4,4,.99f,.88f,1f);
                }
            }
            // Progress dashboard; live values are drawn below.
            rect(18,350,324,10,.10f,.13f,.22f);
            rect(20,352,Math.min(320,Math.max(2,320f*(float)Math.min(1,coins/100000.0))),6,.77f,.55f,.29f);
            rect(18,288,324,61,.14f,.20f,.29f);
            rect(21,294,318,50,.26f,.36f,.43f);
            rect(18,22,324,60,.14f,.18f,.27f);
            rect(22,27,316,50,.25f,.30f,.39f);
            shapes.end();
            batch.setProjectionMatrix(camera.combined);batch.begin();
            label("콘텐츠",129,589);
            label("거리에서 사업가로 성장하기",26,543);
            label("돈 "+fmt(coins)+"  초당 +"+fmt(income()),26,267);
            label("사업 "+businessLevels()+"레벨  광산 "+depth+"층",26,245);
            label("환생 "+rebirths+"회  영혼 "+fmt(souls),26,223);
            label("사업",67,449);label("광산",230,449);
            label("성장",67,394);label("환생",230,394);
            label("출석 보상 받기",83,322);
            label("다음 환생까지 "+fmt(Math.max(0,100000-coins)),25,367);
            label("거리로 돌아가기",83,53);
            batch.end();return;
        }
        if(screen==4){
            // Dedicated prestige view: confirmation is separate from the street action.
            rect(8,8,344,624,.035f,.045f,.09f);
            rect(15,518,330,104,.11f,.08f,.19f);
            rect(19,522,322,96,.19f,.12f,.27f);
            for(int i=0;i<24;i++){
                float angle=i*6.2831853f/24f+elapsed*.17f;
                float radius=77+(float)Math.sin(elapsed+i)*3;
                rect(180+(float)Math.cos(angle)*radius-3,407+(float)Math.sin(angle)*radius-3,6,6,.94f,.67f,.32f);
            }
            rect(138,365,84,84,.42f,.26f,.54f);
            rect(145,372,70,70,.66f,.40f,.78f);
            rect(157,384,46,46,.24f,.14f,.39f);
            rect(18,206,324,109,.09f,.12f,.21f);
            for(int i=0;i<3;i++){
                float y=278-i*30;
                rect(26,y,307,25,.17f,.19f,.30f);
                rect(27,y+21,305,3,.75f,.55f,.31f);
            }
            boolean ready=rebirthGain()>0;
            rect(30,85,300,66,ready?.44f:.24f,ready?.25f:.24f,ready?.60f:.29f);
            rect(35,91,290,54,ready?.64f:.32f,ready?.41f:.32f,ready?.79f:.36f);
            rect(30,20,300,48,.15f,.19f,.28f);
            rect(35,25,290,38,.23f,.29f,.39f);
            shapes.end();
            batch.setProjectionMatrix(camera.combined);batch.begin();
            label("환생",150,597);
            label("새로운 인생을 시작합니다",40,552);
            label("영혼 "+fmt(souls)+"  환생 "+rebirths+"회",35,501);
            label("다음 환생 보상 +"+fmt(rebirthGain()),36,337);
            label("전체 수익 +"+fmt(souls*15)+"%",37,295);
            label("터치와 노동자 초기화",37,265);
            label("사업 성장 능력 유지",37,235);
            label(ready?"환생하기":"100K 돈 필요",109,120);
            label("취소  거리로 돌아가기",65,48);
            batch.end();
            return;
        }
        rect(8,8,344,624,.08f,.11f,.18f);
        // Art is rendered in the sprite batch below at native pixel resolution.
        rect(12,360,336,152,.10f,.13f,.20f);
        if(flash>0)rect(17,364,326,144,.30f,.29f,.18f);
        // Raised pixel-style upgrade panels.
        float[][] buttons={{295,345},{237,287},{179,229},{121,171},{63,113},{8,55}};
        for(int i=0;i<buttons.length;i++){
            float y=buttons[i][0], h=buttons[i][1]-y;
            rect(16,y-3,328,h,.035f,.055f,.09f);
            rect(16,y,328,h,i==5?.35f:.12f,i==5?.20f:.17f,i==5?.16f:.26f);
            rect(19,y+5,322,h-13,i==5?.28f:.16f,i==5?.18f:.24f,i==5?.17f:.34f);
            rect(19,y+5,322,2,.38f,.47f,.58f);
            rect(16,y+h-5,328,5,i==5?.94f:.28f,i==5?.65f:.39f,i==5?.32f:.49f);
            rect(24,y+10,25,25,i==5?.96f:.83f,i==5?.68f:.61f,i==5?.35f:.33f);
            rect(29,y+15,15,15,.18f,.23f,.31f);
            boolean canBuy=true;
            if(screen==0&&i==0)canBuy=coins>=tapCost();
            if(screen==0&&i==1)canBuy=coins>=workerCost();
            if(screen==0&&i==3)canBuy=coins>=drillCost();
            if(screen==0&&i==4)canBuy=coins>=mineCost();
            if(screen==0&&i==5)canBuy=coins>=100000;
            if(screen==1&&i<5)canBuy=coins>=bulkShopCost(i);
            if(screen==2&&i==0)canBuy=coins>=drillCost();
            if(screen==2&&i==1)canBuy=coins>=mineCost();
            rect(331,y+10,4,29,canBuy?.25f:.68f,canBuy?.83f:.29f,canBuy?.50f:.27f);
        }
        // Street lighting and gem glints, drawn as crisp pixel details.
        if(screen==0){
            for(int i=0;i<8;i++){
                float x=24+i*42;
                rect(x,374,24,2,.58f,.50f,.42f);
                rect(x+4,381,17,2,.24f,.28f,.37f);
            }
            float glow=(float)(.6+.3*Math.sin(elapsed*3));
            rect(47,469,9,20,.87f,.57f,.27f);
            rect(49,474,5,12,1f,glow,.42f);
            rect(299,469,9,20,.87f,.57f,.27f);
            rect(301,474,5,12,1f,glow,.42f);
        }else if(screen==2){
            for(int i=0;i<9;i++){
                float x=30+i*35,y=390+(i*17)%90;
                rect(x,y,7,13,.29f,.72f,.91f);
                rect(x+2,y+2,3,8,.76f,.93f,1f);
            }
        }
        // Independent scene particles, animated in world space.
        if(screen==0||screen==2){
            for(int i=0;i<18;i++){
                float speed=screen==2?9f:4f;
                float px=24+(i*59)%312+(float)Math.sin(elapsed*.45f+i)*5;
                float py=375+((i*43+(int)(elapsed*speed))%124);
                float shimmer=.55f+.35f*(float)Math.sin(elapsed*2+i);
                if(screen==0)rect(px,py,2,2,shimmer,.69f,.43f);
                else rect(px,py,3,3,.22f,shimmer,.95f);
            }
        }
        // Business cards have five distinct storefronts and real purchase targets.
        if(screen==1){
            for(int i=0;i<5;i++){
                float y=295-i*58;
                rect(17,y+3,324,43,.12f+i*.025f,.16f+i*.02f,.23f+i*.015f);
                rect(19,y+42,320,3,.60f,.43f,.24f);
                rect(25,y+11,28,27,.18f,.25f,.35f);
                rect(27,y+15,24,13,.28f+i*.08f,.42f,.52f-i*.045f);
                rect(26,y+28,26,7,.74f,.40f+i*.05f,.26f);
                rect(31,y+17,5,9,.93f,.76f,.50f);
                rect(42,y+17,5,9,.93f,.76f,.50f);
                boolean canBuy=coins>=bulkShopCost(i);
                boolean canHire=shops[i]>0&&managers[i]<3&&coins>=managerCost(i);
                rect(57,y+7,173,34,canBuy?.14f:.10f,canBuy?.29f:.15f,canBuy?.23f:.20f);
                rect(231,y+7,108,34,canHire?.19f:.18f,canHire?.42f:.22f,canHire?.33f:.27f);
                rect(231,y+39,108,3,canHire?.46f:.32f,canHire?.78f:.39f,canHire?.57f:.41f);
            }
        }
        // Upgrade iconography belongs to the non-business screens only.
        if(screen!=1){
        float[] ys={295,237,179,121,63,8};
        for(int i=0;i<ys.length;i++){
            float y=ys[i];
            rect(24,y+10,30,29,.18f,.27f,.40f);
            rect(27,y+13,24,23,.09f,.14f,.22f);
            if(i==0){rect(36,y+17,8,14,.98f,.73f,.37f);rect(33,y+29,14,4,1f,.86f,.53f);}
            else if(i==1){rect(35,y+26,10,8,.91f,.76f,.57f);rect(32,y+17,16,9,.38f,.77f,.72f);}
            else if(i==2){rect(30,y+23,20,5,.76f,.83f,.90f);rect(39,y+16,4,18,.70f,.48f,.30f);}
            else if(i==3){rect(33,y+18,15,15,.27f,.73f,.96f);rect(37,y+22,7,7,.80f,.96f,1f);}
            else if(i==4){for(int j=0;j<3;j++)rect(32+j*3,y+17+j*6,16-j*4,4,.91f,.70f,.43f);}
            else{rect(36,y+17,6,18,1f,.80f,.37f);rect(29,y+24,20,5,1f,.80f,.37f);}
            rect(280,y+10,58,29,.16f,.48f,.22f);
            rect(283,y+14,52,21,.30f,.79f,.32f);
        }
        }
        // Growth screen: four career stages with progress bars and milestone nodes.
        if(screen==3){
            rect(21,374,318,124,.07f,.10f,.17f);
            rect(29,405,300,4,.42f,.34f,.29f);
            int stage=Math.min(3,Math.max(tapLevel/30,rebirths));
            for(int i=0;i<4;i++){
                float x=37+i*86;
                boolean reached=i<=stage;
                rect(x,395,28,29,reached?.78f:.22f,reached?.58f:.27f,reached?.30f:.39f);
                rect(x+4,399,20,21,reached?.95f:.13f,reached?.78f:.18f,reached?.47f:.27f);
                rect(x+10,402,8,14,reached?.42f:.32f,reached?.30f:.38f,reached?.23f:.48f);
                if(i<3)rect(x+28,406,58,3,reached?.88f:.34f,reached?.65f:.38f,reached?.37f:.48f);
            }
            for(int i=0;i<3;i++){
                float y=295-i*58;
                rect(20,y+5,317,40,.13f,.18f,.29f);
                rect(20,y+42,317,3,.68f,.50f,.29f);
                rect(277,y+11,61,27,coins>=growthCost(i)?.62f:.30f,coins>=growthCost(i)?.46f:.32f,coins>=growthCost(i)?.24f:.38f);
            }
        }
        if(screen==0){
            rect(263,476,79,31,.32f,.24f,.16f);
            rect(266,480,73,23,.71f,.49f,.25f);
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
        float shake=screen==2&&mineShakeTimer>0?(float)Math.sin(elapsed*95)*mineShakeTimer*9:0;
        batch.draw(scene,18+shake,369,324,136);
        int tier=Math.min(3,Math.max(Math.max(0,tapLevel/30),rebirths));
        int actor=tier*2+((int)(elapsed*3)%2);
        batch.draw(actors[actor],149,389+((int)(elapsed*3)%2)*2+(flash>0?3:0),76,113);
        if(screen==0)batch.draw(streetForeground,18,369,324,136);
        if(screen==2)batch.draw(mineForeground,18+shake,369,324,136);
        if(screen==0)label("콘텐츠",274,495);
        label("거리",18,613);
        label("사업",105,613);
        label("광산",193,613);
        label("임무",281,613);
        label("돈 "+fmt(coins)+"  초당 +"+fmt(income()),28,575);
        label("보석 "+fmt(gems)+" 광물 "+fmt(ore)+" 영혼 "+fmt(souls),28,545);
        label(screen==1?"사업 구매 x"+businessBuyCount+" (터치 변경)":screen==2?biomeName():screen==3?(growthMode==0?"성장 능력":"임무 진행"):"터치해서 돈 획득 +"+fmt(tapPower()),52,501);
        label(screen==1?"사업 "+businessLevels()+"레벨":screen==2?"지하 "+depth+"층  연속 "+mineCombo+"회":screen==3?"임무 보상":rebirths>0?"환생 성장":"거리 노동",125,405);
        if(screen==0){
            label("터치 강화 "+tapLevel+" 비용 "+fmt(tapCost()),58,325);
            label("노동자 고용 "+workerLevel+" 비용 "+fmt(workerCost()),58,267);
            label("광산 채굴 "+depth+"층 +"+fmt(drillLevel*depth*biomeMultiplier()),58,209);
            label("채굴 강화 "+drillLevel+" 비용 "+fmt(drillCost()),58,151);
            label("지하층 확장 비용 "+fmt(mineCost()),58,93);
            label("환생 100K  "+rebirths+"회",58,37);
        } else if(screen==1){
            for(int i=0;i<5;i++){
                int y=326-i*58;
                int next=nextShopMilestone(shops[i]);
                label(shopNames[i]+" Lv."+shops[i]+(next==0?" MAX":" / "+next),60,y+5);
                label("초당 +"+fmt(shopIncome(i)),60,y-13);
                label("구매 x"+businessBuyCount+" "+fmt(bulkShopCost(i)),60,y-27);
                label("관리 Lv."+managers[i]+(managers[i]>=3?" MAX":" "+fmt(managerCost(i))),237,y+5);
                label(shopSpecialName(i)+" x"+String.format(java.util.Locale.US,"%.1f",shopSpecial(i)),237,y-14);
            }
            label("출석 보상 받기",58,37);
        } else if(screen==3){
            if(growthMode==0){
                label("터치 수익 Lv."+growth[0]+" "+fmt(growthCost(0)),58,325);
                label("자동 수익 Lv."+growth[1]+" "+fmt(growthCost(1)),58,267);
                label("전체 수익 Lv."+growth[2]+" "+fmt(growthCost(2)),58,209);
                label("임무 보상 화면",58,151);
                label("출석 보상 받기",58,93);
                label("거리로 돌아가기",58,37);
            }else{
                label("임무 보상 받기 "+(questTier+1),58,325);
                label("출석 보상 받기",58,267);
                label("터치 "+Math.max(0,totalTaps-questTapBase)+"/"+questTapTarget(),58,209);
                label("성장 능력 화면",58,151);
                label("완료 "+missions+"  보석 "+fmt(gems),58,93);
                label("거리로 돌아가기",58,37);
            }
        } else {
            label("곡괭이 강화 "+drillLevel+" "+fmt(drillCost()),58,325);
            label("더 깊이 "+depth+"층 "+fmt(mineCost()),58,267);
            label("광물 "+fmt(ore)+" 판매 +"+fmt(ore*(2+depth)),58,209);
            label("보석 10개 교환",58,151);
            label("출석 보상",58,93);
            label("거리로 돌아가기",58,37);
        }
        if(tapFloatTimer>0 && screen==0)label("+"+fmt(lastTapEarned),147,463+tapFloatTimer*32);
        if(screen==2&&mineRewardTimer>0){
            float rise=(1.3f-mineRewardTimer)*29;
            String reward=mineRewardKind==2?"보물 +"+fmt(mineRewardAmount):mineRewardKind==1?"보석 발견!":"광물 +"+fmt(mineRewardAmount);
            label(reward,102,464+rise);
        }
        label(notice,18,351);
        batch.end();
        // Overlay particles after the SpriteBatch scene so effects are never hidden behind terrain.
        if(effectTimer>0||(screen==2&&mineImpactTimer>0)){
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
        if(effectTimer>0){
            float spread=(.5f-effectTimer)*110;
            for(int i=0;i<8;i++){
                float a=i*6.2831853f/8f;
                float px=effectX+(float)Math.cos(a)*spread;
                float py=effectY+(float)Math.sin(a)*spread;
                if(px>12&&px<348&&py>360&&py<510)
                    rect(px,py,5,5,effectKind==1?.35f:1f,effectKind==1?.82f:.78f,effectKind==1?1f:.28f);
            }
        }
        // Mining impact shockwave, falling fragments, and floating reward sparks.
        if(screen==2&&mineImpactTimer>0){
            float progress=1f-mineImpactTimer/.34f;
            float radius=9+progress*72;
            for(int i=0;i<16;i++){
                float a=i*6.2831853f/16f;
                float px=180+(float)Math.cos(a)*radius;
                float py=447+(float)Math.sin(a)*radius*.65f;
                if(px>22&&px<338&&py>374&&py<503){
                    rect(px,py,5,5,i%3==0?.90f:.36f,i%3==0?.83f:.74f,i%3==0?.57f:.96f);
                    rect(px+2,py+6,2,3,.74f,.89f,1f);
                }
            }
            for(int i=0;i<11;i++){
                float x=130+(i*17)%110+(float)Math.sin(i*2.1f)*progress*24;
                float y=457+(i*19)%39-progress*(22+i%4*7);
                if(y>371)rect(x,y,4+i%3,4+i%3,.39f,.62f,.78f);
            }
            if(progress<.3f)rect(127,420,106,48,.27f,.46f,.61f);
        }
            shapes.end();
        }
    }
    @Override public void pause(){persist();}
    @Override public void dispose(){persist();font.dispose();if(koreanAtlas!=null)koreanAtlas.dispose();streetSprite.dispose();if(streetForeground!=null)streetForeground.dispose();if(mineForeground!=null)mineForeground.dispose();if(streetArt!=null)streetArt.dispose();mineSprite.dispose();shopSprite.dispose();for(Texture t:actors)t.dispose();if(pngActor!=null)pngActor.dispose();batch.dispose();shapes.dispose();}
}
