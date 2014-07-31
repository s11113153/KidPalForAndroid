package tw.com.mobilogics.kidpal;

import org.apache.http.util.ByteArrayBuffer;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import java.util.UUID;

import tw.com.mobilogics.kidpal.model.ActionTimeOut;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class KeyPalDevice extends BluetoothGattCallback {

  private final static String TAG = KeyPalDevice.class.getName();

  public final static UUID IMMEDIATE_ALERT_UUID =
    UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
  public final static UUID LINK_LOSS_UUID =
    UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
  public final static UUID LINK_LOSS_REGISTER_UUID =
    UUID.fromString("00008888-0000-1000-8000-00805f9b34fb");
  public final static UUID LINK_LOSS_SET_UUID =
    UUID.fromString("00008889-0000-1000-8000-00805f9b34fb");
  public final static UUID POWER_UUID =
    UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
  public final static UUID POWER_VALUE_UUID =
    UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
  public final static UUID SERVICE_BUTTON_UUID =
    UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
  public final static UUID SERVICE_BUTTON_NOTIFICATION_UUID =
    UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
  public final static UUID CLIENT_CHARACTERISTIC_CONFIG =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  // 配對該公司的BLE Device Name
  private final static String ALBERTSBLE = "ALBERTSBLE";
  private final static String ALBERTSBLEMINI = "ALBERTSBLEMINI";

  private String mAddress;
  public String getAddress() { return mAddress; }

  private Context mContext = null;

  private BluetoothGatt mBluetoothGatt;
  private BluetoothDevice mBluetoothDevice = null;
  public  String getBluetoothDeviceName() { return mBluetoothDevice.getName(); }
  private KeyPalDeviceImp mKeyPalDeviceImp = null;
  private BluetoothManager mBluetoothManager;
  private BluetoothAdapter mBluetoothAdapter = null;

  private static MediaPlayer mMediaPlayer = null;
  private static boolean alarm = false;

  private boolean remoteAlarm = false;
  public boolean isRemoteAlarm() { return remoteAlarm; }

  private int signalValue = 1;
  public  int getSignalValue() { return signalValue; }

  private int batteryValue = 1;
  public int getBatteryValue() { return batteryValue; }



  private ActionTimeOut mCommectTimeOut = null; // 用來連線到BLE Device時的定時器功能
  // 目前連線狀態
  private int mConnectionState = STATE_DISCONNECTED;
  // BluetoothProfile 連線狀態的屬性
  public static final int STATE_DISCONNECTED  = 0;
  public static final int STATE_CONNECTING    = 1;
  public static final int STATE_CONNECTED     = 2;
  public int getConnectionState() {
    return mConnectionState;
  }


  public KeyPalDevice(Context context, KeyPalDeviceImp keyPalDeviceImp) {
    mContext = context;
    mBluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = mBluetoothManager.getAdapter();
    mKeyPalDeviceImp = keyPalDeviceImp;
  }

  // 根據藍牙位置獲取藍牙名稱, 並判斷是否符合該公司的產品
  public static boolean isKeyPalDevice(BluetoothAdapter adapter, String address) {
    BluetoothDevice device = adapter.getRemoteDevice(address); // 根據藍牙位置獲取遠端藍牙裝置
    String deviceName = device.getName();
    if (ALBERTSBLE.equals(deviceName) || ALBERTSBLEMINI.equals(deviceName)) {
      return true;
    }
    return false;
  }

  public boolean connect(String address) {
    mAddress = address;
    if (mBluetoothAdapter == null && address == null) return false; // 註解一下

    // 根據藍牙位置獲取遠端藍牙裝置
    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
    if(mBluetoothDevice == null) return false;
    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, this);
    if (mBluetoothGatt.connect()) {
      mConnectionState = STATE_CONNECTING;

      mCommectTimeOut = new ActionTimeOut(15000) {
        @Override
        public void onTimeOut() {
          mBluetoothGatt.disconnect();
        }
      };
      return true;
    }

    return false;
  }


  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    Log.w(TAG,"onConnectionStateChangeNew = " + newState);
    switch (newState) {
      case BluetoothProfile.STATE_CONNECTED : {
        mConnectionState = STATE_CONNECTED;
        Log.i(TAG, "Attempting to start service discovery:"
          + gatt.discoverServices());
      } break;

      case BluetoothProfile.STATE_DISCONNECTED : {
        mConnectionState = STATE_DISCONNECTED;
        gatt.close();
        mKeyPalDeviceImp.update(KeyPalDevice.this);
      } break;
    }
  }

  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    Log.d(TAG,"onServicesDiscovered = " + status);
    if (status == BluetoothGatt.GATT_SUCCESS){
      new Thread(new Runnable() {
        @Override
        public void run() {
          showMessage("onServicesDiscovered");
          setCharacteristicNotification(LINK_LOSS_UUID,LINK_LOSS_SET_UUID,true);
        }
      }).start();
    }
  }

  private boolean setCharacteristicNotification(
          UUID service,
          UUID charaUUID,
          boolean enable
  ) {
    BluetoothGattCharacteristic characteristic =
        mBluetoothGatt.getService(service).getCharacteristic(charaUUID);
    mBluetoothGatt.setCharacteristicNotification(characteristic, enable);
    BluetoothGattDescriptor descriptor =
        characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
    if (descriptor == null) {
      Log.d(TAG, "Notification descriptor is null");
      return false;
    }
    if (enable)
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    else
        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    return mBluetoothGatt.writeDescriptor(descriptor);
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    super.onCharacteristicRead(gatt, characteristic, status);
    Log.w(TAG,"onCharacteristicRead ...");
    BluetoothGattService service = characteristic.getService();
    Log.d(TAG,"read uuid = " + characteristic.getUuid().toString());
    Log.d(TAG,"Length = " + characteristic.getValue().length);
    if (service.getUuid().equals(POWER_UUID) && characteristic.getUuid().equals(POWER_VALUE_UUID)) {
      setBatteryValue(characteristic.getValue()[0]);
      sendCommandWrite(LINK_LOSS_UUID, LINK_LOSS_REGISTER_UUID,
        new byte[]{(byte) 0x0f, (byte) 0x00});
      mKeyPalDeviceImp.setting(this);
    }
  }

  @Override
  public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    super.onCharacteristicWrite(gatt, characteristic, status);
    Log.w(TAG,"onCharacteristicWrite ...");
    showMessage("onCharacteristicWrite = " + status);
    if (status == BluetoothGatt.GATT_SUCCESS){
      Log.d(TAG, "UUID = " + characteristic.getUuid().toString());
    }

  }

  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    super.onCharacteristicChanged(gatt, characteristic);
    Log.w(TAG,"onCharacteristicChanged ...");
    BluetoothGattService service
      = characteristic.getService();
    showMessage("service uuid = " + service.getUuid().toString());

    if (service.getUuid().equals(POWER_UUID) && characteristic.getUuid().equals(POWER_VALUE_UUID)){
      setBatteryValue(characteristic.getValue()[0]);
    }

    StringBuffer sb = new StringBuffer();
    for(byte b : characteristic.getValue()){
      sb.append(String.format("[%02x]",b));
    }
    Log.d(TAG, "Value = " + sb.toString());

    byte[] tmpBuffer = characteristic.getValue();
    if (tmpBuffer[0] == (byte)0x06 &&
      tmpBuffer[1] == (byte)0x00 &&
      tmpBuffer[2] == (byte)0x01 &&
      tmpBuffer[3] != (byte)0x02 ){
      Log.d(TAG, "OK");
      setCharacteristicNotification(SERVICE_BUTTON_UUID,
        SERVICE_BUTTON_NOTIFICATION_UUID, true);
      mCommectTimeOut.close();
      mKeyPalDeviceImp.add(this);
    }else if (tmpBuffer.length == 1){
      Log.d(TAG, "Key");
      switch (tmpBuffer[0]){
        case (byte)0x01:
          mContext.sendBroadcast(new Intent(KeyPal.KEYPAL_KEY_EVENT_DOWN));
          break;
        case (byte)0x02:
          mContext.sendBroadcast(new Intent(KeyPal.KEYPAL_KEY_EVENT_UP));
          break;
        case (byte)0x03:
          mContext.sendBroadcast(new Intent(KeyPal.KEYPAL_KEY_EVENT_POWER));
          break;
      }
    }
  }

  @Override
  public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    super.onDescriptorRead(gatt, descriptor, status);
    Log.w(TAG,"onDescriptorRead ...");
  }

  @Override
  public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
    int status) {
    super.onDescriptorWrite(gatt, descriptor, status);
    if (status == BluetoothGatt.GATT_SUCCESS &&
      CLIENT_CHARACTERISTIC_CONFIG.equals(descriptor.getUuid())){
      BluetoothGattCharacteristic characteristic
        = descriptor.getCharacteristic();
      showMessage("characteristic uuid = " + characteristic.getUuid().toString());
      BluetoothGattService service
        = characteristic.getService();
      showMessage("service uuid = " + service.getUuid().toString());
      if (service.getUuid().equals(LINK_LOSS_UUID)){
        if (LINK_LOSS_SET_UUID.equals(characteristic.getUuid())){
          register();
        }
      }else if (service.getUuid().equals(SERVICE_BUTTON_UUID)){
        if (characteristic.getUuid().equals(SERVICE_BUTTON_NOTIFICATION_UUID)){
          new Thread(new Runnable() {
            @Override
            public void run() {
              while (getConnectionState() == STATE_CONNECTED){
                mBluetoothGatt.readRemoteRssi();
                try {
                  Thread.sleep(10000);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
            }
          }).start();
          Log.d(TAG,"SERVICE BUTTON OK");
          setCharacteristicNotification(POWER_UUID,POWER_VALUE_UUID,true);
        }
      }else if (POWER_UUID.equals(service.getUuid())){
        if (POWER_VALUE_UUID.equals(characteristic.getUuid())){
          sendCommandRead(POWER_UUID,POWER_VALUE_UUID);
        }
      }
    }
  }

  @Override
  public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    setRssiValue(rssi);
    showMessage(gatt.getDevice().getName() + " onReadRemoteRssi = " + rssi);
  }

  public void showMessage(String txt){
    Log.w(TAG,txt);
  }

  private boolean register(){
    String[] bleAddress = mBluetoothAdapter.getAddress().split(":");
    byte[] tmpByte = new byte[bleAddress.length];
    for (int i = 0;i < tmpByte.length;i++){
      tmpByte[i] = Integer.decode(String.format("0x%s",bleAddress[i])).byteValue();
    }
    Log.d(TAG, "Address = " + mBluetoothAdapter.getAddress());

    ByteArrayBuffer tmpBuffer = new ByteArrayBuffer(0);
    tmpBuffer.append(new byte[]{(byte)0x06,(byte)0x06},0,2);
    tmpBuffer.append(tmpByte, 0, tmpByte.length);

    return sendCommandWrite(LINK_LOSS_UUID,
      LINK_LOSS_REGISTER_UUID,
      tmpBuffer.toByteArray());
  }

  private boolean sendCommandWrite(UUID serviceUUID, UUID chararUUID,
    final byte[] buffer){
    BluetoothGattCharacteristic characteristic =
      mBluetoothGatt.getService(serviceUUID)
        .getCharacteristic(chararUUID);
    characteristic.setValue(buffer);
    return mBluetoothGatt.writeCharacteristic(characteristic);
  }

  private boolean sendCommandRead(UUID serviceUUID, UUID chararUUID){
    BluetoothGattCharacteristic characteristic =
      mBluetoothGatt.getService(serviceUUID)
        .getCharacteristic(chararUUID);
    return mBluetoothGatt.readCharacteristic(characteristic);
  }
  public synchronized static void alarm(Context context){
    if (!alarm && mMediaPlayer == null){
      //mMediaPlayer = MediaPlayer.create(context, R.raw.alarm);
      //mMediaPlayer.setLooping(true);
      //mMediaPlayer.start();
    }else{
      //mMediaPlayer.stop();
      //mMediaPlayer.release();
      //mMediaPlayer = null;
    }
    //alarm = !alarm;
  }

  private void setBatteryValue(int value){
    Log.d(TAG,"BatteryValue = " + value);
    int tmpValue = 0;
    if (value <= 20)
      tmpValue = 1;
    else if (value <= 30 && value < 20)
      tmpValue = 2;
    else if (value <= 40 && value < 30)
      tmpValue = 3;
    else
      tmpValue = 4;
    if (tmpValue != batteryValue){
      batteryValue = tmpValue;
      mKeyPalDeviceImp.update(this);
    }
  }

  private void setRssiValue(int value){
    Log.d(TAG,"Rssi = " + value);
    int tmpRssi = 0;
    if (value < -83)
      tmpRssi = 1;
    else if (value < -77 && value >= -83)
      tmpRssi = 2;
    else if (value < -71 && value >= -77)
      tmpRssi = 3;
    else if (value < -65 && value >= -71)
      tmpRssi = 4;
    else if (value >= -65)
      tmpRssi = 5;
    if (signalValue != tmpRssi){
      signalValue = tmpRssi;
      mKeyPalDeviceImp.update(this);
    }
  }

  public String getDeviceName(){
    return mBluetoothDevice.getName();
  }

  public void deviceAlarm(){
    UUID uuid = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    if (!remoteAlarm)
      sendCommandWrite(IMMEDIATE_ALERT_UUID,uuid,new byte[]{(byte)0x02});
    else
      sendCommandWrite(IMMEDIATE_ALERT_UUID,uuid,new byte[]{(byte)0x00});
    remoteAlarm = !remoteAlarm;
  }

  public void setDisconnectedAlarm(boolean value){
    UUID uuid = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    if (value){
      sendCommandWrite(LINK_LOSS_UUID,uuid,new byte[]{(byte)0x02});
    }else{
      sendCommandWrite(LINK_LOSS_UUID,uuid,new byte[]{(byte)0x00});
    }
  }
}
