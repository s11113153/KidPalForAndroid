package tw.com.mobilogics.testhellohandy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class WelcomeActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.view_welcome);

    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        finish();
        startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
      }
    }, 10);
  }
}
