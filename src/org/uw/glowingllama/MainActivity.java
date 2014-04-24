package org.uw.glowingllama;

import java.nio.ByteBuffer;

import org.uw.glowingllama.BitFetcher.Bit;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.ToggleButton;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class MainActivity extends FragmentActivity {

	static final String EXTRA_MESSAGE = "org.uw.glowingllama.MESSAGE";
	private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static final int SAMPLE_RATE = 44100;

	final int SYMBOL_LENGTH = 80; //SAMPLE_RATE/600;
	final double PEAK_THRESHOLD = 100d;

	final int FREQUENCY_SPREAD = 1;
	final int PERIODS_PER_FFT_WINDOW = 2;

	final byte[] PREAMBLE = new byte[] { (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA };

	final double FFT_OVERLAP_RATIO = 0.2; // 0 = fft every sample, 1 = fft windows do not overlap

	private Thread listeningThread = null;
	private int frequency = 4410; //10000; //6342; //500; //3700; // Hz

	/** Number of bytes for the AudioRecord instance's internal buffer */
	private final int AUDIORECORD_BUFFER_SIZE_IN_BYTES = Math.max(
			// times 5 arbitrarily, seems to prevent buffer overruns
			AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING) * 5,
			// times two because there are 2 bytes in a short
			SAMPLE_RATE * 2);

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

		SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar1);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				updateFreq(progress);  // Temporarily commented out
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

		});


		Button button = (Button) findViewById(R.id.button);
		button.setOnTouchListener(new View.OnTouchListener() {

			Thread sendThread = null;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (sendThread != null) {
						sendThread.interrupt();
					}
					sendThread = new Thread(new Runnable() {

						@Override
						public void run() {
							EditText editText = (EditText) findViewById(R.id.edit_message);
							String message = editText.getText().toString();

							final short[] buffer = modulate(send(message));
							final int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING);

							final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
									SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
									ENCODING, bufferSize, AudioTrack.MODE_STREAM);
							audioTrack.play();
							while (!Thread.interrupted()) {
								audioTrack.write(buffer, 0, buffer.length);
							}
							audioTrack.stop();
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
		ToggleButton button = (ToggleButton) findViewById(R.id.toggleButton1);
		boolean listening = button.isChecked();

		if (listening) {

			final SimplePlot plot = (SimplePlot) findViewById(R.id.simplePlot);
			final SimplePlot envelopePlot = (SimplePlot) findViewById(R.id.envelopePlot);
			final SimplePlot deltaPlot = (SimplePlot) findViewById(R.id.deltaPlot);
			final SpectrogramPlot spectoPlot = (SpectrogramPlot) findViewById(R.id.spectoPlot);

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


					short lastBitSeen = 0;
					final int fftWindowSize = PERIODS_PER_FFT_WINDOW * SAMPLE_RATE / frequency;
					final int samplesBetweenFFTRecomputes = (int)Math.ceil(fftWindowSize * FFT_OVERLAP_RATIO);
					final int fftSamplesPerBit = (int)((double)SYMBOL_LENGTH / samplesBetweenFFTRecomputes);
					final BitFetcher bitFetcher = new BitFetcher(fftSamplesPerBit, PEAK_THRESHOLD);

					// Compute the Gaussian kernel.
					int envelopeKernelSize = bestOddNumber(fftSamplesPerBit);
					//					int envelopeKernelSize = bestOddNumber((int)(10.0*SAMPLE_RATE/frequency));
					double[] gaussianKernel = new double[envelopeKernelSize];
					double stdDev = envelopeKernelSize / 2.0;   // MAGIC NUMBER
					for (int i = 0; i < envelopeKernelSize; ++i) {
						gaussianKernel[i] = computeGaussian(stdDev, envelopeKernelSize/2.0, i);
					}

					int deltaKernelSize = bestOddNumber(fftSamplesPerBit);
					double[] deltaKernel = new double[deltaKernelSize];
					for (int i = 0; i < deltaKernelSize; ++i) {
						if (i < deltaKernelSize / 2) {
							deltaKernel[i] = -1;
						} else if (i == deltaKernelSize / 2) {
							deltaKernel[i] = 0;
						} else {
							deltaKernel[i] = 1;
						}
					}

					//					int fftWindowSize = 30 * SAMPLE_RATE / frequency;
					// FFT Window should be a multiple of the period of the signal
					//					Log.i("fftWindowSize", Integer.toString(fftWindowSize));
					RingBuffer fftWindow = new RingBuffer(fftWindowSize);
					final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING, AUDIORECORD_BUFFER_SIZE_IN_BYTES);
					record.startRecording();
					short[] newData = new short[AUDIORECORD_BUFFER_SIZE_IN_BYTES / 2];
					int firstRingbufferSize = SYMBOL_LENGTH * 10;   // MAGIC NUMBER
					RingBuffer bandpassData = new RingBuffer(firstRingbufferSize);
					RingBuffer envelopedData = new RingBuffer(bandpassData.size());
					RingBuffer deltaData = new RingBuffer(bandpassData.size());
					int envelopeStartIndex = bandpassData.size() - envelopeKernelSize;
					int deltaStartIndex = envelopedData.size() - deltaKernelSize;

					int timeSinceLastFFT = 0;
					//					double x[] = new double[fftWindowSize*2];
					double x[] = new double[fftWindowSize];
					short[] fftOutput = new short[halfRoundedUp(fftWindowSize)];
					DoubleFFT_1D jfft = new DoubleFFT_1D(fftWindowSize);

					//					int exactPeriod = SAMPLE_RATE / frequency;
					//					if (SAMPLE_RATE % frequency != 0)
					//						Log.w("x", "Sample rate is not evenly divisible by frequency");
					//					RingBuffer buf = new RingBuffer(exactPeriod);

					//					File outputFile = new File(getExternalFilesDir(null), "audio.txt");
					//					assert outputFile.canWrite();
					//					Log.i("x", "Writing audio to " + outputFile.getAbsolutePath());
					//					Writer writer = null;
					//					try {
					//						writer = new BufferedWriter(new FileWriter(outputFile));
					//					} catch (IOException e) {
					//						Log.w("x", "file open failed");
					//						e.printStackTrace();
					//						return;
					//					}

					final ThresholdingBitParser bitParser = new ThresholdingBitParser(SYMBOL_LENGTH, 4000, SYMBOL_LENGTH/2);
					final PacketParser packetParser = new PacketParser(PREAMBLE);

					while (!Thread.interrupted()) {
						// Read in new data.
						int num_read = record.read(newData, 0, newData.length);

						for (int i = 0; i < num_read; ++i) {
							short sample = newData[i];

							Bit bit = bitParser.putSample((short)Math.abs(sample));
							byte[] result = null;
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
							if (result != null) {
								String s = new String(result);
								Log.i("x", "Got packet: '" + s + "'");
							}

							//							try {
							//								writer.write(Short.toString(sample));
							//								writer.write('\n');
							//							} catch (IOException e) {
							//								Log.w("x", "write failed");
							//								e.printStackTrace();
							//								break;
							//							}

							// Visualize incoming data
							//							plot.setSkip(1);
							//							plot.putMultipleSamples(newData);

							if (0 == 1) {

								fftWindow.addElement(sample);
								++timeSinceLastFFT;
								if (timeSinceLastFFT >= samplesBetweenFFTRecomputes) {

									timeSinceLastFFT = 0;

									// Fill our window with the latest audio sample data.
									for (int j = 0; j < fftWindowSize; ++j) {
										x[j] = (double)fftWindow.get(j) / Short.MAX_VALUE;
									}

									// Calculate the FFT.
									//								Log.i("in", Arrays.toString(x));
									jfft.realForward(x);
									//								Log.i("out", Arrays.toString(x));

									//								Log.i("FFT raw output: ", Arrays.toString(x));


									//								Log.i("x", "converting fft output");

									// Get the real-valued output from the FFT
									for (int j = 0; j < fftWindowSize; j += 2) {
										fftOutput[j/2] = (short)Math.abs(x[j] * 1000);
									}

									//								// Find the "dominant" bucket -- that is, the bucket in the FFT results with the
									//								// highest value.
									//								int dominantBucket = 0;
									//								int dominantVal = fftOutput[0];
									//								for (int j = 1; j < fftOutput.length; ++j) {
									//									if (fftOutput[j] > dominantVal) {
									//										dominantVal = fftOutput[j];
									//										dominantBucket = j;
									//									}
									//								}
									//
									//								// Normalize the dominant value
									//								double power = (double)dominantVal / sum(fftOutput);
									//
									// TODO: +2 makes everything work, but why?
									int targetIndex = (int)((double)frequency / SAMPLE_RATE * fftOutput.length) + 2;
									//
									//								// Make sure the dominant frequency is close to what we expect
									//								int dist = Math.abs(dominantBucket - targetIndex);
									//								if (dist > 0) {
									//									power = 0;
									//								}

									final double rawPower = fftOutput[targetIndex] +
											0.5 * fftOutput[targetIndex-1] +
											0.5 * fftOutput[targetIndex+1];

									double amplitude = 0;
									for (int j = 0; j < fftWindowSize; ++j) {
										amplitude += (double)Math.abs(fftWindow.get(j)) / Short.MAX_VALUE;
									}
									amplitude /= fftWindowSize;

									final double power = rawPower / Math.max(sum(fftOutput), 1) * amplitude;

									// freqStrength holds a noisy (pretty accurate) measure of how much the currently
									// playing sound matches the frequency we are listening for. Zero means "no match"
									// and positive values indicate some level of confidence.
									short freqStrength = (short)(power * Short.MAX_VALUE);
									bandpassData.addElement(freqStrength);
									plot.setSkip(1);
									plot.putSample(freqStrength);
									//								plot.putSample((short) targetIndex);
									//								plot.putSample((short) dominantBucket);

									// Calculate the enveloped data point.
									double newEnvelopedPoint = 0;
									for (int j = 0; j < envelopeKernelSize; ++j) {
										newEnvelopedPoint += gaussianKernel[j] * bandpassData.get(envelopeStartIndex+j);
									}
									envelopedData.addElement((short)newEnvelopedPoint);
									//								envelopePlot.setSkip(1);
									//								envelopePlot.putSample((short)newEnvelopedPoint);

									// Calculate the delta point.
									double newDeltaPoint = 0;
									for (int j = 0; j < deltaKernelSize; ++j) {
										newDeltaPoint += deltaKernel[j] * envelopedData.get(deltaStartIndex+j);
									}
									newDeltaPoint = Math.min(Math.max(Short.MIN_VALUE, newDeltaPoint), Short.MAX_VALUE);
									deltaData.addElement((short)newDeltaPoint);
									deltaPlot.setSkip(1);
									deltaPlot.putSample((short)newDeltaPoint);

									// Send it to the bits guy
									BitFetcher.Bit latestBit = bitFetcher.interpretNewSample(newDeltaPoint);
									byte[] packet = null;
									switch(latestBit) {
									case ONE:
										lastBitSeen = (short)1;
										packet = packetParser.reportBit(1);
										break;
									case ZERO:
										lastBitSeen = (short)-1;
										packet = packetParser.reportBit(0);
										break;
									case NOTHING:
										break; // keep lastBitSeen the same
									}
									envelopePlot.setSkip(0);
									envelopePlot.putSample(lastBitSeen);

									if (packet != null) {
										Log.i("x", "FOUND A PACKET!");
									}

									/* //old code

//								Log.i("x", "max fft val=" + max);
								envelopePlot.setSkip(1);
								envelopePlot.reset();
								envelopePlot.putMultipleSamples(fftOutput);

								spectoPlot.addSample(fftOutput);

								int targetIndex = (int)((double)frequency / SAMPLE_RATE * fftOutput.length) + 1;
								int minIndex = Math.max(0, targetIndex - FREQUENCY_SPREAD);
								int maxIndex = Math.min(targetIndex + FREQUENCY_SPREAD + 1, fftOutput.length);

								envelopePlot.setMarks(Arrays.asList(
										envelopePlot.getWidth() + (-fftOutput.length + minIndex) / envelopePlot.getSkip(),
										envelopePlot.getWidth() + (-fftOutput.length + maxIndex) / envelopePlot.getSkip()));

								short bestVal = 0;
								for (int j = minIndex; j < maxIndex; ++j) {
									bestVal = (short)Math.max(bestVal, Math.min(fftOutput[j], 100));
								}
								deltaPlot.setSkip(0);
								deltaPlot.putSample(bestVal);
//
//								plot.setSkip(0);
//								plot.putSample(bestVal);
////								plot.reset();
////								plot.putMultipleSamples(fftWindow);
////
//
//
								bandpassData.addElement(bestVal);
//
								// Calculate the enveloped data point.
								double newEnvelopedPoint = 0;
								for (int j = 0; j < envelopeKernelSize; ++j) {
									newEnvelopedPoint += gaussianKernel[j] * bandpassData.get(envelopeStartIndex+j);
								}
								envelopedData.addElement((short)newEnvelopedPoint);
								plot.setSkip(0);
								plot.putSample((short)newEnvelopedPoint);



//								// Calculate the delta point.
//								double newDeltaPoint = 0;
//								for (int j = 0; j < deltaKernelSize; ++j) {
//									newDeltaPoint += deltaKernel[j] * envelopedData.get(deltaStartIndex+j);
//								}
//								deltaData.addElement((short)newDeltaPoint);
////								deltaPlot.setSkip(0);
////								deltaPlot.putSample((short)newDeltaPoint);
//
//								BitFetcher.Bit latestBit = bitFetcher.interpretNewSample(newDeltaPoint);
//								switch(latestBit) {
//								case ONE:
//									lastBitSeen = (short)1;
//									break;
//								case ZERO:
//									lastBitSeen = (short)-1;
//									break;
//								case NOTHING:
//									break; // keep lastBitSeen the same
//								}
//								deltaPlot.setSkip(0);
//								deltaPlot.putSample(lastBitSeen);
								}
									 */
								}
							}

						}

						//						try {
						//							Thread.sleep(1);
						//						} catch (InterruptedException e) {
						//							break;
						//						}
					}

					Log.i("x", "Stopping listening...");
					record.stop();
					//					try {
					//						writer.flush();
					//						writer.close();
					//					} catch (IOException e) {
					//						Log.w("x", "buf close failed");
					//						e.printStackTrace();
					//					}

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

	public void updateFreq(int progress) {
		Log.i("x", "seekbar: " + progress);
		frequency = Math.max(progress, 1);
	}

	public void pressButton(View view) {
		EditText editText = (EditText) findViewById(R.id.edit_message);
		String message = editText.getText().toString();

		final short[] buffer = modulate(send(message));

		// Play the tone.
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
						SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
						ENCODING, AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING), AudioTrack.MODE_STREAM);
				audioTrack.play();
				audioTrack.write(buffer, 0, buffer.length);
				audioTrack.stop();
			}

		});
		t.start();
	}


	public byte[] send(String message) {

		int headerLength = PREAMBLE.length + 1 + 1 + 4; // Preamble + src + dst + payload length
		byte srcID = 0;
		byte dstID = 1;

		byte[] payload = message.getBytes();
		int packetLength = headerLength + payload.length;

		ByteBuffer packet = ByteBuffer.allocate(packetLength);

		packet.put(PREAMBLE);
		packet.put(srcID);
		packet.put(dstID);
		packet.putInt(payload.length);
		packet.put(payload);

		//    	for (int i = 0; i < packetLength; ++i) {
		//    		packet.put((byte)0xFF);
		//    	}

		return packet.array();
	}

	public short[] modulate(byte[] bits) {
		double amplitude = 1;

		int numSamples = bits.length * 8 * SYMBOL_LENGTH;

		short[] buffer = new short[numSamples];

		String testString = "";

		int idx = 0;
		for (byte b : bits) {
			for (int i = 7; i >= 0; --i) {
				int bit = (b >> i) & 1;

				for (int j = 0; j < SYMBOL_LENGTH; ++j) {
					double sample = amplitude * Math.sin(2 * Math.PI * idx / ((double)SAMPLE_RATE/frequency));
					sample *= bit;
					short shortSample = (short)(sample * Short.MAX_VALUE);
					buffer[idx++] = shortSample;
				}


				if (bit == 0)
					testString += "0";
				else
					testString += "1";

			}
		}

		Log.i("x", "Message is " + testString);

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
