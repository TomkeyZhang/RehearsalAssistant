package urbanstew.RehearsalAssistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

public class IndicatingListView extends ListView
{

	public IndicatingListView(Context context)
	{
		super(context);
	}

	public IndicatingListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public IndicatingListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}
	
	public void setIndication(int position)
	{
		mPosition = position;

    	int viewIndex = position - getFirstVisiblePosition();
    	if(viewIndex<=0 || viewIndex >= getChildCount() - 1)
    		setSelection(position);
    	
    	invalidate();
	}
	
	public void clearIndication()
	{
		restoreBackgroundDrawable();
		mPosition = -1;

    	invalidate();
	}
	
	protected void  onSizeChanged  (int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		if(mPosition != -1)
			setIndication(mPosition);
	}
	
	protected void  onDraw  (Canvas canvas)
	{
		restoreBackgroundDrawable();    	

		if(mPosition != -1)
		{
	    	mSelectedView = getChildAt(mPosition - getFirstVisiblePosition());
		   	if(mSelectedView != null)
		   	{
				mSelectedViewOldDrawable = mSelectedView.getBackground();
	    		mSelectedView.setBackgroundColor(Color.argb(127, 255, 255, 255));
	    	}
		}

		super.onDraw(canvas);
	}

	void restoreBackgroundDrawable()
	{
		if(mSelectedView != null)
			mSelectedView.setBackgroundDrawable(mSelectedViewOldDrawable);
		mSelectedView = null;		
	}
		
	int mPosition = -1;
    View mSelectedView = null;
    Drawable mSelectedViewOldDrawable;
}
