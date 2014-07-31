package tw.com.mobilogics.kidpal;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;

public class DeleteDialog extends Activity implements View.OnClickListener{

  private static Handler mHandler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_delete);
    if (mHandler == null) finish();

    ImageButton imageButtonYes = (ImageButton) findViewById(R.id.imageButtonYes);
    ImageButton imageButtonNo = (ImageButton) findViewById(R.id.imageButtonNo);
    imageButtonYes.setOnClickListener(this);
    imageButtonNo.setOnClickListener(this);
  }

  public static void setHandler(Handler handler) {
    mHandler = handler;
  }

  @Override
  public void onClick(View v) {
    Message msg = null;
    switch (v.getId()) {
      case R.id.imageButtonYes : {
        msg = Message.obtain(null, PersonInfoActivity.DELETE_YES);
      } break;
      case R.id.imageButtonNo : {
        msg = Message.obtain(null, PersonInfoActivity.DELETE_NO);
      } break;
    }
    if (msg != null) mHandler.sendMessage(msg);
    finish();
  }
}
