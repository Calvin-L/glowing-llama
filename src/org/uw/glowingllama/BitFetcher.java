package org.uw.glowingllama;

public class BitFetcher {
	final int symbolLength;
	boolean lastSawAOne;
	int timeSinceLastPeak;
	final PeakDetector peakDetector;
	final PeakDetector troughDetector;
	
	enum Bit 
	{
		NOTHING,
		ONE,
		ZERO
	}
	
	BitFetcher(int symbolLen, double peakThreshold)
	{
		symbolLength = symbolLen;
		lastSawAOne = false;
		timeSinceLastPeak = 0;
		peakDetector = new PeakDetector(peakThreshold);
		troughDetector = new PeakDetector(peakThreshold);
	}
	
	Bit interpretNewSample(double newSample) 
	{
		boolean isPeak = peakDetector.isPeak(newSample);
		boolean isTrough = troughDetector.isPeak(-newSample);
		
		if (isPeak)
			return Bit.ONE;
		else if (isTrough)
			return Bit.ZERO;
		else
			return Bit.NOTHING;
		
//		if (isPeak || isTrough) {
//			timeSinceLastPeak = 0;
//			lastSawAOne = isPeak;
//		}
//		
//		Bit result = Bit.NOTHING;
//		if ((timeSinceLastPeak - (samplesPerBit/2)) % samplesPerBit == 0) {
//			result = lastSawAOne ? Bit.ONE : Bit.ZERO;
//		}
//
//		timeSinceLastPeak++;
//		
//		return result;
		
	}
}
