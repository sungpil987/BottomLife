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
    private float timer, saveTimer, flash;
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
        timer+=delta;saveTimer+=delta;flash=Math.max(0,flash-delta);
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
        rect(8,8,344,624,.10f,.14f,.22f);
        rect(16,524,328,99,.15f,.21f,.31f);
        rect(16,360,328,150,.24f,.30f,.40f);
        rect(27,371,306,128,.10f,.15f,.23f);
        rect(144,386,72,88,.78f,.60f,.39f);
        rect(157,448,46,43,.94f,.77f,.53f);
        rect(137,393,86,25,.20f,.54f,.64f);
        if(flash>0)rect(28,374,304,120,.25f,.43f,.32f);
        float[][] buttons={{295,345},{237,287},{179,229},{121,171},{63,113},{8,55}};
        for(int i=0;i<buttons.length;i++)rect(16,buttons[i][0],328,buttons[i][1]-buttons[i][0],i==5?.43f:.17f,i==5?.29f:.24f,i==5?.22f:.35f);
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        label("BOTTOM LIFE 3.0",28,606);
        label("Coins: "+fmt(coins)+"   +"+fmt(income())+"/sec",28,575);
        label("Gems "+fmt(gems)+"   Ore "+fmt(ore)+"   Souls "+fmt(souls),28,545);
        label("TAP TO EARN  +"+fmt(tapPower()),55,481);
        label("Street hustle",98,451);
        label("Tap Lv."+tapLevel+"   COST "+fmt(tapCost()),28,325);
        label("Workers Lv."+workerLevel+"   COST "+fmt(workerCost()),28,267);
        label("MINE  Depth "+depth+"   +"+fmt(drillLevel*depth)+" ore",28,209);
        label("Drill Lv."+drillLevel+"   COST "+fmt(drillCost()),28,151);
        label("Dig deeper   COST "+fmt(mineCost()),28,93);
        label("REBIRTH   100K coins   x"+rebirths,28,37);
        label(notice,18,351);
        batch.end();
    }
    @Override public void pause(){persist();}
    @Override public void dispose(){persist();font.dispose();batch.dispose();shapes.dispose();}
}
