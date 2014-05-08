package org.uw.glowingllama;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class SpectrogramPlot extends View {

	Bitmap imageOnScreen, imageOffScreen;
	Canvas cOn, cOff;
	Paint somePaintObject = new Paint();


	public SpectrogramPlot(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (w < 1) w = 1;
		if (h < 1) h = 1;
		imageOnScreen = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		imageOffScreen = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		cOn = new Canvas(imageOnScreen);
		cOff = new Canvas(imageOffScreen);
	}


	/**
	 * @param samples positive spectrogram coefficients
	 */
	public void addSample(short[] samples) {
		cOff.drawBitmap(imageOnScreen, -1, 0, somePaintObject);

		double ratio = (double) samples.length / (double) cOff.getHeight();
		for (int y = 0; y < cOff.getHeight(); ++y) {
			double index = ratio * y;
			double i1 = Math.floor(index);
			double interp = index - i1;
			double s1 = samples[(int)i1];
			double s2 = samples[Math.min((int)i1 + 1, samples.length - 1)];
			double val = s1 + interp * (s2 - s1);
			double amt = 1d - (1d / (val + 1));
			//			somePaintObject.setARGB(255, (int)(255 * samples[(int)(ratio*y)] / Short.MAX_VALUE), 0, 0);
			//			int value = (int)(255 * Math.abs(samples[(int)(ratio*y)] / 800));
			int value = (int)(255 * amt);
			if (value < 0 || value > 255)
				Log.w("WARNING", "Value is out of range: " + value);
			value = Math.min(value, 255);
			somePaintObject.setARGB(255, value, 0, 0);
			cOff.drawPoint(cOff.getWidth()-1, y, somePaintObject);
		}

		Bitmap temp = imageOnScreen;
		imageOnScreen = imageOffScreen;
		imageOffScreen = temp;

		Canvas ctemp = cOn;
		cOn = cOff;
		cOff = ctemp;

		postInvalidate();
	}

	@Override
	protected void onDraw(Canvas c) {
		c.drawColor(Color.WHITE);
		c.drawBitmap(imageOnScreen, 0, 0, somePaintObject);
	}


}
