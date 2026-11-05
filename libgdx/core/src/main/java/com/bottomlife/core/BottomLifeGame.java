package com.bottomlife.core;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
public class BottomLifeGame extends ApplicationAdapter {
  private ShapeRenderer shapes;
  private OrthographicCamera camera;
  private Viewport viewport;
  private final Vector3 touch = new Vector3();
  private int taps = 0;
  @Override public void create() {
    shapes = new ShapeRenderer();
    camera = new OrthographicCamera();
    viewport = new FitViewport(360, 640, camera);
    viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
  }
  @Override public void resize(int width, int height) { viewport.update(width,height,true); }
  @Override public void render() {
    if (Gdx.input.justTouched()) { touch.set(Gdx.input.getX(), Gdx.input.getY(),0); viewport.unproject(touch); taps++; }
    Gdx.gl.glClearColor(0.055f,0.07f,0.12f,1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    viewport.apply();
    shapes.setProjectionMatrix(camera.combined);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    shapes.setColor(0.13f,0.18f,0.27f,1);
    shapes.rect(15,15,330,610);
    shapes.setColor(0.95f,0.72f,0.29f,1);
    shapes.rect(35,420,290,175);
    shapes.setColor(0.19f,0.30f,0.44f,1);
    shapes.rect(45,430,270,155);
    shapes.setColor(0.42f,0.82f,0.67f,1);
    shapes.rect(75,470,210,40);
    shapes.setColor(0.95f,0.72f,0.29f,1);
    for(int i=0;i<Math.min(taps%20,20);i++) shapes.rect(55+i*13,360,8,20);
    shapes.end();
  }
  @Override public void dispose() { shapes.dispose(); }
}
