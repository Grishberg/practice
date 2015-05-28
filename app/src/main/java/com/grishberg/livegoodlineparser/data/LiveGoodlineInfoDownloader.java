package com.grishberg.livegoodlineparser.data;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.grishberg.livegoodlineparser.ui.fragments.TopicListActivityFragment;
import com.grishberg.livegoodlineparser.data.livegoodlineparser.LiveGoodlineParser;
import com.grishberg.livegoodlineparser.data.model.NewsElement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by G on 12.05.15.
 * класс для асинхронной загрузки страниц
 */
//TODO: использовать паттерн синглтон
public class LiveGoodlineInfoDownloader
{
	static final String LOG_TAG = "LiveGL.Downloader";

	public static final String	mainUrl	= "http://live.goodline.info/guest";
	private DBHelper		dbHelper;
	private RequestQueue 	queue; // очередь запросов для Volley
	private Context			mainContext;
	//TODO: разобраться с прерыванием потока
	private GetTopicListTask 	mGetTopicListTask;
	private GetNewsBodyTask		mGetNewsBodyTask;
	//public static final String REQUEST_TAG = "VolleyRequestActivity";

	public LiveGoodlineInfoDownloader(Context context)
	{
		this.mainContext	= context;
		this.dbHelper	= new DBHelper(context);
	}

	public Date getMaxDateInCache()
	{
		return this.dbHelper.getMaxStoredDate();
	}

	// остановить фоновые задачи.
	public void onStop()
	{
		if (mGetTopicListTask!= null && mGetTopicListTask.getStatus() == AsyncTask.Status.RUNNING)
			mGetTopicListTask.cancel(true);

		if (mGetNewsBodyTask!= null && mGetNewsBodyTask.getStatus() == AsyncTask.Status.RUNNING)
			mGetNewsBodyTask.cancel(true);
	}
	// функция извлекает список новостей из кэша, либо из сети
	public void getTopicList(int page
			,  Date lastPageDate
			,  boolean insertToTop
			,  IGetTopicListResponseListener listener )
	{
		// 1) проверить в базе наличие данных (в потоке)
		mGetTopicListTask = new GetTopicListTask(page,lastPageDate, insertToTop, listener);
		mGetTopicListTask.execute();
	}

	// функция извлекает страницу с новостью из кэша или из сети
	public void getNewsPage( String url
			,  Date date
			,  IGetNewsResponseListener listener)
	{
		// поискать асинхронно в потоке тело новости
		mGetNewsBodyTask = new GetNewsBodyTask(date,url,listener);
		mGetNewsBodyTask.execute();
	}

	// очистить кэш
	public void clearCache(IClearDbListener listener)
	{
		ClearDbTask clearDbTask = new ClearDbTask();
		clearDbTask.execute(listener);
	}

	// сохранить список новостей в кэш
	public void addNewsToCache(List<NewsElement> topicList)
	{
		dbHelper.storeTopicList(topicList);
	}


	class CachedNewsBodyContainer
	{
		private String body = "";	// тело письма
		private boolean isDescription	= false; // признак того, что это описание
		public CachedNewsBodyContainer(String body, boolean isDescription)
		{
			this.body	= body;
			this.isDescription	= isDescription;
		}
		public String getBody(){return body;}
		public boolean getDescriptionStatus() { return isDescription;}
	}

	// класс для работы с бд
	class DBHelper extends SQLiteOpenHelper
	{
		private static final int DATABASE_VERSION = 2;

		public static final String DATABASE_NAME		= "cache.db";
		public static final String NEWS_TABLE_NAME		= "newscache";
		public static final String IMAGES_TABLE_NAME	= "imagescache";

		public static final String IMAGES_COLUMN_ID		= "id";
		public static final String IMAGES_COLUMN_IMAGE 	= "image";
		public static final String IMAGES_COLUMN_PATH 	= "path";

		public static final String NEWS_COLUMN_DATE 	= "date";
		public static final String NEWS_COLUMN_TITLE 	= "title";
		public static final String NEWS_COLUMN_URL 		= "url";
		public static final String NEWS_COLUMN_BODY		= "body";
		public static final String NEWS_COLUMN_IMAGEURL	= "previewImageUrl";
		public static final String NEWS_COLUMN_ISDESCRIPTION	= "isDescription";

		public DBHelper(Context context)
		{
			// конструктор суперкласса
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			// создаем таблицу для хранения кэша
			db.execSQL("create table "+NEWS_TABLE_NAME+" ("
					+ "date			LONG PRIMARY KEY,"
					+ "title		TEXT ,"
					+ "url			TEXT ,"
					+ "body			TEXT ,"
					+ "previewImageUrl	TEXT, "
					+ "isDescription	INTEGER"
					+ ");");

			// создаем таблицу для хранения картинок
			// ключевым полем будет url картинки, так как
			// не будет 2 разных картинки с 1 url и поиск ведется по url
			db.execSQL("create table " + IMAGES_TABLE_NAME + " ("
//					+ "id			INTEGER PRIMARY KEY autoincrement,"
					+ "url			TEXT PRIMARY KEY,"
					+ "path	TEXT"
					+ ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			Log.w("LOG_TAG", "Обновление базы данных с версии " + oldVersion
					+ " до версии " + newVersion + ", которое удалит все старые данные");
			// Удаляем предыдущую таблицу при апгрейде
			db.execSQL("DROP TABLE IF EXISTS "+ NEWS_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS "+ IMAGES_TABLE_NAME);
			// Создаём новый экземпляр таблицы
			onCreate(db);
		}
		//----------------------------------------------------------------------
		//------------- вспомогательные функции для работы с бд ----------------
		//----------------------------------------------------------------------

		// очистить базу данных
		public boolean clearDb()
		{
			SQLiteDatabase db = this.getWritableDatabase();
			db.delete(NEWS_TABLE_NAME, null, null);
			db.delete(IMAGES_TABLE_NAME, null, null);
			db.close();
			return true;
		}

		// функция извлекает список новостей из кэша, либо из сети
		public List<NewsElement> getTopicList( Date dt)
		{
			List<NewsElement> news	= new ArrayList<NewsElement>();

			// подключаемся к БД
			SQLiteDatabase db		= this.getReadableDatabase();
			if(dt == null)
			{
				dt = new Date();
			}
			Cursor res =  db.rawQuery( "SELECT * FROM " + NEWS_TABLE_NAME
					+" WHERE "+ NEWS_COLUMN_DATE +" < ? ORDER BY "+NEWS_COLUMN_DATE+" DESC limit 10"
					, new String[]{ Long.toString( dt.getTime() )});
			res.moveToFirst();

			while(res.isAfterLast() == false)
			{
				String title	= res.getString(	res.getColumnIndex(NEWS_COLUMN_TITLE));
				String url		= res.getString(res.getColumnIndex(NEWS_COLUMN_URL));
				Long lDate		= res.getLong(res.getColumnIndex(NEWS_COLUMN_DATE));
				String imageUrl	= res.getString(	res.getColumnIndex(NEWS_COLUMN_IMAGEURL));
				NewsElement	newsElement	= new NewsElement(url,title,imageUrl,lDate);
				news.add(newsElement);

				res.moveToNext();
			}

			db.close();
			return  news;
		}

		// функция извлекает страницу с новостью из кэша или из сети
		public CachedNewsBodyContainer getNewsPage(Date dt)
		{
			CachedNewsBodyContainer bodyContainer = null;

			// подключаемся к БД
			SQLiteDatabase db		= this.getReadableDatabase();

			Cursor res =  db.rawQuery( "SELECT "+NEWS_COLUMN_BODY+", "+NEWS_COLUMN_ISDESCRIPTION+" FROM " + NEWS_TABLE_NAME
					+" WHERE "+ NEWS_COLUMN_DATE +" = ? limit 1"
					, new String[]{ Long.toString( dt.getTime() )});
			res.moveToFirst();

			if(!res.isAfterLast())
			{
				String body	= res.getString(	res.getColumnIndex(NEWS_COLUMN_BODY));
				boolean isDescription	= res.getInt( res.getColumnIndex(NEWS_COLUMN_ISDESCRIPTION)) == 1;
				bodyContainer = new CachedNewsBodyContainer( body, isDescription);
			}

			dbHelper.close();
			return bodyContainer;
		}

		public boolean storeTopicList(List<NewsElement> topicList)
		{
			Date dt1 = getMaxStoredDate();
			Date dt2 = getMinStoredDate();

			for (NewsElement currentNews: topicList)
			{
				if(	dt1 == null ||
						currentNews.compareToDate(dt1) > 0 ||
						currentNews.compareToDate(dt2) < 0
						)
				{
					// новости с такой датой нет в базе
					addNews(currentNews);
				}
			}
			return true;
		}


		// добавить новость в бд
		private boolean addNews(NewsElement newsElement)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			ContentValues contentValues = new ContentValues();

			contentValues.put(NEWS_COLUMN_TITLE,	newsElement.getTitle());
			contentValues.put(NEWS_COLUMN_URL,		newsElement.getUrl());
			contentValues.put(NEWS_COLUMN_BODY,		newsElement.getBody());
			contentValues.put(NEWS_COLUMN_DATE, 	newsElement.getDate().getTime() );
			contentValues.put(NEWS_COLUMN_IMAGEURL, newsElement.getImageLink());
			contentValues.put(NEWS_COLUMN_ISDESCRIPTION, 1);
			long status = db.insert(NEWS_TABLE_NAME, null, contentValues);
			db.close();
			return true;
		}

		// обновить текст новости
		private boolean updateNewsBody(Date date, String body)
		{
			SQLiteDatabase db = this.getWritableDatabase();

			ContentValues contentValues = new ContentValues();
			contentValues.put(NEWS_COLUMN_BODY, body);
			contentValues.put(NEWS_COLUMN_ISDESCRIPTION, 0);

			int status	= db.update(NEWS_TABLE_NAME, contentValues, "date = ? ", new String[] { Long.toString(date.getTime()) } );
			db.close();
			return true;
		}

		// обновить картинку в бд
		private boolean updateImage(String url, String path)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			ContentValues contentValues = new ContentValues();
			contentValues.put(IMAGES_COLUMN_PATH,	path);
			db.update(IMAGES_TABLE_NAME, contentValues, "url = ? ", new String[]{url});
			db.close();
			return true;
		}

		// максимальная дата новости в бд
		private Date getMaxStoredDate()
		{
			Date result = null;
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor rs = db.rawQuery("SELECT max( " + NEWS_COLUMN_DATE + " ) from " + NEWS_TABLE_NAME, null);
			//TODO: проверить, может нет данных
			rs.moveToFirst();
			long lDate	= rs.getLong(0);
			if(lDate > 0)
			{
				result = new Date(lDate);
			}
			if (!rs.isClosed())
			{
				rs.close();
			}
			db.close();
			return result;
		}

		// минимальная дата новости в бд
		private Date getMinStoredDate()
		{
			Date result = null;
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor rs = db.rawQuery("SELECT min( " + NEWS_COLUMN_DATE + " ) from " + NEWS_TABLE_NAME, null);
			//TODO: проверить, может нет данных
			rs.moveToFirst();
			long lDate	= rs.getLong(0);
			if(lDate > 0)
			{
				result = new Date(lDate);
			}
			if (!rs.isClosed())
			{
				rs.close();
			}
			db.close();
			return result;
		}
	}

	//---------------------------------------
	// Асинхронное выполнение выборки
	private class GetTopicListTask extends AsyncTask<Void, List<NewsElement>, List<NewsElement> >
	{
		private IGetTopicListResponseListener mListener;
		private int mPage;
		private Date mDate;
		private boolean mIsInserToTop;
		private int mErrorCode;
		public GetTopicListTask(int page, Date date, boolean isInsertToTop, IGetTopicListResponseListener listener)
		{
			super();
			mPage	= page;
			mDate	= date;
			mIsInserToTop	= isInsertToTop;
			mListener		= listener;
		}

		protected List<NewsElement> doInBackground(Void... params)
		{
			List<NewsElement> topicListFromCache	= null;
			List<NewsElement> topicListFromWeb		= null;

			mErrorCode			= 0;
			topicListFromCache	= dbHelper.getTopicList(mDate);

			//использовать onProgressUpdate для вывода промежуточного результата из кэша
			if(topicListFromCache != null && topicListFromCache.size() > 0)
			{
				publishProgress(topicListFromCache);
			}

			// отправка запроса на закачку страницы
			String url = mainUrl;
			//TODO: если статьи добавляются сверху, в цикле добавлять, пока дата любой из статьи
			//		на странице не равна date
			// скорректировать URL в зависимости от страницы
			if(mPage > 1)
			{
				url = String.format("%s/page%d/",mainUrl,mPage);
			}

			// синхронная загрузка из Volley
			RequestQueue queue			= Volley.newRequestQueue(mainContext);
			RequestFuture<String> futureRequest = RequestFuture.newFuture();
			StringRequest getRequest	= new StringRequest(Request.Method.GET
                    , url
					, futureRequest, futureRequest);
			queue.add(getRequest);

            try
			{
                String response = futureRequest.get(TopicListActivityFragment.VOLLEY_SYNC_TIMEOUT, TimeUnit.SECONDS);
				topicListFromWeb = LiveGoodlineParser.getNewsPerPage(response);

				//=============================================================
				//++ для отладки
				//=============================================================
				//if(page == 1 && insertToTop == false)
				//{
				//	if(topicListFromWeb != null && topicListFromWeb.size() > 0)
				//	{
				//		topicListFromWeb.remove(0);
				//	}
				//}
				//--

				// сохранить кэш в базу
				dbHelper.storeTopicList(topicListFromWeb);
				// спарсить полученную строку
            } catch (InterruptedException e)
			{
				mErrorCode = -1;
                e.printStackTrace();
            } catch (ExecutionException e) {
				mErrorCode = -2;
                e.printStackTrace();
            } catch (TimeoutException e) {
				mErrorCode = -3;
                e.printStackTrace();
            }

			// подгрузить в фоне из сети

			return topicListFromWeb;
		}
		protected void onProgressUpdate(List<NewsElement>... progress)
		{
			// отобразить данные из кэша
			List<NewsElement> params	= progress.length > 0 ? progress[0] : null;
			mListener.onResponseGetTopicList(params, true, mErrorCode);
		}
		protected void onPostExecute(List<NewsElement> result)
		{
			mListener.onResponseGetTopicList(result, false, mErrorCode);
		}
	}


	//--------------------------------------------------
	// Асинхронное извлечение тела письма из кэша

	private class GetNewsBodyTask extends AsyncTask<Void, String, String >
	{
		private Date mDate;
		private String mUrl;
		private IGetNewsResponseListener mListener;
		private int mErrorCode;
		public GetNewsBodyTask(Date date, String url, IGetNewsResponseListener listener)
		{
			mDate	= date;
			mUrl	= url;
			mListener	= listener;
		}

		protected String doInBackground(Void... params)
		{
			CachedNewsBodyContainer cachedNewsBody	= null;
			String webNewsBody		= null;

			mErrorCode		= 0;
			//TODO: вернуть признак того, что это часть новости
			cachedNewsBody	= dbHelper.getNewsPage(mDate);

			//использовать onProgressUpdate для вывода промежуточного результата из кэша
			if(cachedNewsBody != null &&
					cachedNewsBody.getBody() != null &&
					cachedNewsBody.getBody().length() >0)
			{
				Log.d(LOG_TAG,"Данные из кэша");
				if (cachedNewsBody.getDescriptionStatus() == true)
				{
					// в кэше превью,
					publishProgress(cachedNewsBody.getBody());
				}
				else
				{
					return cachedNewsBody.getBody();
				}
			}

			// синхронная загрузка из Volley
			RequestQueue queue			= Volley.newRequestQueue(mainContext);
			RequestFuture<String> futureRequest = RequestFuture.newFuture();
			StringRequest getRequest	= new StringRequest(Request.Method.GET
					, mUrl
					, futureRequest, futureRequest);
			queue.add(getRequest);

			try
			{
				String response = futureRequest.get(TopicListActivityFragment.VOLLEY_SYNC_TIMEOUT, TimeUnit.SECONDS);
				// спарсить полученную строку
				webNewsBody = LiveGoodlineParser.getNews(response);
				//сохранить в кэше
				dbHelper.updateNewsBody(mDate,webNewsBody);
				Log.d(LOG_TAG, "Данные из сети");

			} catch (InterruptedException e)
			{
				mErrorCode = -1;
				e.printStackTrace();
			} catch (ExecutionException e)
			{
				mErrorCode = -2;
				e.printStackTrace();
			} catch (TimeoutException e)
			{
				mErrorCode = -3;
				e.printStackTrace();
			}

			return webNewsBody;
		}
		// вывести промежуточный вариант из кэша
		protected void onProgressUpdate(String... progress)
		{
			String result	= progress.length > 0 ? progress[0] : null;
			mListener.onResponseGetNewsPage(result, true, mErrorCode);

		}
		// вывести итоговый вариант
		protected void onPostExecute(String result)
		{
			mListener.onResponseGetNewsPage(result, false, mErrorCode);
		}
	}



	// асинхронная очистка кэша
	private class ClearDbTask extends  AsyncTask<IClearDbListener,Void,Void>
	{
		IClearDbListener inputParams;
		protected Void doInBackground(IClearDbListener... params)
		{
			inputParams	= params.length > 0 ? params[0] : null;
			dbHelper.clearDb();
			return null;
		}
		protected void onProgressUpdate(Void... progress)
		{
		}
		protected void onPostExecute(Void result)
		{
			inputParams.onDone();
		}
	}
}
