package org.uw.glowingllama;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
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

	@Override
	public void onResume() {
		super.onResume();

		int buf1 = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING);
		if (buf1 == AudioRecord.ERROR_BAD_VALUE) {
			throw new RuntimeException("Invalid sample rate or channels or encoding or something for AudioRecord");
		}
		if (buf1 == AudioRecord.ERROR) {
			throw new RuntimeException("Error querying hardware for AudioRecord");
		}

		int buf2 = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING);
		if (buf2 == AudioTrack.ERROR_BAD_VALUE) {
			throw new RuntimeException("Invalid value for AudioTrack");
		}
		if (buf2 == AudioTrack.ERROR) {
			throw new RuntimeException("Error querying hardware for AudioTrack");
		}

		Log.i("x", "Starting...");

		final int bufferSize = Math.max(buf1, buf2);
		final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING, bufferSize);
		final AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING, bufferSize, AudioTrack.MODE_STREAM);

		Log.i("x", "Using buffer size " + bufferSize);

		record.startRecording();
		track.play();
		echoThread = new Thread(new Runnable() {
			@Override
			public void run() {
				short[] data = new short[bufferSize / 2];
				while (true) {
					int num_read = record.read(data, 0, data.length);
					track.write(data, 0, num_read);
					if (num_read > 0) {
						short max = 0;
						for (int i = 0; i < num_read; ++i) {
							max = (short) Math.max(max, data[i]);
						}
						Log.i("x", "Read " + num_read + " samples, max=" + max);
					}
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						Log.i("x", "Stopping...");
						record.stop();
						track.stop();
						break;
					}
				}
			}
		});
		echoThread.start();
	}

	@Override
	public void onPause() {
		super.onPause();
		echoThread.interrupt();
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
    
    public void pressButton(View view) {
    	Intent intent = new Intent(this, DisplayMessageActivity.class);
    	EditText editText = (EditText) findViewById(R.id.edit_message);
    	String message = editText.getText().toString();
    	intent.putExtra(EXTRA_MESSAGE, message);
    	startActivity(intent);
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
