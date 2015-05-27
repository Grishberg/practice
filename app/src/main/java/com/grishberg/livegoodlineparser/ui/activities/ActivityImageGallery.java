package com.grishberg.livegoodlineparser.ui.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.grishberg.livegoodlineparser.R;
import com.grishberg.livegoodlineparser.ui.adapters.FullScreenImageAdapter;

import java.util.ArrayList;

// Балдин Сергей
public class ActivityImageGallery extends AppCompatActivity {
	public static final String INTENT_POSITION = "initentPosition";
	public static final String INTENT_URL_ARRAY = "initentUrlArray";

	private FullScreenImageAdapter adapter;
	private ViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_gallery);

		viewPager = (ViewPager) findViewById(R.id.pgrImgGal);

		Intent i = getIntent();
		int position = i.getIntExtra(INTENT_POSITION, 0);
		ArrayList<String> imageLinksList = i.getStringArrayListExtra(INTENT_URL_ARRAY);
		Resources resources = getResources();
		adapter = new FullScreenImageAdapter(ActivityImageGallery.this,
				imageLinksList,resources);

		viewPager.setAdapter(adapter);

		// displaying selected image first
		viewPager.setCurrentItem(position);
	}
}
