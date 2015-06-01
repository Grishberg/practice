package com.grishberg.livegoodlineparser.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.grishberg.livegoodlineparser.R;
import com.grishberg.livegoodlineparser.ui.fragments.NewsActivityFragment;
import com.grishberg.livegoodlineparser.ui.fragments.TopicListActivityFragment;
import com.grishberg.livegoodlineparser.ui.listeners.ITopicItemClickListener;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.util.ArrayList;
import java.util.List;


public class TopicListActivity extends AppCompatActivity implements ITopicItemClickListener {
	public static final String TAG = "LiveGL.mainActivity";

	TopicListActivityFragment mTopicListFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_topic_list);

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

		// создание фрагментов
		// получим экземпляр FragmentTransaction из нашей Activity
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();

		// добавляем фрагмент
		mTopicListFragment = TopicListActivityFragment.newInstance();
		fragmentTransaction.replace(R.id.topic_list_fragment, mTopicListFragment
				,TopicListActivityFragment.class.getName());

		// если в текущей разметке есть секция под новость, отображаем второй фрагмент
		FrameLayout newsLayout = (FrameLayout) findViewById(R.id.news_fragment);
		if (newsLayout != null) {
			// портретная ориентация планшета, нужно отобразить фрагмент с новостью в том же Layout
			NewsActivityFragment newsFragment = new NewsActivityFragment();
			fragmentTransaction.replace(newsLayout.getId(), newsFragment
					, NewsActivityFragment.class.getName());
		}
		fragmentTransaction.commit();

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String action = intent.getAction();
		if (action.equals(TopicListActivityFragment.COMMAND_UPDATE_FROM_SERVICE) ||
				action.equals(TopicListActivityFragment.COMMAND_OPEN_NEWS_FROM_SERVICE)) {
			if (mTopicListFragment != null) {
				mTopicListFragment.onNewIntent(intent);
			}
		}
	}

	@Override
	public void onBackPressed() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		// если отображается фрагмент со списком новостей, то отработать кнопку назад, закрыть приложение
		TopicListActivityFragment topicListActivityFragment =
				(TopicListActivityFragment) fragmentManager.findFragmentByTag(TopicListActivityFragment.class.getName());
		if (topicListActivityFragment != null) {
			super.onBackPressed();

		} else {
			// если нет, то скрыть фрагмент со статьей и отобразить фрагмент со списком новостей
			NewsActivityFragment newsActivityFragment = (NewsActivityFragment) fragmentManager
					.findFragmentByTag(NewsActivityFragment.class.getName());
			if (newsActivityFragment != null) {
				FragmentTransaction transaction = fragmentManager.beginTransaction()
						.replace(R.id.topic_list_fragment, mTopicListFragment
								,TopicListActivityFragment.class.getName());
				transaction.commit();
			}
		}
	}

	// реакция на нажатие новости в списке новостей
	@Override
	public void onTopicListItemClicked(String title, String url, long date) {
		NewsActivityFragment newsActivityFragment = NewsActivityFragment.newInstance(title, url, date);
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

		// если в текущей разметке есть секция под новость, отображаем второй фрагмент
		FrameLayout newsLayout = (FrameLayout) findViewById(R.id.news_fragment);
		if (newsLayout != null) {
			// портретная ориентация планшета, нужно отобразить фрагмент с новостью в том же Layout
			fragmentTransaction.replace(newsLayout.getId(), newsActivityFragment
					, NewsActivityFragment.class.getName());
		} else {
			// заменить фрагмент со списком на фрагмент с новостью
			fragmentTransaction.replace(R.id.topic_list_fragment, newsActivityFragment
					,NewsActivityFragment.class.getName());
		}
		fragmentTransaction.commit();
	}
}
