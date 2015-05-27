package com.grishberg.livegoodlineparser.ui.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.grishberg.livegoodlineparser.R;

import com.grishberg.livegoodlineparser.ui.activities.NewsActivity;
import com.grishberg.livegoodlineparser.ui.bitmaputils.BitmapTransform;
import com.grishberg.livegoodlineparser.data.IGetNewsResponseListener;
import com.grishberg.livegoodlineparser.data.LiveGoodlineInfoDownloader;
import com.grishberg.livegoodlineparser.ui.listeners.LinkMovementMethodExt;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import com.grishberg.livegoodlineparser.ui.activities.ActivityImageGallery;


/**
 * фрагмент который будет содержать новость.
 */
public class NewsActivityFragment extends Fragment
{
	public static final String TAG = "LiveGL.NewsBody";
	public static final int IMAGE_CLICK = 100;

	private HashMap<String, Drawable> imageCache = new HashMap<String, Drawable>();

	private TextView mTvTitle;
	private TextView mTvNewsBody;
	private ProgressDialog progressDlg;
	private static final int MAX_WIDTH = 1024;
	private static final int MAX_HEIGHT = 768;
	private final int mSize = (int) Math.ceil(Math.sqrt(MAX_WIDTH * MAX_HEIGHT));
	private ArrayList<String> mImageUrlList;
	// загрузчик новостей
	private LiveGoodlineInfoDownloader downloader;
	private Handler mOnClickHandler;

	public NewsActivityFragment()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_news, container, false);

		mTvTitle = (TextView) view.findViewById(R.id.tvNewsFragmentTitle);
		mTvNewsBody = (TextView) view.findViewById(R.id.tvNewsFragmentBody);

		// извлекаем ссылку на статью
		Intent intent = getActivity().getIntent();
		String newsUrl = intent.getStringExtra(TopicListActivityFragment.NEWS_URL_INTENT);
		String newsTitle = intent.getStringExtra(TopicListActivityFragment.NEWS_TITLE_INTENT);
		Long lDate = intent.getLongExtra(TopicListActivityFragment.NEWS_DATE_INTENT, 0);

		Date date = new Date(lDate);

		// изменение title
		NewsActivity parent = (NewsActivity) getActivity();
		if (parent != null)
			parent.setTitle(newsTitle);

		mTvTitle.setText(newsTitle);
		mTvNewsBody.setText(""); // для удобства настройки расположения элемента

		// событие при клике на ссылку или изображение в статье
		mOnClickHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				if (msg.what == IMAGE_CLICK)
				{
					Object span = msg.obj;
					if (span instanceof URLSpan)
					{
						onUrlClick(((URLSpan) span).getURL());
					}
					if (span instanceof ImageSpan)
					{
						onImageClick(((ImageSpan) span).getSource());
					}
				}
			}
		};

		mTvNewsBody.setMovementMethod(LinkMovementMethodExt.getInstance(mOnClickHandler, ImageSpan.class));
		mTvNewsBody.setMovementMethod(LinkMovementMethodExt.getInstance(mOnClickHandler, URLSpan.class));


		progressDlg = new ProgressDialog(getActivity());
		progressDlg.setTitle("Ожидание");
		progressDlg.setMessage("Идет загрузка новости...");
		progressDlg.show();

		downloader = new LiveGoodlineInfoDownloader(getActivity());
		mImageUrlList = new ArrayList<String>();
		getPageContent(newsUrl, date);
		return view;
	}

	//------------------- процедура фоновой загрузки страницы -------------------------
	private void getPageContent(String url, Date date)
	{
		downloader.getNewsPage(getActivity(), url, date, new IGetNewsResponseListener()
		{
			@Override
			public void onResponseGetNewsPage(String newsBody, boolean fromCache, int errorCode)
			{
				if (errorCode == 0)
				{
					doAfterNewsBodyReceived(newsBody, fromCache);
				} else
				{
					progressDlg.dismiss();
					Toast.makeText(getActivity(), "Неудачная попытка соединиться с сервером.", Toast.LENGTH_SHORT).show();

				}
			}
		});
	}

	private void doAfterNewsBodyReceived(String newsBody, boolean fromCache)
	{
		// в тело textView помещается тело статьи, асинхронно подгружаются картинки с сохранением в кэш
		if (fromCache == false)
		{
			hideProgress();
		}
		if (newsBody == null)
		{
			return;
		}
		mImageUrlList = new ArrayList<String>();
		Spanned spanned = Html.fromHtml(newsBody,
				new Html.ImageGetter()
				{
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

	}

	// событие вызывается при клике на ссылку
	private boolean onUrlClick(String url)
	{
		Log.d(TAG, "onUrlClick url = " + url);
		return false;
	}

	// событие вызывается при клике на картинку
	private boolean onImageClick(String imageSourceUrl)
	{
		for (int imageIndex = 0; imageIndex < mImageUrlList.size(); imageIndex++)
		{
			if (mImageUrlList.get(imageIndex).equals(imageSourceUrl))
			{
				// найден индекс изображения в массиве, передать в активити
				showImagesGallery(imageIndex, mImageUrlList);
				Log.d(TAG, "onImageClick imageIndex = " + imageIndex );
				return true;
			}
		}
		return false;
	}

	// отобразить полноэкранный просмотр галереи
	private void showImagesGallery(int imageIndex, ArrayList<String> imageUrlList)
	{
		// launch full screen activity
		Intent intent = new Intent(getActivity(), ActivityImageGallery.class);
		intent.putExtra(ActivityImageGallery.INTENT_POSITION, imageIndex);
		intent.putExtra(ActivityImageGallery.INTENT_URL_ARRAY, imageUrlList);
		try
		{
			getActivity().startActivity(intent);
		}catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void hideProgress()
	{
		// отключаем прогрессбар
		progressDlg.dismiss();
	}
	private void showProgress()
	{
		progressDlg.show();
	}



	//---- inner class фоновой загрузки изображений--------------------------
	class ImageGetterAsyncTask extends AsyncTask<TextView, Void, Bitmap>
	{
		private LevelListDrawable levelListDrawable;
		private Context context;
		private String source;
		private TextView t;

		public ImageGetterAsyncTask(Context context, String source, LevelListDrawable levelListDrawable)
		{
			this.context	= context;
			this.source		= source;
			this.levelListDrawable = levelListDrawable;
		}

		@Override
		protected Bitmap doInBackground(TextView... params)
		{
			t = params[0];
			try
			{
				return Picasso.with(context).load(source)
						.transform(new BitmapTransform(MAX_WIDTH, MAX_HEIGHT))
						.resize(mSize, mSize).centerInside()
						.get();
			} catch (Exception e)
			{
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Bitmap bitmap)
		{
			try
			{
				Drawable d = new BitmapDrawable(getResources(), bitmap);
				Point size = new Point();
				getActivity().getWindowManager().getDefaultDisplay().getSize(size);
				// вычисление коэфициента, для пропорционального изменения размера фотографий
				float multiplier	= (float)size.x /(float) bitmap.getWidth();
				int newWidth		= (int)(bitmap.getWidth() * multiplier);
				int newHeight		= (int)(bitmap.getHeight() * multiplier);

				levelListDrawable.addLevel(1, 1, d);
				// Set bounds width  and height according to the bitmap resized size
				levelListDrawable.setBounds(0, 0, newWidth,newHeight);

				levelListDrawable.setLevel(1);
				t.setText(t.getText()); // invalidate() doesn't work correctly...
			} catch (Exception e) { /* Like a null bitmap, etc. */ }
		}
	}
}
