package org.uw.glowingllama;

import java.nio.ByteBuffer;
import java.util.Arrays;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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

public class MainActivity extends ActionBarActivity {

    static final String EXTRA_MESSAGE = "org.uw.glowingllama.MESSAGE";
	private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static final int SAMPLE_RATE = 8000;

	private Thread echoThread = null;

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

//	@Override
//	public void onResume() {
//		super.onResume();
//
//		int buf1 = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING);
//		if (buf1 == AudioRecord.ERROR_BAD_VALUE) {
//			throw new RuntimeException("Invalid sample rate or channels or encoding or something for AudioRecord");
//		}
//		if (buf1 == AudioRecord.ERROR) {
//			throw new RuntimeException("Error querying hardware for AudioRecord");
//		}
//
//		int buf2 = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING);
//		if (buf2 == AudioTrack.ERROR_BAD_VALUE) {
//			throw new RuntimeException("Invalid value for AudioTrack");
//		}
//		if (buf2 == AudioTrack.ERROR) {
//			throw new RuntimeException("Error querying hardware for AudioTrack");
//		}
//
//		Log.i("x", "Starting...");
//
//		final int bufferSize = Math.max(buf1, buf2);
//		final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING, bufferSize);
//		final AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING, bufferSize, AudioTrack.MODE_STREAM);
//
//		Log.i("x", "Using buffer size " + bufferSize);
//
//		record.startRecording();
//		track.play();
//		echoThread = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				short[] data = new short[bufferSize / 2];
//				while (true) {
//					int num_read = record.read(data, 0, data.length);
//					track.write(data, 0, num_read);
//					if (num_read > 0) {
//						short max = 0;
//						for (int i = 0; i < num_read; ++i) {
//							max = (short) Math.max(max, data[i]);
//						}
//						Log.i("x", "Read " + num_read + " samples, max=" + max);
//					}
//					try {
//						Thread.sleep(5);
//					} catch (InterruptedException e) {
//						Log.i("x", "Stopping...");
//						record.stop();
//						track.stop();
//						break;
//					}
//				}
//			}
//		});
//		echoThread.start();
//	}
//
//	@Override
//	public void onPause() {
//		super.onPause();
//		echoThread.interrupt();
//	}

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
    
    public void pressButton(View view) {
    	
//    	double amplitude = 1;
//    	int numSamples = 50000;
    	int sampleRate = 8000;
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
    	
    	Log.i("We're here", Arrays.toString(buffer));
    	
    	// Play the tone.
    	final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, buffer.length,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
        int numWritten = audioTrack.write(buffer, 0, buffer.length);
        Log.i("x", "Total shorts: " + buffer.length + "; num written: " + numWritten);
    	
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
    	
//    	Log.i("We're here", Arrays.toString(packet.array()));
    	
    	return packet.array();
    }

    public short[] Modulate(byte[] bits) {
    	double amplitude = 1;
    	int sampleRate = 8000;
    	double freqOfTone = 440;  // in Hz 
    	
    	int symbolLength = 400;
    	
    	int numSamples = bits.length * symbolLength * 8;
    	
    	short[] buffer = new short[numSamples];
    	  	
    	int idx = 0;
    	for (byte b : bits) {
    		for (int i = 7; i >= 0; --i) {
    			int bit = (b >> i) & 1;
    			
    			for (int j = 0; j < symbolLength; ++j) {
    	            double sample = amplitude * Math.sin(2 * Math.PI * j / (sampleRate/freqOfTone));
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
