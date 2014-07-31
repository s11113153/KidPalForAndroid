package tw.com.mobilogics.testhellohandy;

public class KeyPal {

  public final static String KEYPAL_KEY_EVENT_UP    = "com.mobilogics.testhellohandy.action.key.up";

  public final static String KEYPAL_KEY_EVENT_POWER = "com.mobilogics.testhellohandy.action.key.power";

  public final static String KEYPAL_KEY_EVENT_DOWN  = "com.mobilogics.testhellohandy.action.key.down";

  public String name;
  public String address;
  public byte[] photo;
  public Category category;
  public boolean alarm;
  public boolean controller;
  public double location_n;
  public double location_e;
  // public boolean status = false;

  public KeyPal() {}

  public KeyPal (
          String name,
          String address,
          byte[] photo,
          Category category,
          boolean alarm,
          boolean controller
  ) {
    this.name = name;
    this.address = address;
    this.photo = photo;
    this.category = category;
    this.alarm = alarm;
    this.controller = controller;
  }

  public final static int PHOTO_DEFAULT = 0;
  public final static int PHOTO_ACTIVITY = 1;

  public static enum Category {
    ALBERTSBLE,
    ALBERTSBLEMINI
  }
}
