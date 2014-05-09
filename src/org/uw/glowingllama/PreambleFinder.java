package org.uw.glowingllama;

import android.util.Log;

public class PreambleFinder {

	final int SAMPLES_PER_BIT;
	final int PREAMBLE_LENGTH_IN_BITS;
	final MinMaxFilter filter;
	final RingBuffer buf;

	public static class PreambleResult {
		public final int threshold;
		public PreambleResult(int threshold) {
			this.threshold = threshold;
		}
	}

	public PreambleFinder(int minMaxFilterSize, int preambleLengthInBits, int samplesPerBit) {
		PREAMBLE_LENGTH_IN_BITS = preambleLengthInBits;
		SAMPLES_PER_BIT = samplesPerBit;
		filter = new MinMaxFilter(minMaxFilterSize);
		buf = new RingBuffer(preambleLengthInBits * samplesPerBit);
	}

	public PreambleResult put(short sample) {
		filter.put((short) Math.abs(sample));
		buf.addElement(filter.max());

		// METHOD 1: average
		//		int avg = 0;
		//		for (int j = 0; j < PREAMBLE_LENGTH_IN_BITS; ++j) {
		//			avg += buf.get(j * SAMPLES_PER_BIT);
		//		}
		//		avg /= PREAMBLE_LENGTH_IN_BITS;
		//		final int threshold = avg;

		// METHOD 2: midpoint
		int min = buf.get(0);
		int max = buf.get(0);
		for (int j = 1; j < PREAMBLE_LENGTH_IN_BITS; ++j) {
			final short val = buf.get(j * SAMPLES_PER_BIT);
			min = Math.min(val, min);
			max = Math.max(val, max);
		}
		//		final int threshold = (max + min) / 2;
		final int threshold = max / 2;

		//		boolean found = (min < max/2); // heuristic: impose some minimum spread
		boolean found = true;
		for (int j = 0; found && j < PREAMBLE_LENGTH_IN_BITS; ++j) {
			final short val = buf.get(j * SAMPLES_PER_BIT);
			found = j % 2 == 0 ? val > threshold : val < threshold;
		}

		if (found) {
			Log.i("x", "Preamble range: " + min + "-" + max);
		}

		return found ? new PreambleResult(threshold) : null;
	}

	public void clear() {
		buf.clear();
	}

}
