package org.uw.glowingllama;

import android.media.AudioFormat;
import android.media.AudioRecord;

public class Constants {

	static final String EXTRA_MESSAGE = "org.uw.glowingllama.MESSAGE";
	static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	static final int SAMPLE_RATE = 44100;
	final static int SYMBOL_LENGTH = 80; //SAMPLE_RATE/600;
	final double PEAK_THRESHOLD = 100d;
	final int FREQUENCY_SPREAD = 1;
	final static int PERIODS_PER_FFT_WINDOW = 2;
	final static byte[] PREAMBLE = new byte[] { (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA };
	final static double FFT_OVERLAP_RATIO = 0.2; // 0 = fft every sample, 1 = fft windows do not overlap
	final static int frequency = 4410; //10000; //6342; //500; //3700; // Hz
	final static int MAX_PACKET_LENGTH_IN_BYTES = 256;

	/** Number of bytes for the AudioRecord instance's internal buffer */
	final static int AUDIORECORD_BUFFER_SIZE_IN_BYTES = Math.max(
			// times 5 arbitrarily, seems to prevent buffer overruns
			AudioRecord.getMinBufferSize(Constants.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, Constants.ENCODING) * 5,
			// times two because there are 2 bytes in a short
			Constants.SAMPLE_RATE * 2);

	final static String SHAKESPEARE = "Shall I compare thee to a summer's day?\n" +
			"Thou art more lovely and more temperate:\n" +
			"Rough winds do shake the darling buds of May,\n" +
			"And summer's lease hath all too short a date:\n" +
			"Sometimes too hot the eye of heaven shines,\n" +
			"And too often is his gold complexion dimm'd:\n" +
			"And every fair from fair sometimes declines,\n" +
			"By chance or natures changing course untrimm'd;\n" +
			"By thy eternal summer shall not fade,\n" +
			"Nor lose possession of that fair thou owest;\n" +
			"Nor shall Death brag thou wander'st in his shade,\n" +
			"When in eternal lines to time thou growest:\n" +
			"So long as men can breathe or eyes can see,\n" +
			"So long lives this and this gives life to thee.\n" +
			"\n" +
			"My mistress' eyes are nothing like the sun;\n" +
			"Coral is far more red, than her lips red:\n" +
			"If snow be white, why then her breasts are dun;\n" +
			"If hairs be wires, black wires grow on her head.\n" +
			"I have seen roses damasked, red and white,\n" +
			"But no such roses see I in her cheeks;\n" +
			"And in some perfumes is there more delight\n" +
			"Than in the breath that from my mistress reeks.\n" +
			"I love to hear her speak, yet well I know\n" +
			"That music hath a far more pleasing sound:\n" +
			"I grant I never saw a goddess go,\n" +
			"My mistress, when she walks, treads on the ground:\n" +
			"And yet by heaven, I think my love as rare,\n" +
			"As any she belied with false compare.";

}
