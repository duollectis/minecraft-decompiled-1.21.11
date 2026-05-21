package net.minecraft.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ErrorReporter;
import org.jspecify.annotations.Nullable;

/**
 * {@code NbtWriteView}.
 */
public class NbtWriteView implements WriteView {

	private final ErrorReporter reporter;
	private final DynamicOps<NbtElement> ops;
	private final NbtCompound nbt;

	NbtWriteView(ErrorReporter reporter, DynamicOps<NbtElement> ops, NbtCompound nbt) {
		this.reporter = reporter;
		this.ops = ops;
		this.nbt = nbt;
	}

	public static NbtWriteView create(ErrorReporter reporter, RegistryWrapper.WrapperLookup registries) {
		return new NbtWriteView(reporter, registries.getOps(NbtOps.INSTANCE), new NbtCompound());
	}

	public static NbtWriteView create(ErrorReporter reporter) {
		return new NbtWriteView(reporter, NbtOps.INSTANCE, new NbtCompound());
	}

	@Override
	public <T> void put(String key, Codec<T> codec, T value) {
		switch (codec.encodeStart(this.ops, value)) {
			case Success<NbtElement> success:
				this.nbt.put(key, (NbtElement) success.value());
				break;
			case Error<NbtElement> error:
				this.reporter.report(new NbtWriteView.EncodeFieldError(key, value, error));
				error.partialValue().ifPresent(partialValue -> this.nbt.put(key, partialValue));
				break;
			default:
				throw new MatchException(null, null);
		}
	}

	@Override
	public <T> void putNullable(String key, Codec<T> codec, @Nullable T value) {
		if (value != null) {
			this.put(key, codec, value);
		}
	}

	@Override
	public <T> void put(MapCodec<T> codec, T value) {
		switch (codec.encoder().encodeStart(this.ops, value)) {
			case Success<NbtElement> success:
				this.nbt.copyFrom((NbtCompound) success.value());
				break;
			case Error<NbtElement> error:
				this.reporter.report(new NbtWriteView.MergeError(value, error));
				error.partialValue().ifPresent(partialValue -> this.nbt.copyFrom((NbtCompound) partialValue));
				break;
			default:
				throw new MatchException(null, null);
		}
	}

	@Override
	public void putBoolean(String key, boolean value) {
		this.nbt.putBoolean(key, value);
	}

	@Override
	public void putByte(String key, byte value) {
		this.nbt.putByte(key, value);
	}

	@Override
	public void putShort(String key, short value) {
		this.nbt.putShort(key, value);
	}

	@Override
	public void putInt(String key, int value) {
		this.nbt.putInt(key, value);
	}

	@Override
	public void putLong(String key, long value) {
		this.nbt.putLong(key, value);
	}

	@Override
	public void putFloat(String key, float value) {
		this.nbt.putFloat(key, value);
	}

	@Override
	public void putDouble(String key, double value) {
		this.nbt.putDouble(key, value);
	}

	@Override
	public void putString(String key, String value) {
		this.nbt.putString(key, value);
	}

	@Override
	public void putIntArray(String key, int[] value) {
		this.nbt.putIntArray(key, value);
	}

	private ErrorReporter makeChildReporter(String key) {
		return this.reporter.makeChild(new ErrorReporter.MapElementContext(key));
	}

	@Override
	public WriteView get(String key) {
		NbtCompound nbtCompound = new NbtCompound();
		this.nbt.put(key, nbtCompound);
		return new NbtWriteView(this.makeChildReporter(key), this.ops, nbtCompound);
	}

	@Override
	public WriteView.ListView getList(String key) {
		NbtList nbtList = new NbtList();
		this.nbt.put(key, nbtList);
		return new NbtWriteView.NbtListView(key, this.reporter, this.ops, nbtList);
	}

	@Override
	public <T> WriteView.ListAppender<T> getListAppender(String key, Codec<T> codec) {
		NbtList nbtList = new NbtList();
		this.nbt.put(key, nbtList);
		return new NbtWriteView.NbtListAppender<>(this.reporter, key, this.ops, codec, nbtList);
	}

	@Override
	public void remove(String key) {
		this.nbt.remove(key);
	}

	@Override
	public boolean isEmpty() {
		return this.nbt.isEmpty();
	}

	public NbtCompound getNbt() {
		return this.nbt;
	}

	/**
	 * {@code AppendToListError}.
	 */
	public record AppendToListError(String name, Object value, Error<?> error) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Failed to append value '" + this.value + "' to list '" + this.name + "': " + this.error.message();
		}
	}

	/**
	 * {@code EncodeFieldError}.
	 */
	public record EncodeFieldError(String name, Object value, Error<?> error) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Failed to encode value '" + this.value + "' to field '" + this.name + "': " + this.error.message();
		}
	}

	/**
	 * {@code MergeError}.
	 */
	public record MergeError(Object value, Error<?> error) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Failed to merge value '" + this.value + "' to an object: " + this.error.message();
		}
	}

	/**
	 * {@code NbtListAppender}.
	 */
	static class NbtListAppender<T> implements WriteView.ListAppender<T> {

		private final ErrorReporter reporter;
		private final String key;
		private final DynamicOps<NbtElement> ops;
		private final Codec<T> codec;
		private final NbtList list;

		NbtListAppender(ErrorReporter reporter, String key, DynamicOps<NbtElement> ops, Codec<T> codec, NbtList list) {
			this.reporter = reporter;
			this.key = key;
			this.ops = ops;
			this.codec = codec;
			this.list = list;
		}

		@Override
		public void add(T value) {
			switch (this.codec.encodeStart(this.ops, value)) {
				case Success<NbtElement> success:
					this.list.add((NbtElement) success.value());
					break;
				case Error<NbtElement> error:
					this.reporter.report(new NbtWriteView.AppendToListError(this.key, value, error));
					error.partialValue().ifPresent(this.list::add);
					break;
				default:
					throw new MatchException(null, null);
			}
		}

		@Override
		public boolean isEmpty() {
			return this.list.isEmpty();
		}
	}

	/**
	 * {@code NbtListView}.
	 */
	static class NbtListView implements WriteView.ListView {

		private final String key;
		private final ErrorReporter reporter;
		private final DynamicOps<NbtElement> ops;
		private final NbtList list;

		NbtListView(String key, ErrorReporter reporter, DynamicOps<NbtElement> ops, NbtList list) {
			this.key = key;
			this.reporter = reporter;
			this.ops = ops;
			this.list = list;
		}

		@Override
		public WriteView add() {
			int i = this.list.size();
			NbtCompound nbtCompound = new NbtCompound();
			this.list.add(nbtCompound);
			return new NbtWriteView(
					this.reporter.makeChild(new ErrorReporter.NamedListElementContext(this.key, i)),
					this.ops,
					nbtCompound
			);
		}

		@Override
		public void removeLast() {
			this.list.removeLast();
		}

		@Override
		public boolean isEmpty() {
			return this.list.isEmpty();
		}
	}
}
