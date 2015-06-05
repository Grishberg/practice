package com.grishberg.livegoodlineparser.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.grishberg.livegoodlineparser.R;
import com.grishberg.livegoodlineparser.data.interfaces.IGetTopilistListener;
import com.grishberg.livegoodlineparser.ui.fragments.NewsActivityFragment;
import com.grishberg.livegoodlineparser.ui.fragments.TopicListActivityFragment;
import com.grishberg.livegoodlineparser.ui.listeners.ITopicListActivityActions;
import com.grishberg.livegoodlineparser.ui.listeners.ITopicListFragmentActions;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.util.ArrayList;
import java.util.List;

// activity shows news topic list and detail news
public class TopicListActivity extends AppCompatActivity implements ITopicListActivityActions {
	public static final String TAG = "LiveGL.mainActivity";
	public static final String SAVE_STATE_DETAILS_NEW	= "saveStateDetailNews";
	private ArrayList<ITopicListFragmentActions> mFragments;
	TopicListActivityFragment mTopicListFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate before super");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_topic_list);
		Log.d(TAG, "onCreate post super");
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

		mFragments	= new ArrayList<>();
		// creating fragments
		// получим экземпляр FragmentTransaction из нашей Activity
		if(savedInstanceState == null) {
			mTopicListFragment = TopicListActivityFragment.newInstance();
			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
			// add topic list fragment
			transaction.add(R.id.topic_list_fragment, mTopicListFragment
					, TopicListActivityFragment.class.getName());
			// if current layout file contains framelayout for detail news fragment, show news fragment
			FrameLayout newsLayout = (FrameLayout) findViewById(R.id.news_fragment);
			if (newsLayout != null) {
				NewsActivityFragment newsFragment = new NewsActivityFragment();
				transaction.replace(newsLayout.getId(), newsFragment
						, NewsActivityFragment.class.getName());
			}
			transaction.commit();
		} else {
			//if orientation changed restore showing detail news state
			boolean detailNewsState	= savedInstanceState.getBoolean(SAVE_STATE_DETAILS_NEW);
			mTopicListFragment	=  (TopicListActivityFragment) getSupportFragmentManager()
					.findFragmentByTag(TopicListActivityFragment.class.getName());
			if(detailNewsState) {
				getSupportFragmentManager().beginTransaction()
						.hide(mTopicListFragment)
						.commit();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SAVE_STATE_DETAILS_NEW, mTopicListFragment.isVisible() == false);
	}

	/**
	 * send incoming Intent to child fragment
	 * @param intent
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String action = intent.getAction();
		for (ITopicListFragmentActions listener: mFragments) {
			if (action.equals(TopicListActivityFragment.COMMAND_UPDATE_FROM_SERVICE) ||
					action.equals(TopicListActivityFragment.COMMAND_OPEN_NEWS_FROM_SERVICE)) {
					listener.onNewIntent(intent);
			}
		}
	}

	/**
	 * send onBackPressed action to child fragment
	 */
	@Override
	public void onBackPressed() {
		FragmentManager fragmentManager = getSupportFragmentManager();

		// if topic list fragment are visible, do default action in back pressed
		if (mTopicListFragment.isVisible()) {
			super.onBackPressed();

		} else {
			// otherwise - hide detail news fragment and show topic list fragment
			NewsActivityFragment newsActivityFragment = (NewsActivityFragment) fragmentManager
					.findFragmentByTag(NewsActivityFragment.class.getName());
			if (newsActivityFragment != null) {
				FragmentTransaction transaction = fragmentManager.beginTransaction()
						.remove(newsActivityFragment);
				transaction.show(mTopicListFragment);
				transaction.commit();
			}
		}
	}

	//action for click on topic list item
	@Override
	public void onTopicListItemClicked(String title, String url, long date) {
		NewsActivityFragment newsActivityFragment = NewsActivityFragment.newInstance(title, url, date);
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

		//TopicListActivityFragment topicListActivityFragment =
		//		(TopicListActivityFragment) getSupportFragmentManager().findFragmentByTag(TopicListActivityFragment.class.getName());

		// если в текущей разметке есть секция под новость, отображаем второй фрагмент
		FrameLayout newsLayout = (FrameLayout) findViewById(R.id.news_fragment);
		if (newsLayout != null) {
			// портретная ориентация планшета, нужно отобразить фрагмент с новостью в том же Layout
			fragmentTransaction.replace(newsLayout.getId(), newsActivityFragment
					, NewsActivityFragment.class.getName());
		} else {
			// заменить фрагмент со списком на фрагмент с новостью
			fragmentTransaction.hide(mTopicListFragment);
			fragmentTransaction.add(R.id.topic_list_fragment, newsActivityFragment
					, NewsActivityFragment.class.getName());
		}
		fragmentTransaction.commit();
	}

	@Override
	public void onRegister(ITopicListFragmentActions fragment) {
		if(mFragments != null) {
			mFragments.add(fragment);
		}
	}

	@Override
	public void onUnregister(ITopicListFragmentActions fragment) {
		mFragments.remove(fragment);
	}
}
