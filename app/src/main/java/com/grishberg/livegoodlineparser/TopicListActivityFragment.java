package com.grishberg.livegoodlineparser;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.LruCache;
import android.view.LayoutInflater;
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

import java.util.ArrayList;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class TopicListActivityFragment extends Fragment  implements SwipeRefreshLayout.OnRefreshListener
{
	public static final String NEWS_URL_INTENT = "currentNewsUrl";
	public static final String NEWS_TITLE_INTENT = "currentNewsTitle";

	// ссылка на основную страницу
	private final String        mainUrl             = "http://live.goodline.info/guest";
	private final int           newsCountPerPage    = 10;
	private ProgressDialog progressDlg;

	// для загрузки изображений
	private RequestQueue requestQueue;
	private ImageLoader imageLoader;
	private ListView lvNews;
	private SwipeRefreshLayout  swipeRefreshLayout;
	private CustomListAdapter   adapter;
	private List<NewsElement> elements;


	public TopicListActivityFragment()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		View view	= inflater.inflate(R.layout.fragment_topic_list, container, false);

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
				//progressDlg.show();

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

		// фоновая загрузка первой страницы
		getPageContent(1, false);
		return view;
	}
	//------------------- процедура фоновой загрузки страницы -------------------------
	private void getPageContent(final int page,final boolean insertToTop)
	{
		String url = mainUrl;
		// скорректировать URL в зависимости от страницы
		if(page > 1)
		{
			url += String.format("/page%d/",page);
		}

		// отправка запроса на закачку страницы
		RequestQueue queue      = Volley.newRequestQueue(getActivity());
		StringRequest getReq    = new StringRequest(Request.Method.GET
				, url
				, new Response.Listener<String>()
		{
			// событие возникает при успешном чтении
			@Override
			public void onResponse(String response)
			{
				boolean dataChanged = false;
				if(response == null || response.length() == 0)
				{
					// отключаем прогрессбар
					progressDlg.dismiss();
					return;
				}

				// парсим статью
				List<NewsElement> newElements = LiveGoodlineParser.getNewsPerPage(response);
				// в зависимости от того, обновляем сверху или снизу, осуществляем нужные действия
				if (insertToTop)
				{
					// pull to refresh - добавление сверху списка
					List<NewsElement> elementsForAppend = new ArrayList<NewsElement>();
					for(int i = newElements.size()-1; i >= 0 ; i--)
					{
						NewsElement currentElement = newElements.get(i);
						//  сравнить новые новости с уже имеющимися в списке
						if( elements.size() > 0)
						{
							if(currentElement.compareTo(elements.get(0)) <= 0)
							{
								newElements.remove(i);
							}
						}
					}
					if(newElements.size() > 0)
					{
						elements.addAll(0,newElements);
						dataChanged = true;
					}
					swipeRefreshLayout.setRefreshing(false);
				}
				else
				{
					// добавление снизу
					if(newElements.size() > 0)
					{
						//---------- для теста pull to refresh--------
						if(page == 1)
						{
							newElements.remove(0);
						}
						//--------------------------------------------
						elements.addAll(newElements);
						dataChanged = true;
					}
				}

				if(dataChanged)
				{
					adapter.notifyDataSetChanged();
				}
				// отключаем прогрессбар
				progressDlg.dismiss();
			}
		}, new Response.ErrorListener()
		{
			// возникла ошибка
			@Override
			public void onErrorResponse(VolleyError error)
			{
				System.out.println("Error ["+error+"]");

				Toast.makeText(getActivity(),"Неудачная попытка соединиться с сервером.",Toast.LENGTH_SHORT).show();
				progressDlg.dismiss();
			}
		});
		queue.add(getReq);
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

		startActivity(intent);

	}
	// для восстановления списка
	public void setNews(List<NewsElement> data)
	{
		this.elements = data;
	}

	public List<NewsElement> getNews()
	{
		return this.elements;
	}
}
