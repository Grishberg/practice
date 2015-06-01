package com.grishberg.livegoodlineparser.data.asynctaskloaders;

import android.content.Context;

/**
 * Created by G on 29.05.15.
 */
public class ClearCacheTask extends BaseAsynctaskLoader {
	public ClearCacheTask(Context context) {
		super(context);

		// прочитать параметры из bundle
	}

	@Override
	public Object loadInBackground() {
		mDbHelper.clearDb();
		return null;
	}

	@Override
	public void releaseListener() {
	}

	@Override
	protected void onUpdateProgress(Object progressResult) {
	}

	@Override
	public void setListener(Object listener) {
	}
}
