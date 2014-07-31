package tw.com.mobilogics.testhellohandy;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;



public class ScanDeviceDialog extends Activity {

  private static AnimationDrawable mAnimationDrawable;
  private static Handler mHandler;
  private static ServiceBLE mServiceBLE;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_scan_device_dialog);
    ImageView anim = (ImageView) findViewById(R.id.anim);
    Drawable drawable = getResources().getDrawable(R.drawable.anim_scan_device_dialog);
    anim.setAlpha(0.9F);
    anim.setBackground(drawable);
    mAnimationDrawable = (AnimationDrawable)anim.getBackground();
    if (!mAnimationDrawable.isRunning()) {
      mAnimationDrawable.start();

      mServiceBLE.setHandler(mHandler);
      mServiceBLE.scan();
      mHandler.postDelayed(new Runnable() {
        @Override
        public void run() {
          mServiceBLE.stop();
          mAnimationDrawable.stop();
          finish();
        }
      }, 3000);
    }
  }

  public static void setServiceBLE(ServiceBLE serviceBLE) {
    mServiceBLE = serviceBLE;
  }

  public static void setHandler(Handler handler) {
    mHandler = handler;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return false;
    //return super.onKeyDown(keyCode, event);
  }
}
