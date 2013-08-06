package org.urbanstew;

import java.util.LinkedList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class VolumeEnvelopeView extends View
{
    /**
     * Constructor.  This version is only needed if you will be instantiating
     * the object manually (not from a layout XML file).
     * @param context
     */
    public VolumeEnvelopeView(Context context) {
        super(context);
        initVolumeEnvelopeView();
    }

    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public VolumeEnvelopeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVolumeEnvelopeView();

        TypedArray a = context.obtainStyledAttributes(attrs,
                urbanstew.RehearsalAssistant.R.styleable.VolumeEnvelopeView);

        // Retrieve the color(s) to be used for this view and apply them.
        // Note, if you only care about supporting a single color, that you
        // can instead call a.getColor() and pass that to setTextColor().
        setColor(a.getColor(urbanstew.RehearsalAssistant.R.styleable.VolumeEnvelopeView_color, 0xFF000000));

        a.recycle();
    }

    private final void initVolumeEnvelopeView() {
        mEnvelopePaint = new Paint();
        mEnvelopePaint.setAntiAlias(false);
        mEnvelopePaint.setColor(0xFF000000);
    }

    /**
     * Sets the text color for this label.
     * @param color ARGB value for the text
     */
    public void setColor(int color) {
        mEnvelopePaint.setColor(color);
        invalidate();
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
    	// TODO Auto-generated method stub
    	super.onSizeChanged(w, h, oldw, oldh);
    	mSize = (w - this.getPaddingLeft() - this.getPaddingRight()) / 2;
    }
        
    public void setNewVolume(int value)
    {
    	if(value!=0)
    		mEnvelope.add((float) Math.sqrt(value) / 164.31981f);
    	else if (!mEnvelope.isEmpty())
    		mEnvelope.add(mEnvelope.getLast());
    	
    	while(mEnvelope.size() >= mSize && !mEnvelope.isEmpty())
    			mEnvelope.remove();
    	invalidate();
    }
    
    public void clearVolume()
    {
    	mEnvelope.clear();
    	invalidate();
    }
    
    /**
     * Render the text
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        int x= 2*(mSize - mEnvelope.size()) - 2;
        int height = (this.getHeight()-this.getPaddingBottom() -this.getPaddingTop()) / 2;
        int mid = this.getHeight() / 2;
        for(Float i : mEnvelope)
        {
        	int offset = (int) (height * i.floatValue());
        	canvas.drawLine(x, mid - offset, x, mid + offset + 1, mEnvelopePaint);
        	x+=2;
        }
    }
    
    private Paint mEnvelopePaint;
    private LinkedList<Float> mEnvelope = new LinkedList<Float>();
    int mSize = 0;
}
