package org.uw.glowingllama;


/**
 * Crappy implementation of a streaming min/max filter.
 */
public class MinMaxFilter {

	final RingBuffer window;

	short min, max;
	int minIndex, maxIndex;

	public MinMaxFilter(int windowSize) {
		window = new RingBuffer(windowSize);
		min = max = window.get(0);
		minIndex = maxIndex = 0;
	}

	public void put(short sample) {
		window.addElement(sample);
		--minIndex;
		--maxIndex;

		if (minIndex < 0) {
			minIndex = window.indexOfMin();
			min = window.get(minIndex);
		} else if (sample < min) {
			minIndex = window.size() - 1;
			min = sample;
		}

		if (maxIndex < 0) {
			maxIndex = window.indexOfMax();
			max = window.get(maxIndex);
		} else if (sample > max) {
			maxIndex = window.size() - 1;
			max = sample;
		}
	}

	/**
	 * @return the smallest element over the last windowSize samples
	 */
	public short min() {
		return min;
	}

	/**
	 * @return the largest element over the last windowSize samples
	 */
	public short max() {
		return max;
	}

}
