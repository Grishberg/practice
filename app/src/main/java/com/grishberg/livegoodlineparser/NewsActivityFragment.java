package com.grishberg.livegoodlineparser;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.grishberg.livegoodlineparser.livegoodlineparser.LiveGoodlineParser;
import com.grishberg.livegoodlineparser.livegoodlineparser.NewsElement;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


/**
 * фрагмент который будет содержать новость.
 */
public class NewsActivityFragment extends Fragment
{
	private HashMap<String, Drawable> imageCache = new HashMap<String, Drawable>();

	private TextView tvTitle;
	private TextView tvNewsBody;
	private ProgressDialog progressDlg;

	// загрузчик новостей
	private LiveGoodlineInfoDownloader downloader;

	public NewsActivityFragment()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		View view			= inflater.inflate(R.layout.fragment_news, container, false);

		tvTitle				= (TextView) view.findViewById(R.id.tvNewsFragmentTitle);
		tvNewsBody			= (TextView) view.findViewById(R.id.tvNewsFragmentBody);
		// извлекаем ссылку на статью
		Intent intent		= getActivity().getIntent();
		String newsUrl		= intent.getStringExtra(TopicListActivityFragment.NEWS_URL_INTENT);
		String newsTitle	= intent.getStringExtra(TopicListActivityFragment.NEWS_TITLE_INTENT);
		Long lDate			= intent.getLongExtra(TopicListActivityFragment.NEWS_DATE_INTENT,0);

		Date date			= new Date(lDate);

		// изменение title
		NewsActivity parent	= (NewsActivity)getActivity();
		if(parent != null)
			parent.setTitle(newsTitle);

		tvTitle.setText(newsTitle);
		tvNewsBody.setText(""); // для удобства настройки расположения элемента

		progressDlg = new ProgressDialog(getActivity());
		progressDlg.setTitle("Ожидание");
		progressDlg.setMessage("Идет загрузка новости...");
		progressDlg.show();

		downloader = new LiveGoodlineInfoDownloader(getActivity());

		getPageContent(newsUrl,date);
		return view;
	}

	//------------------- процедура фоновой загрузки страницы -------------------------
	private void getPageContent(String url,Date date)
	{
		downloader.getNewsPage(getActivity(), url, date, new IGetNewsResponseListener()
				{
					@Override
					public void onResponseGetNewsPage(String newsBody)
					{
						doAfterNewsBodyReceived(newsBody);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError error)
					{
						System.out.println("Error [" + error + "]");
						progressDlg.dismiss();
					}
				});

	}

	private void doAfterNewsBodyReceived(String newsBody)
	{
		// в тело textView помещается тело статьи, асинхронно подгружаются картинки с сохранением в кэш
		Spanned spanned = Html.fromHtml(newsBody,
				new Html.ImageGetter()
				{
					@Override
					public Drawable getDrawable(String source) // вызывается для загрузки изображений
					{
						LevelListDrawable d = new LevelListDrawable();
						Drawable empty = getResources().getDrawable(R.drawable.abc_btn_check_material);;
						d.addLevel(0, 0, empty);
						d.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());
						new ImageGetterAsyncTask(getActivity(), source, d).execute(tvNewsBody);

						return d;
					}
				}, null);
		tvNewsBody.setText(spanned);

		// отключаем прогрессбар
		progressDlg.dismiss();
	}
	//------------------------------
	class ImageGetterAsyncTask extends AsyncTask<TextView, Void, Bitmap>
	{
		private LevelListDrawable levelListDrawable;
		private Context context;
		private String source;
		private TextView t;

		public ImageGetterAsyncTask(Context context, String source, LevelListDrawable levelListDrawable)
		{
			this.context = context;
			this.source = source;
			this.levelListDrawable = levelListDrawable;
		}

		@Override
		protected Bitmap doInBackground(TextView... params)
		{
			t = params[0];
			try
			{
				return Picasso.with(context).load(source).get();
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Bitmap bitmap)
		{
			try {
				Drawable d = new BitmapDrawable(getResources(), bitmap);
				Point size = new Point();
				getActivity().getWindowManager().getDefaultDisplay().getSize(size);
				// Lets calculate the ratio according to the screen width in px
				int multiplier = size.x / bitmap.getWidth();
				levelListDrawable.addLevel(1, 1, d);
				// Set bounds width  and height according to the bitmap resized size
				levelListDrawable.setBounds(0, 0, bitmap.getWidth() * multiplier, bitmap.getHeight() * multiplier);
				levelListDrawable.setLevel(1);
				t.setText(t.getText()); // invalidate() doesn't work correctly...
			} catch (Exception e) { /* Like a null bitmap, etc. */ }
		}
	}
}
