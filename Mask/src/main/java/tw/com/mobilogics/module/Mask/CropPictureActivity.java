package tw.com.mobilogics.module.Mask;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TooManyListenersException;


public class CropPictureActivity extends Activity {
  private final static String TAG = CropPictureActivity.class.getName();
  private static Bitmap mCropBitmap;
  private MaskView mMaskView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_crop_picture);
    mMaskView = (MaskView) findViewById(R.id.mask_view);
    setResult(RESULT_CANCELED);

    try {
      // 將來源的的相片進行壓縮完之後再裁切圖片(利用遮罩)
      ContentResolver cr = this.getContentResolver();
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = false;
      options.inPreferredConfig = Bitmap.Config.RGB_565;
      options.inDither = true;
      options.inSampleSize = 8;

      Bitmap source;
      Bitmap mask = BitmapFactory.decodeResource(getResources(), R.drawable.oval_02);
      Uri uri = getIntent().getData();
      if (uri != null) {
        source = BitmapFactory.decodeStream(cr.openInputStream(uri), new Rect(), options);
        source.compress(Bitmap.CompressFormat.JPEG, 10, new ByteArrayOutputStream());
        mMaskView.setSourceBitmap(getResizedBitmap(source,getPhotoRotate(cr, uri)))
          .setMaskBitmap(mask).startDraw();
        //mMaskView.setSourceBitmap(source).setMaskBitmap(mask).startDraw();
      }else {
        source = (Bitmap) getIntent().getExtras().getParcelable("bitmap");
        Matrix matrix = new Matrix();
        matrix.postScale(2.0F, 2.0F);
        source = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        mMaskView.setSourceBitmap(source).setMaskBitmap(mask).startDraw();
      }
    } catch (FileNotFoundException e) {
      Log.e(TAG, "throw FileNotFoundException");
      e.printStackTrace();
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
  }

  /**
   * 得到相片的rotate大小
   */
  public int getPhotoRotate(ContentResolver cr, Uri uri) {
    Cursor cursor = cr.query(uri,
      new String[]{MediaStore.Images.ImageColumns.ORIENTATION},
      null, null, null);
    try {
      if (cursor.moveToFirst()) {
        return cursor.getInt(0);
      } else {
        return -1;
      }
    } finally {
      cursor.close();
    }
  }
  public static int getImageOrientation(String imagePath){
    int rotate = 0;
    try {
      File imageFile = new File(imagePath);
      ExifInterface exif = new ExifInterface(
        imageFile.getAbsolutePath());
      int orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL);

      switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_270:
          rotate = 270;
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          rotate = 180;
          break;
        case ExifInterface.ORIENTATION_ROTATE_90:
          rotate = 90;
          break;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return rotate;
  }
  public String getRealPathFromURI(Context context, Uri contentUri) {
    Cursor cursor = null;
    try {
      String[] proj = { MediaStore.Images.Media.DATA };
      cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      return cursor.getString(column_index);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * 反轉到正面的相片, Resize = 當前手機尺寸大小
   */
  public Bitmap getResizedBitmap(Bitmap bm, int rotate) {
    DisplayMetrics metric = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metric);
    int newHeight = metric.heightPixels;
    int newWidth = metric.widthPixels;

    int width = bm.getWidth();
    int height = bm.getHeight();
    float scaleWidth  = 1.F;
    float scaleHeight = 1.F;
    if (rotate == 90) {
      //scaleWidth = ((float) newWidth) / height;
      //scaleHeight = ((float) newHeight) / width;
    }else {
      //scaleWidth = ((float) newWidth) / width;
      //scaleHeight = ((float) newHeight) / height;
    }
    /*
    Log.i("phone height", "" + newHeight);
    Log.i("phone width", "" + newWidth);
    Log.i("bm width = ", "" + bm.getWidth());
    Log.i("bm height = ", "" + bm.getHeight());
    Log.i("scaleWidth = ", "" + scaleWidth);
    Log.i("scaleHeight = ","" + scaleHeight);*/
    Log.i("ro", "" + rotate);
    Matrix matrix = new Matrix();
    if (rotate == 90) matrix.postRotate(90);

    matrix.postScale(scaleWidth,scaleHeight);
    Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    return resizedBitmap;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_crop) {
      setCropBitmap();
      finish();
    }else if (item.getItemId() == R.id.action_retake) {
      setResult(RESULT_FIRST_USER);
      finish();
    }

    return super.onOptionsItemSelected(item);
  }

  private void setCropBitmap() {
    mCropBitmap = mMaskView.getOutputBitmap();
    if (mCropBitmap != null) setResult(RESULT_OK);
  }

  public static Bitmap getCropBitmap() {
    return mCropBitmap;
  }
}
