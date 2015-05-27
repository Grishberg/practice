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
		//if (queue != null)
		//{
		//	queue.cancelAll(REQUEST_TAG);
		//}
	}
	// функция извлекает список новостей из кэша, либо из сети
	public void getTopicList(final Context context
			, final int page
			, final Date lastPageDate
			, final boolean insertToTop
			, final IGetTopicListResponseListener listener )
	{
		// 1) проверить в базе наличие данных (в потоке)
		GetTopicListTask task = new GetTopicListTask();
		task.execute(new GetTopicTaskParamers()
		{
			@Override
			public boolean getInsertToTop(){ return insertToTop; }

			@Override
			public int getPage()
			{
				return  page;
			}

			@Override
			public void onDone(List<NewsElement> result, boolean fromCache, int errorCode)
			{
				// вернуть результат
				listener.onResponseGetTopicList(result,fromCache, errorCode);
			}

			@Override
			public Date getDate()
			{
				return lastPageDate;
			}
		});

	}

	// функция извлекает страницу с новостью из кэша или из сети
	public void getNewsPage(final Context context
			, final String url
			, final Date date
			, final IGetNewsResponseListener listener)
	{
		// поискать асинхронно в потоке тело новости
		GetNewsBodyTask getNewsBodyTask = new GetNewsBodyTask();
		getNewsBodyTask.execute(new GetNewsBodyTaskParameters()
		{
			@Override
			public String getUrl() { return  url;}
			@Override
			public Date getDate()
			{
				return date;
			}

			@Override
			public void onDone(String body, boolean fromCache, int errorCode)
			{
				if (body != null)
				{
					// в кэше есть новость - вернуть данные
					listener.onResponseGetNewsPage(body, fromCache, errorCode);
				}
			}
		});
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


	interface GetTopicTaskParamers
	{
		public int 		getPage();
		public void 	onDone(List<NewsElement> result, boolean fromCache,int errorCode);
		public Date 	getDate();
		public boolean	getInsertToTop();
	}
	//---------------------------------------
	// Асинхронное выполнение выборки
	private class GetTopicListTask extends AsyncTask<GetTopicTaskParamers, List<NewsElement>, List<NewsElement> >
	{
		private GetTopicTaskParamers inputParam;
		private int errorCode;
		protected List<NewsElement> doInBackground(GetTopicTaskParamers... params)
		{
			List<NewsElement> topicListFromCache	= null;
			List<NewsElement> topicListFromWeb		= null;
			inputParam			= params.length > 0 ? params[0] : null;
			Date date			= inputParam.getDate();
			int page			= inputParam.getPage();
			boolean	insertToTop	= inputParam.getInsertToTop();
			errorCode			= 0;

			topicListFromCache	= dbHelper.getTopicList(date);

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
			if(page > 1)
			{
				url = String.format("%s/page%d/",mainUrl,page);
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
				errorCode = -1;
                e.printStackTrace();
            } catch (ExecutionException e) {
				errorCode = -2;
                e.printStackTrace();
            } catch (TimeoutException e) {
				errorCode = -3;
                e.printStackTrace();
            }

			// подгрузить в фоне из сети

			return topicListFromWeb;
		}
		protected void onProgressUpdate(List<NewsElement>... progress)
		{
			// отобразить данные из кэша
			List<NewsElement> params	= progress.length > 0 ? progress[0] : null;
			inputParam.onDone(params, true, errorCode);
		}
		protected void onPostExecute(List<NewsElement> result)
		{
			inputParam.onDone(result, false, errorCode);
		}
	}

	//--------------------------------------------------
	// Асинхронное сохранение новостей
	interface StoreTopicTaskParamers
	{
		public List<NewsElement> getTopicNewsList();
	}
	private class StoreTopicListTask extends AsyncTask<StoreTopicTaskParamers, Void, Void >
	{
		private StoreTopicTaskParamers inputParam;
		protected Void doInBackground(StoreTopicTaskParamers... params)
		{
			inputParam = params.length > 0 ? params[0] : null;
			List<NewsElement> topicList = inputParam.getTopicNewsList();

			dbHelper.storeTopicList(topicList);
			return null;
		}
		protected void onProgressUpdate(Void... progress)
		{
		}
		protected void onPostExecute(Void result)
		{

		}
	}

	//--------------------------------------------------
	// Асинхронное извлечение тела письма из кэша
	interface GetNewsBodyTaskParameters
	{
		public Date getDate();
		public void onDone(String body, boolean fromCache, int error);
		public String getUrl();
	}

	private class GetNewsBodyTask extends AsyncTask<GetNewsBodyTaskParameters, String, String >
	{
		private GetNewsBodyTaskParameters inputParam;
		private int errorCode;
		protected String doInBackground(GetNewsBodyTaskParameters... params)
		{
			CachedNewsBodyContainer cachedNewsBody	= null;
			String webNewsBody		= null;
			inputParam	= params.length > 0 ? params[0] : null;
			Date date	= inputParam.getDate();
			String url	= inputParam.getUrl();
			errorCode		= 0;
			//TODO: вернуть признак того, что это часть новости
			cachedNewsBody	= dbHelper.getNewsPage(date);

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
					, url
					, futureRequest, futureRequest);
			queue.add(getRequest);

			try
			{
				String response = futureRequest.get(TopicListActivityFragment.VOLLEY_SYNC_TIMEOUT, TimeUnit.SECONDS);
				// спарсить полученную строку
				webNewsBody = LiveGoodlineParser.getNews(response);
				//сохранить в кэше
				dbHelper.updateNewsBody(date,webNewsBody);
				Log.d(LOG_TAG, "Данные из сети");

			} catch (InterruptedException e)
			{
				errorCode = -1;
				e.printStackTrace();
			} catch (ExecutionException e)
			{
				errorCode = -2;
				e.printStackTrace();
			} catch (TimeoutException e)
			{
				errorCode = -3;
				e.printStackTrace();
			}

			return webNewsBody;
		}
		// вывести промежуточный вариант из кэша
		protected void onProgressUpdate(String... progress)
		{
			String result	= progress.length > 0 ? progress[0] : null;
			inputParam.onDone(result, true, errorCode);

		}
		// вывести итоговый вариант
		protected void onPostExecute(String result)
		{
			inputParam.onDone(result, false, errorCode);
		}
	}

	//-----------------------------------------------------
	// асинхронное сохранение тела новости
	interface StoreNewsBodyTaskParamers
	{
		public String	getNewsBody();
		public Date		getDate();
	}

	private class StoreNewsBodyTask extends AsyncTask<StoreNewsBodyTaskParamers, Void, Void >
	{
		private StoreNewsBodyTaskParamers inputParam;
		protected Void doInBackground(StoreNewsBodyTaskParamers... params)
		{
			inputParam = params.length > 0 ? params[0] : null;
			String newsBody = inputParam.getNewsBody();
			Date date		= inputParam.getDate();
			dbHelper.updateNewsBody(date, newsBody);
			return null;
		}
		protected void onProgressUpdate(Void... progress)
		{
		}
		protected void onPostExecute(Void result)
		{

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
