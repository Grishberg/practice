package com.grishberg.livegoodlineparser.ui.listeners;

import com.grishberg.livegoodlineparser.data.interfaces.IGetTopilistListener;

/**
 * Created by grigoriy on 04.06.15.
 */
public interface ITopicListActivityActions {
	/**
	 * event for click on topic list item
	 * @param title - news title
	 * @param url	- news url
	 * @param date	- news date
	 */
	void onTopicListItemClicked(String title, String url, long date);
	void onRegister(ITopicListFragmentActions fragment);
	void onUnregister(ITopicListFragmentActions fragment);
}
