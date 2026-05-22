package net.minecraft.util;

import net.minecraft.util.annotation.SuppressLinter;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Делегирующая обёртка над {@link DataOutput}.
 * Все вызовы методов перенаправляются к вложенному делегату.
 * Предназначена для расширения через наследование с переопределением нужных методов.
 */
public class DelegatingDataOutput implements DataOutput {

	private final DataOutput delegate;

	public DelegatingDataOutput(DataOutput delegate) {
		this.delegate = delegate;
	}

	@Override
	public void write(int value) throws IOException {
		delegate.write(value);
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		delegate.write(bytes);
	}

	@Override
	public void write(byte[] bytes, int offset, int length) throws IOException {
		delegate.write(bytes, offset, length);
	}

	@Override
	public void writeBoolean(boolean value) throws IOException {
		delegate.writeBoolean(value);
	}

	@Override
	public void writeByte(int value) throws IOException {
		delegate.writeByte(value);
	}

	@Override
	public void writeShort(int value) throws IOException {
		delegate.writeShort(value);
	}

	@Override
	public void writeChar(int value) throws IOException {
		delegate.writeChar(value);
	}

	@Override
	public void writeInt(int value) throws IOException {
		delegate.writeInt(value);
	}

	@Override
	public void writeLong(long value) throws IOException {
		delegate.writeLong(value);
	}

	@Override
	public void writeFloat(float value) throws IOException {
		delegate.writeFloat(value);
	}

	@Override
	public void writeDouble(double value) throws IOException {
		delegate.writeDouble(value);
	}

	@SuppressLinter(reason = "Delegation is not use")
	@Override
	public void writeBytes(String string) throws IOException {
		delegate.writeBytes(string);
	}

	@Override
	public void writeChars(String string) throws IOException {
		delegate.writeChars(string);
	}

	@Override
	public void writeUTF(String string) throws IOException {
		delegate.writeUTF(string);
	}
}
