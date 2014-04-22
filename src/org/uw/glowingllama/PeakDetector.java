package org.uw.glowingllama;

public class PeakDetector {
	boolean increasing;
	double peakThreshold;
	double previousSample;
	
	PeakDetector(double threshold)
	{
		increasing = false;
		peakThreshold = threshold;
		previousSample = 0;
	}
	
	boolean isPeak(double newSample)
	{
		boolean result = (increasing && newSample <= previousSample && newSample > peakThreshold);
		increasing = newSample > previousSample;
		previousSample = newSample;
		return result;
	}
}
