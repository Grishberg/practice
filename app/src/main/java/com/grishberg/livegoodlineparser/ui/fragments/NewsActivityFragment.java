package com.grishberg.livegoodlineparser.ui.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.grishberg.livegoodlineparser.R;

import com.grishberg.livegoodlineparser.data.asynctaskloaders.GetNewsTask;
import com.grishberg.livegoodlineparser.data.containers.NewsBodyContainer;
import com.grishberg.livegoodlineparser.data.interfaces.IGetNewsListener;
import com.grishberg.livegoodlineparser.data.model.MyTagHandler;
import com.grishberg.livegoodlineparser.ui.bitmaputils.ImageGetterAsyncTask;
import com.grishberg.livegoodlineparser.ui.listeners.LinkMovementMethodExt;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.HashMap;

import com.grishberg.livegoodlineparser.ui.activities.ActivityImageGallery;


/**
 * фрагмент который будет содержать новость.
 */
public class NewsActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks
		, IGetNewsListener {
	public static final String TAG = "LiveGL.NewsBody";
	public static final int IMAGE_CLICK = 100;
	private static final int TASK_ID_GET_NEWS_BODY = 2;
	public static final String PARAM_TITLE = "paramTitle";
	public static final String PARAM_DATE = "paramDate";
	public static final String PARAM_URL = "paramUrl";
	public static final String RESTORE_NEWS_BODY 			= "restoreNewsBody";
	public static final String RESTORE_NEWS_SCROLL_OFFSET	= "restoreNewsOffset";
	public static final String RESTORE_NEWS_TITLE			= "restoreNewsTitle";


	private HashMap<String, Drawable> imageCache = new HashMap<String, Drawable>();

	private TextView mTvTitle;
	private TextView mTvNewsBody;
	private ScrollView mScrollView;
	private long mDate;
	private ArrayList<String> mImageUrlList;
	private String	mNewsBody;
	private String	mNewsTitle;
	private int		mNewsImagesWidth;
	private int		mNewsImagesHeight;
	private Handler mOnSpannClickHandler;

	public static NewsActivityFragment newInstance(String newsTitle, String url, long date) {
		NewsActivityFragment instance = new NewsActivityFragment();

		Bundle args = new Bundle();
		args.putString(PARAM_TITLE, newsTitle);
		args.putLong(PARAM_DATE, date);
		args.putString(PARAM_URL, url);
		instance.setArguments(args);
		return instance;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	public NewsActivityFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_news, container, false);

		mNewsBody	= "";
		mNewsTitle	= "";
		Bundle args = getArguments();
		String url	= "";

		if (args != null) {
			// извлекаем параметры
			mNewsTitle	= args.getString(PARAM_TITLE);
			url		= args.getString(PARAM_URL);
			mDate		= args.getLong(PARAM_DATE, 0);
		}
		mTvTitle = (TextView) view.findViewById(R.id.tvNewsFragmentTitle);
		mTvNewsBody = (TextView) view.findViewById(R.id.tvNewsFragmentBody);
		mScrollView	= (ScrollView) view.findViewById(R.id.news_fragment_scroll);

		mTvTitle.setText(mNewsTitle);
		mTvNewsBody.setText(""); // для удобства настройки расположения элемента

		// событие при клике на ссылку или изображение в статье
		mOnSpannClickHandler = new Handler() {
			public void handleMessage(Message msg) {
				if (msg.what == IMAGE_CLICK) {
					Object[] span = (Object[]) msg.obj;
					for (Object currentSpan : span) {
						if (currentSpan instanceof URLSpan) {
							onUrlClick(((URLSpan) currentSpan).getURL());
						}
						if (currentSpan instanceof ImageSpan) {
							onImageClick(((ImageSpan) currentSpan).getSource());
						}
					}
				}
			}
		};

		mTvNewsBody.setMovementMethod(new LinkMovementMethodExt(mOnSpannClickHandler
				, new Class[]{ImageSpan.class, URLSpan.class}));

		mImageUrlList = new ArrayList<String>();

		initiliazeNewsBodyData(savedInstanceState, url);

		return view;
	}

	/**
	 * initialize news body according to restoring after change screen orientation
	 * @param savedInstanceState save state bundle
	 * @param url news url
	 */
	private void initiliazeNewsBodyData(Bundle savedInstanceState, String url) {
		// restore news body
		if (savedInstanceState != null) {
			mNewsBody	= savedInstanceState.getString(RESTORE_NEWS_BODY);
			mNewsTitle	= savedInstanceState.getString(RESTORE_NEWS_TITLE);
			mTvTitle.setText(mNewsTitle);
			// get textView size when it draws on screen
			mTvNewsBody.post(new Runnable() {
				@Override
				public void run() {
					mNewsImagesHeight	= mScrollView.getMeasuredHeight();
					mNewsImagesWidth = mTvNewsBody.getMeasuredWidth();
					doAfterNewsBodyReceived(mNewsBody, false, mNewsImagesWidth, mNewsImagesHeight);
				}
			});

			final int newsTopOffset	= savedInstanceState.getInt(RESTORE_NEWS_SCROLL_OFFSET);
			if(newsTopOffset > 0)
				mScrollView.post(new Runnable() {
					public void run() {
						mScrollView.scrollTo(0, newsTopOffset);
					}
				});
		} else {
			if(url.length() > 0) {
				// get textView size when it draws on screen
				mTvNewsBody.post(new Runnable() {
					@Override
					public void run() {
						mNewsImagesHeight	= mScrollView.getMeasuredHeight();
						mNewsImagesWidth = mTvNewsBody.getMeasuredWidth();
					}
				});
				getPageContent(url, mDate);
			}
		}
	}

	/**
	 * save news body state
	 * @param outState
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(RESTORE_NEWS_BODY, mNewsBody);
		outState.putString(RESTORE_NEWS_TITLE, mNewsTitle);
	}

	/**
	 * create download news body task
	 * @param url
	 * @param date
	 */
	private void getPageContent(String url, long date) {
		Bundle bundle = new Bundle();
		bundle.putString(GetNewsTask.PARAM_URL, url);
		bundle.putLong(GetNewsTask.PARAM_DATE, date);

		getLoaderManager().restartLoader(TASK_ID_GET_NEWS_BODY, bundle, this);
	}

	/**
	 * event when get news task returns result
	 * @param loader reference to task
	 * @param data	NewsBodyContainer result
	 */
	@Override
	public void onLoadFinished(Loader loader, Object data) {
		hideProgress();
		NewsBodyContainer result = (NewsBodyContainer) data;
		if (result.getErrorCode() < 0) {
			// some error
		} else {
			mNewsBody	= result.getNews();
			doAfterNewsBodyReceived(result.getNews(), false, mNewsImagesWidth, mNewsImagesHeight);
		}
	}

	/**
	 * event when get news task update progress ( returns some data from cache)
	 * @param progressResult NewsBodyContainer result
	 */
	@Override
	public void onProgress(NewsBodyContainer progressResult) {
		if (progressResult.getErrorCode() < 0) {
			// some error
		} else {
			doAfterNewsBodyReceived(progressResult.getNews(), true, mNewsImagesWidth, mNewsImagesHeight);
		}
	}

	@Override
	public Loader onCreateLoader(int id, Bundle args) {
		Loader loader = null;
		switch (id) {
			case TASK_ID_GET_NEWS_BODY:
				loader = new GetNewsTask(getActivity(), args, this);
				break;
		}
		if(loader != null) {
			loader.forceLoad();
		}
		return loader;
	}


	@Override
	public void onLoaderReset(Loader loader) {

	}

	/**
	 * convert news body text into spanned elements and assign to textView
	 * @param newsBody news body text
	 * @param fromCache flag shows that data was received from cache
	 */
	private void doAfterNewsBodyReceived(String newsBody, boolean fromCache
			,final int imagesWidth
			,final int imagesHeight) {
		try {
			// в тело textView помещается тело статьи, асинхронно подгружаются картинки с сохранением в кэш
			if (!fromCache) {
				hideProgress();
			}
			if (newsBody == null) {
				return;
			}
			final Resources resources	=  getResources();
			mImageUrlList = new ArrayList<String>();

			Spanned spanned = Html.fromHtml(newsBody,
					new Html.ImageGetter() {
						// called for each image-url
						@Override
						public Drawable getDrawable(String source)
						{
							// fill images url- array for showing in gallery
							mImageUrlList.add(source);

							LevelListDrawable d = new LevelListDrawable();
							Drawable empty = resources.getDrawable(R.drawable.abc_btn_check_material);
							;
							d.addLevel(0, 0, empty);
							d.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());
							new ImageGetterAsyncTask(getActivity(),resources, source, d
									,imagesWidth, imagesHeight)
									.execute(mTvNewsBody);
							return d;
						}
					}, new MyTagHandler());

			mTvNewsBody.setText(spanned);
		} catch (Exception ex) {
			ex.printStackTrace();
			Log.d(TAG, " error on set news body " + ex.toString());
		}
	}

	/**
	 * event when user click on image
	 * @param url
	 * @return
	 */
	private boolean onUrlClick(String url) {
		Log.d(TAG, "onUrlClick url = " + url);
		return false;
	}

	/** even when user click on image in news
	 *
	 * @param imageSourceUrl
	 * @return
	 */
	private boolean onImageClick(String imageSourceUrl) {

		for (int imageIndex = 0; imageIndex < mImageUrlList.size(); imageIndex++) {
			if (mImageUrlList.get(imageIndex).equals(imageSourceUrl)) {
				showImagesGallery(imageIndex, mImageUrlList);
				Log.d(TAG, "onImageClick imageIndex = " + imageIndex);
				return true;
			}
		}
		return false;
	}

	/**
	 * call gallery activity
	 * @param imageIndex
	 * @param imageUrlList
	 */
	private void showImagesGallery(int imageIndex, ArrayList<String> imageUrlList) {
		Intent intent = new Intent(getActivity(), ActivityImageGallery.class);
		intent.putExtra(ActivityImageGallery.INTENT_POSITION, imageIndex);
		intent.putExtra(ActivityImageGallery.INTENT_URL_ARRAY, imageUrlList);
		getActivity().startActivity(intent);
	}

	// TODO: show progress after news body
	private void hideProgress() {
	}

	private void showProgress() {
	}
}
