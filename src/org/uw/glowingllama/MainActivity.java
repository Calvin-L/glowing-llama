package org.uw.glowingllama;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends FragmentActivity {

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			String receivedMessage = bundle.getString("receivedMessage");
			TextView msgHistTextView =
					(TextView)findViewById(R.id.messageHistory);
			if (receivedMessage == null)
				receivedMessage = "(null!?)";
			if (msgHistTextView == null) {
				Log.e("x", "Couldn't find text view!?");
				return;
			}
			msgHistTextView.append(receivedMessage + "\n");
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

	private void sendMessageText(final String message) {
		Thread sendThread = new Thread(new Runnable() {

			@Override
			public void run() {
				final short[] buffer = modulate(send(message));
				final int bufferSize = AudioTrack.getMinBufferSize(Constants.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, Constants.ENCODING);

				final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
						Constants.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
						Constants.ENCODING, bufferSize, AudioTrack.MODE_STREAM);
				audioTrack.play();
				audioTrack.write(buffer, 0, buffer.length);
				audioTrack.stop();
			}

		});
		sendThread.start();
	}


	public void sendLongMessage(View view) {
		sendMessageText(Constants.SHAKESPEARE);
		postMessage("I'm sending Shakespeare text");
	}


	public void sendMessage(View view) {
		EditText editText = (EditText) findViewById(R.id.edit_message);
		final String message = editText.getText().toString();
		sendMessageText(message);
		postMessage("Me: " + message);
	}

	public void postMessage(String message) {
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putString("receivedMessage", message);
		msg.setData(bundle);
		handler.sendMessage(msg);
	}

	private Thread listenThread = null;

	private void stopListening() {
		if (listenThread != null) {
			listenThread.interrupt();
			try {
				listenThread.join();
			} catch (InterruptedException e) {
				// OK FINE we'll stop
			}
			listenThread = null;
		}
	}

	interface OnPacket {
		void packetReceived(byte[] bytes, String decoded);
	}

	private void startListening(final OnPacket callback) {
		listenThread = new Thread(new Runnable() {
			@Override
			public void run() {
				new DeModulator().listen(getExternalFilesDir(null), callback);
			}
		});
		listenThread.start();
	}

	private final ArrayList<Byte> collectedBytes = new ArrayList<Byte>();
	public void toggleListenLongMessage(View view) {
		ToggleButton button = (ToggleButton) findViewById(R.id.longMessageListenToggle);
		boolean listening = button.isChecked();
		stopListening();
		if (listening) {
			startListening(new OnPacket() {
				@Override
				public void packetReceived(byte[] bytes, String message) {
					postMessage("Friend: " + message);
					for (byte b : bytes) {
						collectedBytes.add(b);
					}
				}
			});
		} else {
			// TODO: do the comparison
			byte[] expectedBytes = Constants.SHAKESPEARE.getBytes();
			int numDifferentBits = bitDiff(expectedBytes, collectedBytes);
			Log.i("x", "I did the comparison thing; diff = " + numDifferentBits + " / " + expectedBytes.length * 8);
			collectedBytes.clear();
		}
	}

	private int bitDiff(byte[] expectedBytes, ArrayList<Byte> collectedBytes) {
		int diff = 0;
		int len = Math.min(expectedBytes.length, collectedBytes.size());

		diff += Math.abs(expectedBytes.length - collectedBytes.size()) * 8;

		for (int i = 0; i < len; ++i) {
			byte b1 = expectedBytes[i];
			byte b2 = collectedBytes.get(i);

			byte comparison = (byte) (b1 ^ b2);
			for (int bitIndex = 0; bitIndex < 8; ++bitIndex) {
				diff += (comparison >> bitIndex) & 1;
			}
		}

		return diff;
	}

	public void toggleNormalListen(View view) {
		ToggleButton button = (ToggleButton) findViewById(R.id.regularListenToggle);
		boolean listening = button.isChecked();
		stopListening();
		if (listening) {
			startListening(new OnPacket() {
				@Override
				public void packetReceived(byte[] bytes, String message) {
					postMessage("Friend: " + message);
				}
			});
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
