package org.uw.glowingllama;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import android.util.Log;

/**
 * Given a stream of bits, this class identifies the packet preamble and parses
 * the packet header. The packet contents are returned as a byte array. It acts
 * like a state machine with three states:
 * <ul>
 * 	<li>waiting for preamble
 *  <li>reading header
 *  <li>reading packet
 * </ul>
 */
public class PacketParser {

	/** The preamble we're looking for */
	private final byte[] preamble;

	private enum Mode {
		WAITING_FOR_PREAMBLE,
		READING_HEADER,
		READING_PACKET;
	}
	private Mode mode = Mode.WAITING_FOR_PREAMBLE;
	private int readLen = 0; // expected packet length when READING_PACKET

	/**
	 * If mode == WAITING_FOR_PREAMBLE, contains most recent bits (enough to
	 * identify the preamble).
	 * Otherwise, empty.
	 */
	private final RingBuffer preambleDetection;

	/**
	 * If mode == WAITING_FOR_PREAMBLE, empty.
	 * If mode == READING_HEADER, holds header bits.
	 * If mode == READING_PACKET, holds packet bits.
	 */
	private final ArrayList<Integer> bits = new ArrayList<Integer>();

	public PacketParser(byte[] preamble) {
		this.preamble = preamble;
		preambleDetection = new RingBuffer(preamble.length * 8);
	}

	/**
	 * Called when a bit is observed on the stream.
	 * @param i zero or one
	 * @return a parsed packet, or null if more bits are needed
	 */
	protected byte[] reportBit(int i) {
		//    	Log.i("x", "Got bit: " + i);
		byte[] result = null;

		switch (mode) {
		case WAITING_FOR_PREAMBLE:
			// add this sample to the preamble buffer
			preambleDetection.addElement((short)i);

			// if the last few bits equal the preamble, transition to the
			// READING_HEADER state
			if (preambleFound(preamble, preambleDetection)) {
				preambleDetection.clear();
				Log.i("x", "PREAMBLE FOUND!");
				mode = Mode.READING_HEADER;
			}
			break;
		case READING_HEADER:
			// add this to the list of bits
			bits.add(i);
			// if we have read enough bits to equal the length of the header...
			if (bits.size() == 8 + 8 + 32) { // src + dst + len
				// convert the bits to bytes
				byte[] header = asBytes(bits);
				// convert some of the bytes to find the length of the packet
				readLen = packetLength(header);
				Log.i("x", "HEADER PARSED, BITS=" + bits + ", BYTES=" + Arrays.toString(header) + ", LEN=" + readLen);
				if (readLen > 0) {
					// if the packet is nonempty, transition to READING_PACKET
					mode = Mode.READING_PACKET;
				} else {
					// if the packet is empty, transition back to WAITING_FOR_PREAMBLE
					mode = Mode.WAITING_FOR_PREAMBLE;
				}
				// we're done with the header, clear the bits
				bits.clear();
			}
			break;
		case READING_PACKET:
			// add this to the list of bits
			bits.add(i);
			// if we've found enough bits...
			if (bits.size() >= readLen * 8) {
				// convert the bits to bytes
				result = asBytes(bits);
				Log.i("x", "PACKET EXTRACTED, BYTES=" + Arrays.toString(result));
				// transition back to WAITING_FOR_PREAMBLE
				mode = Mode.WAITING_FOR_PREAMBLE;
				// clear the bits
				bits.clear();
			}
		}
		return result;
	}

	/**
	 * Extract the length field from the bytes of the header
	 * @param header   bytes from a packet header
	 * @return the length of the packet
	 */
	private int packetLength(byte[] header) {
		// the last 4 bytes of the header are the length
		int i = header.length - 4;
		// bitwise magic to assemble an int from 4 bytes
		return (header[i] << 24) | (header[i+1] << 16) | (header[i+2] << 8) | (header[i+3]);
	}

	/**
	 * Converts a list of bits (0 or 1) to an array of bytes
	 * @param bits   the bits!
	 * @return the bytes!
	 */
	private byte[] asBytes(ArrayList<Integer> bits) {
		if (bits.size() % 8 != 0) {
			Log.w("PacketParser", "asBytes() called but number of bits is not a multiple of 8!?");
		}
		// thanks to truncation, we will just ignore any extra bits at the end
		int numBytes = bits.size() / 8;
		// allocate a byte buffer to store the bytes
		ByteBuffer buf = ByteBuffer.allocate(numBytes);
		int i = 0;
		byte val = 0;
		// loop through each bit
		for (int idx = 0; idx < numBytes * 8; ++idx) {
			int bit = bits.get(idx);
			// shift the byte left
			val <<= 1;
			// put the bit in the rightmost position
			val |= bit;
			i++;
			// if 8 bits have elapsed, put the byte in the buffer
			if (i >= 8) {
				buf.put(val);
				val = 0;
				i = 0;
			}
		}
		return buf.array();
	}

	/**
	 * Does the preamble match the ring buffer?
	 * @param preamble             the preamble as a byte array
	 * @param preambleDetection    a ring buffer filled with 0s and 1s
	 * @return true iff the bits of the preamble match the bits of the ring buffer
	 */
	private boolean preambleFound(byte[] preamble, RingBuffer preambleDetection) {
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
