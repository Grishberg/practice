package com.grishberg.livegoodlineparser.sheduling;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.grishberg.livegoodlineparser.R;
import com.grishberg.livegoodlineparser.data.asynctaskloaders.GetNewsTask;
import com.grishberg.livegoodlineparser.data.asynctaskloaders.GetTopicListTask;
import com.grishberg.livegoodlineparser.data.containers.NewsContainer;
import com.grishberg.livegoodlineparser.data.model.NewsDbHelper;
import com.grishberg.livegoodlineparser.ui.activities.TopicListActivity;
import com.grishberg.livegoodlineparser.ui.fragments.TopicListActivityFragment;
import com.grishberg.livegoodlineparser.data.livegoodlineparser.LiveGoodlineParser;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CheckNewsUpdatesService extends IntentService
{
	public static final String LOG_TAG = "LiveGL.service";
	private static final int MY_NOTIFICATION_ID=1;

	NotificationManager notificationManager;
	private NewsDbHelper mDbHelper;

	public CheckNewsUpdatesService()
	{
		super(CheckNewsUpdatesService.class.getName());
		mDbHelper	= new NewsDbHelper(getApplicationContext());
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		Log.d(LOG_TAG," проверка на наличие новых новостей");
		// синхронная загрузка из Volley
		RequestQueue queue			= Volley.newRequestQueue(getApplicationContext());
		RequestFuture<String> futureRequest = RequestFuture.newFuture();
		StringRequest getRequest	= new StringRequest(Request.Method.GET
				, GetTopicListTask.mainUrl
				, futureRequest, futureRequest);
		queue.add(getRequest);

		try
		{
			// синхронное извлечение тела страницы
			String response = futureRequest.get(TopicListActivityFragment.VOLLEY_SYNC_TIMEOUT, TimeUnit.SECONDS);
			// парсинг полученной строки
			List<NewsContainer> topicListFromWeb = LiveGoodlineParser.getNewsPerPage(response);

			Date date = mDbHelper.getMaxStoredDate();

			if(date == null) { 	date = new Date(0); }

			boolean turnOnSound = true;
			for(int i = topicListFromWeb.size()-1; i >= 0 ; i--)
			{
				NewsContainer currentNews = topicListFromWeb.get(i);
				//  сравнить новые новости с датой самых свежих в кэше
				if( currentNews.getDate().compareTo(date) <= 0)
				{
					topicListFromWeb.remove(i);
				}
			}

			if(topicListFromWeb != null && topicListFromWeb.size() > 0)
			{
				addNotification(topicListFromWeb, true, intent);
				Log.d(LOG_TAG,"сохранение новых сообщений в кэш");
				mDbHelper.storeTopicList(topicListFromWeb);
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	public String getNewsCaptionByCount(int count)
	{
		switch (count%10)
		{
			case 0:
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
				return String.format("Добавлено %d новостей",count);
			case 1:
				return String.format("Добавлено %d новость",count);
			case 3:
			case 4:
				return String.format("Добавлено %d новости",count);
		}
		return "";
	}

	// запуск уведомления
	public void addNotification(List<NewsContainer> topicList, boolean turnOnSound, Intent intent)
	{

		String message = "";
		Notification notification;
		// послать команду на обновление активити, либо на открытие новости.
		Intent notificationIntent = new Intent(getApplicationContext(), TopicListActivity.class);


		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				|Intent.FLAG_ACTIVITY_SINGLE_TOP
		); //

		if (topicList.size() == 1)
		{
			//COMMAND_OPEN_NEWS_FROM_SERVICE
			notificationIntent.setAction(TopicListActivityFragment.COMMAND_OPEN_NEWS_FROM_SERVICE);
			notificationIntent.putExtra(TopicListActivityFragment.NEWS_DATE_INTENT, topicList.get(0).getDate().getTime());
			notificationIntent.putExtra(TopicListActivityFragment.NEWS_URL_INTENT, topicList.get(0).getUrl());
			notificationIntent.putExtra(TopicListActivityFragment.NEWS_TITLE_INTENT, topicList.get(0).getTitle());
			message = topicList.get(0).getTitle();
		}
		else
		{
			notificationIntent.setAction(TopicListActivityFragment.COMMAND_UPDATE_FROM_SERVICE);
			message	= getNewsCaptionByCount(topicList.size());
		}

		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


		if(turnOnSound )
		{
			notification = new NotificationCompat.Builder(getApplicationContext())
					.setContentTitle(getString(R.string.app_name))
					.setContentText(message)
					.setTicker("Обновления.")
					.setWhen(System.currentTimeMillis())
					.setAutoCancel(true)
					.setSmallIcon(R.mipmap.ic_launcher)
					.setDefaults(Notification.DEFAULT_SOUND)
					.build();
		}
		else
		{
			notification = new NotificationCompat.Builder(getApplicationContext())
					.setContentTitle(getString(R.string.app_name))
					.setContentText(message)
					.setTicker("Обновления.")
					.setWhen(System.currentTimeMillis())
					.setAutoCancel(true)
					.setSmallIcon(R.mipmap.ic_launcher)
					.build();
		}

		notification.contentIntent = pendingIntent;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(MY_NOTIFICATION_ID, notification);

	}

	@Override
	public void onCreate()
	{
		// TODO Auto-generated method stub
		Log.d(LOG_TAG," service created");
		super.onCreate();
		notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
	}


}
