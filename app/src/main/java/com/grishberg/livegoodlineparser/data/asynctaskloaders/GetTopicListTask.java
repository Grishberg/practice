package com.grishberg.livegoodlineparser.data.asynctaskloaders;

import android.content.Context;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.grishberg.livegoodlineparser.data.containers.NewsContainer;
import com.grishberg.livegoodlineparser.data.containers.TopicListContainer;
import com.grishberg.livegoodlineparser.data.interfaces.IGetTopilistListener;
import com.grishberg.livegoodlineparser.data.livegoodlineparser.LiveGoodlineParser;
import com.grishberg.livegoodlineparser.ui.fragments.TopicListActivityFragment;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by G on 29.05.15.
 */
public class GetTopicListTask extends BaseAsynctaskLoader
{
	public static final String	mainUrl	= "http://live.goodline.info/guest";
	public static final String	PARAM_DATE	= "paramDate";
	public static final String	PARAM_PAGE	= "paramPage";
	public static final String	PARAM_INSERT_TO_TOP	= "paramInserToTop";


	private Context	mContext;
	private IGetTopilistListener mListener;
	private long mDate;
	private int mPage;
	private boolean mInsertToTop;
	public GetTopicListTask(Context context, Bundle bundle)
	{
		super(context);
		mContext	= context;
		// прочитать параметры из bundle
		mDate	= bundle.getLong(PARAM_DATE,0);
		mPage	= bundle.getInt(PARAM_PAGE, 1);
		mInsertToTop = bundle.getBoolean(PARAM_INSERT_TO_TOP);

	}

	@Override
	public Object loadInBackground()
	{
		int errorCode		= 0;
		List<NewsContainer> topicListFromCache	= null;
		List<NewsContainer> topicListFromWeb	= null;

		topicListFromCache	= mDbHelper.getTopicList(mDate);

		//использовать onProgressUpdate для вывода промежуточного результата из кэша
		if(topicListFromCache != null && topicListFromCache.size() > 0)
		{
			publishProgress( new TopicListContainer(topicListFromCache, errorCode, mInsertToTop));
		}

		// отправка запроса на закачку страницы
		String url = mainUrl;
		//TODO: если статьи добавляются сверху, в цикле добавлять, пока дата любой из статьи
		//		на странице не равна date
		// скорректировать URL в зависимости от страницы
		if(mPage > 1)
		{
			url = String.format("%s/page%d/", mainUrl, mPage);
		}

		// синхронная загрузка из Volley
		RequestQueue queue			= Volley.newRequestQueue(mContext);
		RequestFuture<String> futureRequest = RequestFuture.newFuture();
		StringRequest getRequest	= new StringRequest(Request.Method.GET
				, url
				, futureRequest, futureRequest);
		queue.add(getRequest);

		try
		{
			String response = futureRequest.get(TopicListActivityFragment.VOLLEY_SYNC_TIMEOUT, TimeUnit.SECONDS);
			topicListFromWeb = LiveGoodlineParser.getNewsPerPage(response);

			//=============================================================
			//++ для отладки
			//=============================================================
			//if(page == 1 && insertToTop == false)
			//{
			//	if(topicListFromWeb != null && topicListFromWeb.size() > 0)
			//	{
			//		topicListFromWeb.remove(0);
			//	}
			//}
			//--

			// сохранить кэш в базу
			mDbHelper.storeTopicList(topicListFromWeb);
			// спарсить полученную строку
		} catch (InterruptedException e)
		{
			errorCode = -1;
			e.printStackTrace();
		} catch (ExecutionException e) {
			errorCode = -2;
			e.printStackTrace();
		} catch (TimeoutException e) {
			errorCode = -3;
			e.printStackTrace();
		}

		// подгрузить в фоне из сети
		return new TopicListContainer(topicListFromWeb, errorCode, mInsertToTop);
	}

	@Override
	protected void publishProgress(Object progressResult)
	{
		if(mListener != null)
		{
			mListener.onProgress((TopicListContainer)progressResult);
		}
	}

	@Override
	public void setListener(Object listener)
	{
		mListener	= (IGetTopilistListener) listener;
	}

	@Override
	public void releaseListener()
	{
		mListener	= null;
	}

	@Override
	protected void onStopLoading()
	{
		super.onStopLoading();
		mListener	= null;
	}
}
