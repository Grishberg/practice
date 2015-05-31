package com.grishberg.livegoodlineparser.data.containers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by G on 29.05.15.
 */
public class NewsContainer implements Comparable
{
	private String mUrl      = "";
	private String mTitle    = "";
	private String mBody     = "";
	private String mImageLink= "";
	private String mDateStr  = "";
	private Date mDate       = null;

	public  NewsContainer()
	{

	}

	public  NewsContainer(String url, String title, String body, String imageLink, String dateStr)
	{
		mUrl        = url;
		mTitle      = title;
		mBody       = body;
		mImageLink  = imageLink;
		mDate       = new Date(1,1,1);
		SimpleDateFormat 	formatter		= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		DateFormat 			dateFormat		= new SimpleDateFormat("dd.MM.yyyy");
		DateFormat 			timeFormat		= new SimpleDateFormat(" HH:mm:ss");
		String 				convertedDate	= dateStr;
		if(dateStr.length() > 0)
		{
			try
			{
				// формирование даты
				String dayPrefix    = "";
				mDate              	= formatter.parse(dateStr);
				Date currentDate    = new Date();
				if(mDate.getDate() == currentDate.getDate() &&
						mDate.getMonth() == currentDate.getMonth() &&
						mDate.getYear()  == currentDate.getYear())
				{
					dayPrefix   = "Сегодня";
				} else if(mDate.getDate() == currentDate.getDate()-1 &&
						mDate.getMonth() == currentDate.getMonth() &&
						mDate.getYear()  == currentDate.getYear())
				{
					dayPrefix   = "Вчера";
				} else
				{
					dayPrefix   = dateFormat.format(mDate);
				}
				// формирование времени
				convertedDate   = dayPrefix + timeFormat.format(mDate);

			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		mDateStr    = convertedDate;
	}

	public  NewsContainer(String url
			, String    title
			, String    previewImageUrl
			, Long      lDate)
	{
		mUrl        = url;
		mTitle      = title;
		mBody       = "";
		mImageLink  = previewImageUrl;
		mDate       = new Date(lDate);

		DateFormat dateFormat       = new SimpleDateFormat("dd.MM.yyyy");
		DateFormat timeFormat       = new SimpleDateFormat(" HH:mm:ss");
		String convertedDate        = "";

		try
		{
			// формирование даты
			String dayPrefix    = "";
			Date currentDate    = new Date();
			if(mDate.getDate()   == currentDate.getDate() &&
					mDate.getMonth() == currentDate.getMonth() &&
					mDate.getYear()  == currentDate.getYear())
			{
				dayPrefix   = "Сегодня";
			} else if(mDate.getDate()    == currentDate.getDate()-1 &&
					mDate.getMonth()     == currentDate.getMonth() &&
					mDate.getYear()      == currentDate.getYear())
			{
				dayPrefix   = "Вчера";
			} else
			{
				dayPrefix   = dateFormat.format(mDate);
			}
			// формирование времени
			convertedDate   = dayPrefix + timeFormat.format(mDate);

		} catch (Exception e)
		{
			e.printStackTrace();
		}

		mDateStr    = convertedDate;
	}

	@Override
	public int compareTo(Object obj)
	{
		NewsContainer entry	= (NewsContainer) obj;
		if (mDate == null || entry.mDate == null) return -1;
		return mDate.compareTo(entry.mDate);
	}

	public int compareToDate(Date dt)
	{
		if (mDate == null || dt == null) return -1;
		return mDate.compareTo(dt);
	}

	public String getUrl(){return mUrl;}
	public String getTitle(){return  mTitle;}
	public String getBody(){return  mBody;}
	public String getImageLink(){return mImageLink;}
	public String getDateStr(){return mDateStr;}
	public Date getDate(){return mDate;}
}
