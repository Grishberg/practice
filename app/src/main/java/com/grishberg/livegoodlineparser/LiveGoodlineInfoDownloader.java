package com.grishberg.livegoodlineparser;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.ImageView;

import com.grishberg.livegoodlineparser.livegoodlineparser.NewsElement;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by G on 12.05.15.
 * класс для асинхронной загрузки страниц
 */
public class LiveGoodlineInfoDownloader
{
	private DBHelper		dbHelper;
	private Context			context;
	private SQLiteDatabase	db;


	public LiveGoodlineInfoDownloader(Context context)
	{
		this.context	= context;
		this.dbHelper	= new DBHelper(context);
	}



	class DBHelper extends SQLiteOpenHelper
	{

		public static final String DATABASE_NAME		= "cahce.db";
		public static final String CACHE_TABLE_NAME		= "cahcetable";
		public static final String CONTACTS_COLUMN_ID	= "id";
		public static final String CONTACTS_COLUMN_NAME = "name";
		public static final String CONTACTS_COLUMN_EMAIL = "email";
		public static final String CONTACTS_COLUMN_STREET = "street";
		public static final String CONTACTS_COLUMN_CITY = "place";
		public static final String CONTACTS_COLUMN_PHONE = "phone";

		public DBHelper(Context context)
		{
			// конструктор суперкласса
			super(context, "myDB", null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			// создаем таблицу для хранения кэша
			db.execSQL("create table "+CACHE_TABLE_NAME+" ("
					+ "id			INTEGER PRIMARY KEY autoincrement,"
					+ "date			DATETIME DEFAULT,"
					+ "title		TEXT ,"
					+ "url			TEXT ,"
					+ "body			TEXT ,"
					+ "imageLink	TEXT ,"
					+ "image		BLOB"
					+ ");");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{

		}
		//----------------------------------------------------------------------
		//------------- вспомогательные функции для работы с бд ----------------
		//----------------------------------------------------------------------
		private void getTopicList(Context context
				, String url
				, int page
				, IGetTopicListResponseListener listener)
		{
			// подключаемся к БД
			SQLiteDatabase db = dbHelper.getWritableDatabase();
		}

		private void getNewsPage(Context context
				, String url
				, int page
				, IGetNewsResponseListener listener)
		{
			SQLiteDatabase db = dbHelper.getWritableDatabase();

			dbHelper.close();
		}

		// добавить новость в бд
		private void addNews(NewsElement newsElement)
		{

		}

		// обновить текст новости
		private void updateNewsBody(Date dt, String body)
		{

		}

		// обновить картинку в бд
		private void updateNewsPreviewImage(Date dt, ImageView image)
		{

		}

		// максимальная дата новости в бд
		private Date getMaxStoredDate()
		{
			Date result = new Date(1,1,1);

			Cursor resultSet = db.rawQuery("SELECT max(dt) from " +CACHE_TABLE_NAME ,null);
			resultSet.moveToFirst();
			String username = resultSet.getString(1);
			String password = resultSet.getString(2);

			return result;
		}

		// минимальная дата новости в бд
		private Date getMinStoredDate()
		{
			Date result = new Date(1,1,1);

			return result;
		}
	}

}
