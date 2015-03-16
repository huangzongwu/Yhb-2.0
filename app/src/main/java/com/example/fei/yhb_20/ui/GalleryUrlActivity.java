package com.example.fei.yhb_20.ui;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.fei.yhb_20.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.truba.touchgallery.GalleryWidget.BasePagerAdapter;
import ru.truba.touchgallery.GalleryWidget.GalleryViewPager;
import ru.truba.touchgallery.GalleryWidget.UrlPagerAdapter;

public class GalleryUrlActivity extends ActionBarActivity {
    private GalleryViewPager mViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_url);
        Intent intent = getIntent();
        if (intent!=null){
            List<String> items = new ArrayList<String>();
            Collections.addAll(items,intent.getStringArrayExtra("photoUrls"));
            UrlPagerAdapter pagerAdapter = new UrlPagerAdapter(this, items,intent.getIntExtra("currentItem",0));

            pagerAdapter.setOnItemChangeListener(new BasePagerAdapter.OnItemChangeListener()
            {
                @Override
                public void onItemChange(int currentPosition)
                {
                    Toast.makeText(GalleryUrlActivity.this, "Current item is " + currentPosition, Toast.LENGTH_SHORT).show();
                }
            });

            mViewPager = (GalleryViewPager)findViewById(R.id.viewer);
            mViewPager.setOffscreenPageLimit(3);
            mViewPager.setAdapter(pagerAdapter);
            mViewPager.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GalleryUrlActivity.this.finish();
                }
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery_url, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
