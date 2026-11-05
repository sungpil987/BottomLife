package com.bottomlife.rebuild;
import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.bottomlife.core.BottomLifeGame;
public class AndroidLauncher extends AndroidApplication {
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
    config.useImmersiveMode = true;
    initialize(new BottomLifeGame(), config);
  }
}
