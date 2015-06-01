package com.grishberg.livegoodlineparser.data.asynctaskloaders;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.grishberg.livegoodlineparser.data.containers.CachedNewsContainer;
import com.grishberg.livegoodlineparser.data.containers.NewsBodyContainer;
import com.grishberg.livegoodlineparser.data.containers.NewsContainer;
import com.grishberg.livegoodlineparser.data.interfaces.IGetNewsListener;
import com.grishberg.livegoodlineparser.data.livegoodlineparser.LiveGoodlineParser;
import com.grishberg.livegoodlineparser.ui.fragments.TopicListActivityFragment;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by G on 29.05.15.
 */
public class GetNewsTask extends BaseAsynctaskLoader
{
	public static final String	TAG			= "LiveGL.GetNewsTask";
	public static final String	PARAM_DATE	= "paramDate";
	public static final String	PARAM_URL	= "paramUrl";

	private IGetNewsListener mListener;
	private Context mContext;
	private long mDate;
	private String mUrl;
	public GetNewsTask(Context context, Bundle bundle,IGetNewsListener listener)
	{
		super(context);
		mContext	= context;
		mListener	= listener;
		// прочитать параметры из bundle
		if(bundle != null)
		{
			mDate = bundle.getLong(PARAM_DATE, 0);
			mUrl = bundle.getString(PARAM_URL);
		}
	}

	@Override
	public Object loadInBackground()
	{
		CachedNewsContainer cachedNewsBody	= null;
		String webNewsBody					= null;
		int errorCode						= 0;

		cachedNewsBody	= mDbHelper.getNewsPage(mDate);
		//использовать publishProgress для вывода промежуточного результата из кэша
		if(cachedNewsBody != null &&
				cachedNewsBody.getBody() != null &&
				cachedNewsBody.getBody().length() >0)
		{
			Log.d(TAG, "Данные из кэша");
			if (cachedNewsBody.getDescriptionStatus() == true)
			{
				// в кэше превью,
				publishProgress(new NewsBodyContainer(cachedNewsBody.getBody(), errorCode));
			}
			else
			{
				return new NewsBodyContainer(cachedNewsBody.getBody(), errorCode);
			}
		}

		// синхронная загрузка из Volley
		RequestQueue queue					= Volley.newRequestQueue(mContext);
		RequestFuture<String> futureRequest = RequestFuture.newFuture();
		StringRequest getRequest			= new StringRequest(Request.Method.GET
				, mUrl
				, futureRequest, futureRequest);
		queue.add(getRequest);

		try
		{
			String response = futureRequest.get(TopicListActivityFragment.VOLLEY_SYNC_TIMEOUT, TimeUnit.SECONDS);
			// спарсить полученную строку
			webNewsBody = LiveGoodlineParser.getNews(response);
			//сохранить в кэше
			mDbHelper.updateNewsBody(mDate,webNewsBody);
			Log.d(TAG, "Данные из сети");

		} catch (InterruptedException e)
		{
			errorCode = -1;
			e.printStackTrace();
		} catch (ExecutionException e)
		{
			errorCode = -2;
			e.printStackTrace();
		} catch (TimeoutException e)
		{
			errorCode = -3;
			e.printStackTrace();
		}

		return new NewsBodyContainer(webNewsBody, errorCode);
	}

	@Override
	protected void onUpdateProgress(Object progressResult)
	{
		if(mListener != null)
		{
			mListener.onProgress((NewsBodyContainer)progressResult);
		}
	}
	@Override
	public void setListener(Object listener)
	{
		mListener	= (IGetNewsListener) listener;
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
