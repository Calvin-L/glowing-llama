package org.uw.glowingllama;

import org.uw.glowingllama.BitFetcher.Bit;

import android.util.Log;

/**
 * A class for extracting bits from a raw signal. Give it the absolute value
 * of the incoming audio samples ({@link #putSample(short)}) and it will give
 * you a {@link BitFetcher.Bit}. Note that it will output bits a lot; detection
 * of the actual packets takes place in {@link PacketParser}.
 */
public class ThresholdingBitParser {

	private final int expectedBitLength;
	private int threshold;
	private final int requiredHighCount;

	/** the number of samples since the start of the last bit */
	private int n;

	/** the number of high samples since the start of the last bit */
	private int highCount;

	/** the number of samples since the last high sample */
	private int samplesSinceLastHighSample;

	/**
	 * Construct an instance.
	 * @param expectedBitLength    the number of samples per bit
	 * @param threshold            the amplitude cutoff
	 * @param requiredHighCount    the number of "high" points (above the threshold)
	 *                             before a bit gets to be considered a 1
	 */
	public ThresholdingBitParser(int expectedBitLength, int threshold, int requiredHighCount) {
		this.expectedBitLength = expectedBitLength;
		this.threshold = threshold;
		this.requiredHighCount = requiredHighCount;
		n = 0;
		highCount = 0;
		samplesSinceLastHighSample = Integer.MAX_VALUE;
	}

	StringBuilder builder = new StringBuilder();
	public Bit putSample(short sample) {
		Bit result = Bit.NOTHING;

		if (sample > getThreshold()) {
			// This is a high point!
			++highCount;

			// Alignment: if it's been a long time since the last high sample,
			// then we are likely at the start of a bit.
			// (TODO: 4 is a really arbitrary number)
			if (samplesSinceLastHighSample > expectedBitLength * 4 / 5) {
				if (n >= expectedBitLength / 2) {
					// Hrm... it *kinda* looks like there should be a zero
					// here since enough low samples have elapsed...
					result = Bit.ZERO;
				}
				//else {
				//	Log.i(getClass().toString(), "Dropped a bit??? n=" + n + "/" + expectedBitLength);
				//}
				//Log.i(getClass().toString(), "realigned; delta=" + n);
				n = 0;
			}

			samplesSinceLastHighSample = 0;
		} else {
			++samplesSinceLastHighSample;
		}

		// If enough samples have elapsed, then emit a bit (depending on how
		// many high samples you found).
		if (n >= expectedBitLength) {
			result = highCount > requiredHighCount ? Bit.ONE : Bit.ZERO;
			if (result == Bit.ONE){
				Log.i("x", "HC=" + highCount);
			}
			n = 0;
			highCount = 0;
		}

		++n;

		if (result != Bit.NOTHING)
			builder.append(result == Bit.ZERO ? "0" : "1");
		if (builder.length() >= 64) {
			Log.i(getClass().toString(), builder.toString());
			builder = new StringBuilder();
		}

		return result;
	}

	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

}
