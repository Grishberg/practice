package com.grishberg.livegoodlineparser.data.containers;

/**
 * Created by G on 30.05.15.
 */
public class NewsBodyContainer
{
	private String mNews;
	private int mErrorCode;

	public NewsBodyContainer(String newsContainer, int errorCode)
	{
		mNews		= newsContainer;
		mErrorCode	= errorCode;
	}

	public String getNews()
	{
		return mNews;
	}

	public int getErrorCode()
	{
		return mErrorCode;
	}
}
