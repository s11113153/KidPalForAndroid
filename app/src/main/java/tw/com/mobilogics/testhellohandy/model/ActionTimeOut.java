package tw.com.mobilogics.testhellohandy.model;

import android.util.Log;

// 在設定的時間過後就會關閉, 期間並不動作, 只是個定時器功能
public abstract class ActionTimeOut extends Thread implements Runnable {

  private static final String TAG = ActionTimeOut.class.getName();

  // 限制時間
  private long mTimeOut;

  // 是否運行中
  private boolean isStatus;

  private ActionTimeOut() {}

  public ActionTimeOut(long timeOut) {
    mTimeOut = timeOut;
    isStatus = true;
    start();
  }

  abstract public void onTimeOut();

  @Override
  public synchronized void start() {
    if (mTimeOut > 0) super.start();
  }

  @Override
  public void run() {
    long nowTime = System.currentTimeMillis();
    while (true) {
      if ( System.currentTimeMillis() - nowTime < mTimeOut && isStatus) {
        try {
          Thread.sleep(1000);
        }catch (InterruptedException e) { e.printStackTrace(); }
        continue;
      }
      break;
    }
    if (isStatus) onTimeOut();
  }
  public void close() { isStatus = false; }
}
