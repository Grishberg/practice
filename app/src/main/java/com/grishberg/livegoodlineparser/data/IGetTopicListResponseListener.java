package com.grishberg.livegoodlineparser.data;

import com.grishberg.livegoodlineparser.data.livegoodlineparser.NewsElement;

import java.util.List;

/**
 * Created by G on 12.05.15.
 */
public interface IGetTopicListResponseListener
{
	public void onResponseGetTopicList(List<NewsElement> topicList, boolean fromCache);
}
