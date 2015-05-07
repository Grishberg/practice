package com.grishberg.livegoodlineparser.livegoodlineparser;

import android.provider.DocumentsContract;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by g on 06.05.15.
 * класс для парсинга контента
 */
public class LiveGoodlineParser
{
    final String newsTag        = "<article class=\"topic topic-type-topic js-topic out-topic\">";
    final String shortBodyTag   = "<div class=\"topic-content text\">";
    final String titleTag       = "<h2 class=\"topic-title word-wrap\">";

    public LiveGoodlineParser()
    {

    }
    // парсинг статей
    public static List<NewsElement> getNewsPerPage(String content)
    {
        List<NewsElement> elements = new ArrayList<NewsElement>();
        //TODO: спарсить content

        Document doc        = Jsoup.parse(content);
        Element container = doc.body().getElementById("container");
        Element wr          = container.getElementById("wrapper");
        Element cont        = wr.getElementById("content");
        Element newsBlock   = cont.getElementsByClass("list-topic").first();
        Elements articleBlocks = newsBlock.getElementsByTag("article");
        for(Element e: articleBlocks)
        {
            // цикл по блокам со статьями
            NewsElement element = null;
            try
            {
                // блок с картинкой
                Element divPreview  = e.getElementsByClass("preview").first();
                Element imageLinkElement   = divPreview.getElementsByTag("a").first();
                String  imageLink   = imageLinkElement.getElementsByTag("img").first().attr("src");
                //String imageLink    = imageLinkElement.attr("href");

                // блок с описанием
                //
                Element divTopic    = e.getElementsByClass("wraps").first();
                Element header      = divTopic.getElementsByTag("header").first();
                Element headerTitle = header.getElementsByClass("topic-title").first();
                Element title       = headerTitle.getElementsByTag("a").first();
                String articleUrl  = title.attr("href");
                String articleTitle  = title.html();

                Element timeElement = header.getElementsByTag("time").first();
                String timeStr      = timeElement.attr("datetime");

                Element articleBodyBlock    = divTopic.getElementsByClass("topic-content").first();
                String articleBody  = articleBodyBlock.text().replace("Читать дальше","");

                element             = new NewsElement(articleUrl, articleTitle, articleBody, imageLink, timeStr);
                elements.add(element);
            }
            catch (Exception error)
            {
                error.printStackTrace();
            }
        }
        return elements;
    }

    // парсинг отдельной статьи
    public static NewsElement getNews(String content)
    {
        NewsElement result = null;

        Document doc        = Jsoup.parse(content);
        try
        {
            Element container = doc.body().getElementById("container");
            Element wr          = container.getElementById("wrapper");
            Element cont        = wr.getElementById("content");
            Element newsBlock   = cont.getElementsByClass("topic-content").first();
            //String articleBody  = newsBlock.text();
            String articleBody  = newsBlock.html();

            result = new NewsElement("","",articleBody,"","");
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }


}
