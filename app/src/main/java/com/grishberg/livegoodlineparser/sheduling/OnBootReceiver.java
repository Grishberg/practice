package com.grishberg.livegoodlineparser.sheduling;
// запуск аларм менеджера при загрузке телефона
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.grishberg.livegoodlineparser.ui.fragments.TopicListActivityFragment;

public class OnBootReceiver extends BroadcastReceiver
{
	public final static String LOG_TAG	= "LiveGL.OnBoot";
	public OnBootReceiver()
	{
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.d(LOG_TAG,"on boor received");

		//TODO: обратиться к настройкам, что бы узнать, нужно ли стартовать фоновое обновление.
		if("android.intent.action.BOOT_COMPLETED".equals(intent.getAction()))
		{
			new AlarmReceiver().startTask(context, TopicListActivityFragment.UPDATE_NEWS_DURATION);
		}
	}
}
