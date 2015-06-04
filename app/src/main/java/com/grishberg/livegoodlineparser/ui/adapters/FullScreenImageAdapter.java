package com.grishberg.livegoodlineparser.ui.adapters;

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
 * Edited by Rylov Grigoriy
 */
public class FullScreenImageAdapter extends PagerAdapter{

	private static final String TAG = "LiveGL.FsAdapter";
	private Activity 			mActivity;
	private ArrayList<String>	mImageUrls;
	private TouchImageView		mImageContainer;

	private int mScreenWidth, mScreenHeight;
	private int mSize;

	public FullScreenImageAdapter(ActivityImageGallery activityImageGallery
			, ArrayList<String> imageLinksList
			, Resources resources) {
		mActivity	= activityImageGallery;
		mImageUrls	= imageLinksList;
	}

	@Override
	public int getCount() {
		return mImageUrls.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((RelativeLayout) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {

		LayoutInflater inflater = (LayoutInflater) mActivity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.image_gallery_item, container, false);
		mImageContainer = (TouchImageView) viewLayout.findViewById(R.id.imgDispImgGal);
		loadImage( mImageUrls.get(position));
		((ViewPager) container).addView(viewLayout);
		return viewLayout;
	}

	/**
	 * asynchronously load image in mImageContainer
	 * @param url
	 */
	public void loadImage(String url)
	{
		ImageLoader imageLoader = ImageLoader.getInstance();
		DisplayImageOptions options = new DisplayImageOptions.Builder().cacheInMemory(true)
				.cacheOnDisc(true).resetViewBeforeLoading(true)
						//TODO: chose image resources for show in different state
				//.showImageForEmptyUri(fallback)
				//.showImageOnFail(fallback)
				//.showImageOnLoading(fallback) // картинка во время загрузки
				.build();


		//download and display image from url
		imageLoader.displayImage(url, mImageContainer, options);
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		((ViewPager) container).removeView((RelativeLayout) object);
	}

}