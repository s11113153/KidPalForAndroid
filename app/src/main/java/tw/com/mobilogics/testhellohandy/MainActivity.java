package tw.com.mobilogics.testhellohandy;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
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

  // Progressbrar MenuItem
  private static MenuItem mMenuItem;

  private final static Handler mHandler = new Handler();

  private PlaceholderFragment mFragment = new PlaceholderFragment();

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

  private static final DisplayMetrics mDisplayMetrics = new DisplayMetrics();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_main);

    if (savedInstanceState != null) return;
    requestOpenBLE();
    ///requestOpenGPS();
    startService(new Intent(MainActivity.this, ServiceBLE.class));
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
      mServiceBLE.setHandler(null);
      unbindService(mConnect);
    }
    super.onPause();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    mMenuItem = menu.add("");
    mMenuItem.setEnabled(false);
    mMenuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    //return super.onCreateOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_ble_scan : {
        if (mScanState) break;
        mServiceBLE.setHandler(mHandler);
        mServiceBLE.scan();
        mScanState = true;
        mMenuItem.setActionView(R.layout.probar);
        mMenuItem.expandActionView();
        mHandler.postDelayed(new Runnable() {
          @Override
          public void run() {
            mServiceBLE.stop();
            mMenuItem.collapseActionView();
            mMenuItem.setActionView(null);
            mScanState = false;
          }
        }, 5000);
      } return true;
    }
    return super.onOptionsItemSelected(item);
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


  public class PlaceholderFragment extends Fragment implements View.OnClickListener {
    public static final int UPDATE_LIST = 10;
    private String[] key;

    // 接收到ServiceBLE update通知消息時,更新ListView數據
    private Handler mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (msg.what == UPDATE_LIST) {
          try {
            Object[] objects = mServiceBLE.getMap().keySet().toArray();
            key = Arrays.copyOf(objects, objects.length, String[].class);
            mListView.setAdapter(new ListOfAdapter(getActivity(), R.layout.row_device, key));
            ((ListOfAdapter) mListView.getAdapter()).notifyDataSetChanged();
          }catch (NullPointerException e) {
            key = new String[4];
            mListView.setAdapter(new ListOfAdapter(getActivity(), R.layout.row_device, key));
            ((ListOfAdapter) mListView.getAdapter()).notifyDataSetChanged();
          }
        }
      }
    };

    @Override
    public void onClick(View v) {

    }

    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.fragment_main, container, false);
      mListView = (ListView) rootView.findViewById(R.id.listView);
      mListView.setSelector(new ColorDrawable(Color.TRANSPARENT));

      while (mServiceBLE == null) {
        try {
          Thread.sleep(100);
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
        //mInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        this.mInflater = LayoutInflater.from(context);
        this.key = objects;
      }

      @Override
      public int getCount() {
        //return key.length;
        return 4;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mInflater.inflate(R.layout.row_main_setting, null);
        // 配對
        if (key.length == 4 && mServiceBLE.getMap() == null && position == 0) {
          ImageView imageViewLeft = (ImageView) convertView.findViewById(R.id.imageViewLeft);
          Drawable scan = getResources().getDrawable(R.drawable.bg_main_pair_start);
          imageViewLeft.getLayoutParams().height = getScreenHeight() / 4;
          imageViewLeft.setBackground(scan);
          ImageView imageViewRight = (ImageView) convertView.findViewById(R.id.imageViewRight);
          imageViewRight.setBackground(null);
          TextView textViewName = (TextView) convertView.findViewById(R.id.textViewName);
          textViewName.setText("");

          imageViewLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (mScanState == true) return;
              mScanState = true;
              ScanDeviceDialog.setServiceBLE(mServiceBLE);
              ScanDeviceDialog.setHandler(mHandler);
              startActivity(new Intent(MainActivity.this, ScanDeviceDialog.class));
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
          ImageView imageViewLeft = (ImageView) convertView.findViewById(R.id.imageViewLeft);
          imageViewLeft.setBackground(null);
          Drawable help = getResources().getDrawable(R.drawable.bg_main_help);
          ImageView imageViewRight = (ImageView) convertView.findViewById(R.id.imageViewRight);
          imageViewRight.getLayoutParams().height = getScreenHeight() / 4;
          imageViewRight.setBackground(help);
          TextView textViewName = (TextView) convertView.findViewById(R.id.textViewName);
          textViewName.setText("");

          imageViewRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              startActivity(new Intent(MainActivity.this, HelpDialog.class));
            }
          });
        }

        /*
        // 新增
        if (key.length == 4 && mServiceBLE.getMap() == null && position ==  1 || position == 2) {
          ImageView imageViewLeft = (ImageView) convertView.findViewById(R.id.imageViewLeft);
          Drawable child = getResources().getDrawable(R.drawable.bg_new_child);
          imageViewLeft.getLayoutParams().height = getScreenHeight() / 4;
          imageViewLeft.setBackground(child);
          Drawable light = getResources().getDrawable(R.drawable.bg_white_light);
          ImageView imageViewRight = (ImageView) convertView.findViewById(R.id.imageViewRight);
          imageViewRight.getLayoutParams().height = getScreenHeight() / 4;
          imageViewRight.setBackground(light);
          TextView textViewName = (TextView) convertView.findViewById(R.id.textViewName);
          textViewName.setText("Enter you Name");

          // 新增Child
          imageViewLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              Toast.makeText(MainActivity.this,"child",Toast.LENGTH_LONG).show();
            }
          });

          // 改變亮燈
          imageViewRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              Toast.makeText(MainActivity.this,"light",Toast.LENGTH_LONG).show();
              startActivity(new Intent(MainActivity.this, LightingChooseDialog.class));
            }
          });
        }*/

        // 已經有找到小孩了
        if (mServiceBLE.getMap() != null) {
          convertView = mInflater.inflate(R.layout.row_main_setting, null);
          ImageView imageViewLeft = (ImageView) convertView.findViewById(R.id.imageViewLeft);
          TextView textViewName = (TextView) convertView.findViewById(R.id.textViewName);
          ImageView imageViewRight = (ImageView) convertView.findViewById(R.id.imageViewRight);


          int scanPosition = (mServiceBLE.getMap().size() >= 4) ? -1: mServiceBLE.getMap().size();

          int helpPosition = (mServiceBLE.getMap().size() >= 3) ? -1 : 4;

          // 新增小孩位置
          if (position < mServiceBLE.getMap().size()
                    && scanPosition != -1
                    && helpPosition != -1
                    && position != scanPosition
                    && position != helpPosition - 1
          ) {
              Drawable child = getResources().getDrawable(R.drawable.bg_new_child);
              imageViewLeft.getLayoutParams().height = getScreenHeight() / 4;
              imageViewLeft.setBackground(child);
              Drawable light = getResources().getDrawable(R.drawable.bg_white_light);
              imageViewRight.getLayoutParams().height = getScreenHeight() / 4;
              imageViewRight.setBackground(light);

              KeyPal keyPal = mServiceBLE.getMap().get(key[position]);
              textViewName.setText("" + keyPal.address);

              // 新增Child
              imageViewLeft.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                  startActivity(new Intent(MainActivity.this, PersonInfoActivity.class));
                }
              });

              // 改變亮燈
              imageViewRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                  startActivity(new Intent(MainActivity.this, LightingChooseDialog.class));
                }
              });
          }

          // 掃描位置
          if (scanPosition != -1 && position == scanPosition) {
            Drawable scan = getResources().getDrawable(R.drawable.bg_main_pair_start);
            imageViewLeft.getLayoutParams().height = getScreenHeight() / 4;
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
                startActivity(new Intent(MainActivity.this, ScanDeviceDialog.class));
                new Handler().postDelayed(new Runnable() {
                  @Override
                  public void run() {
                    mScanState = false;
                  }
                },3500);
              }
            });
          }

          // 幫助位置
          if (helpPosition != -1 && position == 3) {
            imageViewLeft.setBackground(null);
            Drawable help = getResources().getDrawable(R.drawable.bg_main_help);
            imageViewRight.getLayoutParams().height = getScreenHeight() / 4;
            imageViewRight.setBackground(help);
            textViewName.setText("");
            imageViewRight.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HelpDialog.class));
              }
            });
          }


/*
          KeyPal keyPal = mServiceBLE.getMap().get(key[position]);
          //name.setText(keyPal.name);
          // 如果有儲存相片的話
          if (keyPal.photo != null && keyPal.photo.length > 0) {
            //Bitmap bitmap = BitmapFactory.decodeByteArray(keyPal.photo, 0, keyPal.photo.length);
            //Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            //face.setBackground(drawable);
          }
          convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              //跳轉到其他Activity
            }
          });
          final KeyPalDevice keyPalDevice = mServiceBLE.getDevicesMap().get(keyPal.address);
          if (keyPalDevice == null) {
            final KeyPal tmpKeyPal = keyPal;

          }
*/
        }
        return convertView;
      }
    }
    /**
     * @return 去除 status bar 之後的高度
     */
    public int getScreenHeight() {
      getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
      int result = 0;
      int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
      if (resourceId > 0) {
        result = getResources().getDimensionPixelSize(resourceId);
        result += result % 4;
      }
      int screenHeight = mDisplayMetrics.heightPixels - result;
      return screenHeight;
    }
  }
}
