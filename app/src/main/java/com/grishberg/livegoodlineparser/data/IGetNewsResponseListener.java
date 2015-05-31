package com.grishberg.livegoodlineparser.data;

/**
 * Created by G on 12.05.15.
 */
public interface IGetNewsResponseListener
{
	public void onResponseGetNewsPage(String newsBody, boolean fromCache, int errorCode);
}
