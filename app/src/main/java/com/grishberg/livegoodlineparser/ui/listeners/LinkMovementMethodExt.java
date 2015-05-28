package com.grishberg.livegoodlineparser.ui.listeners;

import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ImageSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import com.grishberg.livegoodlineparser.ui.fragments.NewsActivityFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by G on 26.05.15.
 */


public class LinkMovementMethodExt extends LinkMovementMethod {
	//private static LinkMovementMethod sInstance;
	private Handler mHandler			= null;
	private List<Class> mSpanClassList	= null;
	public LinkMovementMethodExt(Handler handler, Class[] spanClassArray) {
		super();
		mHandler	= handler;
		mSpanClassList = new ArrayList<Class>();
		for(Class currentSpan: spanClassArray)
		{
			mSpanClassList.add(currentSpan);
		}
	}

	private void addClass(Class spanClass)
	{
		if(mSpanClassList == null)
		{
			mSpanClassList	= new ArrayList<Class>();
		}
		mSpanClassList.add(spanClass);
	}

	@Override
	public boolean onTouchEvent(TextView widget, Spannable buffer,
								MotionEvent event) {
		int action = event.getAction();
		if (action == MotionEvent.ACTION_UP ||
				action == MotionEvent.ACTION_DOWN) {
			int x = (int) event.getX();
			int y = (int) event.getY();

			x -= widget.getTotalPaddingLeft();
			y -= widget.getTotalPaddingTop();

			x += widget.getScrollX();
			y += widget.getScrollY();

			Layout layout = widget.getLayout();
			int line = layout.getLineForVertical(y);
			int off = layout.getOffsetForHorizontal(line, x);
			/**
			 * get you interest span, here get ImageSpan that you click
			 */
			for(Class spanClass: mSpanClassList)
			{
				Object[] spans = buffer.getSpans(off, off, spanClass);
				if (spans.length != 0)
				{
					if (action == MotionEvent.ACTION_UP)
					{
						Message message = mHandler.obtainMessage();
						message.obj = spans;
						message.what = NewsActivityFragment.IMAGE_CLICK;
						message.sendToTarget();
						return true;
					}
				}
			}
		}

		return super.onTouchEvent(widget, buffer, event);
	}

}