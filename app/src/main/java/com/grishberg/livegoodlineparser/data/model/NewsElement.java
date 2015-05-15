package com.grishberg.livegoodlineparser.data.model;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by g on 06.05.15.
 * класс контейнер для хранения новости
 */
public class NewsElement implements Comparable
{
	private String url      = "";
	private String title    = "";
	private String body     = "";
	private String imageLink= "";
	private String dateStr  = "";
	private Date date       = null;
	public  NewsElement()
	{

	}

	public  NewsElement(String url, String title, String body, String imageLink, String dateStr)
	{
		this.url        = url;
		this.title      = title;
		this.body       = body;
		this.imageLink  = imageLink;
		this.date       = new Date(1,1,1);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		DateFormat timeFormat = new SimpleDateFormat(" HH:mm:ss");
		String convertedDate    = dateStr;
		if(dateStr.length() > 0)
		{
			try
			{
				// формирование даты
				String dayPrefix    = "";
				date                = formatter.parse(dateStr);
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

		this.dateStr    = convertedDate;
	}

	public  NewsElement(String url
			, String    title
			, String    previewImageUrl
			, Long      lDate)
	{
		this.url        = url;
		this.title      = title;
		this.body       = "";
		this.imageLink  = previewImageUrl;
		this.date       = new Date(lDate);

		DateFormat dateFormat       = new SimpleDateFormat("dd.MM.yyyy");
		DateFormat timeFormat       = new SimpleDateFormat(" HH:mm:ss");
		String convertedDate        = "";

		try
		{
			// формирование даты
			String dayPrefix    = "";
			Date currentDate    = new Date();
			if(date.getDate()   == currentDate.getDate() &&
					date.getMonth() == currentDate.getMonth() &&
					date.getYear()  == currentDate.getYear())
			{
				dayPrefix   = "Сегодня";
			} else if(date.getDate()    == currentDate.getDate()-1 &&
					date.getMonth()     == currentDate.getMonth() &&
					date.getYear()      == currentDate.getYear())
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

		this.dateStr    = convertedDate;
	}

	@Override
	public int compareTo(Object obj)
	{
		NewsElement entry = (NewsElement) obj;
		if (this.date == null || entry.date == null) return -1;
		return this.date.compareTo(entry.date);
	}
	public int compareToDate(Date dt)
	{
		if (this.date == null || dt == null) return -1;
		return this.date.compareTo(dt);
	}
	public String getUrl(){return url;}
	public String getTitle(){return  title;}
	public String getBody(){return  body;}
	public String getImageLink(){return imageLink;}
	public String getDateStr(){return dateStr;}
	public Date getDate(){return date;}
}
