package com.grishberg.livegoodlineparser.data.asynctaskloaders;

import android.content.Context;

/**
 * Created by G on 29.05.15.
 * async task for clearing cache
 */
public class ClearCacheTask extends BaseAsynctaskLoader {
	public ClearCacheTask(Context context) {
		super(context);

	}

	@Override
	public Object loadInBackground() {
		mDbHelper.clearDb();
		return null;
	}

	@Override
	protected void onUpdateProgress(Object progressResult) {
	}

	@Override
	public void setListener(Object listener) {
	}
}
