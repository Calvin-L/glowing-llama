package org.uw.glowingllama;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import android.util.Log;

public class PacketParser {
	
	private final byte[] preamble;
    private final RingBuffer preambleDetection;
    
    private enum Mode {
    	WAITING_FOR_PREAMBLE,
    	READING_HEADER,
    	READING_PACKET;
    }
    private Mode mode = Mode.WAITING_FOR_PREAMBLE;
    private int readLen = 0; // expected packet length when READING_PACKET
    
    /**
     * If mode == WAITING_FOR_PREAMBLE, empty.
     * If mode == READING_HEADER, holds header bits.
     * If mode == READING_PACKET, holds packet bits.
     */
    private ArrayList<Integer> bits = new ArrayList<Integer>();
    
    public PacketParser(byte[] preamble) {
    	this.preamble = preamble;
    	preambleDetection = new RingBuffer(preamble.length * 8);
    }
    
    /**
     * Called when a bit is observed on the stream.
     * @param i zero or one
     * @return a parsed packet, or null if still waiting
     */
    protected byte[] reportBit(int i) {
//    	Log.i("x", "Got bit: " + i);
    	byte[] result = null;
		switch (mode) {
		case WAITING_FOR_PREAMBLE:
			preambleDetection.addElement((short)i);
			if (preambleFound()) {
				Log.i("x", "PREAMBLE FOUND!");
				mode = Mode.READING_HEADER;
			}
			break;
		case READING_HEADER:
			bits.add(i);
			if (bits.size() == 8 + 8 + 32) { // src + dst + len
				byte[] header = asBytes(bits);
				readLen = packetLength(header);
				Log.i("x", "HEADER PARSED, BITS=" + bits + ", BYTES=" + Arrays.toString(header) + ", LEN=" + readLen);
				if (readLen > 0) {
					mode = Mode.READING_PACKET;	
				} else {
					mode = Mode.WAITING_FOR_PREAMBLE;
				}
				bits.clear();
			}
			break;
		case READING_PACKET:
			bits.add(i);
			if (bits.size() >= readLen * 8) {
				result = asBytes(bits);
				Log.i("x", "PACKET EXTRACTED, BYTES=" + Arrays.toString(result));
				mode = Mode.WAITING_FOR_PREAMBLE;
				bits.clear();
			}
		}
		return result;
	}
    
    private int packetLength(byte[] header) {
    	int i = header.length - 4;
    	return (header[i] << 24) | (header[i+1] << 16) | (header[i+2] << 8) | (header[i+3]);
    }

	private byte[] asBytes(ArrayList<Integer> bits) {
		if (bits.size() % 8 != 0) {
			Log.w("PacketParser", "asBytes() called but number of bits is not a multiple of 8!?");
		}
		int numBytes = bits.size() / 8;
		ByteBuffer buf = ByteBuffer.allocate(numBytes);
		int i = 0;
		byte val = 0;
		for (int idx = 0; idx < numBytes * 8; ++idx) {
			int bit = bits.get(idx);
			val <<= 1;
			val |= bit;
			i++;
			if (i >= 8) {
				buf.put(val);
				val = 0;
				i = 0;
			}
		}
		return buf.array();
	}

	private boolean preambleFound() {
		int index = 0;
		for (int b = 0; b < preamble.length; ++b) {
			for (int i = 7; i >= 0; --i)  {
				short expected = (short)((preamble[b] >> i) & 1);
				short real = preambleDetection.get(index);
				if (real != expected)
					return false;
				++index;
			}
		}
		return true;
	}
}
