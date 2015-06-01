package com.grishberg.livegoodlineparser.data.containers;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by G on 29.05.15.
 */
public class NewsContainer implements Comparable, Parcelable
{
	private String 	mUrl      = "";
	private String 	mTitle    = "";
	private String 	mBody     = "";
	private String 	mImageLink= "";
	private String 	mDateStr  = "";
	private Long 	mDate       = 0L;

	public  NewsContainer()
	{

	}

	public  NewsContainer(String url, String title, String body, String imageLink, String dateStr)
	{
		mUrl        = url;
		mTitle      = title;
		mBody       = body;
		mImageLink  = imageLink;
		mDate       = 0L;
		Date date	= new Date(mDate);
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
				date              	= formatter.parse(dateStr);
				mDate				= date.getTime();

				Date currentDate    = new Date();
				if(date.getDate() == currentDate.getDate() &&
						date.getMonth() == currentDate.getMonth() &&
						date.getYear()  == currentDate.getYear())
				{
					dayPrefix   = "Сегодня";
				} else if(date.getDate() == currentDate.getDate()-1 &&
						date.getMonth() == currentDate.getMonth() &&
						date.getYear()  == currentDate.getYear())
				{
					dayPrefix   = "Вчера";
				} else
				{
					dayPrefix   = dateFormat.format(date);
				}
				// формирование времени
				convertedDate   = dayPrefix + timeFormat.format(date);

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
			, Long      date)
	{
		mUrl        = url;
		mTitle      = title;
		mBody       = "";
		mImageLink  = previewImageUrl;
		mDate       = date;
		Date dtDate	= new Date(mDate);
		DateFormat dateFormat       = new SimpleDateFormat("dd.MM.yyyy");
		DateFormat timeFormat       = new SimpleDateFormat(" HH:mm:ss");
		String convertedDate        = "";

		try
		{
			// формирование даты
			String dayPrefix    = "";
			Date currentDate    = new Date();
			if(dtDate.getDate()   == currentDate.getDate() &&
					dtDate.getMonth() == currentDate.getMonth() &&
					dtDate.getYear()  == currentDate.getYear())
			{
				dayPrefix   = "Сегодня";
			} else if(dtDate.getDate()    == currentDate.getDate()-1 &&
					dtDate.getMonth()     == currentDate.getMonth() &&
					dtDate.getYear()      == currentDate.getYear())
			{
				dayPrefix   = "Вчера";
			} else
			{
				dayPrefix   = dateFormat.format(dtDate);
			}
			// формирование времени
			convertedDate   = dayPrefix + timeFormat.format(dtDate);

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

	public int compareToDate(Long date)
	{
		return mDate.compareTo(date);
	}

	public String getUrl(){return mUrl;}
	public String getTitle(){return  mTitle;}
	public String getBody(){return  mBody;}
	public String getImageLink(){return mImageLink;}
	public String getDateStr(){return mDateStr;}
	public Long getDate(){return mDate;}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(mUrl);
		dest.writeString(mTitle);
		dest.writeString(mBody);
		dest.writeString(mImageLink);
		dest.writeString(mDateStr);
		dest.writeLong(mDate);
	}
}
