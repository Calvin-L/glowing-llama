package org.uw.glowingllama;

import org.uw.glowingllama.BitFetcher.Bit;

public class ThresholdingBitParser {

	private final int expectedBitLength;
	private final int threshold;
	private final int requiredHighCount;
	private int n;
	private int highCount;
	private int samplesSinceLastHighSample;

	public ThresholdingBitParser(int expectedBitLength, int threshold, int requiredHighCount) {
		this.expectedBitLength = expectedBitLength;
		this.threshold = threshold;
		this.requiredHighCount = requiredHighCount;
		n = 0;
		highCount = 0;
		samplesSinceLastHighSample = Integer.MAX_VALUE;
	}

	public Bit putSample(short sample) {
		Bit result = Bit.NOTHING;

		if (sample > threshold) {
			// a point for the ones
			++highCount;

			// alignment: if it's been a long time since the last high sample,
			// then we are likely at the start of a bit
			if (samplesSinceLastHighSample > expectedBitLength * 128) {
				n = 0;
			}

			samplesSinceLastHighSample = 0;
		} else {
			++samplesSinceLastHighSample;
		}

		if (n >= expectedBitLength) {
			result = highCount > requiredHighCount ? Bit.ONE : Bit.ZERO;
			n = 0;
			highCount = 0;
		}

		++n;
		return result;
	}

}
