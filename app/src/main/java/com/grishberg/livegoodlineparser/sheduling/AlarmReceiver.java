package com.grishberg.livegoodlineparser.sheduling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class AlarmReceiver extends BroadcastReceiver
{
	public AlarmReceiver()
	{
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		// разблокировка, что бы запустить во время сна
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, AlarmReceiver.class.getName());
		//Acquire the lock
		wl.acquire();

		Intent intentStartService = new Intent(context, CheckNewsUpdatesService.class);
		context.startService(intentStartService);

		//Release the lock
		wl.release();
	}

	public void startTask(Context context,int timeInMillisec)
	{
		AlarmManager alarmManager =(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), timeInMillisec, pi);
	}

	public void cancelAlarm(Context context)
	{
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}
}
