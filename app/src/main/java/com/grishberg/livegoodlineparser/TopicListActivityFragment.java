package com.grishberg.livegoodlineparser;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.grishberg.livegoodlineparser.livegoodlineparser.LiveGoodlineParser;
import com.grishberg.livegoodlineparser.livegoodlineparser.NewsElement;
import com.grishberg.livegoodlineparser.sheduling.AlarmReceiver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class TopicListActivityFragment extends Fragment  implements SwipeRefreshLayout.OnRefreshListener
{
	static final String LOG_TAG = "LiveGL.TL";
	public static final String NEWS_URL_INTENT = "currentNewsUrl";
	public static final String NEWS_TITLE_INTENT = "currentNewsTitle";
	public static final String NEWS_DATE_INTENT		= "currentNewsDate";
	// ссылка на основную страницу

	private final int           newsCountPerPage    = 10;
	private ProgressDialog progressDlg;

	// для загрузки изображений
	private RequestQueue requestQueue;
	private ImageLoader imageLoader;
	private ListView lvNews;
	private SwipeRefreshLayout  swipeRefreshLayout;
	private CustomListAdapter   adapter;
	private List<NewsElement> elements;

	// загрузчик новостей
	private LiveGoodlineInfoDownloader downloader;
	private AlarmReceiver	alarmReceiver;


	public TopicListActivityFragment()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		View view	= inflater.inflate(R.layout.fragment_topic_list, container, false);
		setHasOptionsMenu(true);

		// для фоновой загрузки изображений через Volley
		requestQueue = Volley.newRequestQueue(view.getContext());
		imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache()
		{
			private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(newsCountPerPage);
			public void putBitmap(String url, Bitmap bitmap)
			{
				mCache.put(url, bitmap);
			}
			public Bitmap getBitmap(String url)
			{
				return mCache.get(url);
			}
		});

		downloader = new LiveGoodlineInfoDownloader(getActivity());

		// для пул даун ту рефреш
		swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh);
		swipeRefreshLayout.setOnRefreshListener(this);

		//настройка list view
		lvNews    = (ListView)view.findViewById(R.id.listView);

		//----------------- реализация inifinite scroll ------------------------
		lvNews.setOnScrollListener(new InfinityScrollListener(newsCountPerPage)
		{
			// событие возникает во время того как скроллинг дойдет до конца
			@Override
			public void loadMore(int page, int totalItemsCount)
			{
				// загрузить еще данных
				getPageContent(page, false);
			}
		});

		elements    = new ArrayList<NewsElement>();

		//TODO: проверить, не восстановлен ли вид после поворота

		adapter     = new CustomListAdapter(view.getContext(), imageLoader,elements);
		lvNews.setAdapter(adapter);


		// обработка нажатия на элемент списка
		lvNews.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			// реакция на нажатие по элементу
			public void onItemClick(AdapterView<?> parent, View view,
									int position, long id)
			{
				NewsElement currentNews = (NewsElement) adapter.getItem(position);
				OnLvClicked(currentNews);
			}
		});

		// отображение прогрессбара
		progressDlg = new ProgressDialog(getActivity());
		progressDlg.setTitle("Ожидание");
		progressDlg.setMessage("Идет обновление новостей...");
		progressDlg.show();

		alarmReceiver	= new AlarmReceiver();

		// фоновая загрузка первой страницы
		getPageContent(1, false);
		return view;
	}

	public void onStartAlarm()
	{
		alarmReceiver.startTask(getActivity(),60*1000);

	}

	public void onStopAlarm()
	{
		alarmReceiver.cancelAlarm(getActivity());

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.menu_topic_list, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		switch (id)
		{
			case R.id.action_settings:
				// выполнить очистку кэша
				this.downloader.clearCache(new IClearDbListener()
				{
					@Override
					public void onDone()
					{
						Toast.makeText(getActivity(), "Кэш очищен.", Toast.LENGTH_SHORT).show();
					}
				});
				return true;
			case R.id.action_start_alarm:
				onStartAlarm();
				return true;

			case R.id.action_stop_alarm:
				onStopAlarm();
				return true;

		}

		return super.onOptionsItemSelected(item);
	}
	//------------------- процедура фоновой загрузки страницы -------------------------
	private void getPageContent(final int page,final boolean insertToTop)
	{

		// время последнего элемента
		Date lastTopicListDate = null;
		if(insertToTop)
		{
			if(this.elements.size() > 0)
			{
				lastTopicListDate = elements.get(0).getDate();
			}
		}
		else
		{
			if(this.elements.size() > 0)
			{
				lastTopicListDate = elements.get(elements.size()-1).getDate();
			}
		}
		// запрос на загрузку страницы
		downloader.getTopicList(getActivity()
				, page, lastTopicListDate, insertToTop
				, new IGetTopicListResponseListener()
				{
					@Override
					public void onResponseGetTopicList(List<NewsElement> topicList)
					{
						doAfterTopicListReceived(topicList, insertToTop, page);
					}
				}
				, new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError error)
					{
						Log.d(LOG_TAG, " on received TL from volley Error [" + error + "]");
						System.out.println("Error [" + error + "]");
						Toast.makeText(getActivity(), "Неудачная попытка соединиться с сервером.", Toast.LENGTH_SHORT).show();
						progressDlg.dismiss();
					}
				}
		);
	}

	//TODO: нужно синхронизировать , може Synchronized?
	private /*synchronized*/ void doAfterTopicListReceived(List<NewsElement> topicList, boolean insertToTop, int page)
	{
		// отключаем прогрессбар
		progressDlg.dismiss();
		boolean dataChanged = false;
		if(topicList == null || topicList.size() == 0)
		{
			return;
		}
		if (insertToTop)
		{
			// pull to refresh - добавление сверху списка
			//List<NewsElement> elementsForAppend = new ArrayList<NewsElement>();
			for(int i = topicList.size()-1; i >= 0 ; i--)
			{
				NewsElement currentElement = topicList.get(i);
				//  сравнить новые новости с уже имеющимися в списке
				if( elements.size() > 0)
				{
					// если данная новость по времени позже самой свежей - удалить из списка
					if(currentElement.compareTo(elements.get(0)) <= 0)
					{
						topicList.remove(i);
					}
				}
			}
			// если в списке остались элементы, то добавить в основной список новостей
			if(topicList.size() > 0)
			{
				elements.addAll(0,topicList);
				dataChanged = true;
			}
			swipeRefreshLayout.setRefreshing(false);
		}
		else
		{
			// добавление в конец списка новостей
			if(topicList.size() > 0)
			{
				//---------- для теста pull to refresh--------
				if(page == 1)
				{
					topicList.remove(0);
				}
				//--------------------------------------------
				// нужно проверить, есть ли такие новости уже в списке, если есть - заменить
				for(NewsElement currentNews:  topicList)
				{
					for(NewsElement currentLvElement: elements)
					{
						if(currentLvElement.compareTo(currentNews) == 0)
						{
							// если в списке есть такая новость, то обновить
							currentLvElement = currentNews;
							break;
						}
						if(currentLvElement.compareTo(currentNews) < 0)
						{
							break;
						}
					}
				}
				elements.addAll(topicList);
				dataChanged = true;
			}
		}
		// если были изменения в данных - обновить ListView
		if(dataChanged)
		{
			adapter.notifyDataSetChanged();
		}
		// отключаем прогрессбар
		progressDlg.dismiss();
	}
	//----- событие при pull down to refresh
	@Override
	public void onRefresh()
	{
		// начинаем показывать прогресс
		swipeRefreshLayout.setRefreshing(true);
		getPageContent(1,true);
	}

	//----- обработка фоновой загрузки отдельнойновости
	public void OnLvClicked(NewsElement selectedItem)
	{

		//TODO: создать новый активити с фреймом
		// передать в новый активити данные
		Intent intent = new Intent(getActivity(), NewsActivity.class);

		intent.putExtra(TopicListActivityFragment.NEWS_URL_INTENT, selectedItem.getUrl());
		intent.putExtra(TopicListActivityFragment.NEWS_TITLE_INTENT, selectedItem.getTitle());
		intent.putExtra(TopicListActivityFragment.NEWS_DATE_INTENT, selectedItem.getDate().getTime());
		startActivity(intent);

	}

}
