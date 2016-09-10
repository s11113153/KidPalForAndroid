package tw.com.mobilogics.kidpal;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;


public class MainActivity extends Activity {

  private final static String TAG = MainActivity.class.getName();

  private static final int REQUEST_ENABLE_BT = 1;

  private static ServiceBLE mServiceBLE = null;

  // Scan期間防止使用者多次Scan
  private static boolean mScanState = false;

  private static PlaceholderFragment mFragment = new PlaceholderFragment();

  private final ServiceConnection mConnect = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      mServiceBLE = ((ServiceBLE.MyBinder)service).getService();
      getFragmentManager().beginTransaction()
        .replace(R.id.container, mFragment)
        .commit();
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
      mServiceBLE = null;
    }
  };

  private static int ACTUAL_SCREEN_HEIGHT = 0;

  private static MainActivity mActivity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    if (savedInstanceState != null) return;

    mActivity = this;
    requestOpenBLE();
    ///requestOpenGPS();
    startService(new Intent(MainActivity.this, ServiceBLE.class));

    final View activityRootView = findViewById(R.id.container);
    activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        ACTUAL_SCREEN_HEIGHT = activityRootView.getHeight();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Service比前端(Activity)重要
    bindService(new Intent(this, ServiceBLE.class), mConnect, Context.BIND_ABOVE_CLIENT);
  }

  @Override
  protected void onPause() {
    if (mServiceBLE != null) {
      if (mScanState) {
        mServiceBLE.stop();
        mScanState = false;
      }
      unbindService(mConnect);
    }
    super.onPause();
  }

  private void requestOpenBLE() {
    // 不支援 BLE 關閉並結束
    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
      Toast.makeText(this, "BLE is not supported", Toast.LENGTH_LONG).show();
      finish();
    }
    // 獲取 BluetoothAdapter
    final BluetoothManager mBluetoothManager =
      (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
    // 判斷藍牙是否打開, 沒打開要求用戶打開藍牙
    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  }

  private void requestOpenGPS() {
    // 判斷是否打開GPS, 沒打開要求用戶打開GPS
    LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
      !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      Toast.makeText(this, "Please open gps.", Toast.LENGTH_LONG).show();
      startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }
  }

  public static class PlaceholderFragment extends Fragment {

    private ListView mListView;

    public static final int UPDATE_LIST = 10;

    private String[] key;

    // 接收到ServiceBLE update通知消息時,更新ListView數據
    private Handler mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (msg.what == UPDATE_LIST) {
          Log.e(TAG, "Update MainActivity UI");
          try {
            Object[] objects = mServiceBLE.getMap().keySet().toArray();
            key = Arrays.copyOf(objects, objects.length, String[].class);
            mListView.setAdapter(new ListOfAdapter(getActivity(), R.layout.row_main_setting, key));
            ((ListOfAdapter) mListView.getAdapter()).notifyDataSetChanged();
          }catch (NullPointerException e) {
            e.printStackTrace();
            key = new String[4];
            mListView.setAdapter(new ListOfAdapter(getActivity(), R.layout.row_main_setting, key));
            ((ListOfAdapter) mListView.getAdapter()).notifyDataSetChanged();
          }
        }
      }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.fragment_main, container, false);
      mListView = (ListView) rootView.findViewById(R.id.listView);
      mListView.setSelector(new ColorDrawable(Color.TRANSPARENT));
      while (mServiceBLE == null) {
        try { Thread.sleep(100);
        } catch (InterruptedException e) { e.printStackTrace(); }
      }
      mServiceBLE.setHandler(mHandler);
      mHandler.sendMessage(Message.obtain(null, UPDATE_LIST));
      return rootView;
    }

    private class ListOfAdapter extends ArrayAdapter<String> {
      private LayoutInflater mInflater;
      private String[] key;

      public ListOfAdapter(Context context, int resource, String[] objects) {
        super(context, resource, objects);
        this.mInflater = LayoutInflater.from(context);
        this.key = objects;
      }

      @Override
      public int getCount() {
        return 4;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mInflater.inflate(R.layout.row_main_setting, null);
        ImageView imageViewLeft  = (ImageView) convertView.findViewById(R.id.imageViewLeft);
        ImageView imageViewRight = (ImageView) convertView.findViewById(R.id.imageViewRight);
        TextView  textViewName   = (TextView)  convertView.findViewById(tw.com.mobilogics.kidpal.R.id.editTextName);
        ImageView imageViewFrame = (ImageView)convertView.findViewById(R.id.imageViewFrame);
        //FrameLayout frameLayout = (FrameLayout) convertView.findViewById(R.id.frameLayout);

        // 配對
        if (key.length == 4 && mServiceBLE.getMap() == null && position == 0) {
          imageViewLeft.setBackground(getResources().getDrawable(R.drawable.ic_scan));
          imageViewRight.setBackground(null);
          textViewName.setText("");
          imageViewLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (mScanState == true) return;
              mScanState = true;
              ScanDeviceDialog.setServiceBLE(mServiceBLE);
              ScanDeviceDialog.setHandler(mHandler);
              startActivity(new Intent(mActivity, ScanDeviceDialog.class));
              new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                  mScanState = false;
                }
              },3500);
            }
          });
        }

        // 幫助
        if (key.length == 4 && mServiceBLE.getMap() == null && position == 3) {
          imageViewLeft.setBackground(null);
          imageViewRight.setBackground(getResources().getDrawable(R.drawable.ic_tutorial));
          textViewName.setText("");
          imageViewRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              startActivity(new Intent(mActivity, HelpDialog.class));
            }
          });
        }

        // 已經有找到小孩了
        if (mServiceBLE.getMap() != null) {
          int scanPosition = (mServiceBLE.getMap().size() >= 4) ? -1: mServiceBLE.getMap().size();
          int helpPosition = (mServiceBLE.getMap().size() >= 3) ? -1 : 4;

          // 新增小孩位置
          if (position < mServiceBLE.getMap().size()
            && scanPosition != -1
            && helpPosition != -1
            && position != scanPosition
            && position != helpPosition - 1
            ) {
            imageViewLeft.setBackground(getResources().getDrawable(R.drawable.ic_face_non));
            imageViewRight.setBackground(getResources().getDrawable(R.drawable.ic_homepage_light_off));
            KeyPal keyPal = mServiceBLE.getMap().get(key[position]);
            final String address = keyPal.address;
            if (keyPal.name != null) {
              textViewName.setText("" + keyPal.name);
            }

            if (keyPal.photo != null && keyPal.photo.length > 0) {
              Bitmap bitmap = BitmapFactory.decodeByteArray(keyPal.photo, 0, keyPal.photo.length);
              Drawable drawable = new BitmapDrawable(getResources(), bitmap);
              imageViewLeft.setBackground(drawable);
              imageViewFrame.setVisibility(View.VISIBLE);

              // 親子鍵狀態
              final KeyPalDevice keyPalDevice = mServiceBLE.getDevicesMap().get(keyPal.address);

              Drawable status = null;
              if (keyPalDevice != null) {
                keyPalDevice.setHandler(mHandler);
                switch (keyPalDevice.getConnectionState()) {
                  case KeyPalDevice.STATE_CONNECTING : {
                    Log.e("KeyPalDevice is connecting", "ing");
                  } break;
                  case KeyPalDevice.STATE_CONNECTED : {
                    Log.e("KeyPalDevice is connect", "con");
                    status = null;
                  } break;
                  case KeyPalDevice.STATE_DISCONNECTED : {
                    Log.e("KeyPalDevice is disConnect", "discon");
                    status = getResources().getDrawable(R.drawable.ic_cover_lost);
                    status.setAlpha(180);
                  } break;
                }

                // 弱電訊號
                int signalValue= keyPalDevice.getSignalValue();
                Log.w("signal", " : " + signalValue);
                imageViewLeft.setImageDrawable(null);
                if (signalValue < 2)
                        imageViewLeft.setImageResource(R.drawable.ic_cover_power);
              }else { // 尚未連接
                status = getResources().getDrawable(R.drawable.ic_cover_lost);
                status.setAlpha(180);
                Log.e("KeyPalDevice is null", "bye");
              }
              imageViewFrame.setBackground(status);
            }

            // 新增Child
            imageViewLeft.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                PersonInfoActivity.setServiceBLEHandler(mHandler);
                PersonInfoActivity.setServiceBLE(mServiceBLE);
                Intent intent = new Intent(mActivity, PersonInfoActivity.class);
                intent.putExtra("mac", address);
                startActivity(intent);
              }
            });

            // 改變亮燈
            imageViewRight.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                startActivity(new Intent(mActivity, LightingChooseDialog.class));
              }
            });
          }

          // 掃描位置
          if (scanPosition != -1 && position == scanPosition) {
            Drawable scan = getResources().getDrawable(R.drawable.ic_scan);
            imageViewLeft.setBackground(scan);
            imageViewRight.setBackground(null);
            textViewName.setText("");

            imageViewLeft.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                if (mScanState == true) return;
                mScanState = true;
                ScanDeviceDialog.setServiceBLE(mServiceBLE);
                ScanDeviceDialog.setHandler(mHandler);
                startActivity(new Intent(mActivity, ScanDeviceDialog.class));
                new Handler().postDelayed(new Runnable() {
                  @Override
                  public void run() {
                    mScanState = false;
                  }
                }, 3500);
              }
            });
          }

          // 幫助位置
          if (helpPosition != -1 && position == 3) {
            imageViewLeft.setBackground(null);
            imageViewRight.setBackground(getResources().getDrawable(R.drawable.ic_tutorial));
            textViewName.setText("");
            imageViewRight.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                startActivity(new Intent(mActivity, HelpDialog.class));
              }
            });
          }
        }

        convertView.setMinimumHeight(ACTUAL_SCREEN_HEIGHT / 4);
        return convertView;
      }
    }

  }
}
