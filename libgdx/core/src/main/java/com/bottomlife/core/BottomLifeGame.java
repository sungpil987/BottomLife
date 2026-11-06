package com.bottomlife.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.GL20;
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
    private OrthographicCamera camera;
    private Viewport viewport;
    private Preferences save;
    private final Vector3 touch = new Vector3();
    private double coins, gems, ore, souls;
    private int tapLevel, workerLevel, drillLevel, depth, rebirths;
    private float timer, saveTimer, flash, elapsed;
    private String notice = "Tap the street to earn!";
    private static final float W=360, H=640;

    @Override public void create() {
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.35f);
        camera = new OrthographicCamera();
        viewport = new FitViewport(W,H,camera);
        viewport.update(Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),true);
        save = Gdx.app.getPreferences("bottomlife_libgdx_v1");
        coins=Double.longBitsToDouble(save.getLong("coins",0L)); gems=Double.longBitsToDouble(save.getLong("gems",0L));
        ore=Double.longBitsToDouble(save.getLong("ore",0L)); souls=Double.longBitsToDouble(save.getLong("souls",0L));
        tapLevel=save.getInteger("tap",1); workerLevel=save.getInteger("workers",0);
        drillLevel=save.getInteger("drill",1); depth=save.getInteger("depth",1);
        rebirths=save.getInteger("rebirths",0);
        long last=save.getLong("last",System.currentTimeMillis());
        double seconds=Math.min(14400,Math.max(0,(System.currentTimeMillis()-last)/1000.0));
        coins+=seconds*income()*0.5;
        if(seconds>60) notice="Offline earnings collected!";
    }
    private double multiplier(){return 1+souls*0.15;}
    private double tapPower(){return Math.ceil((1+tapLevel*2)*multiplier());}
    private double income(){return workerLevel*(1+workerLevel*0.45)*multiplier();}
    private double tapCost(){return Math.ceil(20*Math.pow(1.22,tapLevel-1));}
    private double workerCost(){return Math.ceil(50*Math.pow(1.3,workerLevel));}
    private double drillCost(){return Math.ceil(30*Math.pow(1.25,drillLevel-1));}
    private double mineCost(){return Math.ceil(10*Math.pow(1.2,depth-1));}
    private void persist(){
        save.putLong("coins",Double.doubleToRawLongBits(coins)); save.putLong("gems",Double.doubleToRawLongBits(gems));
        save.putLong("ore",Double.doubleToRawLongBits(ore)); save.putLong("souls",Double.doubleToRawLongBits(souls));
        save.putInteger("tap",tapLevel); save.putInteger("workers",workerLevel);
        save.putInteger("drill",drillLevel); save.putInteger("depth",depth);
        save.putInteger("rebirths",rebirths);
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
        if(coins<price){notice="Not enough coins!";return;}
        coins-=price;
        if(action==0)tapLevel++;
        if(action==1)workerLevel++;
        if(action==2)drillLevel++;
        if(action==3)depth++;
        notice="Upgrade purchased!";
        persist();
    }
    private void input(float x,float y){
        if(y>=360 && y<=510){coins+=tapPower();flash=0.16f;notice="+"+fmt(tapPower())+" coins";return;}
        if(y>=295 && y<345)buy(tapCost(),0);
        else if(y>=237 && y<287)buy(workerCost(),1);
        else if(y>=179 && y<229){
            ore+=drillLevel*depth;
            if(Math.random()<Math.min(0.45,0.05+depth*0.005))gems++;
            notice="Mined +"+fmt(drillLevel*depth)+" ore";
            persist();
        }
        else if(y>=121 && y<171)buy(drillCost(),2);
        else if(y>=63 && y<113)buy(mineCost(),3);
        else if(y>=8 && y<55){
            if(coins<100000){notice="Rebirth requires 100K coins";return;}
            double gain=Math.max(1,Math.floor(Math.sqrt(coins/100000)));
            souls+=gain;rebirths++;
            coins=0;ore=0;tapLevel=1;workerLevel=0;drillLevel=1;depth=1;
            notice="Reborn! +"+fmt(gain)+" souls";persist();
        }
    }
    @Override public void resize(int width,int height){viewport.update(width,height,true);}
    private void rect(float x,float y,float w,float h,float r,float g,float b){
        shapes.setColor(r,g,b,1);shapes.rect(x,y,w,h);
    }
    private void label(String s,float x,float y){
        font.draw(batch,s,x,y);
    }
    @Override public void render(){
        float delta=Math.min(Gdx.graphics.getDeltaTime(),0.1f);
        timer+=delta;saveTimer+=delta;elapsed+=delta;flash=Math.max(0,flash-delta);
        if(timer>=0.1f){coins+=income()*timer;timer=0;}
        if(saveTimer>=10){persist();saveTimer=0;}
        if(Gdx.input.justTouched()){
            touch.set(Gdx.input.getX(),Gdx.input.getY(),0);
            viewport.unproject(touch);input(touch.x,touch.y);
        }
        Gdx.gl.glClearColor(.045f,.065f,.11f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Layered pixel-art city: skyline, windows, street, animated character.
        rect(8,8,344,624,.08f,.11f,.18f);
        rect(12,522,336,106,.12f,.18f,.30f);
        for(int i=0;i<8;i++){
            int x=14+i*44; int h=22+(i*37%48);
            rect(x,525,38,h,.16f+(i%3)*.025f,.23f,.34f);
            for(int wy=530;wy<525+h-4;wy+=13)
                for(int wx=x+6;wx<x+34;wx+=12)
                    if((wx+wy+i)%3!=0)rect(wx,wy,5,7,.94f,.73f,.38f);
        }
        rect(12,360,336,152,.16f,.24f,.31f);
        rect(17,366,326,139,.22f,.32f,.39f);
        rect(21,371,318,128,.30f,.36f,.39f);
        rect(21,371,318,32,.23f,.26f,.29f);
        for(int i=0;i<7;i++)rect(30+i*48,385,24,3,.83f,.78f,.56f);
        // Shop front and its glowing sign.
        rect(26,422,96,73,.35f,.25f,.27f);
        rect(32,430,84,50,.15f,.22f,.31f);
        rect(26,481,96,13,.93f,.56f,.29f);
        for(int i=0;i<5;i++)rect(33+i*17,482,9,10,.99f,.80f,.48f);
        rect(250,417,73,78,.24f,.26f,.35f);
        rect(257,427,59,48,.12f,.16f,.24f);
        rect(262,438,49,6,.77f,.64f,.39f);
        // Pixel character with a two-frame idle animation.
        float bob=((int)(elapsed*3)%2==0)?0:2;
        float cx=174, cy=414+bob;
        rect(cx-20,cy-3,14,9,.14f,.16f,.23f);
        rect(cx+7,cy-3,14,9,.14f,.16f,.23f);
        rect(cx-20,cy+5,40,35,.23f,.60f,.67f);
        rect(cx-26,cy+12,8,20,.92f,.70f,.48f);
        rect(cx+19,cy+12,8,20,.92f,.70f,.48f);
        rect(cx-16,cy+40,32,30,.95f,.75f,.53f);
        rect(cx-18,cy+64,36,10,.22f,.17f,.19f);
        rect(cx-20,cy+56,8,12,.22f,.17f,.19f);
        rect(cx-9,cy+54,4,4,.11f,.13f,.18f);
        rect(cx+7,cy+54,4,4,.11f,.13f,.18f);
        rect(cx-4,cy+45,9,3,.60f,.34f,.31f);
        // Coin sparkle and tap feedback.
        float sparkle=(float)Math.sin(elapsed*4);
        rect(214,465+sparkle*3,14,14,.99f,.78f,.30f);
        rect(218,468+sparkle*3,6,8,1f,.94f,.57f);
        if(flash>0){
            rect(124,477,108,5,1f,.87f,.45f);
            rect(132,407,96,5,1f,.87f,.45f);
        }
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
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        label("BOTTOM LIFE 3.1",28,606);
        label("Coins: "+fmt(coins)+"   +"+fmt(income())+"/sec",28,575);
        label("Gems "+fmt(gems)+"   Ore "+fmt(ore)+"   Souls "+fmt(souls),28,545);
        label("TAP TO EARN  +"+fmt(tapPower()),72,501);
        label("STREET HUSTLE",104,405);
        label("TAP Lv."+tapLevel+"   "+fmt(tapCost()),58,325);
        label("WORKERS Lv."+workerLevel+"   "+fmt(workerCost()),58,267);
        label("MINE Depth "+depth+"   +"+fmt(drillLevel*depth)+" ore",58,209);
        label("DRILL Lv."+drillLevel+"   "+fmt(drillCost()),58,151);
        label("DIG DEEPER   "+fmt(mineCost()),58,93);
        label("REBIRTH 100K   x"+rebirths,58,37);
        label(notice,18,351);
        batch.end();
    }
    @Override public void pause(){persist();}
    @Override public void dispose(){persist();font.dispose();batch.dispose();shapes.dispose();}
}
