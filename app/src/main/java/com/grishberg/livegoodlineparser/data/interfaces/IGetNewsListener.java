package com.grishberg.livegoodlineparser.data.interfaces;

import com.grishberg.livegoodlineparser.data.containers.NewsBodyContainer;

/**
 * Created by G on 29.05.15.
 */
public interface IGetNewsListener
{
	void onProgress(NewsBodyContainer progressResult);
}
