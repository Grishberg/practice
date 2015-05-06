package com.grishberg.livegoodlineparser;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.*;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.grishberg.livegoodlineparser.livegoodlineparser.LiveGoodlineParser;
import com.grishberg.livegoodlineparser.livegoodlineparser.NewsElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener
{
    // ссылка на основную страницу
    private final String mainUrl       = "http://live.goodline.info/guest";
    private final int newsCountPerPage  = 10;
    ProgressDialog progressDlg;

    // для загрузки изображений
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private ListView    lvNews;
    private SwipeRefreshLayout  swipeRefreshLayout;
    private CustomListAdapter   adapter;
    private List<NewsElement>   elements;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache()
        {
            private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(newsCountPerPage);
            public void putBitmap(String url, Bitmap bitmap)
            {
                mCache.put(url, bitmap);
            }
            public Bitmap getBitmap(String url)
            {
                return mCache.get(url);
            }
        });
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        lvNews    = (ListView)findViewById(R.id.listView);

        //----------------- реализация inifinite scroll ------------------------
        lvNews.setOnScrollListener(new InfinityScrollListener(newsCountPerPage)
        {
            @Override
            public void loadMore(int page, int totalItemsCount)
            {
                // загрузить еще данных
                getPageContent(page);
            }
        });

        elements    = new ArrayList<NewsElement>();
        adapter     = new CustomListAdapter(getApplicationContext(), imageLoader,elements);
        lvNews.setAdapter(adapter);

        progressDlg = new ProgressDialog(this);
        progressDlg.setTitle("Статус");
        progressDlg.setMessage("Идет обновление новостей...");

        // обработка нажатия на элемент списка
        lvNews.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            // реакция на нажатие по элементу
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id)
            {
                NewsElement currentNews = (NewsElement) adapter.getItem(position);
                OnLvClicked(currentNews);
            }
        });

        progressDlg.show();
        getPageContent(1);
    }

    //------------------- процедура фоновой загрузки страницы -------------------------
    private void getPageContent(int page)
    {
        String url = mainUrl;
        // скорректировать URL в зависимости от страницы
        if(page > 1)
        {
            url += String.format("/page%d/",page);
        }
        progressDlg.show();

        RequestQueue queue      = Volley.newRequestQueue(this);

        // отправка запроса на закачку страницы
        StringRequest getReq    = new StringRequest(Request.Method.GET
                , url
                , new Response.Listener<String>()
        {
            // событие возникает при успешном чтении
            @Override
            public void onResponse(String response)
            {
                // парсим статью
                List<NewsElement> newElements = LiveGoodlineParser.getNewsPerPage(response);
                elements.addAll(newElements);
                // отобразить на фрагменте статью целиком
                adapter.notifyDataSetChanged();
                // отключаем прогрессбар
                progressDlg.dismiss();
            }
        }, new Response.ErrorListener()
        {
            // возникла ошибка
            @Override
            public void onErrorResponse(VolleyError error)
            {
                System.out.println("Error ["+error+"]");
                progressDlg.dismiss();
            }
        });
        queue.add(getReq);
    }

    //----- событие при pull down to refresh
    @Override
    public void onRefresh()
    {
        // говорим о том, что собираемся начать
        Toast.makeText(this, "started", Toast.LENGTH_SHORT).show();
        // начинаем показывать прогресс
        swipeRefreshLayout.setRefreshing(true);
        // ждем 3 секунды и прячем прогресс
        swipeRefreshLayout.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                swipeRefreshLayout.setRefreshing(false);
                // говорим о том, что собираемся закончить
                Toast.makeText(MainActivity.this, "finished", Toast.LENGTH_SHORT).show();
            }
        }, 3000);
    }

    //----- обработка фоновой загрузки новости
    public void OnLvClicked(NewsElement selectedItem)
    {
        //TODO: создать задачу загрузки полной статьи
        //----------- фоновая загрузка списка новостей
        RequestQueue queue      = Volley.newRequestQueue(this);

        // отправка запроса на закачку страницы
        StringRequest getReq    = new StringRequest(Request.Method.GET
                , selectedItem.getUrl()
                , new Response.Listener<String>()
        {
            // событие возникает при успешном чтении
            @Override
            public void onResponse(String response)
            {
                // парсим статью
                NewsElement newElements = LiveGoodlineParser.getNews(response);

                // отобразить на фрагменте статью целиком

                // отключаем прогрессбар
                progressDlg.dismiss();
            }
        }, new Response.ErrorListener()
        {
            // возникла ошибка
            @Override
            public void onErrorResponse(VolleyError error)
            {
                System.out.println("Error ["+error+"]");
                progressDlg.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
