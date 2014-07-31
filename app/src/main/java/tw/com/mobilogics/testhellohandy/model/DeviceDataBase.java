package tw.com.mobilogics.testhellohandy.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.util.Log;

import java.security.Key;
import java.util.HashMap;

import tw.com.mobilogics.testhellohandy.KeyPal;

public class DeviceDataBase extends SQLiteOpenHelper {

  private final static String TAG = DeviceDataBase.class.getName();

  private final static int VERSION = 1; // 資料庫版本
  private final static String DATA_BASE_NAME = "database.db"; // 資料庫名稱
  private final static String TABLE_NAME = "KeyPal";// 資料表名稱

  // 資料表欄位
  private final static String TITLE_ID = "_id";

  private final static String TITLE_NAME = "_name";

  private final static String TITLE_ADDRESS = "_address";

  private final static String TITLE_PHOTO = "_photo";

  private final static String TITLE_CATEGORY = "_category";

  private final static String TITLE_STATUS = "_status";

  public final static String TITLE_LOCATION_N = "_location_n";

  public final static String TITLE_LOCATION_E = "_location_e";

  private final static String TITLE_ALARM = "_alarm";

  private final static String TITLE_CONTROLLER = "_controller";

  // SQL
  private final static String SQL_CREATE_TABLE = "CREATE TABLE "
    + TABLE_NAME       + "( "
    + TITLE_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT,"
    + TITLE_NAME       + " VARCHAR(20),"
    + TITLE_ADDRESS    + " VARCHAR(50),"
    + TITLE_CATEGORY   + " INTEGER,"
    + TITLE_STATUS     + " INTEGER,"
    + TITLE_CONTROLLER + " NUMERIC,"
    + TITLE_PHOTO      + " BLOB,"
    + TITLE_LOCATION_N + " REAL,"
    + TITLE_LOCATION_E + " REAL,"
    + TITLE_ALARM      + " NUMERIC"
    +
  " );";
  private static final String SQL_DROP_TABLE = "DROP TABLE " + TABLE_NAME;

  public DeviceDataBase(Context context) {
    super(context, DATA_BASE_NAME, null, VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(SQL_CREATE_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL(SQL_DROP_TABLE);
  }

  /**
   * 列印 KidPal 資料表的所有欄位資料
   */
  public void printKeyPal() {
    SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
    Cursor cursor = sqLiteDatabase.query(TABLE_NAME, null, null, null, null, null, null);
    cursor.moveToFirst();
    for (int i=0; i< cursor.getCount(); i++) {
      Log.i("print KeyPal ", TITLE_ID         + " : " + cursor.getString(0));
      Log.i("print KeyPal ", TITLE_NAME       + " : " + cursor.getString(1));
      Log.i("print KeyPal ", TITLE_ADDRESS    + " : " + cursor.getString(2));
      Log.i("print KeyPal ", TITLE_PHOTO      + " : " + cursor.getString(3));
      Log.i("print KeyPal ", TITLE_CATEGORY   + " : " + cursor.getString(4));
      Log.i("print KeyPal ", TITLE_STATUS     + " : " + cursor.getString(5));
      Log.i("print KeyPal ", TITLE_LOCATION_N + " : " + cursor.getString(6));
      Log.i("print KeyPal ", TITLE_LOCATION_E + " : " + cursor.getString(7));
      Log.i("print KeyPal ", TITLE_ALARM      + " : " + cursor.getString(8));
      Log.i("print KeyPal ", TITLE_CONTROLLER + " : " + cursor.getString(9));
      Log.w("print KeyPal","-------------------------------------------------");
      cursor.moveToNext();
    }
  }

  /**
   * Parent Method {@link #isExist(String)}
   */
  public Cursor getCursor(String address) {
    SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
    Cursor cursor = sqLiteDatabase.query(
      TABLE_NAME,
      null,
      TITLE_ADDRESS + " like \"" + address + "\"",
      null,
      null,
      null,
      null
    );

    if (cursor.getCount() <= 0) return null;
    cursor.moveToFirst();
    return cursor;
  }

  /***
   * @param address KeyPal Device address
   * @return KeyPal Table 是否有該 KeyPal Device address 資料
   */
  public boolean isExist(String address) {
    Cursor cursor = this.getCursor(address);
    if (cursor == null || address == null) {
      return false;
    }
    if (!cursor.getString(cursor.getColumnIndex(TITLE_ADDRESS)).equals(address))
      return false;
    return true;
  }

  /***
   * 資料庫增加一筆 KeyPal 物件的資料 (除了 _id, _location_n , _location_e)
   */
  public long add(KeyPal keyPal) {
    SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
    ContentValues contentValues = new ContentValues();
    contentValues.put(TITLE_NAME, keyPal.name);
    contentValues.put(TITLE_ADDRESS, keyPal.address);
    contentValues.put(TITLE_CATEGORY, keyPal.category.ordinal());
    contentValues.put(TITLE_STATUS , 0); // 預設未連接狀態, 等待update去更新
    contentValues.put(TITLE_PHOTO , keyPal.photo);
    contentValues.put(TITLE_CONTROLLER, keyPal.controller);
    contentValues.put(TITLE_ALARM, keyPal.alarm);
    long value = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
    sqLiteDatabase.close();
    return value;
  }

  /***
   * 根據 KeyPal Device address 更新數據
   */
  public boolean update(ContentValues contentValues, String address) {
    SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
    int count = sqLiteDatabase.update(TABLE_NAME, contentValues, TITLE_ADDRESS + " like ?", new String[]{String.valueOf(address)});
    sqLiteDatabase.close();
    return (count > 0) ? true : false;
  }

  /***
   * 根據KeyPal Device address 刪除數據
   */
  public boolean delete(String address) {
    SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
    int num = sqLiteDatabase.delete(TABLE_NAME,
        TITLE_ADDRESS + " like ?",
        new String[]{String.valueOf(address)}
    );
    return (num > 0) ? true : false;
  }

  public boolean changeName(String address, String name) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(TITLE_NAME, name);
    return update(contentValues,address);
  }

  public Cursor getCursor() {
    SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
    Cursor cursor = sqLiteDatabase.query(TABLE_NAME, null, null, null, null, null, null);
    if (cursor.getCount() <= 0) return null;
    return  cursor;
  }

  public KeyPal getKeyPal(String address) {
    Cursor cursor = getCursor(address); // aleardy moveToFirst
    if (cursor == null) return null;
    KeyPal keyPal = new KeyPal();
    keyPal.name = cursor.getString(cursor.getColumnIndex(TITLE_NAME));
    keyPal.category = KeyPal.Category.values()[
        cursor.getInt(cursor.getColumnIndex(TITLE_CATEGORY))];
    keyPal.photo = cursor.getBlob(cursor.getColumnIndex(TITLE_PHOTO));
    keyPal.address = cursor.getString(cursor.getColumnIndex(TITLE_ADDRESS));
    keyPal.alarm = cursor.getInt(cursor.getColumnIndex(TITLE_ALARM)) == 1 ? true : false;
    keyPal.controller = cursor.getInt(cursor.getColumnIndex(TITLE_CONTROLLER)) == 1 ? true : false;
    keyPal.location_n = cursor.getDouble(cursor.getColumnIndex(TITLE_LOCATION_N));
    keyPal.location_e = cursor.getDouble(cursor.getColumnIndex(TITLE_LOCATION_E));
    return keyPal;
  }

  /**
   * 回傳所有 KeyPal 資料表的資料
   */
  public HashMap<String, KeyPal> getKeyPalMap() {
    HashMap<String, KeyPal> map = new HashMap<String, KeyPal>();
    Cursor cursor = getCursor();
    if (cursor == null) return  null;
    cursor.moveToFirst();
    do {
      KeyPal keyPal = new KeyPal();
      keyPal.address = cursor.getString(cursor.getColumnIndex(TITLE_ADDRESS));
      keyPal.photo = cursor.getBlob(cursor.getColumnIndex(TITLE_PHOTO));
      keyPal.name = cursor.getString(cursor.getColumnIndex(TITLE_NAME));
      keyPal.alarm = cursor.getInt(cursor.getColumnIndex(TITLE_ALARM)) == 1 ? true : false;
      keyPal.controller = cursor.getInt(cursor.getColumnIndex(TITLE_CONTROLLER)) == 1 ? true : false;
      keyPal.category = KeyPal.Category.values()[cursor.getInt(cursor.getColumnIndex(TITLE_CATEGORY))];
      keyPal.location_n = cursor.getDouble(cursor.getColumnIndex(TITLE_LOCATION_N));
      keyPal.location_e = cursor.getDouble(cursor.getColumnIndex(TITLE_LOCATION_E));
      map.put(keyPal.address,keyPal);
    }while (cursor.moveToNext());
    return map;
  }
}
