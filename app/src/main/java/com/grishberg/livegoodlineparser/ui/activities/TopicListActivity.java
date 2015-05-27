package com.grishberg.livegoodlineparser.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.grishberg.livegoodlineparser.R;
import com.grishberg.livegoodlineparser.ui.fragments.TopicListActivityFragment;


public class TopicListActivity extends AppCompatActivity
{
	public static final String LOG_TAG = "LiveGL.mainActivity";
	private Intent lastIntent = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_topic_list);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		String action = intent.getAction();
		if( action != null)
		{
			if( action.equals(TopicListActivityFragment.COMMAND_UPDATE_FROM_SERVICE))
			{
				// обновить список новостей
				// запустить обновление списка
				lastIntent = intent;
			}
			else if( action.equals(TopicListActivityFragment.COMMAND_OPEN_NEWS_FROM_SERVICE))
			{
				lastIntent = intent;
			}
			else
			{
				lastIntent = null;
			}
		}
	}
	// вернуть последний интент и обнулить
	public Intent getLastIntent()
	{
		if (lastIntent == null) return  null;
		Intent resultIntent = (Intent)lastIntent.clone();
		lastIntent			= null;
		return resultIntent;
	}
}
