package tw.com.mobilogics.kidpal;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.security.Key;

import tw.com.mobilogics.kidpal.model.DeviceDataBase;


public class PersonInfoActivity extends Activity implements View.OnClickListener , View.OnLongClickListener{
  private static final String Tag = PersonInfoActivity.class.getName();

  public static final int DELETE_YES = 20;
  public static final int DELETE_NO = 30;

  private ImageButton mImageButtonChild;

  private ImageView mImageViewBatteryPower;

  private ImageButton mImageButtonTakePicuture;

  private ImageButton mImageButtonRadar;

  private EditText mEditText;

  // 要求照相機選取相片
  private final static int REQUEST_CAMERA = 0;

  // 要求相簿中選取相片
  private final static int REQUERT_PHOTO = 1;

  // 要求裁切
  private final static int REQUEST_CROP = 2;

  private static DeviceDataBase mDataBase;

  private static String address;

  private static Handler mServiceBLEHandler;

  private static Handler mDeleteHandler;

  private static ServiceBLE mServiceBLE;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(tw.com.mobilogics.kidpal.R.layout.activity_person_info);
    mDataBase = new DeviceDataBase(this);
    if (getIntent() != null) {
      address = getIntent().getStringExtra("mac");
    }
    initial();

    mDeleteHandler = new Handler(){
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
          case DELETE_YES : {
            if (mDataBase.delete(address)) {
              //通知更新
              Message message = Message.obtain(null, MainActivity.PlaceholderFragment.UPDATE_LIST);
              mServiceBLEHandler.sendMessage(message);
              //Drawable drawable = getResources().getDrawable(R.drawable.ic_face_non);
              //mImageButtonChild.setBackground(drawable);
              startActivity(new Intent(PersonInfoActivity.this, MainActivity.class));
              finish();
            }
          } break;
          case DELETE_NO : {
            // no things
          } break;
        }
      }
    };
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (RESULT_OK == resultCode) {
      switch (requestCode) {
        case REQUERT_PHOTO : {
          Intent cropIntent = new Intent(this, tw.com.mobilogics.module.Mask.CropPictureActivity.class);
          cropIntent.setData(intent.getData());
          startActivityForResult(cropIntent, REQUEST_CROP);
        } break;

        case REQUEST_CAMERA : {
          Intent cropIntent = new Intent(this, tw.com.mobilogics.module.Mask.CropPictureActivity.class);
          if (intent.getData() != null) {
           // cropIntent.setData(intent.getData());
          }
          Bitmap bitmap = (Bitmap)intent.getExtras().get("data");
          //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new ByteArrayOutputStream());
          cropIntent.putExtra("bitmap", bitmap);
          startActivityForResult(cropIntent, REQUEST_CROP);
        } break;

        case REQUEST_CROP : {
          Bitmap bitmap = tw.com.mobilogics.module.Mask.CropPictureActivity.getCropBitmap();
          Drawable drawable = new BitmapDrawable(getResources(), bitmap);
          mImageButtonChild.setBackground(drawable);
          ContentValues contentValues = new ContentValues();
          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
          contentValues.put(DeviceDataBase.TITLE_PHOTO, stream.toByteArray());
          mDataBase.update(contentValues,address);

          /*
          Matrix matrix = new Matrix();
          //matrix.setScale(368.F / bitmap.getWidth(), 368.F / bitmap.getHeight());
          bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);// 規定成尺寸大小

          Log.e("bitmap w", "" + bitmap.getWidth());
          Log.e("bitmap h", "" + bitmap.getHeight());


          Bitmap childFrame = BitmapFactory.decodeResource(getResources(),R.drawable.ic_face_frame).copy(
            Bitmap.Config.ARGB_8888, true);

          Log.e("chlidFrame H :", "" + childFrame.getHeight());
          Log.e("chlidFrame W :", "" + childFrame.getWidth());

          Paint paint = new Paint();
          paint.setFilterBitmap(true);
          paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
          Canvas canvas = new Canvas(childFrame);
          canvas.drawBitmap(childFrame, 0, 0, paint);
          canvas.drawBitmap(bitmap, 0, 0, paint);

          ContentValues contentValues = new ContentValues();
          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          childFrame.compress(Bitmap.CompressFormat.JPEG, 100, stream);
          contentValues.put(DeviceDataBase.TITLE_PHOTO, stream.toByteArray());
          mDataBase.update(contentValues,address);

          mImageButtonChild.setBackground(new ColorDrawable(Color.TRANSPARENT));
          mImageButtonChild.setScaleType(ImageView.ScaleType.FIT_XY);
          mImageButtonChild.setImageBitmap(childFrame);
*/
          //通知更新
          Message msg = Message.obtain(null, MainActivity.PlaceholderFragment.UPDATE_LIST);
          mServiceBLEHandler.sendMessage(msg);

        } break;
      }
    }else if (RESULT_FIRST_USER == resultCode) { // 重新拍照
      if (REQUEST_CROP == requestCode) {
        Intent cropIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //File tmpFile = new File(Environment.getExternalStorageDirectory(),"image.jpg");
        //Uri uri = Uri.fromFile(tmpFile);
        //cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(cropIntent, REQUEST_CAMERA);
      }
    }
  }

  private void initial() {
    mImageButtonChild = (ImageButton) findViewById(tw.com.mobilogics.kidpal.R.id.imageButtonChild);
    mEditText = (EditText) findViewById(tw.com.mobilogics.kidpal.R.id.editText);
    mImageViewBatteryPower = (ImageView) findViewById(tw.com.mobilogics.kidpal.R.id.imageViewBatteryPower);
    mImageButtonTakePicuture = (ImageButton) findViewById(tw.com.mobilogics.kidpal.R.id.imageButtonTakePicture);
    mImageButtonRadar = (ImageButton) findViewById(tw.com.mobilogics.kidpal.R.id.imageButtonRadar);

    mImageButtonChild.setOnClickListener(this);
    mImageButtonChild.setOnLongClickListener(this);
    mImageButtonTakePicuture.setOnClickListener(this);
    mImageButtonRadar.setOnClickListener(this);

    mEditText.setSingleLine();
    mEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);

    if (address != null) {
      KeyPal keyPal = mDataBase.getKeyPal(address);
      // 設定照片
      if (keyPal.photo != null && keyPal.photo.length > 0) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(keyPal.photo, 0, keyPal.photo.length);
        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
       mImageButtonChild.setBackground(drawable);
      }
      // 設定名稱
      if (keyPal.name != null) {
        mEditText.setText(keyPal.name);
      }
      // 設定電量
      final KeyPalDevice keyPalDevice = mServiceBLE.getDevicesMap().get(keyPal.address);
      if (keyPalDevice != null) {
        int power = keyPalDevice.getBatteryValue();
        Toast.makeText(PersonInfoActivity.this, "電量等級為1-4, 目前電量為" + power, Toast.LENGTH_LONG).show();

        Drawable drawable = null;
        switch (power) {
          case 4 : {
            drawable = getResources().getDrawable(R.drawable.ic_battery_3);
          } break;
          case 3 : {
            drawable = getResources().getDrawable(R.drawable.ic_battery_2);
          } break;
          case 2 : {
            drawable = getResources().getDrawable(R.drawable.ic_battery_1);
          } break;
          case 1 : {
            drawable = getResources().getDrawable(R.drawable.ic_battery_0);
          } break;
        }
        if (drawable != null) mImageViewBatteryPower.setBackground(drawable);
      }
    }

    //編輯Name
    mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
          mEditText.clearFocus();
          InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
          ContentValues contentValues = new ContentValues();
          contentValues.put(DeviceDataBase.TITLE_NAME, mEditText.getText().toString());
          mDataBase.update(contentValues,address);
          //通知更新
          Message msg = Message.obtain(null, MainActivity.PlaceholderFragment.UPDATE_LIST);
          mServiceBLEHandler.sendMessage(msg);
          return true;
        }
        return false;
      }
    });
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      // call photo to pick child picture
      case tw.com.mobilogics.kidpal.R.id.imageButtonChild : {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUERT_PHOTO);
      } break;

      // call camera to take picture  for child
      case tw.com.mobilogics.kidpal.R.id.imageButtonTakePicture : {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //File tmpFile = new File(Environment.getExternalStorageDirectory(),"image.jpg");
        //Uri uri = Uri.fromFile(tmpFile);
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, REQUEST_CAMERA);

      } break;

      // ??
      case tw.com.mobilogics.kidpal.R.id.imageButtonRadar : {

      } break;
    }
  }

  public int dpToPx(int dp) {
    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
    int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    return px;
  }

  @Override
  public boolean onLongClick(View v) {
    if (v.getId() == R.id.imageButtonChild) {
      if (address == null) return false;

     // KeyPal keyPal = mDataBase.getKeyPal(address);
     // if (keyPal.photo != null && keyPal.photo.length > 0) {
        DeleteDialog.setHandler(mDeleteHandler);
        startActivity(new Intent(this, DeleteDialog.class));
        return true;
     // }
    }
    return false;
  }

  public static void setServiceBLE(ServiceBLE serviceBLE) {
    mServiceBLE = serviceBLE;
  }

  public static void setServiceBLEHandler(Handler serviceBLEHandler) {
    mServiceBLEHandler = serviceBLEHandler;
  }
}
