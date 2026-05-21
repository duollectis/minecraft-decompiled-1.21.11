package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;

import java.io.DataInput;
import java.io.IOException;

/**
 * {@code NbtType}.
 */
public interface NbtType<T extends NbtElement> {

	T read(DataInput input, NbtSizeTracker tracker) throws IOException;

	NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException;

	default void accept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
		switch (visitor.start(this)) {
			case CONTINUE:
				this.doAccept(input, visitor, tracker);
			case HALT:
			default:
				break;
			case BREAK:
				this.skip(input, tracker);
		}
	}

	void skip(DataInput input, int count, NbtSizeTracker tracker) throws IOException;

	void skip(DataInput input, NbtSizeTracker tracker) throws IOException;

	String getCrashReportName();

	String getCommandFeedbackName();

	static NbtType<NbtEnd> createInvalid(int type) {
		return new NbtType<NbtEnd>() {
			private IOException createException() {
				return new IOException("Invalid tag id: " + type);
			}

			/**
			 * Read.
			 *
			 * @param dataInput data input
			 * @param nbtSizeTracker nbt size tracker
			 *
			 * @return NbtEnd — результат операции
			 */
			public NbtEnd read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
				throw this.createException();
			}

			@Override
			public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker)
			throws IOException {
				throw this.createException();
			}

			@Override
			public void skip(DataInput input, int count, NbtSizeTracker tracker) throws IOException {
				throw this.createException();
			}

			@Override
			public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
				throw this.createException();
			}

			@Override
			public String getCrashReportName() {
				return "INVALID[" + type + "]";
			}

			@Override
			public String getCommandFeedbackName() {
				return "UNKNOWN_" + type;
			}
		};
	}

	/**
	 * {@code OfFixedSize}.
	 */
	public interface OfFixedSize<T extends NbtElement> extends NbtType<T> {

		@Override
		default void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
			input.skipBytes(this.getSizeInBytes());
		}

		@Override
		default void skip(DataInput input, int count, NbtSizeTracker tracker) throws IOException {
			input.skipBytes(this.getSizeInBytes() * count);
		}

		int getSizeInBytes();
	}

	/**
	 * {@code OfVariableSize}.
	 */
	public interface OfVariableSize<T extends NbtElement> extends NbtType<T> {

		@Override
		default void skip(DataInput input, int count, NbtSizeTracker tracker) throws IOException {
			for (int i = 0; i < count; i++) {
				this.skip(input, tracker);
			}
		}
	}
}
