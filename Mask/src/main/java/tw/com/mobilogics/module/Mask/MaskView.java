package tw.com.mobilogics.module.Mask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MaskView extends View {

  /*** 記錄當前操作的狀態值*/
  private int currentStatus;

  public static final int STATUS_ZOOM_INIT = 0;

  /*** 圖片放大狀態值 */
  public static final int STATUS_ZOOM_OUT = 1;

  /*** 圖片縮小狀態值 */
  public static final int STATUS_ZOOM_IN = 2;

  /*** 圖片拖動狀態值 */
  public static final int STATUS_MOVE = 3;

  private static Bitmap mBitmapSource , mBitmapMask;

  private static Matrix mMatrix = new Matrix();

  /*** View 元件的寬度 */
  private int VIEW_WIDTH;

  /*** View 元件的高度 */
  private int VIEW_HEIGHT;

  /*** 記錄圖片在矩陣上的橫向偏移量 */
  private float totalTranslateX;

  /*** 記錄圖片在矩陣上的縱向偏移量 */
  private float totalTranslateY;

  /*** 記錄圖片在矩陣上的總縮放比例 */
  private float totalRatio = 1;
  /*** 記錄手指移動的距離所造成的縮放比例 */
  private float scaledRatio = 1;

  /*** 記錄圖片初始化時的縮放比例 */
  private float initRatio = 1;

  /*** 記錄上次兩指之間的距離 */
  //private float lastFingerDis = 1;
  private float lastFingerDis = -1;

  /*** 記錄當前圖片的寬度, 圖片被縮放時,這個值會一起變動 */
  private float currentBitmapWidth;

  /*** 記錄當前圖片的高度, 圖片被縮放時,這個值會一起變動 */
  private float currentBitmapHeight;

  /*** 記錄兩指同時放在屏幕上時, 中心點的x坐標值 */
  private float centerPointX;

  /*** 記錄兩指同時放在屏幕上時, 中心點的y坐標值 */
  private float centerPointY;

  /*** 記錄手指在在x坐標方向上的移動距離 */
  private float movedDistanceX;

  /*** 記錄手指在在y坐標方向上的移動距離 */
  private float movedDistanceY;

  /*** 記錄上次手指移動時的x坐標 */
  private float lastXMove = -1;

  /*** 記錄上次手指移動時的y坐標 */
  private float lastYMove = -1;

  public MaskView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (changed) {
      VIEW_WIDTH = getWidth();
      VIEW_HEIGHT = getHeight();
      currentStatus = STATUS_ZOOM_INIT;
    }
  }

  /** 設定背景來源照片 */
  public MaskView setSourceBitmap(Bitmap source) {
    mBitmapSource = source;
    return this;
  }

  public void startDraw() {
    invalidate();
  }

  /** 設定遮罩物件 */
  public MaskView setMaskBitmap(Bitmap mask) {
    mBitmapMask = mask;
    return this;
  }

  /** 計算兩手指之間中心點坐標 */
  private void calCenterPointBetweenFingers(MotionEvent event) {
    float first  [] = {event.getX(0), event.getY(0)};
    float second [] = {event.getX(1), event.getY(1)};
    centerPointX = (first[0] + second[0]) / 2;
    centerPointY = (first[1] + second[1]) / 2;
  }


  /** 計算兩手指之間的距離(畢氏定理) */
  private float calDistanceBetweenFingers(MotionEvent event) {
    float disX = Math.abs(event.getX(0) - event.getX(1));
    float disY = Math.abs(event.getY(0) - event.getY(1));
    return (float)Math.sqrt(disX * disX + disY * disY);
  }

  /** 處理圖片移動 */
  private void move() {
    mMatrix.reset();
    // 根據手指移動的距離計算出偏移量
    float translateX = totalTranslateX + movedDistanceX;
    float translateY = totalTranslateY + movedDistanceY;

    // 圖片按照現在的縮放比例縮放
    mMatrix.postScale(totalRatio, totalRatio);

    // 根據移動距離進行偏移
    mMatrix.postTranslate(translateX, translateY);
    totalTranslateX = translateX;
    totalTranslateY = translateY;
  }

  /** 對圖片縮放處理 */
  private void zoom() {
    mMatrix.reset();
    // 將圖片按照比例縮放
    mMatrix.postScale(totalRatio, totalRatio);

    float scaledWidth = mBitmapSource.getWidth() * totalRatio;
    float scaledHeight = mBitmapSource.getHeight() * totalRatio;
    float translateX = 0f;
    float translateY = 0f;
    // 圖片寬度小於當前螢幕寬度時 , 按照螢幕中心的橫坐標水平縮放, 否則根據按兩手指的中心點的橫坐標水平縮放
    if (currentBitmapWidth < VIEW_WIDTH) {
      translateX = (VIEW_WIDTH - scaledWidth) / 2f;

    } else {
      //translateX = totalTranslateX * scaledRatio + centerPointX * (1 - scaledRatio);

      // 圖片縮放後在水平方向不會偏移出螢幕
      if (translateX > 0) {
        translateX = 0;
      } else if (VIEW_WIDTH - translateX > scaledWidth) {
        translateX = VIEW_WIDTH - scaledWidth;
      }
    }
    // 圖片高度小於當前螢幕高度時 , 按照螢幕中心的縱坐標垂直縮放, 否則根據按兩手指的中心點的縱坐標垂直縮放
    if (currentBitmapHeight < VIEW_HEIGHT) {
      translateY = (VIEW_HEIGHT - scaledHeight) / 2f;
    } else {
      translateY = totalTranslateY * scaledRatio + centerPointY * (1 - scaledRatio);
      // 圖片縮放後在垂直方向不會偏移出螢幕
      if (translateY > 0) {
        translateY = 0;
      } else if (VIEW_HEIGHT - translateY > scaledHeight) {
        translateY = VIEW_HEIGHT - scaledHeight;
      }
    }
    // 對縮放後圖片偏移, 讓縮放後的中心點位置不變
    mMatrix.postTranslate(translateX, translateY);
    totalTranslateX = translateX;
    totalTranslateY = translateY;
    currentBitmapWidth = scaledWidth;
    currentBitmapHeight = scaledHeight;
  }


  @Override
  public boolean onTouchEvent(MotionEvent event) {
    // 範圍如果不為遮照範圍 return
    //if ( event.getX() < MASK_LEFT || event.getX() > MASK_LEFT + MASK_WIDTH ) return false;
    //if ( event.getY() < MASK_TOP  || event.getY() > MASK_TOP + MASK_HEIGHT)  return false;

    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_POINTER_DOWN:
        if (event.getPointerCount() == 2) {
          // 計算兩手指間的距離
          lastFingerDis = calDistanceBetweenFingers(event);
        }
        break;
      case MotionEvent.ACTION_MOVE:
        if (event.getPointerCount() == 1) { // 單手拖動狀態

          //手指觸發的位置
          float xMove = event.getX();
          float yMove = event.getY();

          //初始化為一開始點的位置
          if (lastXMove == -1 && lastYMove == -1) {
            lastXMove = xMove;
            lastYMove = yMove;
          }
          // 更改當前狀態
          currentStatus = STATUS_MOVE;

          movedDistanceX = xMove - lastXMove;
          movedDistanceY = yMove - lastYMove;
          invalidate();
          lastXMove = xMove;
          lastYMove = yMove;
        } else if (event.getPointerCount() == 2) {
          // 兩手指縮放
          calCenterPointBetweenFingers(event);
          float fingerDis = calDistanceBetweenFingers(event);
          if (fingerDis > lastFingerDis) {
            currentStatus = STATUS_ZOOM_OUT;
          } else {
            currentStatus = STATUS_ZOOM_IN;
          }
          // 圖片最大允許放大4倍, 最小可以到初始化比例
          if ((currentStatus == STATUS_ZOOM_OUT && totalRatio < 4 * initRatio)
            || (currentStatus == STATUS_ZOOM_IN && totalRatio > initRatio)) {
            scaledRatio = (float) (fingerDis / lastFingerDis);
            totalRatio = totalRatio * scaledRatio;
            if (totalRatio > 4 * initRatio) {
              totalRatio = 4 * initRatio;
            } else if (totalRatio < initRatio) {
              totalRatio = initRatio;
            }
            invalidate();
            lastFingerDis = fingerDis;
          }
        }
        break;
      case MotionEvent.ACTION_POINTER_UP:
        // 兩手指同時縮放完之後, 兩手指釋放
        if (event.getPointerCount() == 2) {
          lastXMove = -1;
          lastYMove = -1;
        }
        break;
      case MotionEvent.ACTION_UP:
        // 手指最後一個離開螢幕時
        lastXMove = -1;
        lastYMove = -1;
        break;
      default:
        break;
    }
    return true;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    switch (currentStatus) {
      case STATUS_ZOOM_INIT : {
        mMatrix.reset();
        if (mBitmapSource == null) return;
        if (mBitmapSource.getHeight() > mBitmapSource.getWidth()) {
          int w = (VIEW_WIDTH - mBitmapSource.getWidth()) / 2;
          int h = (VIEW_HEIGHT - mBitmapSource.getHeight()) / 2;
          mMatrix.postTranslate(w,h);
          totalTranslateX = w;
          totalTranslateY = h;
        }else {
          int h = (VIEW_HEIGHT - mBitmapSource.getHeight()) / 2;
          mMatrix.postTranslate(0,h);
          totalTranslateX = 0;
          totalTranslateY = h;

        }
      } break;
      case STATUS_ZOOM_OUT :
      case STATUS_ZOOM_IN :
        zoom();
        break;
      case STATUS_MOVE :
        move();
        break;
      default:
        break;
    }
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setFilterBitmap(true);
    canvas.drawBitmap(mBitmapSource, mMatrix, paint);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
  }

  /**
   * 處理最下層要被裁的Bitmap(通常是相片)
   * 底層根據使用者滑動來調整
   * 最後裁圖的時候會移動到 (使用getHandleMaskBitmap的mask Bitmap(0,0)的下面) 進行裁圖
   */
  private Bitmap getHandleSourceBitmap() {
    this.setDrawingCacheEnabled(true);
    this.buildDrawingCache();
    Bitmap bmp = Bitmap.createBitmap(this.getDrawingCache());
    this.setDrawingCacheEnabled(false);
    int width =  (this.getWidth()  - mBitmapMask.getWidth())  / 2;
    int height = (this.getHeight() - mBitmapMask.getHeight()) / 2;
    Matrix matrix = new Matrix();
    matrix.postTranslate( width * -1,  height * -1);
    Bitmap tmpBitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(tmpBitmap);
    canvas.drawBitmap(bmp ,matrix, new Paint());
    return tmpBitmap;
  }


  /**
   * 處理傳進來的 mBitmapMask 並回傳調整過後的 bitmap
   * 調整過後的maskBitmap起始位置為(0,0)
   */
  private Bitmap getHandleMaskBitmap() {
    //mBitmapOutPutMask = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
    Bitmap bitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    //int width = (this.getWidth()  - mBitmapMask.getWidth())   / 2;
    //int height = (this.getHeight() - mBitmapMask.getHeight())  / 2;
    canvas.drawBitmap(mBitmapMask, new Matrix(), new Paint());
    return bitmap;
  }

  /**
   * 進行裁切並回傳裁切後的圖片
   */
  public Bitmap getOutputBitmap() {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setFilterBitmap(true);
    Bitmap bitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawBitmap(getHandleSourceBitmap(), 0, 0, paint);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    canvas.drawBitmap(getHandleMaskBitmap(), 0, 0, paint);
    return bitmap;
  }
}


