package com.grishberg.livegoodlineparser.data;

import com.grishberg.livegoodlineparser.data.model.NewsElement;

/**
 * Created by G on 12.05.15.
 */
public interface IGetNewsResponseListener
{
	public void onResponseGetNewsPage(String newsBody, boolean fromCache);
}
