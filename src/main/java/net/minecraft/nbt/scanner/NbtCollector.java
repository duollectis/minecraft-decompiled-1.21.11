package net.minecraft.nbt.scanner;

import net.minecraft.nbt.*;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * {@code NbtCollector}.
 */
public class NbtCollector implements NbtScanner {

	private final Deque<NbtCollector.Node> queue = new ArrayDeque<>();

	public NbtCollector() {
		this.queue.addLast(new NbtCollector.RootNode());
	}

	public @Nullable NbtElement getRoot() {
		return this.queue.getFirst().getValue();
	}

	protected int getDepth() {
		return this.queue.size() - 1;
	}

	private void append(NbtElement nbt) {
		this.queue.getLast().append(nbt);
	}

	@Override
	public NbtScanner.Result visitEnd() {
		this.append(NbtEnd.INSTANCE);
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitString(String value) {
		this.append(NbtString.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitByte(byte value) {
		this.append(NbtByte.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitShort(short value) {
		this.append(NbtShort.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitInt(int value) {
		this.append(NbtInt.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitLong(long value) {
		this.append(NbtLong.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitFloat(float value) {
		this.append(NbtFloat.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitDouble(double value) {
		this.append(NbtDouble.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitByteArray(byte[] value) {
		this.append(new NbtByteArray(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitIntArray(int[] value) {
		this.append(new NbtIntArray(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitLongArray(long[] value) {
		this.append(new NbtLongArray(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitListMeta(NbtType<?> entryType, int length) {
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.NestedResult startListItem(NbtType<?> type, int index) {
		this.pushStack(type);
		return NbtScanner.NestedResult.ENTER;
	}

	@Override
	public NbtScanner.NestedResult visitSubNbtType(NbtType<?> type) {
		return NbtScanner.NestedResult.ENTER;
	}

	@Override
	public NbtScanner.NestedResult startSubNbt(NbtType<?> type, String key) {
		this.queue.getLast().setKey(key);
		this.pushStack(type);
		return NbtScanner.NestedResult.ENTER;
	}

	private void pushStack(NbtType<?> type) {
		if (type == NbtList.TYPE) {
			this.queue.addLast(new NbtCollector.ListNode());
		}
		else if (type == NbtCompound.TYPE) {
			this.queue.addLast(new NbtCollector.CompoundNode());
		}
	}

	@Override
	public NbtScanner.Result endNested() {
		NbtCollector.Node node = this.queue.removeLast();
		NbtElement nbtElement = node.getValue();
		if (nbtElement != null) {
			this.queue.getLast().append(nbtElement);
		}

		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result start(NbtType<?> rootType) {
		this.pushStack(rootType);
		return NbtScanner.Result.CONTINUE;
	}

	/**
	 * {@code CompoundNode}.
	 */
	static class CompoundNode implements NbtCollector.Node {

		private final NbtCompound value = new NbtCompound();
		private String key = "";

		@Override
		public void setKey(String key) {
			this.key = key;
		}

		@Override
		public void append(NbtElement value) {
			this.value.put(this.key, value);
		}

		@Override
		public NbtElement getValue() {
			return this.value;
		}
	}

	/**
	 * {@code ListNode}.
	 */
	static class ListNode implements NbtCollector.Node {

		private final NbtList value = new NbtList();

		@Override
		public void append(NbtElement value) {
			this.value.unwrapAndAdd(value);
		}

		@Override
		public NbtElement getValue() {
			return this.value;
		}
	}

	/**
	 * {@code Node}.
	 */
	interface Node {

		default void setKey(String key) {
		}

		void append(NbtElement value);

		@Nullable NbtElement getValue();
	}

	/**
	 * {@code RootNode}.
	 */
	static class RootNode implements NbtCollector.Node {

		private @Nullable NbtElement value;

		@Override
		public void append(NbtElement value) {
			this.value = value;
		}

		@Override
		public @Nullable NbtElement getValue() {
			return this.value;
		}
	}
}
