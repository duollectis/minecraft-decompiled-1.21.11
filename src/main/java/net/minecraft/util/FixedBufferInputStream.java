package net.minecraft.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Буферизованный входной поток с фиксированным буфером фиксированного размера.
 * <p>
 * В отличие от {@link java.io.BufferedInputStream}, использует буфер фиксированного
 * размера без возможности изменения. Оптимизирован для случаев, когда размер буфера
 * известен заранее и не должен меняться в процессе работы.
 */
public class FixedBufferInputStream extends InputStream {

	private static final int DEFAULT_BUFFER_SIZE = 8192;

	private final InputStream stream;
	private final byte[] buffer;
	private int end;
	private int start;

	public FixedBufferInputStream(InputStream stream) {
		this(stream, DEFAULT_BUFFER_SIZE);
	}

	public FixedBufferInputStream(InputStream stream, int size) {
		this.stream = stream;
		this.buffer = new byte[size];
	}

	@Override
	public int read() throws IOException {
		if (start >= end) {
			fill();

			if (start >= end) {
				return -1;
			}
		}

		return Byte.toUnsignedInt(buffer[start++]);
	}

	@Override
	public int read(byte[] dest, int offset, int length) throws IOException {
		int available = getAvailableBuffer();

		if (available <= 0) {
			if (length >= buffer.length) {
				return stream.read(dest, offset, length);
			}

			fill();
			available = getAvailableBuffer();

			if (available <= 0) {
				return -1;
			}
		}

		int toRead = Math.min(length, available);
		System.arraycopy(buffer, start, dest, offset, toRead);
		start += toRead;

		return toRead;
	}

	@Override
	public long skip(long count) throws IOException {
		if (count <= 0L) {
			return 0L;
		}

		long available = getAvailableBuffer();

		if (available <= 0L) {
			return stream.skip(count);
		}

		long toSkip = Math.min(count, available);
		start = (int) (start + toSkip);

		return toSkip;
	}

	@Override
	public int available() throws IOException {
		return getAvailableBuffer() + stream.available();
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}

	private int getAvailableBuffer() {
		return end - start;
	}

	private void fill() throws IOException {
		end = 0;
		start = 0;
		int bytesRead = stream.read(buffer, 0, buffer.length);

		if (bytesRead > 0) {
			end = bytesRead;
		}
	}
}
