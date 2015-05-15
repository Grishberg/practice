package com.grishberg.livegoodlineparser.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.grishberg.livegoodlineparser.R;
import com.grishberg.livegoodlineparser.bitmaputils.CircleTransform;
import com.grishberg.livegoodlineparser.data.model.NewsElement;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by g on 06.05.15.
 */
public class CustomListAdapter extends BaseAdapter
{
    Context context;
    private List<NewsElement>   items;
    private LayoutInflater      inflater;
    private Picasso             mPicasso;
    public CustomListAdapter(Context context,List<NewsElement> elements)
    {
        this.items          = elements;
        this.context        = context;
        inflater            = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPicasso            = Picasso.with(context.getApplicationContext());
    }
    @Override
    public int getCount()
    {
        return items.size();
    }

    @Override
    public Object getItem(int location)
    {
        return items.get(location);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    // пункт списка
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // используем созданные, но не используемые view
        View view = convertView;
        if (view == null)
        {
            view = inflater.inflate(R.layout.tableview_cell, parent, false);
        }

        NewsElement p = (NewsElement)getItem(position);

        // заполняем View
        ((TextView) view.findViewById(R.id.tvTitle)).setText(p.getTitle());
        ((TextView) view.findViewById(R.id.tvDate)).setText(p.getDateStr());
        ImageView img = (ImageView) view.findViewById(R.id.thumbnail);

        if(p.getImageLink().length() > 0)
        {
            mPicasso.load(p.getImageLink()).transform(new CircleTransform()).into(img);
        }
        else
        {
            // отображать пустую картинку для таких случаев
            img.setImageResource(R.drawable.goodlinelogomini);
        }
        return view;
    }
}
