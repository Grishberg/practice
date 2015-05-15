package com.grishberg.livegoodlineparser;

import com.grishberg.livegoodlineparser.livegoodlineparser.NewsElement;
import java.util.List;

/**
 * Created by G on 12.05.15.
 */
public interface IGetNewsResponseListener
{
	public void onResponseGetNewsPage(String newsBody, boolean fromCache);
}
