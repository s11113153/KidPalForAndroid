package tw.com.mobilogics.testhellohandy;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

public class LightingChooseDialog extends Activity implements View.OnClickListener {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.dialog_lighting_chooes);

    ImageView imageViewLightRed = (ImageView) findViewById(R.id.imageViewLightRed);
    imageViewLightRed.setOnClickListener(this);

    ImageView imageViewLightWhite = (ImageView) findViewById(R.id.imageViewLightWhite);
    imageViewLightWhite.setOnClickListener(this);

    ImageView imageViewLightRedWhite = (ImageView) findViewById(R.id.imageViewLightRedWhite);
    imageViewLightRedWhite.setOnClickListener(this);

    ImageView imageViewLightOff = (ImageView) findViewById(R.id.imageViewLightOff);
    imageViewLightOff.setOnClickListener(this);
  }
  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.imageViewLightRed : {
        Toast.makeText(LightingChooseDialog.this, "Red", Toast.LENGTH_LONG).show();
      }break;

      case R.id.imageViewLightWhite : {
        Toast.makeText(LightingChooseDialog.this, "White", Toast.LENGTH_LONG).show();
      }break;

      case R.id.imageViewLightRedWhite : {
        Toast.makeText(LightingChooseDialog.this, "Red+White", Toast.LENGTH_LONG).show();
      }break;

      case R.id.imageViewLightOff : {
        Toast.makeText(LightingChooseDialog.this, "Off", Toast.LENGTH_LONG).show();
      }break;
     }
  }
}
