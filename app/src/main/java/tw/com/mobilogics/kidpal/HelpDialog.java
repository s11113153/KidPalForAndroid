package tw.com.mobilogics.kidpal;

import com.viewpagerindicator.CirclePageIndicator;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import java.util.ArrayList;

public class HelpDialog extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(tw.com.mobilogics.kidpal.R.layout.dialog_help);
    final ArrayList<View> viewArrayList = new ArrayList<View>();
//    LayoutInflater inflater = getLayoutInflater().from(this);
//    viewArrayList.add(inflater.inflate(R.layout.activity_main, null));

    ImageView viewRed = new ImageView(this);
    viewRed.setBackground(getResources().getDrawable(tw.com.mobilogics.kidpal.R.drawable.ic_light_red_off));
    viewArrayList.add(viewRed);

    ImageView viewWhite = new ImageView(this);
    viewWhite.setBackground(getResources().getDrawable(tw.com.mobilogics.kidpal.R.drawable.ic_light_white_off));
    viewArrayList.add(viewWhite);

    ImageView ViewRedWhite = new ImageView(this);
    ViewRedWhite.setBackground(getResources().getDrawable(tw.com.mobilogics.kidpal.R.drawable.ic_light_cycle_off));
    viewArrayList.add(ViewRedWhite);

    ImageView viewOff = new ImageView(this);
    viewOff.setBackground(getResources().getDrawable(tw.com.mobilogics.kidpal.R.drawable.ic_light_off));
    viewArrayList.add(viewOff);

    PagerAdapter adapter = new PagerAdapter() {
      @Override
      public int getCount() {
        return viewArrayList.size();
      }

      @Override
      public boolean isViewFromObject(View view, Object o) {
        return view == (View) o;
      }

      @Override
      public Object instantiateItem(ViewGroup container, int position) {
        container.addView(viewArrayList.get(position));
        return viewArrayList.get(position);
      }

      @Override
      public void destroyItem(ViewGroup container, int position, Object object) {
        //super.destroyItem(container, position, object);
        container.removeView(viewArrayList.get(position));
      }
    };

    ViewPager viewPager = (ViewPager) findViewById(tw.com.mobilogics.kidpal.R.id.viewPager);
    viewPager.setAdapter(adapter);
    CirclePageIndicator mIndicator = (CirclePageIndicator)findViewById(tw.com.mobilogics.kidpal.R.id.indicator);
    mIndicator.setStrokeColor(Color.WHITE);
    mIndicator.setFillColor(Color.BLACK);
    mIndicator.setRadius(15);
    mIndicator.setViewPager(viewPager);
  }
}
