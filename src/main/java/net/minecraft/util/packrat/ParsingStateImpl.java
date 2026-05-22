package net.minecraft.util.packrat;

import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Базовая реализация {@link ParsingState}, обеспечивающая мемоизацию результатов разбора
 * (packrat-кэш) и стек отсечений (cut). Мемоизация гарантирует, что каждое правило
 * вычисляется не более одного раза для каждой позиции курсора, что даёт линейную
 * сложность разбора PEG-грамматик.
 */
public abstract class ParsingStateImpl<S> implements ParsingState<S> {

	private ParsingStateImpl.@Nullable MemoizedData[] memoStack = new ParsingStateImpl.MemoizedData[256];
	private final ParseErrorList<S> errors;
	private final ParseResults results = new ParseResults();
	private ParsingStateImpl.@Nullable Cutter[] cutters = new ParsingStateImpl.Cutter[16];
	private int topCutterIndex;
	private final ParsingStateImpl<S>.ErrorSuppressing errorSuppressingState = new ParsingStateImpl.ErrorSuppressing();

	protected ParsingStateImpl(ParseErrorList<S> errors) {
		this.errors = errors;
	}

	@Override
	public ParseResults getResults() {
		return results;
	}

	@Override
	public ParseErrorList<S> getErrors() {
		return errors;
	}

	/**
	 * Выполняет разбор по заданному правилу с мемоизацией результата.
	 * Если результат для текущей позиции курсора уже вычислен — возвращает его из кэша,
	 * не вызывая правило повторно.
	 */
	@Override
	public <T> @Nullable T parse(ParsingRuleEntry<S, T> rule) {
		int cursor = getCursor();
		ParsingStateImpl.MemoizedData memoizedData = pushMemoizedData(cursor);
		int slotIndex = memoizedData.get(rule.getSymbol());

		if (slotIndex != -1) {
			ParsingStateImpl.MemoizedValue<T> cached = memoizedData.get(slotIndex);

			if (cached != null) {
				if (cached == ParsingStateImpl.MemoizedValue.EMPTY) {
					return null;
				}

				setCursor(cached.markAfterParse());
				return cached.value();
			}
		} else {
			slotIndex = memoizedData.push(rule.getSymbol());
		}

		T parsed = rule.getRule().parse(this);
		ParsingStateImpl.MemoizedValue<T> memoValue = parsed == null
				? ParsingStateImpl.MemoizedValue.empty()
				: new ParsingStateImpl.MemoizedValue<>(parsed, getCursor());

		memoizedData.put(slotIndex, memoValue);
		return parsed;
	}

	private ParsingStateImpl.MemoizedData pushMemoizedData(int cursor) {
		int currentLength = memoStack.length;

		if (cursor >= currentLength) {
			int newLength = Util.nextCapacity(currentLength, cursor + 1);
			ParsingStateImpl.MemoizedData[] expanded = new ParsingStateImpl.MemoizedData[newLength];
			System.arraycopy(memoStack, 0, expanded, 0, currentLength);
			memoStack = expanded;
		}

		ParsingStateImpl.MemoizedData slot = memoStack[cursor];

		if (slot == null) {
			slot = new ParsingStateImpl.MemoizedData();
			memoStack[cursor] = slot;
		}

		return slot;
	}

	@Override
	public Cut pushCutter() {
		int currentLength = cutters.length;

		if (topCutterIndex >= currentLength) {
			int newLength = Util.nextCapacity(currentLength, topCutterIndex + 1);
			ParsingStateImpl.Cutter[] expanded = new ParsingStateImpl.Cutter[newLength];
			System.arraycopy(cutters, 0, expanded, 0, currentLength);
			cutters = expanded;
		}

		int index = topCutterIndex++;
		ParsingStateImpl.Cutter cutter = cutters[index];

		if (cutter == null) {
			cutter = new ParsingStateImpl.Cutter();
			cutters[index] = cutter;
		} else {
			cutter.reset();
		}

		return cutter;
	}

	@Override
	public void popCutter() {
		topCutterIndex--;
	}

	@Override
	public ParsingState<S> getErrorSuppressingState() {
		return errorSuppressingState;
	}

	/**
	 * Изменяемый флаг отсечения (cut). Переиспользуется через {@link #reset()},
	 * чтобы избежать лишних аллокаций при каждом вызове {@code anyOf}.
	 */
	static class Cutter implements Cut {

		private boolean cut;

		@Override
		public void cut() {
			cut = true;
		}

		@Override
		public boolean isCut() {
			return cut;
		}

		public void reset() {
			cut = false;
		}
	}

	/**
	 * Делегирующая обёртка над внешним состоянием, которая подавляет накопление ошибок.
	 * Используется в lookahead-термах, где неудача не должна загрязнять список ошибок.
	 */
	class ErrorSuppressing implements ParsingState<S> {

		private final ParseErrorList<S> errors = new ParseErrorList.Noop<>();

		@Override
		public ParseErrorList<S> getErrors() {
			return errors;
		}

		@Override
		public ParseResults getResults() {
			return ParsingStateImpl.this.getResults();
		}

		@Override
		public <T> @Nullable T parse(ParsingRuleEntry<S, T> rule) {
			return ParsingStateImpl.this.parse(rule);
		}

		@Override
		public S getReader() {
			return ParsingStateImpl.this.getReader();
		}

		@Override
		public int getCursor() {
			return ParsingStateImpl.this.getCursor();
		}

		@Override
		public void setCursor(int cursor) {
			ParsingStateImpl.this.setCursor(cursor);
		}

		@Override
		public Cut pushCutter() {
			return ParsingStateImpl.this.pushCutter();
		}

		@Override
		public void popCutter() {
			ParsingStateImpl.this.popCutter();
		}

		@Override
		public ParsingState<S> getErrorSuppressingState() {
			return this;
		}
	}

	/**
	 * Плоский массив-кэш для одной позиции курсора. Хранит пары (Symbol, MemoizedValue)
	 * в чередующихся слотах: чётный индекс — символ, нечётный — значение.
	 * Линейный поиск оправдан, так как количество правил на позицию невелико.
	 */
	static class MemoizedData {

		public static final int SIZE_PER_SYMBOL = 2;
		private static final int MISSING = -1;
		private Object[] values = new Object[16];
		private int top;

		public int get(Symbol<?> symbol) {
			for (int i = 0; i < top; i += 2) {
				if (values[i] == symbol) {
					return i;
				}
			}

			return MISSING;
		}

		public int push(Symbol<?> symbol) {
			int slotIndex = top;
			top += 2;
			int valueSlot = slotIndex + 1;
			int currentLength = values.length;

			if (valueSlot >= currentLength) {
				int newLength = Util.nextCapacity(currentLength, valueSlot + 1);
				Object[] expanded = new Object[newLength];
				System.arraycopy(values, 0, expanded, 0, currentLength);
				values = expanded;
			}

			values[slotIndex] = symbol;
			return slotIndex;
		}

		public <T> ParsingStateImpl.@Nullable MemoizedValue<T> get(int index) {
			return (ParsingStateImpl.MemoizedValue<T>) values[index + 1];
		}

		public void put(int index, ParsingStateImpl.MemoizedValue<?> value) {
			values[index + 1] = value;
		}
	}

	/**
	 * Иммутабельный контейнер для мемоизированного результата разбора.
	 * {@link #EMPTY} — сентинел для неудачного разбора (null-результат).
	 */
	record MemoizedValue<T>(@Nullable T value, int markAfterParse) {

		public static final ParsingStateImpl.MemoizedValue<?> EMPTY = new ParsingStateImpl.MemoizedValue<>(null, -1);

		public static <T> ParsingStateImpl.MemoizedValue<T> empty() {
			return (ParsingStateImpl.MemoizedValue<T>) EMPTY;
		}
	}
}
