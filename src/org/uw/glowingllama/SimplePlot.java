package org.uw.glowingllama;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class SimplePlot extends View {
	
	RingBuffer samples;
	Paint black = new Paint();

	public SimplePlot(Context context, AttributeSet attrs) {
		super(context, attrs);
		black.setColor(Color.BLACK);
		samples = new RingBuffer(Math.max(getWidth(), 1));
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		samples.resize(Math.max(w, 1));		
	}

	@Override
	protected void onDraw(Canvas c) {
		c.drawColor(Color.WHITE);
		
		int w = getWidth();
		int h = getHeight();
		c.drawLine(0, h/2, w, h/2, black);
		
		int x = 0;
		synchronized(samples) {
			for (short s : samples) {
				c.drawLine(x, h/2, x, -(int)((h/2) * ((double)s / 33000)) + h/2, black);
				x++;
			}
		}
	}
	
	int sampleNum = 0;
	
	public void putSample(short sample) {
		putMultipleSamples(new short[] {sample}); 
//		synchronized(samples) {
//			++sampleNum;
//			if (sampleNum % 100 == 0) {
//				samples.addElement(sample);
//				postInvalidate();
//			}
//		}
	}
	
	public void putMultipleSamples(short[] newSamples) {
		synchronized(samples) {
			for (short s : newSamples) {
				++sampleNum;
				if (sampleNum % 50 == 0) {
					samples.addElement(s);
				}
			}
		}
		postInvalidate();
	}
	
	
	public void reset() {
		samples.reset();
	}

}
