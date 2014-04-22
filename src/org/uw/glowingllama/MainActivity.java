package org.uw.glowingllama;

import java.nio.ByteBuffer;
import java.util.Arrays;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
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

public class MainActivity extends ActionBarActivity {

    static final String EXTRA_MESSAGE = "org.uw.glowingllama.MESSAGE";
	private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static final int SAMPLE_RATE = 44100;
	
	int SYMBOL_LENGTH = SAMPLE_RATE/500;

	private Thread listeningThread = null;
	private int frequency = 10000; //500; //3700; // Hz

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
//				updateFreq(progress);  // Temporarily commented out
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

					    	final short[] buffer = Modulate(Send(message));

					    	final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
					                ENCODING, AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING), AudioTrack.MODE_STREAM);
					        audioTrack.play();
							while (!Thread.interrupted()) {
						        audioTrack.write(buffer, 0, buffer.length);
							}
					    	
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
            
            assert plot != null;
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
    			
    			

    			
				@Override
				public void run() {
					
					BitFetcher bitFetcher = new BitFetcher(SYMBOL_LENGTH, 50);
					short lastBitSeen = 0;
					
					// Compute the Gaussian kernel.
					int envelopeKernelSize = bestOddNumber((int)(2*SAMPLE_RATE/frequency));
//					int envelopeKernelSize = bestOddNumber((int)(10.0*SAMPLE_RATE/frequency));
					double[] gaussianKernel = new double[envelopeKernelSize];
					double stdDev = envelopeKernelSize / 2.0;   // MAGIC NUMBER
					for (int i = 0; i < envelopeKernelSize; ++i) {
						gaussianKernel[i] = computeGaussian(stdDev, envelopeKernelSize/2.0, i);
					}

					int deltaKernelSize = bestOddNumber((int)(2.0*SAMPLE_RATE/frequency));
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
					
					int fftWindowSize = bestPowerOfTwo((SAMPLE_RATE / frequency) * 10);
					double fftOverlapRatio = 0.1;
					RingBuffer fftWindow = new RingBuffer(fftWindowSize);
    			
					int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING);
		    		final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING, bufferSize);
		    		record.startRecording();
		    		short[] newData = new short[bufferSize / 2];
		    		int firstRingbufferSize = SYMBOL_LENGTH * 10;   // MAGIC NUMBER
//		    		RingBuffer absData = new RingBuffer(firstRingbufferSize); 
		    		RingBuffer bandpassData = new RingBuffer(firstRingbufferSize);
		    		RingBuffer envelopedData = new RingBuffer(bandpassData.size());
		    		RingBuffer deltaData = new RingBuffer(bandpassData.size());
		    		int envelopeStartIndex = bandpassData.size() - envelopeKernelSize;
		    		int deltaStartIndex = envelopedData.size() - deltaKernelSize;
		    				    		
		    		
		    		int timeSinceLastFFT = 0;
					double x[] = new double[fftWindowSize];
					
					boolean increasing = false;
					double prev = 0;
		    		
					while (true) {
						// Read in new data.
						int num_read = record.read(newData, 0, newData.length);
						
						for (int i = 0; i < num_read; ++i) {
							short sample = newData[i];
							
							fftWindow.addElement(sample);
							++timeSinceLastFFT;
//							if (true) {
							if (timeSinceLastFFT >= fftWindowSize*fftOverlapRatio) {
							
								timeSinceLastFFT = 0;
								for (int j = 0; j < fftWindow.size(); ++j) {
									x[j] = (double)fftWindow.get(j) / Short.MAX_VALUE;
								}
								
								DoubleFFT_1D jfft = new DoubleFFT_1D(fftWindowSize);
								jfft.realForward(x);
								
								
								short[] fftOutput = new short[x.length];
//								Log.i("x", "converting fft output");
							    double max = 0;
								for (int j = 0; j < x.length; ++j) {
									max = Math.max(max, x[j]);
									fftOutput[j] = (short)(x[j] * Short.MAX_VALUE / fftWindowSize);
//									Log.i("x", "got fft value: " + x[j]);
								}
//								Log.i("x", "max fft val=" + max);
//								envelopePlot.setSkip(25);
//								envelopePlot.reset();
//								envelopePlot.putMultipleSamples(fftOutput);
								
								final int spread = 5;
								int targetIndex = (int)((double)frequency / SAMPLE_RATE * fftWindowSize);
								int minIndex = Math.max(0, targetIndex - spread);
								int maxIndex = Math.min(targetIndex + spread, fftWindowSize);
								
//								envelopePlot.setMarks(Arrays.asList(
//										envelopePlot.getWidth() + (-fftWindowSize + minIndex) / envelopePlot.getSkip(),
//										envelopePlot.getWidth() + (-fftWindowSize + maxIndex) / envelopePlot.getSkip()));
								
								short bestVal = 0;
								for (int j = minIndex; j < maxIndex; ++j) {
									bestVal = (short)Math.max(bestVal, Math.abs(fftOutput[j]));
								}
//								deltaPlot.setSkip(0);
//								deltaPlot.putSample(bestVal);
								
								//plot.setSkip(0);
								//plot.putSample(bestVal);
								
								
								
								bandpassData.addElement(bestVal);
								
								// Calculate the enveloped data point.
								double newEnvelopedPoint = 0;
								for (int j = 0; j < envelopeKernelSize; ++j) {
									newEnvelopedPoint += gaussianKernel[j] * bandpassData.get(envelopeStartIndex+j);
								}
								envelopedData.addElement((short)newEnvelopedPoint);
								
								envelopePlot.setSkip(0);
								envelopePlot.putSample((short)newEnvelopedPoint);
								
								// Calculate the delta point.
								double newDeltaPoint = 0;
								for (int j = 0; j < deltaKernelSize; ++j) {
									newDeltaPoint += deltaKernel[j] * envelopedData.get(deltaStartIndex+j);
								}
								deltaData.addElement((short)newDeltaPoint);
								deltaPlot.setSkip(0);
								deltaPlot.putSample((short)newDeltaPoint);
								
								BitFetcher.Bit latestBit = bitFetcher.interpretNewSample(newDeltaPoint);
								plot.setSkip(0);
								switch(latestBit) {
								case ONE:
									lastBitSeen = (short)1;
									break;
								case ZERO:
									lastBitSeen = (short)-1;
									break;
								}
								plot.putSample(lastBitSeen);
								
//								
//								{
//									final int PEAK_THRESHOLD = 50;
//									// Peak identification
//									if (increasing && newDeltaPoint <= prev && newDeltaPoint > PEAK_THRESHOLD) {
//										// We found a peak!
//										plot.setSkip(0);
//										plot.putSample((short)3000);
//									} else {
//										// No peak here!
//										plot.putSample((short)0);
//									}
//									increasing = newDeltaPoint > prev;
//									prev = newDeltaPoint;
//								}
								
							
							}
							
//							absData.addElement((short) Math.abs(sample));
//							
//							// Calculate the enveloped data point.
//							double newEnvelopedPoint = 0;
//							for (int j = 0; j < envelopeKernelSize; ++j) {
//								newEnvelopedPoint += gaussianKernel[j] * absData.get(convolveStartIndex+j);
//							}
//							envelopedData.addElement((short)newEnvelopedPoint);
//							
//							// Calculate the delta point.
//							double newDeltaPoint = 0;
//							for (int j = 0; j < deltaKernelSize; ++j) {
//								newDeltaPoint += deltaKernel[j] * envelopedData.get(deltaStartIndex+j);
//							}
//							deltaData.addElement((short)newDeltaPoint);

							
//							plot.putSample(sample);
//							envelopePlot.putSample((short)newEnvelopedPoint);
//							deltaPlot.putSample((short)newDeltaPoint);
										
						}
						
						try {
							Thread.sleep(5);
						} catch (InterruptedException e) {
							Log.i("x", "Stopping listening...");
							record.stop();
							break;
						}
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
    
    public void updateFreq(int progress) {
    	Log.i("x", "seekbar: " + progress);
    	frequency = Math.max(progress, 1);
    }
    
    @SuppressLint("NewApi")
	public void pressButton(View view) {
    	EditText editText = (EditText) findViewById(R.id.edit_message);
    	String message = editText.getText().toString();

    	final short[] buffer = Modulate(Send(message));

//        final SimplePlot envelopePlot = (SimplePlot) findViewById(R.id.envelopePlot);
//        envelopePlot.setSkip(500);
//    	envelopePlot.putMultipleSamples(Arrays.copyOfRange(buffer, buffer.length / 2 - 100000, buffer.length / 2));
//    	Log.i("x", "I am envelope plot: " + envelopePlot);
    	
    	// Play the tone.
    	Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
		    	final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
		                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
		                ENCODING, AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING), AudioTrack.MODE_STREAM);
		        audioTrack.play();
		        audioTrack.write(buffer, 0, buffer.length);
				
			}
    		
    	});
    	t.start();
    }
    
     
    public byte[] Send(String message) {

    	int headerLength = 4 + 1 + 1 + 4; // Preamble + src + dst + payload length
    	int preamble = 0xAAAAAAAA;
    	byte srcID = 0;
    	byte dstID = 1;
    	
    	byte[] payload = message.getBytes();
    	int packetLength = headerLength + payload.length;
    	
//    	packetLength = 4; // temporary hack
    	
    	ByteBuffer packet = ByteBuffer.allocate(packetLength);
    	
//    	packet.putInt(preamble);   
//    	packet.put(srcID);  
//    	packet.put(dstID);  
//    	packet.putInt(payload.length);
    	packet.put(payload);
    	
    	return packet.array();
    }

    public short[] Modulate(byte[] bits) {
    	double amplitude = 1;
    	
    	
    	int numSamples = bits.length * SYMBOL_LENGTH * 8;
    	
    	short[] buffer = new short[numSamples];
    	  	
    	String testString = "";
    	
    	int idx = 0;
    	for (byte b : bits) {
    		for (int i = 7; i >= 0; --i) {
    			int bit = (b >> i) & 1;
    			
    			for (int j = 0; j < SYMBOL_LENGTH; ++j) {
//    				double freq = (bit == 0) ? frequency : frequency / 2;
//    				double sample = amplitude * Math.sin(2 * Math.PI * j / SAMPLE_RATE * frequency);
    	            double sample = amplitude * Math.sin(2 * Math.PI * j / (SAMPLE_RATE/frequency));
    	            sample *= bit;
    	            short shortSample = (short)(sample * 32767);
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
