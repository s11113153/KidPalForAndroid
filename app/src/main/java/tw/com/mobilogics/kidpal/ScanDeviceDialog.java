package tw.com.mobilogics.kidpal;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;


public class ScanDeviceDialog extends Activity {

  private static AnimationDrawable mAnimationDrawable;
  private static Handler mHandler;
  private static ServiceBLE mServiceBLE;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(tw.com.mobilogics.kidpal.R.layout.dialog_scan_device_dialog);
    ImageView anim = (ImageView) findViewById(tw.com.mobilogics.kidpal.R.id.anim);
    Drawable drawable = getResources().getDrawable(tw.com.mobilogics.kidpal.R.drawable.anim_scan_device_dialog);
    //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.anim_scan_device_dialog);
    //ByteArrayOutputStream stream = new ByteArrayOutputStream();
    //bitmap.compress(Bitmap.CompressFormat.JPEG,10,stream);
    anim.setAlpha(0.9F);
    anim.setBackground(drawable);
    //anim.setImageBitmap(bitmap);
    mAnimationDrawable = (AnimationDrawable)anim.getBackground();
    if (!mAnimationDrawable.isRunning()) {
      mAnimationDrawable.start();

      mServiceBLE.setHandler(mHandler);
      mServiceBLE.scan();
      mHandler.postDelayed(new Runnable() {
        @Override
        public void run() {
          mServiceBLE.stop();
          if (mAnimationDrawable != null) {
            mAnimationDrawable.stop();
            mAnimationDrawable = null;
          }
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
