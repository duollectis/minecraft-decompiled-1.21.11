package net.minecraft.util.packrat;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Стек фреймов для хранения промежуточных результатов разбора.
 * Каждый фрейм содержит пары (Symbol, value), соответствующие
 * успешно разобранным нетерминалам текущего правила.
 * Поддерживает откат (popFrame), дублирование (duplicateFrames) и
 * выбор текущего фрейма (chooseCurrentFrame) для реализации альтернатив PEG.
 */
public final class ParseResults {

	private static final int MISSING = -1;
	private static final int ENTRY_SIZE = 2;
	private static final Object FRAME = new Object() {
		@Override
		public String toString() {
			return "frame";
		}
	};

	private @Nullable Object[] stack = new Object[128];
	private int stackTop = 0;
	private int stackBottom = 0;

	public ParseResults() {
		stack[0] = FRAME;
		stack[1] = null;
	}

	private int indexOf(Symbol<?> symbol) {
		for (int i = stackTop; i > stackBottom; i -= ENTRY_SIZE) {
			Object entry = stack[i];

			assert entry instanceof Symbol;

			if (entry == symbol) {
				return i + 1;
			}
		}

		return MISSING;
	}

	public int indexOf(Symbol<?>... symbols) {
		for (int i = stackTop; i > stackBottom; i -= ENTRY_SIZE) {
			Object entry = stack[i];

			assert entry instanceof Symbol;

			for (Symbol<?> symbol : symbols) {
				if (symbol == entry) {
					return i + 1;
				}
			}
		}

		return MISSING;
	}

	private void expandIfNeeded(int amount) {
		int capacity = stack.length;
		int needed = stackTop + 1 + amount * ENTRY_SIZE;

		if (needed >= capacity) {
			int newCapacity = Util.nextCapacity(capacity, needed + 1);
			Object[] expanded = new Object[newCapacity];
			System.arraycopy(stack, 0, expanded, 0, capacity);
			stack = expanded;
		}

		assert isValid();
	}

	private void addFrame() {
		stackTop += ENTRY_SIZE;
		stack[stackTop] = FRAME;
		stack[stackTop + 1] = stackBottom;
		stackBottom = stackTop;
	}

	public void pushFrame() {
		expandIfNeeded(1);
		addFrame();

		assert isValid();
	}

	private int getPreviousStackBottom(int current) {
		return (Integer) stack[current + 1];
	}

	public void popFrame() {
		assert stackBottom != 0;

		stackTop = stackBottom - ENTRY_SIZE;
		stackBottom = getPreviousStackBottom(stackBottom);

		assert isValid();
	}

	public void duplicateFrames() {
		int savedBottom = stackBottom;
		int frameSize = (stackTop - stackBottom) / ENTRY_SIZE;

		expandIfNeeded(frameSize + 1);
		addFrame();

		int src = savedBottom + ENTRY_SIZE;
		int dst = stackTop;

		for (int i = 0; i < frameSize; i++) {
			dst += ENTRY_SIZE;
			Object symbol = stack[src];

			assert symbol != null;

			stack[dst] = symbol;
			stack[dst + 1] = null;
			src += ENTRY_SIZE;
		}

		stackTop = dst;

		assert isValid();
	}

	public void clearFrameValues() {
		for (int i = stackTop; i > stackBottom; i -= ENTRY_SIZE) {
			assert stack[i] instanceof Symbol;

			stack[i + 1] = null;
		}

		assert isValid();
	}

	public void chooseCurrentFrame() {
		int prevBottom = getPreviousStackBottom(stackBottom);
		int dst = prevBottom;
		int src = stackBottom;

		while (src < stackTop) {
			dst += ENTRY_SIZE;
			src += ENTRY_SIZE;

			Object symbol = stack[src];

			assert symbol instanceof Symbol;

			Object value = stack[src + 1];
			Object existing = stack[dst];

			if (existing != symbol) {
				stack[dst] = symbol;
				stack[dst + 1] = value;
			} else if (value != null) {
				stack[dst + 1] = value;
			}
		}

		stackTop = dst;
		stackBottom = prevBottom;

		assert isValid();
	}

	public <T> void put(Symbol<T> symbol, @Nullable T value) {
		int index = indexOf(symbol);

		if (index != MISSING) {
			stack[index] = value;
		} else {
			expandIfNeeded(1);
			stackTop += ENTRY_SIZE;
			stack[stackTop] = symbol;
			stack[stackTop + 1] = value;
		}

		assert isValid();
	}

	public <T> @Nullable T get(Symbol<T> symbol) {
		int index = indexOf(symbol);
		return (T) (index != MISSING ? stack[index] : null);
	}

	public <T> T getOrThrow(Symbol<T> symbol) {
		int index = indexOf(symbol);

		if (index == MISSING) {
			throw new IllegalArgumentException("No value for atom " + symbol);
		}

		return (T) stack[index];
	}

	public <T> T getOrDefault(Symbol<T> symbol, T fallback) {
		int index = indexOf(symbol);
		return (T) (index != MISSING ? stack[index] : fallback);
	}

	@SafeVarargs
	public final <T> @Nullable T getAny(Symbol<? extends T>... symbols) {
		int index = indexOf(symbols);
		return (T) (index != MISSING ? stack[index] : null);
	}

	@SafeVarargs
	public final <T> T getAnyOrThrow(Symbol<? extends T>... symbols) {
		int index = indexOf(symbols);

		if (index == MISSING) {
			throw new IllegalArgumentException("No value for atoms " + Arrays.toString((Object[]) symbols));
		}

		return (T) stack[index];
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean firstInFrame = true;

		for (int i = 0; i <= stackTop; i += ENTRY_SIZE) {
			Object entry = stack[i];

			if (entry == FRAME) {
				sb.append('|');
				firstInFrame = true;
			} else {
				if (!firstInFrame) {
					sb.append(',');
				}

				firstInFrame = false;
				sb.append(entry).append(':').append(stack[i + 1]);
			}
		}

		return sb.toString();
	}

	/**
	 * Возвращает содержимое текущего фрейма в виде Map для отладки и тестирования.
	 */
	@VisibleForTesting
	public Map<Symbol<?>, ?> toSymbolKeyedMap() {
		HashMap<Symbol<?>, Object> map = new HashMap<>();

		for (int i = stackTop; i > stackBottom; i -= ENTRY_SIZE) {
			map.put((Symbol<?>) stack[i], stack[i + 1]);
		}

		return map;
	}

	public boolean areFramesPlacedCorrectly() {
		for (int i = stackTop; i > 0; i--) {
			if (stack[i] == FRAME) {
				return false;
			}
		}

		if (stack[0] != FRAME) {
			throw new IllegalStateException("Corrupted stack");
		}

		return true;
	}

	private boolean isValid() {
		assert stackBottom >= 0;
		assert stackTop >= stackBottom;

		for (int i = 0; i <= stackTop; i += ENTRY_SIZE) {
			Object entry = stack[i];

			if (entry != FRAME && !(entry instanceof Symbol)) {
				return false;
			}
		}

		for (int bottom = stackBottom; bottom != 0; bottom = getPreviousStackBottom(bottom)) {
			if (stack[bottom] != FRAME) {
				return false;
			}
		}

		return true;
	}
}
