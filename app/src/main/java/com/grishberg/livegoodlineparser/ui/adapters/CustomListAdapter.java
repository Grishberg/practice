package com.grishberg.livegoodlineparser.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.grishberg.livegoodlineparser.R;
import com.grishberg.livegoodlineparser.data.containers.NewsContainer;
import com.grishberg.livegoodlineparser.ui.bitmaputils.CircleTransform;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by g on 06.05.15.
 * ListView adapter for news topic list
 */
public class CustomListAdapter extends BaseAdapter
{
    Context mContext;
    private List<NewsContainer> mItems;
    private LayoutInflater      mInflater;
    private Picasso             mPicasso;

    public CustomListAdapter(Context context,List<NewsContainer> elements)
    {
        mItems          = elements;
        mContext        = context;
        mInflater       = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPicasso        = Picasso.with(context.getApplicationContext());
    }
    @Override
    public int getCount()
    {
        return mItems.size();
    }

    @Override
    public Object getItem(int location)
    {
        return mItems.get(location);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    // draw news topic list element
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final ViewHolder holder;

        // setup ViewHolder for fast finding
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.tableview_cell, parent, false);
            holder  = new ViewHolder();
            holder.tvTitle  = (TextView) convertView.findViewById(R.id.tvTitle);
            holder.tvDate   = (TextView) convertView.findViewById(R.id.tvDate);
            holder.imgIcon  = (ImageView) convertView.findViewById(R.id.thumbnail);
            holder.progressBar  = (ProgressBar) convertView.findViewById(R.id.icon_loading_spinner);
            convertView.setTag(holder);
        } else {
            holder  = (ViewHolder)  convertView.getTag();
        }

        NewsContainer currentNews = (NewsContainer)getItem(position);

        // fill viewholder with data
        holder.tvTitle.setText(currentNews.getTitle());
        holder.tvDate.setText(currentNews.getDateStr());

        // async download image from url src and transform into circle shape
        if(currentNews.getImageLink().length() > 0) {
            mPicasso.load(currentNews.getImageLink())
                    .resizeDimen(R.dimen.news_cell_image_size, R.dimen.news_cell_image_size)
                    .transform(new CircleTransform())
                    .into(holder.imgIcon, new Callback() {
                        @Override
                        public void onSuccess() {
                            holder.imgIcon.setVisibility(View.VISIBLE);
                            holder.progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {
                            holder.imgIcon.setVisibility(View.VISIBLE);
                            holder.progressBar.setVisibility(View.GONE);

                        }
                    });
        }
        else {
            //show empty picture if image url does not exists
            holder.progressBar.setVisibility(View.GONE);
            holder.imgIcon.setVisibility(View.VISIBLE);
            holder.imgIcon.setImageResource(R.drawable.goodlinelogomini);
        }
        return convertView;
    }

    /**
     * helper class - ViewHolder
     */
    private static class ViewHolder{
        TextView    tvTitle;
        TextView    tvDate;
        ImageView   imgIcon;
        ProgressBar progressBar;
    }
}
