package com.grishberg.livegoodlineparser.data.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.grishberg.livegoodlineparser.data.containers.CachedNewsContainer;
import com.grishberg.livegoodlineparser.data.containers.NewsContainer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by G on 29.05.15.
 */
public class NewsDbHelper extends SQLiteOpenHelper
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

	public NewsDbHelper(Context context)
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

	//TODO: перед вызовом проверить на null
	// функция извлекает список новостей из кэша, либо из сети
	public List<NewsContainer> getTopicList( long dt)
	{
		List<NewsContainer> news	= new ArrayList<NewsContainer>();
		// подключаемся к БД
		SQLiteDatabase db			= this.getReadableDatabase();

		Cursor res =  db.rawQuery( "SELECT * FROM " + NEWS_TABLE_NAME
				+" WHERE "+ NEWS_COLUMN_DATE +" < ? ORDER BY "+NEWS_COLUMN_DATE+" DESC limit 10"
				, new String[]{ Long.toString( dt )});
		res.moveToFirst();

		while(res.isAfterLast() == false)
		{
			String title	= res.getString(	res.getColumnIndex(NEWS_COLUMN_TITLE));
			String url		= res.getString(	res.getColumnIndex(NEWS_COLUMN_URL));
			Long lDate		= res.getLong(		res.getColumnIndex(NEWS_COLUMN_DATE));
			String imageUrl	= res.getString(	res.getColumnIndex(NEWS_COLUMN_IMAGEURL));
			NewsContainer	newsElement	= new NewsContainer(url,title,imageUrl,lDate);
			news.add(newsElement);
			res.moveToNext();
		}
		db.close();
		return  news;
	}

	// функция извлекает страницу с новостью из кэша или из сети
	public CachedNewsContainer getNewsPage(long dt)
	{
		CachedNewsContainer bodyContainer	= null;

		// подключаемся к БД
		SQLiteDatabase db		= this.getReadableDatabase();

		Cursor res =  db.rawQuery( "SELECT "+NEWS_COLUMN_BODY+", "+NEWS_COLUMN_ISDESCRIPTION+" FROM " + NEWS_TABLE_NAME
				+" WHERE "+ NEWS_COLUMN_DATE +" = ? limit 1"
				, new String[]{ Long.toString( dt )});
		res.moveToFirst();

		if(!res.isAfterLast())
		{
			String body				= res.getString(	res.getColumnIndex(NEWS_COLUMN_BODY));
			boolean isDescription	= res.getInt( res.getColumnIndex(NEWS_COLUMN_ISDESCRIPTION)) == 1;
			bodyContainer 			= new CachedNewsContainer( body, isDescription);
		}

		db.close();
		return bodyContainer;
	}

	public boolean storeTopicList(List<NewsContainer> topicList)
	{
		long dt1 = getMaxStoredDate();
		long dt2 = getMinStoredDate();

		for (NewsContainer currentNews: topicList)
		{
			if(	dt1 == -1 ||
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
	private boolean addNews(NewsContainer newsElement)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues contentValues = new ContentValues();

		contentValues.put(NEWS_COLUMN_TITLE,	newsElement.getTitle());
		contentValues.put(NEWS_COLUMN_URL,		newsElement.getUrl());
		contentValues.put(NEWS_COLUMN_BODY,		newsElement.getBody());
		contentValues.put(NEWS_COLUMN_DATE, 	newsElement.getDate());
		contentValues.put(NEWS_COLUMN_IMAGEURL, newsElement.getImageLink());
		contentValues.put(NEWS_COLUMN_ISDESCRIPTION, 1);
		long status = db.insert(NEWS_TABLE_NAME, null, contentValues);
		db.close();
		return true;
	}

	// обновить текст новости
	public boolean updateNewsBody(long date, String body)
	{
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues contentValues = new ContentValues();
		contentValues.put(NEWS_COLUMN_BODY, body);
		contentValues.put(NEWS_COLUMN_ISDESCRIPTION, 0);

		int status	= db.update(NEWS_TABLE_NAME, contentValues, "date = ? ", new String[] { Long.toString(date) } );
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
	public long getMaxStoredDate()
	{
		long result = -1;
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor rs = db.rawQuery("SELECT max( " + NEWS_COLUMN_DATE + " ) from " + NEWS_TABLE_NAME, null);
		//TODO: проверить, может нет данных
		rs.moveToFirst();
		result	= rs.getLong(0);
		if (!rs.isClosed())
		{
			rs.close();
		}
		db.close();
		return result;
	}

	// минимальная дата новости в бд
	private long getMinStoredDate()
	{
		long result = -1;
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor rs = db.rawQuery("SELECT min( " + NEWS_COLUMN_DATE + " ) from " + NEWS_TABLE_NAME, null);
		//TODO: проверить, может нет данных
		rs.moveToFirst();
		result	= rs.getLong(0);

		if (!rs.isClosed())
		{
			rs.close();
		}
		db.close();
		return result;
	}
}
