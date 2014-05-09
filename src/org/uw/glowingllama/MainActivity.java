package org.uw.glowingllama;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Random;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends FragmentActivity {

	private Thread listeningThread = null;

	/** Number of bytes for the AudioRecord instance's internal buffer */
	private final int AUDIORECORD_BUFFER_SIZE_IN_BYTES = Math.max(
			// times 5 arbitrarily, seems to prevent buffer overruns
			AudioRecord.getMinBufferSize(Constants.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, Constants.ENCODING) * 5,
			// times two because there are 2 bytes in a short
			Constants.SAMPLE_RATE * 2);


	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			String receivedMessage = bundle.getString("receivedMessage");
			TextView msgHistTextView =
					(TextView)findViewById(R.id.messageHistory);
			msgHistTextView.append("Friend: " + receivedMessage + "\n");
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
			.add(R.id.container, new PlaceholderFragment())
			.commit();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		TextView msgHistTextView = (TextView)findViewById(R.id.messageHistory);
		msgHistTextView.setMovementMethod(new ScrollingMovementMethod());


		Button button = (Button) findViewById(R.id.sendInputTextButton);
		button.setOnTouchListener(new View.OnTouchListener() {

			Thread sendThread = null;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (sendThread != null) {
						sendThread.interrupt();
					}

					EditText editText = (EditText) findViewById(R.id.edit_message);
					final String message = editText.getText().toString();
					TextView messageHistory = (TextView) findViewById(R.id.messageHistory);
					messageHistory.append("Me: " + message + "\n");

					sendThread = new Thread(new Runnable() {

						@Override
						public void run() {
							EditText editText = (EditText) findViewById(R.id.edit_message);
							String message = Constants.SHAKESPEARE; //editText.getText().toString();
							//							pressButton(null);
							//							if (1 == 0) {
							final short[] buffer = modulate(send(message));
							final int bufferSize = AudioTrack.getMinBufferSize(Constants.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, Constants.ENCODING);

							final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
									Constants.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
									Constants.ENCODING, bufferSize, AudioTrack.MODE_STREAM);
							audioTrack.play();
							while (!Thread.interrupted()) {
								audioTrack.write(buffer, 0, buffer.length);
							}
							audioTrack.stop();
							//							}
						}

					});
					sendThread.start();
					break;
				case MotionEvent.ACTION_UP:
					if (sendThread != null) {
						sendThread.interrupt();
						sendThread = null;
					}
					break;
				}
				return false;
			}

		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}



	public void toggleListen(View view) {
		ToggleButton button = (ToggleButton) findViewById(R.id.regularListenToggle);
		boolean listening = button.isChecked();

		if (listening) {

			//            assert plot != null;
			listeningThread = new Thread(new Runnable() {
				public int bestOddNumber(int number) {
					if (number%2 == 0)
						return number + 1;
					else
						return number;
				}

				public int bestPowerOfTwo(int n) {
					int m = (int) (Math.log(n) / Math.log(2));
					return (1 << m);
				}

				public double computeGaussian(double stdDev, double mean, double x) {
					return 1.0 / (stdDev * Math.sqrt(2*Math.PI)) * Math.exp(-(x-mean)*(x-mean)/(2*stdDev*stdDev));
				}

				public short[] asShorts(double[] a, int scale) {
					short[] result = new short[a.length];
					for (int i = 0; i < a.length; ++i) {
						result[i] = (short)(a[i] * scale);
					}
					return result;
				}

				public int halfRoundedUp(int x) {
					return (x >> 1) + (x & 1);
				}

				private int sum(short[] a) {
					int sum = 0;
					for (short s : a) {
						sum += s;
					}
					return sum;
				}

				@Override
				public void run() {



					//					{
					//						int windowSize = 3 * SAMPLE_RATE / frequency;
					//						Log.i("Window size", "" + windowSize);
					//						double[] x = new double[windowSize];
					//						for (int i = 0; i < x.length; ++i) {
					//							x[i] = Math.sin(2 * Math.PI * i / ((double)SAMPLE_RATE/frequency));
					////							x[i] = Math.sin(2 * Math.PI * i / 8);
					//						}
					//
					//						plot.reset();
					//						plot.setSkip(1);
					//						plot.putMultipleSamples(asShorts(x, 300));
					//
					//						DoubleFFT_1D jfft = new DoubleFFT_1D(windowSize);
					//						jfft.realForward(x);
					//
					//						for (int i = 0; i < x.length; i += 2) {
					//							x[i] = 0;
					//						}
					//
					//						envelopePlot.reset();
					//						envelopePlot.setSkip(1);
					//						envelopePlot.putMultipleSamples(asShorts(x, 300));
					//
					//						int targetIndex = (int)((double)frequency / SAMPLE_RATE * windowSize + 1) * 2;
					//						Log.i("target index", "" + targetIndex);
					//						envelopePlot.setMarks(Arrays.asList(
					//								envelopePlot.getWidth() + (-windowSize*2 + 0) / envelopePlot.getSkip(),
					//								envelopePlot.getWidth() + (-windowSize*2 + targetIndex) / envelopePlot.getSkip(),
					//								envelopePlot.getWidth() + (-windowSize*2 + windowSize * 2 - 1) / envelopePlot.getSkip()));
					//
					//						Log.i("result", Arrays.toString(x));
					//					}

					final int fftWindowSize = Constants.PERIODS_PER_FFT_WINDOW * Constants.SAMPLE_RATE / Constants.frequency;
					final int samplesBetweenFftRecomputes = (int)Math.ceil(fftWindowSize * Constants.FFT_OVERLAP_RATIO);
					final int fftSamplesPerBit = (int)((double)Constants.SYMBOL_LENGTH / samplesBetweenFftRecomputes);

					// Compute the Gaussian kernel.
					int envelopeKernelSize = bestOddNumber(fftSamplesPerBit);
					//					int envelopeKernelSize = bestOddNumber((int)(10.0*SAMPLE_RATE/frequency));
					double[] gaussianKernel = new double[envelopeKernelSize];
					double stdDev = envelopeKernelSize / 2.0;   // MAGIC NUMBER
					for (int i = 0; i < envelopeKernelSize; ++i) {
						gaussianKernel[i] = computeGaussian(stdDev, envelopeKernelSize/2.0, i);
					}

					// Compute the delta kernel, for finding the derivative of a signal.
					//					int deltaKernelSize = bestOddNumber((int)(2.0*SAMPLE_RATE/frequency));
					int deltaKernelSize = bestOddNumber(fftSamplesPerBit);
					double[] deltaKernel = new double[deltaKernelSize];
					for (int i = 0; i < deltaKernelSize; ++i) {
						if (i < deltaKernelSize / 2)
							deltaKernel[i] = -1;
						else if (i == deltaKernelSize / 2)
							deltaKernel[i] = 0;
						else
							deltaKernel[i] = 1;
					}

					// Containers to track results and relevant indices.
					short[] newData = new short[AUDIORECORD_BUFFER_SIZE_IN_BYTES / 2];


					//					int exactPeriod = SAMPLE_RATE / frequency;
					//					if (SAMPLE_RATE % frequency != 0)
					//						Log.w("x", "Sample rate is not evenly divisible by frequency");
					//					RingBuffer buf = new RingBuffer(exactPeriod);

					// Initialize writing to file.
					File outputFile = new File(getExternalFilesDir(null), "audio.txt");
					Log.i("x", "Writing audio to " + outputFile.getAbsolutePath());
					Writer writer = null;

					File rawOutputFile = new File(getExternalFilesDir(null), "raw_audio.txt");
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

					//					final ThresholdingBitParser bitParser = new ThresholdingBitParser(SYMBOL_LENGTH, 4000, SYMBOL_LENGTH/4);
					final ThresholdingBitParser bitParser = new ThresholdingBitParser(Constants.SYMBOL_LENGTH, 4000, 15);
					final PacketParser packetParser = new PacketParser();

					final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, Constants.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, Constants.ENCODING, AUDIORECORD_BUFFER_SIZE_IN_BYTES);
					record.startRecording();

					final int bitsInPreamble = Constants.PREAMBLE.length*8;


					final int samplesPerWavelength = Constants.SAMPLE_RATE / Constants.frequency;
					PreambleFinder preambleFinder = new PreambleFinder(samplesPerWavelength * 2, bitsInPreamble, Constants.SYMBOL_LENGTH);
					//					MinMaxFilter maxWindow = new MinMaxFilter(); // MAGIC NUMBER
					//					RingBuffer maybePreambleSignal = new RingBuffer(SYMBOL_LENGTH*PREAMBLE.length*8);
					//					short[] maybePreambleSubset = new short[bitsInPreamble];


					boolean readingPacket = false;
					int skip = 0;
					long n = 0;

					while (!Thread.interrupted()) {
						// Read in new data.
						int num_read = record.read(newData, 0, newData.length);

						for (int i = 0; i < num_read; ++i, ++n) {
							short sample = newData[i];

							if (skip > 0) {
								--skip;
								continue;
							}

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
									Log.i("x", "Got packet: '" + s + "'");
									Message msg = handler.obtainMessage();
									Bundle bundle = new Bundle();
									bundle.putString("receivedMessage", s);
									msg.setData(bundle);
									handler.sendMessage(msg);

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


			});
			listeningThread.start();
		} else {
			if (listeningThread != null) {
				listeningThread.interrupt();
				listeningThread = null;
			}
		}
	}


	private static final char[] alphabet = new char[] {
		'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
		' ', '.', '?', ':', ';', '"', '\'', '*', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
	public String genStr(int nchars, int seed) {
		final char[] chars = new char[nchars];
		Random r = new Random(seed);
		for (int i = 0; i < nchars; ++i) {
			chars[i] = alphabet[r.nextInt(alphabet.length)];
		}
		return new String(chars);
	}

	public void pressButton(View view) {
		//		EditText editText = (EditText) findViewById(R.id.edit_message);
		//		String message = editText.getText().toString();
		//		final short[] buffer = modulate(send(message));

		// Play the tone.
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
						Constants.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
						Constants.ENCODING, AudioTrack.getMinBufferSize(Constants.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, Constants.ENCODING), AudioTrack.MODE_STREAM);
				audioTrack.play();

				final int npackets = 3;
				final int charsPerPacket = 30;
				for (int i = 0; i < npackets; ++i) {
					final String s = genStr(charsPerPacket, i);
					Log.i("x", "sending '" + s + "'");
					final short[] buffer = modulate(send(s));
					audioTrack.write(buffer, 0, buffer.length);
				}

				audioTrack.stop();
			}

		});
		t.start();
	}


	public byte[] send(String message) {

		int headerLength = Constants.PREAMBLE.length + 2; // Preamble + payload length

		byte[] allBytes = message.getBytes();
		int numPackets = (allBytes.length + Constants.MAX_PACKET_LENGTH_IN_BYTES - 1) / Constants.MAX_PACKET_LENGTH_IN_BYTES;

		int totalBytes = allBytes.length + (numPackets * headerLength);
		ByteBuffer packets = ByteBuffer.allocate(totalBytes);

		for (int i = 0; i < allBytes.length; i += Constants.MAX_PACKET_LENGTH_IN_BYTES) {
			int payloadLength = Math.min(Constants.MAX_PACKET_LENGTH_IN_BYTES, allBytes.length - i);
			packets.put(Constants.PREAMBLE);
			packets.putShort((short)payloadLength);
			packets.put(allBytes, i, payloadLength);

		}

		return packets.array();
		//		int packetLength = headerLength + payload.length;
		//
		//		ByteBuffer packet = ByteBuffer.allocate(packetLength);
		//
		//		packet.put(PREAMBLE);
		//		packet.putShort((short) payload.length);
		//		packet.put(payload);
		//
		//		return packet.array();
	}

	public short[] modulate(byte[] bits) {
		double amplitude = 1;

		int numSamples = bits.length * 8 * Constants.SYMBOL_LENGTH;

		short[] buffer = new short[numSamples];

		//		String testString = "";

		int idx = 0;
		for (byte b : bits) {
			for (int i = 7; i >= 0; --i) {
				int bit = (b >> i) & 1;

				for (int j = 0; j < Constants.SYMBOL_LENGTH; ++j) {
					double sample = amplitude * Math.sin(2 * Math.PI * idx / ((double)Constants.SAMPLE_RATE/Constants.frequency));
					sample *= bit;
					short shortSample = (short)(sample * Short.MAX_VALUE);
					buffer[idx++] = shortSample;
				}


				//				if (bit == 0)
				//					testString += "0";
				//				else
				//					testString += "1";

			}
		}

		//		Log.i("x", "Message is " + testString);

		assert idx == numSamples;

		return buffer;
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}

}
