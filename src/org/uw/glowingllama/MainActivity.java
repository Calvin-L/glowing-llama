package org.uw.glowingllama;

import java.nio.ByteBuffer;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.ToggleButton;

public class MainActivity extends ActionBarActivity {

    static final String EXTRA_MESSAGE = "org.uw.glowingllama.MESSAGE";
	private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static final int SAMPLE_RATE = 44100;

	private Thread listeningThread = null;
	private int frequency = 3700; // Hz

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
				updateFreq(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				
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
            assert plot != null;
    		listeningThread = new Thread(new Runnable() {

				@Override
				public void run() {
					int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING);
		    		final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING, bufferSize);
		    		record.startRecording();
		    		short[] data = new short[bufferSize / 2];
		    		
					while (true) {
						int num_read = record.read(data, 0, data.length);
						for (int i = 0; i < num_read; ++i) {
							plot.putSample(data[i]);
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
    
    public void pressButton(View view) {
    	
//    	double amplitude = 1;
//    	int numSamples = 50000;
//    	double freqOfTone = 440;  // in Hz 
//    	double[] sample = new double[numSamples];
//    	short[] buffer = new short[numSamples];
//    	
//    	
//    	// Get the tone.
//    	for (int i = 0; i < numSamples; ++i) {
//            sample[i] = amplitude * Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
//            buffer[i] = (short) (sample[i] * 32767);
//        }

    	EditText editText = (EditText) findViewById(R.id.edit_message);
    	String message = editText.getText().toString();

    	
    	short[] buffer = Modulate(Send(message));
    	
    	// Play the tone.
    	final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, buffer.length,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
        audioTrack.write(buffer, 0, buffer.length);
    	
//    	Intent intent = new Intent(this, DisplayMessageActivity.class);
//    	EditText editText = (EditText) findViewById(R.id.edit_message);
//    	String message = editText.getText().toString();
//    	intent.putExtra(EXTRA_MESSAGE, message);
//    	startActivity(intent);
    }
    
     
    public byte[] Send(String message) {

    	int headerLength = 4 + 1 + 1 + 4; // Preamble + src + dst + payload length
    	int preamble = 0xAAAAAAAA;
    	byte srcID = 0;
    	byte dstID = 1;
    	
    	byte[] payload = message.getBytes();
    	int packetLength = headerLength + payload.length;
    	
    	ByteBuffer packet = ByteBuffer.allocate(packetLength);
    	
    	packet.putInt(preamble);   
    	packet.put(srcID);  
    	packet.put(dstID);  
    	packet.putInt(payload.length);
    	packet.put(payload);
    	
    	return packet.array();
    }

    public short[] Modulate(byte[] bits) {
    	double amplitude = 1;
    	
    	int symbolLength = SAMPLE_RATE/20;
    	
    	int numSamples = bits.length * symbolLength * 8;
    	
    	short[] buffer = new short[numSamples];
    	  	
    	int idx = 0;
    	for (byte b : bits) {
    		for (int i = 7; i >= 0; --i) {
    			int bit = (b >> i) & 1;
    			
    			for (int j = 0; j < symbolLength; ++j) {
    	            double sample = amplitude * Math.sin(2 * Math.PI * j / (SAMPLE_RATE/frequency));
    	            sample *= bit;
    	            short shortSample = (short)(sample * 32767);
    	            buffer[idx++] = shortSample;
    			}
    			
    		}
    	}
    	
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
