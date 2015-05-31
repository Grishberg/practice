package com.grishberg.livegoodlineparser.data.livegoodlineparser;


import com.grishberg.livegoodlineparser.data.containers.NewsBodyElementContainer;
import com.grishberg.livegoodlineparser.data.containers.NewsContainer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by g on 06.05.15.
 * класс для парсинга контента
 */
public class LiveGoodlineParser
{
    public LiveGoodlineParser()
    {

    }
    // парсинг статей
    public static List<NewsContainer> getNewsPerPage(String content)
    {
        List<NewsContainer> elements = new ArrayList<NewsContainer>();

        Document doc            = Jsoup.parse(content);
        Element container       = doc.body().getElementById("container");
        Element wr              = container.getElementById("wrapper");
        Element cont            = wr.getElementById("content");
        Element newsBlock       = cont.getElementsByClass("list-topic").first();
        if(newsBlock != null)
        {
            Elements articleBlocks  = newsBlock.getElementsByTag("article");
            for(Element e: articleBlocks)
            {
                // цикл по блокам со статьями
                NewsContainer element = null;
                try
                {
                    String imageLink    = "";
                    // блок с картинкой
                    Element divPreview  = e.getElementsByClass("preview").first();
                    if(divPreview != null)
                    {
                        // может не быть картинки
                        Element imageLinkElement = divPreview.getElementsByTag("a").first();
                        imageLink = imageLinkElement.getElementsByTag("img").first().attr("src");
                    }

                    // блок с описанием
                    //
                    Element divTopic    = e.getElementsByClass("wraps").first();
                    Element header      = divTopic.getElementsByTag("header").first();
                    Element headerTitle = header.getElementsByClass("topic-title").first();
                    Element title       = headerTitle.getElementsByTag("a").first();
                    String articleUrl   = title.attr("href");
                    String articleTitle = title.html();

                    Element timeElement = header.getElementsByTag("time").first();
                    String timeStr      = timeElement.attr("datetime");

                    Element articleBodyBlock    = divTopic.getElementsByClass("topic-content").first();
                    String articleBody  = articleBodyBlock.text().replace("Читать дальше","");

                    element             = new NewsContainer(articleUrl, articleTitle, articleBody, imageLink, timeStr);
                    elements.add(element);
                }
                catch (Exception error)
                {
                    error.printStackTrace();
                }
            }
        }

        return elements;
    }

    // парсинг отдельной статьи
    public static String getNews(String content)
    {
        String result = null;

        Document doc        = Jsoup.parse(content);
        try
        {
            Element container = doc.body().getElementById("container");
            Element wr          = container.getElementById("wrapper");
            Element cont        = wr.getElementById("content");
            Element newsBlock   = cont.getElementsByClass("topic-content").first();
            //String articleBody  = newsBlock.text();
            String articleBody  = newsBlock.html().replace("<img ","<img align=\"center\" ");

            result = articleBody;
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<NewsBodyElementContainer>    parseNewsBody(String newsBody)
    {
        List<NewsBodyElementContainer> result   = new ArrayList<>();

        return result;
    }

}
