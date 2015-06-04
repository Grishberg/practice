package com.grishberg.livegoodlineparser.data.asynctaskloaders;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.grishberg.livegoodlineparser.data.containers.TopicListContainer;
import com.grishberg.livegoodlineparser.data.interfaces.IGetTopilistListener;
import com.grishberg.livegoodlineparser.data.model.NewsDbHelper;

/**
 * Created by G on 29.05.15.
 */

/**
 * jlkjlasd
 */
public abstract class BaseAsynctaskLoader extends AsyncTaskLoader
{
	public static final int	CODE_ERROR		= -1;
	public static final String PARAM_ID		= "paramId";
	public static final String BASE_TAG		= "AsyncLoader";
	public static final int MSGCODE_MESSAGE	= 100;
	protected NewsDbHelper 			mDbHelper;

	// Hndler for calling onUpdateProgress method in UI thread
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			if(msg.what == MSGCODE_MESSAGE){
				if(msg.obj != null) {
					onUpdateProgress(msg.obj);
				}
			}
		}
	};

	public BaseAsynctaskLoader(Context context)
	{
		super(context);
		mDbHelper = new NewsDbHelper(context);
	}

	/**
	 * post result for update progress in UI while task's thread is running
	 * @param progressResult any object to retrive in UI thread
	 */
	protected void publishProgress(Object progressResult )
	{
		Message message = mHandler.obtainMessage();
		message.obj = progressResult;
		message.what = MSGCODE_MESSAGE;
		message.sendToTarget();
	}

	/**
	 * method calls in UI thread
	 * @param progressResult - result for return in UI thread
	 */
	abstract protected void onUpdateProgress( Object progressResult);

	/**
	 * set listener for update progress in UI thread
	 * @param listener
	 */
	abstract public void setListener(Object listener);
}
