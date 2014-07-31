package tw.com.mobilogics.testhellohandy;

import com.google.android.gms.maps.model.LatLng;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.security.Key;
import java.util.HashMap;

import javax.security.auth.login.LoginException;

import tw.com.mobilogics.testhellohandy.model.DeviceDataBase;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ServiceBLE extends Service implements KeyPalDeviceImp {

  private final static String TAG = ServiceBLE.class.getName();

  private final IBinder mMyBinder = new MyBinder();

  private BluetoothManager mBluetoothManager = null;
  private BluetoothAdapter mBluetoothAdapter = null;

  private LocationManager mLocationManager = null;
  private Location mLocation = null;
  private String bestProvider = LocationManager.GPS_PROVIDER;

  private static DeviceDataBase mDataBase = null;

  /*** MainActivity.PlaceholderFragment getView */
  private static HashMap<String, KeyPalDevice> mDevicesMap = new HashMap<String, KeyPalDevice>(); // BLE DEVICE 物件
  public HashMap<String, KeyPalDevice> getDevicesMap() { return mDevicesMap; }

  /*** MainActivity.PlaceholderFragment getView */
  private HashMap<String, KeyPal> mMap = new HashMap<String, KeyPal>();
  public  HashMap<String, KeyPal> getMap() {
    mMap = mDataBase.getKeyPalMap();
    return  mMap;
  }

  private static Handler mHandler = null;
  public static void setHandler(Handler handler) { mHandler = handler; }

  @Override
  public IBinder onBind(Intent intent) {
    Log.e(TAG, "onBind");
    return mMyBinder;
  }

  public class MyBinder extends Binder {
    public ServiceBLE getService() {
      return ServiceBLE.this;
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.e(TAG, "onStartCommnad is start");
    // return super.onStartCommand(intent, flags, startId);

    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = mBluetoothManager.getAdapter();

    mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    Criteria criteria = new Criteria();
    criteria.setPowerRequirement(Criteria.POWER_LOW);
    bestProvider = mLocationManager.getBestProvider(criteria, true);
    mLocation = mLocationManager.getLastKnownLocation(bestProvider);
    if (mLocation == null) mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    mLocationManager.requestLocationUpdates(bestProvider, 1000, 1 , new LocationListener() {
      @Override
      public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged N = " + location.getLatitude() + "  E = " + location.getLongitude());
        mLocation = location;
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {

      }

      @Override
      public void onProviderEnabled(String provider) {

      }

      @Override
      public void onProviderDisabled(String provider) {

      }
    });

    mDataBase = new DeviceDataBase(this);

    mMap = getMap(); // ??

    return START_STICKY;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    Log.e(TAG, "onUnbind");
    return super.onUnbind(intent);
  }

  @Override
  public void onDestroy() {
    Log.e(TAG, "onDestory");
    super.onDestroy();
  }

  public void scan() {
    Log.d(TAG, "scan");
    mBluetoothAdapter.startLeScan(mLeScanCallback);
  }

  public void stop() {
    Log.d(TAG, "stop");
    mBluetoothAdapter.stopLeScan(mLeScanCallback);
  }

  private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
      if (mHandler != null) {
        mHandler.post(new Runnable() {
          @Override
          public void run() {
            addConnectKeyPalDevice(device);
          }
        });
      } else {
        Log.e(TAG, " ServiceBLE of Handler is null");
      }
    }

    private synchronized void addConnectKeyPalDevice(BluetoothDevice device) {
      Log.i(TAG, " KeyPal Device 裝置的大小為 : " + mDevicesMap.size());

      // 如果已經有配對KeyPal並且沒有取消該KeyPal連線,return
      if (mDevicesMap.size() != 0) {
        KeyPalDevice keyPalDevice = mDevicesMap.get(device.getAddress());
        if (keyPalDevice != null) {
          if (keyPalDevice.getConnectionState() != KeyPalDevice.STATE_DISCONNECTED) return;
        }
      }

      // 搜尋到該目標的BLE裝置
      if (KeyPalDevice.isKeyPalDevice(mBluetoothAdapter, device.getAddress())) {
        KeyPalDevice keyPalDevice = new KeyPalDevice(ServiceBLE.this, ServiceBLE.this);
        keyPalDevice.connect(device.getAddress());
       // add(keyPalDevice);
      }
    }
  };


  @Override
  public void add(KeyPalDevice keyPalDevice) {
    Log.d(TAG, "add keyPalDevice " + keyPalDevice.getAddress() );

    if (mMap == null) mMap = new HashMap<String, KeyPal>();

    // KeyPal HashMap 不存在該KeyPal Device Address
    if (mMap.get(keyPalDevice.getAddress()) == null) {
      KeyPal keyPal = new KeyPal("", keyPalDevice.getAddress(), null, KeyPal.Category.valueOf(keyPalDevice.getBluetoothDeviceName()), true, true);
      mMap.put(keyPalDevice.getAddress(), keyPal);
      // 儲存到db
      if (!mDataBase.isExist(keyPalDevice.getAddress())) {
        if (mDataBase.add(keyPal) <= 0) {
          Log.e(TAG, "add to db error");return;
        }
        Log.i(TAG, "add one KeyPal to Database successful");
      }
    }

    if (mDevicesMap.get(keyPalDevice.getAddress()) == null) {
      mDevicesMap.put(keyPalDevice.getAddress(), keyPalDevice);
    }

    update(keyPalDevice);
  }


  private LatLng getLocation(Location location) {
    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
    Log.i(TAG,"getLocation N = " + latLng.latitude + "  :  E = " + latLng.longitude);
    return latLng;
  }


  // 更新 KeyPal 所在的經緯度資料
  @Override
  public void update(KeyPalDevice keyPalDevice) {
    if (keyPalDevice.getConnectionState() == KeyPalDevice.STATE_DISCONNECTED) {
      mDevicesMap.remove(keyPalDevice.getAddress());
      if (mMap.get(keyPalDevice.getAddress()) != null) {
        LatLng latLng = getLocation(mLocation);
        ContentValues contentValues = new ContentValues();
        contentValues.put(DeviceDataBase.TITLE_LOCATION_E, latLng.latitude);
        contentValues.put(DeviceDataBase.TITLE_LOCATION_N, latLng.longitude);
        mDataBase.update(contentValues, keyPalDevice.getAddress());
      }
    }
    if (mHandler == null) return;
    // notification update UI
    Message msg = Message.obtain(null, MainActivity.PlaceholderFragment.UPDATE_LIST);
    mHandler.sendMessage(msg);
  }

  @Override
  public void setting(KeyPalDevice keyPalDevice) {

  }
}
