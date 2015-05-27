package com.grishberg.livegoodlineparser.ui.fragments;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.grishberg.livegoodlineparser.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class FullScreenViewActivityFragment extends Fragment
{

	public static final String TAG = "LiveGL.FullScreenView";
	public static final String INTENT_POSITION = "position";
	public static final String INTENT_URL_ARRAY = "urlArray";

	public FullScreenViewActivityFragment()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_full_screen_view, container, false);
	}
}
