package net.minecraft.nbt.scanner;

import net.minecraft.nbt.*;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Реализация {@link NbtScanner}, которая собирает NBT-структуру в памяти.
 * <p>
 * Использует стек {@link Node} для отслеживания текущего контекста:
 * при входе в компаунд или список добавляет соответствующий узел,
 * при выходе — извлекает и добавляет результат в родительский узел.
 * Корневой элемент доступен через {@link #getRoot()}.
 */
public class NbtCollector implements NbtScanner {

	private final Deque<NbtCollector.Node> queue = new ArrayDeque<>();

	public NbtCollector() {
		queue.addLast(new NbtCollector.RootNode());
	}

	public @Nullable NbtElement getRoot() {
		return queue.getFirst().getValue();
	}

	protected int getDepth() {
		return queue.size() - 1;
	}

	private void append(NbtElement nbt) {
		queue.getLast().append(nbt);
	}

	@Override
	public NbtScanner.Result visitEnd() {
		append(NbtEnd.INSTANCE);
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitString(String value) {
		append(NbtString.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitByte(byte value) {
		append(NbtByte.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitShort(short value) {
		append(NbtShort.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitInt(int value) {
		append(NbtInt.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitLong(long value) {
		append(NbtLong.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitFloat(float value) {
		append(NbtFloat.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitDouble(double value) {
		append(NbtDouble.of(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitByteArray(byte[] value) {
		append(new NbtByteArray(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitIntArray(int[] value) {
		append(new NbtIntArray(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitLongArray(long[] value) {
		append(new NbtLongArray(value));
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result visitListMeta(NbtType<?> entryType, int length) {
		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.NestedResult startListItem(NbtType<?> type, int index) {
		pushStack(type);
		return NbtScanner.NestedResult.ENTER;
	}

	@Override
	public NbtScanner.NestedResult visitSubNbtType(NbtType<?> type) {
		return NbtScanner.NestedResult.ENTER;
	}

	@Override
	public NbtScanner.NestedResult startSubNbt(NbtType<?> type, String key) {
		queue.getLast().setKey(key);
		pushStack(type);
		return NbtScanner.NestedResult.ENTER;
	}

	private void pushStack(NbtType<?> type) {
		if (type == NbtList.TYPE) {
			queue.addLast(new NbtCollector.ListNode());
		}
		else if (type == NbtCompound.TYPE) {
			queue.addLast(new NbtCollector.CompoundNode());
		}
	}

	@Override
	public NbtScanner.Result endNested() {
		NbtCollector.Node node = queue.removeLast();
		NbtElement element = node.getValue();
		if (element != null) {
			queue.getLast().append(element);
		}

		return NbtScanner.Result.CONTINUE;
	}

	@Override
	public NbtScanner.Result start(NbtType<?> rootType) {
		pushStack(rootType);
		return NbtScanner.Result.CONTINUE;
	}

	/**
	 * Узел для сборки {@link NbtCompound}.
	 * Хранит текущий ключ, который устанавливается перед добавлением каждого поля.
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
			this.value.put(key, value);
		}

		@Override
		public NbtElement getValue() {
			return value;
		}
	}

	/**
	 * Узел для сборки {@link NbtList}.
	 * Добавляет элементы через {@link NbtList#unwrapAndAdd} для корректной обработки обёрток.
	 */
	static class ListNode implements NbtCollector.Node {

		private final NbtList value = new NbtList();

		@Override
		public void append(NbtElement element) {
			value.unwrapAndAdd(element);
		}

		@Override
		public NbtElement getValue() {
			return value;
		}
	}

	/**
	 * Интерфейс узла стека сборки.
	 * Каждый узел накапливает дочерние элементы и возвращает готовый результат.
	 */
	interface Node {

		default void setKey(String key) {
		}

		void append(NbtElement value);

		@Nullable NbtElement getValue();
	}

	/**
	 * Корневой узел — хранит единственный элемент верхнего уровня.
	 */
	static class RootNode implements NbtCollector.Node {

		private @Nullable NbtElement value;

		@Override
		public void append(NbtElement value) {
			this.value = value;
		}

		@Override
		public @Nullable NbtElement getValue() {
			return value;
		}
	}
}
