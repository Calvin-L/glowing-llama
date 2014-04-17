package org.uw.glowingllama;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SimplePlot extends View {
	
	LinkedList<Short> samples = new LinkedList<Short>();
	Paint black = new Paint();

	public SimplePlot(Context context, AttributeSet attrs) {
		super(context, attrs);
		black.setColor(Color.BLACK);
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
		synchronized(samples) {
			++sampleNum;
			if (sampleNum % 10 == 0) {
				samples.add(sample);
			}
			while (samples.size() > this.getWidth()) {
				samples.removeFirst();
			}
		}
		this.postInvalidate();
	}
	
	public void putRingBuffer(RingBuffer buffer) {
		for (short e : buffer) {
			putSample(e);
		}
	}

}
