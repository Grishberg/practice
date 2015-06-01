package com.grishberg.livegoodlineparser.ui.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.grishberg.livegoodlineparser.R;

import com.grishberg.livegoodlineparser.data.asynctaskloaders.GetNewsTask;
import com.grishberg.livegoodlineparser.data.containers.NewsBodyContainer;
import com.grishberg.livegoodlineparser.data.interfaces.IGetNewsListener;
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


	private HashMap<String, Drawable> imageCache = new HashMap<String, Drawable>();

	private TextView mTvTitle;
	private TextView mTvNewsBody;
	//private ProgressDialog progressDlg;
	private static final int MAX_WIDTH = 1024;
	private static final int MAX_HEIGHT = 768;
	private final int mSize = (int) Math.ceil(Math.sqrt(MAX_WIDTH * MAX_HEIGHT));
	private String mUrl;
	private long mDate;
	private ArrayList<String> mImageUrlList;
	private boolean mFirstRun;
	private boolean mIsNeedGetNews;
	// загрузчик новостей
	private Handler mOnSpannClickHandler;
	private ImageLoader mImageLoader;

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

		String newsUrl = "";
		String newsTitle = "";
		Long date = 0L;
		Bundle args = getArguments();

		mFirstRun = true;
		if (savedInstanceState != null) {
			mFirstRun = false;
		}
		if (args != null) {
			// извлекаем параметры
			newsTitle = args.getString(PARAM_TITLE);
			mUrl = args.getString(PARAM_URL);
			mDate = args.getLong(PARAM_DATE, 0);
			mIsNeedGetNews = true;
		}
		mTvTitle = (TextView) view.findViewById(R.id.tvNewsFragmentTitle);
		mTvNewsBody = (TextView) view.findViewById(R.id.tvNewsFragmentBody);

		mImageLoader = ImageLoader.getInstance();

		mTvTitle.setText(newsTitle);
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

//		progressDlg = new ProgressDialog(getActivity());
//		progressDlg.setTitle("Ожидание");
//		progressDlg.setMessage("Идет загрузка новости...");
//		progressDlg.show();

		mImageUrlList = new ArrayList<String>();
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mIsNeedGetNews) {
			if (mFirstRun) {
				mFirstRun = false;
				getPageContent(mUrl, mDate);
			} else {
				// восстановление при повороте экрана
				//Loader loader = getLoaderManager().initLoader(TASK_ID_GET_NEWS_BODY, null, this);
				//((GetNewsTask) loader).setListener(this);
			}
		}
	}

	//------------------- процедура фоновой загрузки страницы -------------------------
	private void getPageContent(String url, long date) {
		Bundle bundle = new Bundle();
		bundle.putString(GetNewsTask.PARAM_URL, url);
		bundle.putLong(GetNewsTask.PARAM_DATE, date);

		getLoaderManager().restartLoader(TASK_ID_GET_NEWS_BODY, bundle, this);
	}

	//----------- результаты выполнения асинхронных методов
	@Override
	public void onLoadFinished(Loader loader, Object data) {
		hideProgress();
		NewsBodyContainer result = (NewsBodyContainer) data;
		if (result.getErrorCode() < 0) {
			// ошибка
		} else {
			doAfterNewsBodyReceived(result.getNews(), false);
		}
	}

	@Override
	public void onProgress(NewsBodyContainer progressResult) {
		if (progressResult.getErrorCode() < 0) {
			// ошибка
		} else {
			doAfterNewsBodyReceived(progressResult.getNews(), true);
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

	private void doAfterNewsBodyReceived(String newsBody, boolean fromCache) {
		try {
			// в тело textView помещается тело статьи, асинхронно подгружаются картинки с сохранением в кэш
			if (fromCache == false) {
				hideProgress();
			}
			if (newsBody == null) {
				return;
			}
			mImageUrlList = new ArrayList<String>();
			Spanned spanned = Html.fromHtml(newsBody,
					new Html.ImageGetter() {
						@Override
						public Drawable getDrawable(String source) // вызывается для загрузки изображений
						{
							// заполнить массив ссылок на изображения
							mImageUrlList.add(source);
							LevelListDrawable d = new LevelListDrawable();
							Drawable empty = getResources().getDrawable(R.drawable.abc_btn_check_material);
							;
							d.addLevel(0, 0, empty);
							d.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());
							new ImageGetterAsyncTask(getActivity(), source, d).execute(mTvNewsBody);

							return d;
						}
					}, null);

			mTvNewsBody.setText(spanned);
		} catch (Exception ex) {
			ex.printStackTrace();
			Log.d(TAG, " error on set news body " + ex.toString());
		}
	}

	// событие вызывается при клике на ссылку
	private boolean onUrlClick(String url) {
		Log.d(TAG, "onUrlClick url = " + url);
		return false;
	}

	// событие вызывается при клике на картинку
	private boolean onImageClick(String imageSourceUrl) {

		for (int imageIndex = 0; imageIndex < mImageUrlList.size(); imageIndex++) {
			if (mImageUrlList.get(imageIndex).equals(imageSourceUrl)) {
				// найден индекс изображения в массиве, передать в активити
				showImagesGallery(imageIndex, mImageUrlList);
				Log.d(TAG, "onImageClick imageIndex = " + imageIndex);
				return true;
			}
		}
		return false;
	}

	// отобразить полноэкранный просмотр галереи
	private void showImagesGallery(int imageIndex, ArrayList<String> imageUrlList) {
		// launch full screen activity
		Intent intent = new Intent(getActivity(), ActivityImageGallery.class);
		intent.putExtra(ActivityImageGallery.INTENT_POSITION, imageIndex);
		intent.putExtra(ActivityImageGallery.INTENT_URL_ARRAY, imageUrlList);
		try {
			getActivity().startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void hideProgress() {
		// отключаем прогрессбар
		//progressDlg.dismiss();
	}

	private void showProgress() {
		//progressDlg.show();
	}


	//---- inner class фоновой загрузки изображений--------------------------
	class ImageGetterAsyncTask extends AsyncTask<TextView, Void, Bitmap> {
		private LevelListDrawable levelListDrawable;
		private Context context;
		private String source;
		private TextView t;

		public ImageGetterAsyncTask(Context context, String source, LevelListDrawable levelListDrawable) {
			this.context = context;
			this.source = source;
			this.levelListDrawable = levelListDrawable;
		}

		@Override
		protected Bitmap doInBackground(TextView... params) {
			t = params[0];
			try {
				Bitmap bmp = mImageLoader.loadImageSync(source);
				//bmp = Picasso.with(context).load(source)
				//		.transform(new BitmapTransform(MAX_WIDTH, MAX_HEIGHT))
				//		.resize(mSize, mSize).centerInside()
				//		.get();
				return bmp;
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Bitmap bitmap) {
			try {
				Drawable d = new BitmapDrawable(getResources(), bitmap);
				Point size = new Point();
				getActivity().getWindowManager().getDefaultDisplay().getSize(size);
				// вычисление коэфициента, для пропорционального изменения размера фотографий
				float multiplier = (float) size.x / (float) bitmap.getWidth();
				int newWidth = (int) (bitmap.getWidth() * multiplier);
				int newHeight = (int) (bitmap.getHeight() * multiplier);

				levelListDrawable.addLevel(1, 1, d);
				// Set bounds width  and height according to the bitmap resized size
				levelListDrawable.setBounds(0, 0, newWidth, newHeight);

				levelListDrawable.setLevel(1);
				t.setText(t.getText()); // invalidate() doesn't work correctly...
			} catch (Exception e) { /* Like a null bitmap, etc. */ }
		}
	}
}
