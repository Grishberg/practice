package com.grishberg.livegoodlineparser.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.grishberg.livegoodlineparser.R;
import com.grishberg.livegoodlineparser.ui.fragments.TopicListActivityFragment;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.util.ArrayList;
import java.util.List;


public class TopicListActivity extends AppCompatActivity
{
	public static final String LOG_TAG = "LiveGL.mainActivity";

	public interface IActionsListener
	{
		boolean onBackPressed();
		void	onNewIntent(Intent intent);
	}

	private List<IActionsListener> mListeners;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_topic_list);

		mListeners	= new ArrayList<>();
		// UNIVERSAL IMAGE LOADER SETUP
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
				.cacheOnDisc(true).cacheInMemory(true)
				.imageScaleType(ImageScaleType.EXACTLY)
				.displayer(new FadeInBitmapDisplayer(300)).build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext())
				.defaultDisplayImageOptions(defaultOptions)
				.memoryCache(new WeakMemoryCache())
				.discCacheSize(100 * 1024 * 1024).build();

		ImageLoader.getInstance().init(config);
		// END - UNIVERSAL IMAGE LOADER SETUP
	}

	public void addListener(IActionsListener listener)
	{
		mListeners.add(listener);
	}

	public void removeListener(IActionsListener listener)
	{
		mListeners.remove(listener);
	}


	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		String action = intent.getAction();
		if( action.equals(TopicListActivityFragment.COMMAND_UPDATE_FROM_SERVICE) ||
				action.equals(TopicListActivityFragment.COMMAND_OPEN_NEWS_FROM_SERVICE)	)
		{
			for (IActionsListener listener : mListeners)
			{
				listener.onNewIntent(intent);
			}
		}
	}
}
