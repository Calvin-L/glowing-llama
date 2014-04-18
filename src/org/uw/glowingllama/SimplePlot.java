package org.uw.glowingllama;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class SimplePlot extends View {
	
	RingBuffer samples;
	Paint drawPaint = new Paint();
	int skip = 100;
	List<Integer> marks = Collections.emptyList();

	public SimplePlot(Context context, AttributeSet attrs) {
		super(context, attrs);
		drawPaint.setColor(Color.BLACK);
		samples = new RingBuffer(Math.max(getWidth(), 1));
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		samples.resize(Math.max(w, 1));		
	}
	
	public void setMarks(List<Integer> mks) {
		marks = mks;
	}

	@Override
	protected void onDraw(Canvas c) {
		c.drawColor(Color.WHITE);
		
		int w = getWidth();
		int h = getHeight();
		drawPaint.setColor(Color.BLACK);
		c.drawLine(0, h/2, w, h/2, drawPaint);
		
		drawPaint.setColor(Color.RED);
		for (int mark : marks) {
			c.drawLine(mark, 0, mark, h, drawPaint);
		}
		
		int x = 0;
		synchronized(samples) {
			short min = 0;
			short max = 0;
			for (short s : samples) {
				min = (short)Math.min(min, s);
				max = (short)Math.max(max, s);
			}
			double scale = Math.max(1, Math.max(-min, max));
			drawPaint.setARGB(255, (int)(255 * scale / Short.MAX_VALUE), 0, 0);
			for (short s : samples) {
				c.drawLine(x, h/2, x, -(int)((h/2) * ((double)s / scale)) + h/2, drawPaint);
				x++;
			}
		}
	}
	
	public void setSkip(int n) {
		skip = n;
	}
	
	int sampleNum = 0;
	
	public void putSample(short sample) {
		putMultipleSamples(new short[] {sample});
	}
	
	public void putMultipleSamples(short[] newSamples) {
//		Log.i("PLOT: " + this, "I got some stuff: " + newSamples.length);
		boolean didAdd = false;
		synchronized(samples) {
			for (short s : newSamples) {
				++sampleNum;
				if (skip <= 1 || sampleNum % skip == 0) {
					samples.addElement(s);
					didAdd = true;
				}
			}
		}
		if (didAdd) {
			postInvalidate();
		}
	}
	
	
	public void reset() {
		synchronized (samples) {
			sampleNum = 0;
			samples.reset();
		}
	}

	public int getSkip() {
		return skip;
	}

}
