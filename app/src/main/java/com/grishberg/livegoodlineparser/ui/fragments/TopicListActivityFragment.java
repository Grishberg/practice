package com.grishberg.livegoodlineparser.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.grishberg.livegoodlineparser.data.asynctaskloaders.ClearCacheTask;
import com.grishberg.livegoodlineparser.data.asynctaskloaders.GetTopicListTask;
import com.grishberg.livegoodlineparser.data.containers.NewsBodyContainer;
import com.grishberg.livegoodlineparser.data.containers.NewsContainer;
import com.grishberg.livegoodlineparser.data.containers.TopicListContainer;
import com.grishberg.livegoodlineparser.data.interfaces.IGetTopilistListener;
import com.grishberg.livegoodlineparser.ui.listeners.InfinityScrollListener;
import com.grishberg.livegoodlineparser.R;
import com.grishberg.livegoodlineparser.ui.activities.NewsActivity;
import com.grishberg.livegoodlineparser.ui.activities.TopicListActivity;
import com.grishberg.livegoodlineparser.ui.adapters.CustomListAdapter;
import com.grishberg.livegoodlineparser.data.interfaces.IClearDbListener;
import com.grishberg.livegoodlineparser.sheduling.AlarmReceiver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class TopicListActivityFragment extends Fragment  implements
		LoaderManager.LoaderCallbacks
	, SwipeRefreshLayout.OnRefreshListener
	, TopicListActivity.IActionsListener
	, AdapterView.OnItemClickListener
	, IGetTopilistListener
	, IClearDbListener
{
	static final String LOG_TAG = "LiveGL.TL";
	public static final String COMMAND_UPDATE_FROM_SERVICE		= "commandUpdateFromCheckNewsService";
	public static final String COMMAND_OPEN_NEWS_FROM_SERVICE	= "commandOpenNewsFromCheckNewsService";


	public static final String NEWS_URL_INTENT		= "currentNewsUrl";
	public static final String NEWS_TITLE_INTENT 	= "currentNewsTitle";
	public static final String NEWS_DATE_INTENT		= "currentNewsDate";
	public static final int UPDATE_NEWS_DURATION	= 30*60*1000;
	public static final int VOLLEY_SYNC_TIMEOUT		= 20;

	private static final int	TASK_ID_GET_TOPIC_LIST	= 1;
	private static final int	TASK_ID_CLEAR_CACHE	= 3;

	private final int           newsCountPerPage    = 10;
	private boolean				mFirstRun;
	private ProgressDialog 		mProgressDlg;

	private ListView 			mNews;
	private ProgressBar 		mProgressBar;
	private SwipeRefreshLayout  mSwipeRefreshLayout;
	private CustomListAdapter 	mAdapter;
	private List<NewsContainer> mElements;

	// загрузчик новостей

	private AlarmReceiver		mAlarmReceiver;
	private boolean				mDownloadingPage = false;
	private int 				mShortAnimationDuration;

	public TopicListActivityFragment()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		View view	= inflater.inflate(R.layout.fragment_topic_list, container, false);
		setHasOptionsMenu(true);

		if(savedInstanceState != null)
		{
			// восстановление после поворота
			//TODO: загрузить список новостей
			// восстановить позицию на экране
		}
		else
		{
			mFirstRun = true;
		}
		((TopicListActivity)getActivity()).addListener(this);

		// для пул даун ту рефреш
		mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh);
		mSwipeRefreshLayout.setOnRefreshListener(this);

		//настройка list view
		mNews    = (ListView)view.findViewById(R.id.listView);

		//----------------- реализация inifinite scroll ------------------------
		mNews.setOnScrollListener(new InfinityScrollListener(newsCountPerPage)
		{
			// событие возникает во время того как скроллинг дойдет до конца
			@Override
			public void loadMore(int page, int totalItemsCount)
			{
				// загрузить еще данных
				getPageContent(page, false);
			}
		});

		mElements    = new ArrayList<NewsContainer>();

		//TODO: проверить, не восстановлен ли вид после поворота

		mAdapter     = new CustomListAdapter(view.getContext(), mElements);
		mNews.setAdapter(mAdapter);


		// обработка нажатия на элемент списка
		mNews.setOnItemClickListener(this);

		// скрыть список новостей
		mNews.setVisibility(View.GONE);

		// настройка анимации перехода
		mShortAnimationDuration = getResources().getInteger(
				android.R.integer.config_shortAnimTime);


		// отображение прогрессбара
		mProgressBar	= (ProgressBar) view.findViewById(R.id.loading_spinner);

		mProgressDlg = new ProgressDialog(getActivity());
		mProgressDlg.setTitle("Ожидание");
		mProgressDlg.setMessage("Идет обновление новостей...");

		mAlarmReceiver	= new AlarmReceiver();

		return view;
	}

	@Override
	public boolean onBackPressed()
	{
		return false;
	}

	@Override
	public void onNewIntent(Intent intent)
	{
		String action = intent.getAction();
		if( action != null)
		{
			if( action.equals(TopicListActivityFragment.COMMAND_UPDATE_FROM_SERVICE))
			{
				// обновить список новостей
				// запустить обновление списка
				getPageContent(1, true);
			}
			if( action.equals(TopicListActivityFragment.COMMAND_OPEN_NEWS_FROM_SERVICE))
			{
				// запустить обновление списка
				getPageContent(1, true);

				// открыть нужную новость
				String url		= intent.getStringExtra(TopicListActivityFragment.NEWS_URL_INTENT);
				Date date		= new Date( intent.getLongExtra(TopicListActivityFragment.NEWS_DATE_INTENT, 0));
				String title = intent.getStringExtra(TopicListActivityFragment.NEWS_TITLE_INTENT);
				startDetailPage(date, url, title);
			}
			Log.d(LOG_TAG," onResume, action = "+action);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if(mFirstRun)
		{
			mFirstRun = false;
			// фоновая загрузка первой страницы
			getPageContent(1, false);
		}
		else
		{
			Loader loader	= getLoaderManager().initLoader(TASK_ID_GET_TOPIC_LIST, null, this);
			((GetTopicListTask)loader).setListener(this);
		}
	}

	// реакция на нажатие на элемент списка
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		NewsContainer currentNews = (NewsContainer) mAdapter.getItem(position);
		startDetailPage(currentNews.getDate(), currentNews.getUrl(), currentNews.getTitle());
	}

	public void onStartAlarm()
	{
		mAlarmReceiver.startTask(getActivity(), TopicListActivityFragment.UPDATE_NEWS_DURATION);
	}

	public void onStopAlarm()
	{
		mAlarmReceiver.cancelAlarm(getActivity());
	}

	//VIEW скрытие прогрессбара и отображение списка
	private void hideProgressBar()
	{
		mNews.setAlpha(0f);
		mNews.setVisibility(View.VISIBLE);

		mNews.animate()
				.alpha(1f)
				.setDuration(mShortAnimationDuration)
				.setListener(null);

		mProgressBar.animate()
				.alpha(0f)
				.setDuration(mShortAnimationDuration)
				.setListener(new AnimatorListenerAdapter()
				{
					@Override
					public void onAnimationEnd(Animator animation)
					{
						mProgressBar.setVisibility(View.GONE);
					}
				});
	}

	//------------------- процедура фоновой загрузки страницы -------------------------
	private void getPageContent(int page, boolean insertToTop)
	{
		// не запускать обновление страницы, если уже идет загрузка.
		if (mDownloadingPage) return;
		mDownloadingPage	= true;

		Log.d(LOG_TAG, "on get page, page = " + page + ", insertToTop = " + insertToTop);

		// если обновление новостей - отобразить прогресс
		if (insertToTop)
		{
			mProgressDlg.show();
		}
		// время последнего элемента
		Date lastTopicListDate = null;
		if(insertToTop)
		{
			if(mElements.size() > 0)
			{
				lastTopicListDate = mElements.get(0).getDate();
			}
		}
		else
		{
			if(mElements.size() > 0)
			{
				lastTopicListDate = mElements.get(mElements.size() - 1 ).getDate();
			}
		}
		// запрос на загрузку страницы
		Bundle params	= new Bundle();
		params.putBoolean(GetTopicListTask.PARAM_INSERT_TO_TOP, insertToTop);
		params.putInt(GetTopicListTask.PARAM_PAGE, page);
		Loader loader	= getLoaderManager().initLoader(TASK_ID_GET_TOPIC_LIST, params, this);
	}

	//---------------- обработка фоновой задачи --------------
	@Override
	public void onProgress(TopicListContainer progressResult)
	{
		doAfterTopicListReceived(progressResult.getNewsList(), progressResult.isInsertToTop(), true);
	}

	@Override
	public Loader onCreateLoader(int id, Bundle args)
	{
		Loader loader	= null;
		switch (id)
		{
			case TASK_ID_GET_TOPIC_LIST:
				loader	=  new GetTopicListTask(getActivity(),args);
				((GetTopicListTask)loader).setListener(this);
				break;

			case TASK_ID_CLEAR_CACHE:
				loader	=  new ClearCacheTask(getActivity());
				break;
		}
		if(loader != null)
		{
			loader.forceLoad();
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader loader, Object data)
	{
		TopicListContainer result	= (TopicListContainer) data;

		if(result.getErrorCode() < 0)
		{
			// ошибка
			Log.d(LOG_TAG, " ошибка во время загрузки из volley");
			mDownloadingPage = false;
			if(mElements.size() == 0)
			{
				hideProgressBar();
			}
			if(result.isInsertToTop())
			{
				mSwipeRefreshLayout.setRefreshing(false);
			}
			Toast.makeText(getActivity(), "Неудачная попытка соединиться с сервером.", Toast.LENGTH_SHORT).show();
		}
		else
		{
			doAfterTopicListReceived(result.getNewsList(), result.isInsertToTop(), false);
		}
		mProgressDlg.dismiss();
	}

	@Override
	public void onLoaderReset(Loader loader)
	{

	}
	//------------------------------------------------------

	private  void doAfterTopicListReceived(List<NewsContainer> topicList
			, boolean insertToTop, boolean fromCache)
	{

		boolean dataChanged = false;
		if(insertToTop)
		{
			mProgressDlg.dismiss();
		}
		if(mElements.size() == 0)
		{
			hideProgressBar();
		}
		if(topicList == null || topicList.size() == 0)
		{
			return;
		}
		if (insertToTop)
		{
			// pull to refresh - добавление сверху списка
			for(int i = topicList.size()-1; i >= 0 ; i--)
			{
				NewsContainer currentElement = topicList.get(i);
				//  сравнить новые новости с уже имеющимися в списке
				if( mElements.size() > 0)
				{
					// если данная новость по времени позже самой свежей - удалить из списка
					if(currentElement.compareTo(mElements.get(0)) <= 0)
					{
						topicList.remove(i);
					}
				}
			}
			// если в списке остались элементы, то добавить в основной список новостей
			if(topicList.size() > 0)
			{
				mElements.addAll(0,topicList);
				dataChanged = true;
			}
			mSwipeRefreshLayout.setRefreshing(false);
		}
		else
		{
			// добавление в конец списка новостей

			if(topicList.size() > 0)
			{
				//--------------------------------------------
				// нужно проверить, есть ли такие новости уже в списке, если есть - заменить

				for(int i = topicList.size() -1; i >=0  ; i--)
				{
					NewsContainer currentNews	= topicList.get(i);
					for(NewsContainer currentLvElement: mElements)
					{
						if(currentLvElement.compareTo(currentNews) == 0)
						{
							// если в списке есть такая новость, то обновить
							currentLvElement = currentNews;
							topicList.remove(i);
							break;
						}
						if(currentLvElement.compareTo(currentNews) < 0)
						{
							break;
						}
					}
				}
				if(topicList.size() > 0)
				{
					mElements.addAll(topicList);
				}
				dataChanged = true;
			}
		}
		// если были изменения в данных - обновить ListView
		if(dataChanged)
		{
			mAdapter.notifyDataSetChanged();
		}
		// отключаем прогрессбар
		mProgressDlg.dismiss();
	}
	//----- событие при pull down to refresh
	@Override
	public void onRefresh()
	{
		// начинаем показывать прогресс
		if(mDownloadingPage == false)
		{
			mSwipeRefreshLayout.setRefreshing(true);
			getPageContent(1, true);
		}
		else
		{
			mSwipeRefreshLayout.setRefreshing(false);
		}
	}

	//----- обработка фоновой загрузки отдельной новости
	public void startDetailPage(Date date, String url, String title)
	{

		//TODO: создать новый активити с фреймом
		// передать в новый активити данные
		Intent intent = new Intent(getActivity(), NewsActivity.class);

		intent.putExtra(TopicListActivityFragment.NEWS_URL_INTENT, url);
		intent.putExtra(TopicListActivityFragment.NEWS_TITLE_INTENT, title);
		intent.putExtra(TopicListActivityFragment.NEWS_DATE_INTENT, date.getTime());
		startActivity(intent);
	}


	//------------- функции отображения меню -----------------
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
				getLoaderManager().initLoader(TASK_ID_CLEAR_CACHE, null, this);
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

	@Override
	public void onCacheCleared()
	{
		Toast.makeText(getActivity(), "Кэш очищен.", Toast.LENGTH_SHORT).show();
	}
}
