package com.diplodesigns.huewatch.RoomSettings;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.wearable.view.GridViewPager;
import android.view.View;
import android.view.WindowInsets;

import com.diplodesigns.huewatch.Main.MainWatchActivity;
import com.diplodesigns.huewatch.R;
import com.google.gson.Gson;
import com.philips.lighting.model.PHGroup;
import com.philips.lighting.model.PHLight;

import java.util.List;

public class RoomSettingsActivity extends Activity {
    private GridViewPager pager;
    public static Context context;
    public static PHGroup room;
    public static List<PHLight> allLights;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_room_settings);

        Gson gson = new Gson();
        room = gson.fromJson(getIntent().getStringExtra("Room"), PHGroup.class);
        allLights = MainWatchActivity.lights;
        context = this;

       // final WatchViewStub stub = (WatchViewStub) findViewById(R.id.room_settings_stub);
        final Resources res = getResources();

//        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
//            @Override
//            public void onLayoutInflated(WatchViewStub stub) {
                // Get the list component from the layout of the activity
                pager = (GridViewPager) findViewById(R.id.roomPager);
                pager.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                        // Adjust page margins:
                        //   A little extra horizontal spacing between pages looks a bit
                        //   less crowded on a round display.
                        final boolean round = insets.isRound();
                        int rowMargin = res.getDimensionPixelOffset(R.dimen.page_row_margin);
                        int colMargin = res.getDimensionPixelOffset(round ?
                                R.dimen.page_column_margin_round : R.dimen.page_column_margin);
                        pager.setPageMargins(rowMargin, colMargin);

                        // GridViewPager relies on insets to properly handle
                        // layout for round displays. They must be explicitly
                        // applied since this listener has taken them over.
                        pager.onApplyWindowInsets(insets);
                        return insets;
                    }
                });
                pager.setAdapter(new SampleGridPagerAdapter(context, getFragmentManager()));
        //    }
      //  });
    }
}
