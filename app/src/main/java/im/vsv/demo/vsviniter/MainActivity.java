package im.vsv.demo.vsviniter;

import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import im.vsv.demo.vsviniter.utils.VsvIniter;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    // use in application.
    if (VsvIniter.init(this)) {
      loadLib("ijkffmpeg");
    }
  }

  private void loadLib(String libName) {
    libName = System.mapLibraryName(libName);
    Log.e("vsv", "loadLib: " + VsvIniter.getLibPath(this) + libName);
    System.load(VsvIniter.getLibPath(this) + libName);
  }
}
