package org.uw.glowingllama;

import java.util.Iterator;

public class RingBuffer implements Iterable<Short> {
	int bufferSize;
	short[] buffer;
	int head;

	public RingBuffer(int capacity) {
		bufferSize = capacity;
		buffer = new short[bufferSize];
		for (int i = 0; i < bufferSize; ++i) {
			buffer[i] = 0;
		}
		head = 0;
	}

	public void addElement(short element) {
		buffer[head] = element;
		++head;
		head = head % bufferSize;
	}

	public void addAll(short[] element) {
		for (short e : element) {
			addElement(e);
		}
	}

	public void addAll(RingBuffer b) {
		for (short s : b) {
			addElement(s);
		}
	}

	public short get(int index) {
		assert index >= 0;
		assert index < bufferSize;
		return buffer[(index + head) % bufferSize];
	}

	public int size() {
		return bufferSize;
	}

	public void resize(int newCapacity) {
		RingBuffer b = new RingBuffer(newCapacity);
		b.addAll(this);
		buffer = b.buffer;
		bufferSize = newCapacity;
		head = b.head;
	}

	public void reset() {
		head = 0;
	}

	public short min() {
		return get(indexOfMin());
	}

	public short max() {
		return get(indexOfMax());
	}

	public int indexOfMin() {
		int index = 0;
		short min = get(0);
		for (int i = 1; i < size(); ++i) {
			short s = get(i);
			if (s < min) {
				min = s;
				index = i;
			}
		}
		return index;
	}

	public int indexOfMax() {
		int index = 0;
		short max = get(0);
		for (int i = 1; i < size(); ++i) {
			short s = get(i);
			if (s > max) {
				max = s;
				index = i;
			}
		}
		return index;
	}

	public int sum() {
		int sum = 0;
		for (short s : buffer) {
			sum += s;
		}
		return sum;
	}

	@Override
	public Iterator<Short> iterator() {
		return new Iterator<Short>() {

			int i = 0;

			@Override
			public boolean hasNext() {
				return i < size();
			}

			@Override
			public Short next() {
				return get(i++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

	public void clear() {
		for (int i = 0; i < buffer.length; ++i) {
			buffer[i] = 0;
		}
	}

}
