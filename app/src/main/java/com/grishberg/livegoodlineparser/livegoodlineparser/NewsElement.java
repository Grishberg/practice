package com.grishberg.livegoodlineparser.livegoodlineparser;

import com.android.volley.toolbox.StringRequest;

/**
 * Created by g on 06.05.15.
 * класс контейнер для хранения новости
 */
public class NewsElement
{
    private String url      = "";
    private String title    = "";
    private String body     = "";
    private String imageLink    = "";
    private String dateStr  = "";
    public  NewsElement()
    {

    }

    public  NewsElement(String url, String title, String body, String imageLink, String date)
    {
        this.url    = url;
        this.title  = title;
        this.body   = body;
        this.imageLink  = imageLink;
        this.dateStr    = date;
    }

    public String getUrl(){return url;}
    public String getTitle(){return  title;}
    public String getBody(){return  body;}
    public String getImageLink(){return imageLink;}
    public String getDate(){return dateStr;}
}
