package org.uw.glowingllama;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class DeModulator {

	public void listen(File dataDir, MainActivity.OnPacket callback) {

		// Containers to track results and relevant indices.
		short[] newData = new short[Constants.AUDIORECORD_BUFFER_SIZE_IN_BYTES / 2];

		// Initialize writing to file.
		File outputFile = new File(dataDir, "audio.txt");
		Log.i("x", "Writing audio to " + outputFile.getAbsolutePath());
		Writer writer = null;

		File rawOutputFile = new File(dataDir, "raw_audio.txt");
		Log.i("x", "Writing audio to " + rawOutputFile.getAbsolutePath());
		Writer rawWriter = null;

		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			rawWriter = new BufferedWriter(new FileWriter(rawOutputFile));
		} catch (IOException e) {
			Log.w("x", "file open failed");
			e.printStackTrace();
			return;
		}

		// TODO: some magic numbers
		final ThresholdingBitParser bitParser = new ThresholdingBitParser(Constants.SYMBOL_LENGTH, 4000, 15);
		final PacketParser packetParser = new PacketParser();

		final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, Constants.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, Constants.ENCODING, Constants.AUDIORECORD_BUFFER_SIZE_IN_BYTES);
		record.startRecording();

		final int bitsInPreamble = Constants.PREAMBLE.length*8;

		final int samplesPerWavelength = Constants.SAMPLE_RATE / Constants.frequency;
		PreambleFinder preambleFinder = new PreambleFinder(samplesPerWavelength * 2, bitsInPreamble, Constants.SYMBOL_LENGTH);

		boolean readingPacket = false;
		long n = 0;

		while (!Thread.interrupted()) {
			// Read in new data.
			int num_read = record.read(newData, 0, newData.length);

			for (int i = 0; i < num_read; ++i, ++n) {
				short sample = newData[i];

				if (readingPacket) {

					BitFetcher.Bit bit = bitParser.putSample((short) Math.abs(sample));
					byte[] result = null;

					// if we found a bit, send it to the packet parser
					switch (bit) {
					case ZERO:
						result = packetParser.reportBit(0);
						break;
					case ONE:
						result = packetParser.reportBit(1);
						break;
					case NOTHING:
						break; // eh...
					}

					// if we found a packet, hooray!
					if (result != null) {
						String s = new String(result);
						Log.i("x", "Got packet: " + s);
						callback.packetReceived(s);
						readingPacket = false;
					}

				} else {

					PreambleFinder.PreambleResult preamble = preambleFinder.put(sample);
					if (preamble != null) {
						Log.i("Some Tag", "New Preamble Found! Threshold: " + preamble.threshold);
						Log.i("x", "Preamble start: " + n);
						preambleFinder.clear();
						bitParser.setThreshold(preamble.threshold);
						readingPacket = true;
					}

				}



			}

		}

		Log.i("x", "Stopping listening...");
		record.stop();
		try {
			writer.flush();
			writer.close();
			rawWriter.flush();
			rawWriter.close();
		} catch (IOException e) {
			Log.w("x", "buf close failed");
			e.printStackTrace();
		}

	}

}
