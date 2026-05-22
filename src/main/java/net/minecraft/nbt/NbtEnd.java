package net.minecraft.nbt;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Синглтон-тег конца составного тега ({@code TAG_End}).
 * <p>
 * Используется как маркер завершения {@link NbtCompound} при бинарной сериализации.
 * Не несёт полезных данных — только сигнализирует парсеру о конце блока.
 */
public final class NbtEnd implements NbtElement {

	private static final int SIZE = 8;

	public static final NbtType<NbtEnd> TYPE = new NbtType<NbtEnd>() {
		@Override
		public NbtEnd read(DataInput input, NbtSizeTracker tracker) {
			tracker.add(SIZE);
			return NbtEnd.INSTANCE;
		}

		@Override
		public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) {
			tracker.add(SIZE);
			return visitor.visitEnd();
		}

		@Override
		public void skip(DataInput input, int count, NbtSizeTracker tracker) {
		}

		@Override
		public void skip(DataInput input, NbtSizeTracker tracker) {
		}

		@Override
		public String getCrashReportName() {
			return "END";
		}

		@Override
		public String getCommandFeedbackName() {
			return "TAG_End";
		}
	};

	public static final NbtEnd INSTANCE = new NbtEnd();

	private NbtEnd() {
	}

	@Override
	public void write(DataOutput output) throws IOException {
	}

	@Override
	public int getSizeInBytes() {
		return SIZE;
	}

	@Override
	public byte getType() {
		return END_TYPE;
	}

	@Override
	public NbtType<NbtEnd> getNbtType() {
		return TYPE;
	}

	@Override
	public String toString() {
		StringNbtWriter writer = new StringNbtWriter();
		writer.visitEnd(this);
		return writer.getString();
	}

	@Override
	public NbtEnd copy() {
		return this;
	}

	@Override
	public void accept(NbtElementVisitor visitor) {
		visitor.visitEnd(this);
	}

	@Override
	public NbtScanner.Result doAccept(NbtScanner visitor) {
		return visitor.visitEnd();
	}
}
