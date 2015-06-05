package com.grishberg.livegoodlineparser.ui.bitmaputils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * Created by grigoriy on 05.06.15.
 */
public class ImageGetterAsyncTask extends AsyncTask<TextView, Void, Bitmap> {
	private LevelListDrawable mLevelListDrawable;
	private Context mContext;
	private Resources mResources;
	private String		mImageUrl;
	private float 		mImageWidth;
	private float		mImageHeight;
	private TextView	mContainer;

	public ImageGetterAsyncTask(Context context
			, Resources resources
			, String source
			, LevelListDrawable levelListDrawable
			, int imageWidth
			, int imageHeight) {
		mContext 			= context;
		mImageUrl 			= source;
		mLevelListDrawable	= levelListDrawable;
		mImageWidth			= (float)imageWidth;
		mImageHeight		= (float)imageHeight;
		mResources			= resources;
	}

	@Override
	protected Bitmap doInBackground(TextView... params) {
		mContainer = params[0];
		try {
			Bitmap bmp = ImageLoader.getInstance().loadImageSync(mImageUrl);
			return bmp;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	protected void onPostExecute(final Bitmap bitmap) {
		try {
			BitmapDrawable d	= new BitmapDrawable(mResources, bitmap);
//			Drawable d = new BitmapDrawable(mResources, bitmap);

			// calculate ratio h:m and resize image container
			float multiplier	= mImageWidth < mImageHeight ?
					mImageWidth / (float) bitmap.getWidth()
					:mImageHeight / (float) bitmap.getHeight() ;
			int newWidth		= (int) (bitmap.getWidth() * multiplier);
			int newHeight		= (int) (bitmap.getHeight() * multiplier);
			//d.setGravity(Gravity.AXIS_X_SHIFT);
			mLevelListDrawable.addLevel(1, 1, d);
			// Set bounds width  and height according to the bitmap resized size
			//mLevelListDrawable.setBounds(0, 0, (int)mImageWidth, newHeight);
			mLevelListDrawable.setBounds(0, 0, newWidth, newHeight);
			mLevelListDrawable.setLevel(1);
			mContainer.setText(mContainer.getText()); // invalidate() doesn't work correctly...
		} catch (Exception e) { /* Like a null bitmap, etc. */ }
	}
}