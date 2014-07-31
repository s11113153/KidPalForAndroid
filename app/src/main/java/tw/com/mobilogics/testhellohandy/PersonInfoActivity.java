package tw.com.mobilogics.testhellohandy;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


public class PersonInfoActivity extends Activity implements View.OnClickListener {
  private static final String Tag = PersonInfoActivity.class.getName();
  private ImageButton mImageButtonChild;
  private TextView mTextViewName;
  private ImageView mImageViewBatteryPower;
  private ImageButton mImageButtonTakePicuture;
  private ImageButton mImageButtonRadar;

  // 要求相簿中選取相片
  private final static int REQUERT_PHOTO = 1;
  // 要求照相機選取相片
  private final static int REQUEST_CAMERA = 0;
  // 要求裁切
  private final static int REQUEST_CROP = 2;



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_person_info);
    initial();
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
          mImageButtonChild.setImageBitmap(bitmap);
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
    setTitle("Name");
    mImageButtonChild = (ImageButton) findViewById(R.id.imageButtonChild);
    mTextViewName = (TextView) findViewById(R.id.textViewName);
    mImageViewBatteryPower = (ImageView) findViewById(R.id.imageViewBatteryPower);
    mImageButtonTakePicuture = (ImageButton) findViewById(R.id.imageButtonTakePicture);
    mImageButtonRadar = (ImageButton) findViewById(R.id.imageButtonRadar);

    mImageButtonChild.setOnClickListener(this);
    mImageButtonTakePicuture.setOnClickListener(this);
    mImageButtonRadar.setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      // call photo to pick child picture
      case R.id.imageButtonChild : {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUERT_PHOTO);
      } break;

      // call camera to take picture  for child
      case R.id.imageButtonTakePicture : {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //File tmpFile = new File(Environment.getExternalStorageDirectory(),"image.jpg");
        //Uri uri = Uri.fromFile(tmpFile);
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, REQUEST_CAMERA);

      } break;

      // ??
      case R.id.imageButtonRadar : {

      } break;
    }
  }


}
