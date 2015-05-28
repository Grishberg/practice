package com.grishberg.livegoodlineparser.ui.adapters;

/**
 * Created by Сергей Балдин on 26.05.15.
 */
import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.method.Touch;
import android.util.Log;
import android.widget.ImageView;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.grishberg.livegoodlineparser.R;
import com.grishberg.livegoodlineparser.ui.activities.ActivityImageGallery;
import com.grishberg.livegoodlineparser.ui.bitmaputils.BitmapTransform;
import com.grishberg.livegoodlineparser.ui.bitmaputils.TouchImageView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by Балдин Сергей on 26.05.2015.
 */
/*
public class FullScreenImageAdapter extends PagerAdapter {

	private Activity _activity;
	private ArrayList<String> mImageLinks;
	private LayoutInflater inflater;
	private int mScreenWidth, mScreenHeight;
	private int mSize;

	// constructor
	public FullScreenImageAdapter(Activity activity,
								  ArrayList<String> imageLinks) {
		this._activity		= activity;
		this.mImageLinks 	= imageLinks;

		// запомнить размеры окна для масштабирования измображения
		Point size			= new Point();
		this._activity.getWindowManager().getDefaultDisplay().getSize(size);
		mScreenWidth		= size.x;
		mScreenHeight		= size.y;
		mSize = (int) Math.ceil(Math.sqrt(mScreenWidth * mScreenHeight));
	}

	@Override
	public int getCount() {
		return this.mImageLinks.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((RelativeLayout) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		ImageView imgDisplay;

		inflater = (LayoutInflater) _activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.image_gallery_item, container,
				false);

		imgDisplay = (ImageView) viewLayout.findViewById(R.id.imgDispImgGal);

		String currentImageLink = mImageLinks.get(position);
		try
		{
			Picasso.with(_activity).load(currentImageLink)
					.transform(new BitmapTransform(mScreenWidth, mScreenHeight))
					.resize(mSize, mSize).centerInside()
					.into(imgDisplay);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		((ViewPager) container).addView(viewLayout);

		return viewLayout;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		((ViewPager) container).removeView((RelativeLayout) object);

	}
}
*/

/**
 * Created by Балдин Сергей on 26.05.2015.
 */
public class FullScreenImageAdapter extends PagerAdapter{

	private static final String TAG = "LiveGL.FsAdapter";
	private Activity _activity;
	private ArrayList<String> mImageLinks;
	private LayoutInflater inflater;
	private TouchImageView mImgDisplay;

	// для ресайза
	private int mScreenWidth, mScreenHeight;
	private int mSize;

	public FullScreenImageAdapter(ActivityImageGallery activityImageGallery, ArrayList<String> imageLinksList, Resources resources) {
		this._activity = activityImageGallery;
		this.mImageLinks = imageLinksList;

		// запомнить размеры окна для масштабирования измображения
		Point size			= new Point();
		this._activity.getWindowManager().getDefaultDisplay().getSize(size);
		mScreenWidth		= size.x;
		mScreenHeight		= size.y;
		mSize = mScreenWidth < mScreenHeight? mScreenWidth: mScreenHeight;

	}

	@Override
	public int getCount() {
		return this.mImageLinks.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((RelativeLayout) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {

		inflater = (LayoutInflater) _activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.image_gallery_item, container,
				false);

		mImgDisplay = (TouchImageView) viewLayout.findViewById(R.id.imgDispImgGal);

		loadImage( mImageLinks.get(position));

		((ViewPager) container).addView(viewLayout);

		return viewLayout;
	}

	public void loadImage(String url)
	{
		ImageLoader imageLoader = ImageLoader.getInstance();
		DisplayImageOptions options = new DisplayImageOptions.Builder().cacheInMemory(true)
				.cacheOnDisc(true).resetViewBeforeLoading(true)
				//.showImageForEmptyUri(fallback)
				//.showImageOnFail(fallback)
				//.showImageOnLoading(fallback) // картинка во время загрузки
				.build();


		//download and display image from url
		imageLoader.displayImage(url, mImgDisplay, options);
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		((ViewPager) container).removeView((RelativeLayout) object);
	}

}