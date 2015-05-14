package com.grishberg.livegoodlineparser;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.grishberg.livegoodlineparser.livegoodlineparser.LiveGoodlineParser;
import com.grishberg.livegoodlineparser.livegoodlineparser.NewsElement;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CheckNewsUpdatesService extends IntentService
{
	public static final String LOG_TAG = "LiveGL.service";
	public static final String ACTION_Update = "com.grishberg.livegoodlineinfo.UPDATE";
	private static final int MY_NOTIFICATION_ID=1;

	NotificationManager notificationManager;
	Notification notification;

	public CheckNewsUpdatesService()
	{
		super(CheckNewsUpdatesService.class.getName());
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
		Log.d(LOG_TAG," on Handle intent");

		//final String broadcast = intent.getStringExtra(Datagate.RESPONSE_BROADCAST);
		//final Request request = (Request) intent.getSerializableExtra(REQUEST);
		//final Bundle bundle = intent.getParcelableExtra(Datagate.BUNDLE);
		// синхронная загрузка из Volley
		RequestQueue queue			= Volley.newRequestQueue(getApplicationContext());
		RequestFuture<String> futureRequest = RequestFuture.newFuture();
		StringRequest getRequest	= new StringRequest(Request.Method.GET
				, LiveGoodlineInfoDownloader.mainUrl
				, futureRequest, futureRequest);
		queue.add(getRequest);

		try
		{
			String response = futureRequest.get(30, TimeUnit.SECONDS);
			// спарсить полученную строку
			List<NewsElement> topicListFromWeb = LiveGoodlineParser.getNewsPerPage(response);
			// TODO: сохранить кэш в базу
			LiveGoodlineInfoDownloader downloader = new LiveGoodlineInfoDownloader(getApplicationContext());
			Date date = downloader.getMaxDateInCache();
			if(date == null)
			{
				date = new Date(0);
			}
			boolean wasNotifications = false;
			for(NewsElement currentNews: topicListFromWeb)
			{
				if (currentNews.getDate().compareTo(date) > 0)
				{
					// отобразить новость
					// послать команду на обновление активити, либо на открытие новости.
					Intent notificationIntent = new Intent(getApplicationContext(), TopicListActivity.class);
					notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
							| Intent.FLAG_ACTIVITY_SINGLE_TOP);

					PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
							notificationIntent, 0);

					if(wasNotifications == false)
					{
						notification = new NotificationCompat.Builder(getApplicationContext())
								.setContentTitle(getString(R.string.app_name))
								.setContentText(currentNews.getTitle())
								.setTicker("Notification!")
								.setWhen(System.currentTimeMillis())
								.setAutoCancel(true)
								.setSmallIcon(R.drawable.goodlinelogomini)
								.setDefaults(Notification.DEFAULT_SOUND)
								.build();
						wasNotifications = true;
					}
					else
					{
						notification = new NotificationCompat.Builder(getApplicationContext())
								.setContentTitle(getString(R.string.app_name))
								.setContentText(currentNews.getTitle())
								.setTicker("Notification!")
								.setWhen(System.currentTimeMillis())
								.setAutoCancel(true)
								.setSmallIcon(R.drawable.goodlinelogomini)
								.build();
					}

					notification.contentIntent = pendingIntent;
					notification.flags |= Notification.FLAG_AUTO_CANCEL;
					notificationManager.notify(MY_NOTIFICATION_ID, notification);
				}
				else
				{
					//topicListFromWeb.remove(currentNews);
				}
				//TODO: добавить в кэш
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
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
