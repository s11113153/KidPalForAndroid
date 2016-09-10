package tw.com.mobilogics.kidpal;

import com.imageframeanimation.AnimationSpeed;
import com.imageframeanimation.AnimatorControl;
import com.imageframeanimation.FrameImageAnimator;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;


public class ScanDeviceDialog extends Activity {

  public static int[] mImageToAnimate = new int[] {
    R.drawable.ic_scan_flash1,
    R.drawable.ic_scan_flash2,
    R.drawable.ic_scan_flash3,
    R.drawable.ic_scan_flash4,
    R.drawable.ic_scan_flash5,
    R.drawable.ic_scan_flash6,
    R.drawable.ic_scan_flash7,
    R.drawable.ic_scan_flash8,
  };

  private static FrameImageAnimator mAnimateImageView;
  private static AnimatorControl mAnimatorControl;

  private static Handler mHandler;
  private static ServiceBLE mServiceBLE;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_scan_device);
    mAnimateImageView = (FrameImageAnimator) findViewById(R.id.animater_IV);

    mAnimatorControl = new AnimatorControl();

    // Setting the Animation Speed (SLOW,MEDIUM or FAST)
    final int mAnimationSpeed = AnimationSpeed.FAST;

    // Applying the Images to Animate with the speed AnimationControl
    mAnimatorControl.mApplyFrames(mImageToAnimate, ScanDeviceDialog.this,
      mAnimateImageView, mAnimationSpeed);

    // Starts The Animation
    mAnimatorControl.start();

    mServiceBLE.setHandler(mHandler);
    mServiceBLE.scan();

    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        mServiceBLE.stop();
        finish();
      }
    }, 3000);

  }

  public static void setServiceBLE(ServiceBLE serviceBLE) {
    mServiceBLE = serviceBLE;
  }

  public static void setHandler(Handler handler) {
    mHandler = handler;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) { return false; }
}
