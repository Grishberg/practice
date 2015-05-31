package com.grishberg.livegoodlineparser.data.containers;

import java.util.List;

/**
 * Created by G on 29.05.15.
 */
public class TopicListContainer
{
	private int					mErrorCode;
	private List<NewsContainer>	mNewsList;
	private boolean				mInsertToTop;

	public TopicListContainer(List<NewsContainer> news, int errorCode, boolean insertToTop)
	{
		mNewsList		= news;
		mErrorCode		= errorCode;
		mInsertToTop	= insertToTop;
	}

	public int getErrorCode()
	{
		return mErrorCode;
	}

	public List<NewsContainer> getNewsList()
	{
		return mNewsList;
	}

	public boolean isInsertToTop()
	{
		return mInsertToTop;
	}
}
