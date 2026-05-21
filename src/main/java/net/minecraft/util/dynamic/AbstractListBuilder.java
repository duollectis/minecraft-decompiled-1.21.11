package net.minecraft.util.dynamic;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;

import java.util.function.UnaryOperator;

/**
 * {@code AbstractListBuilder}.
 */
abstract class AbstractListBuilder<T, B> implements ListBuilder<T> {

	private final DynamicOps<T> ops;
	protected DataResult<B> builder = DataResult.success(this.initBuilder(), Lifecycle.stable());

	protected AbstractListBuilder(DynamicOps<T> ops) {
		this.ops = ops;
	}

	/**
	 * Ops.
	 *
	 * @return DynamicOps — результат операции
	 */
	public DynamicOps<T> ops() {
		return this.ops;
	}

	/**
	 * Инициализирует builder.
	 *
	 * @return B — результат операции
	 */
	protected abstract B initBuilder();

	/**
	 * Add.
	 *
	 * @param builder builder
	 * @param value value
	 *
	 * @return B — результат операции
	 */
	protected abstract B add(B builder, T value);

	/**
	 * Build.
	 *
	 * @param builder builder
	 * @param prefix prefix
	 *
	 * @return DataResult — результат операции
	 */
	protected abstract DataResult<T> build(B builder, T prefix);

	/**
	 * Add.
	 *
	 * @param value value
	 *
	 * @return ListBuilder — результат операции
	 */
	public ListBuilder<T> add(T value) {
		this.builder = this.builder.map(object2 -> this.add((B) object2, value));
		return this;
	}

	/**
	 * Add.
	 *
	 * @param value value
	 *
	 * @return ListBuilder — результат операции
	 */
	public ListBuilder<T> add(DataResult<T> value) {
		this.builder = this.builder.apply2stable(this::add, value);
		return this;
	}

	/**
	 * With errors from.
	 *
	 * @param result result
	 *
	 * @return ListBuilder — результат операции
	 */
	public ListBuilder<T> withErrorsFrom(DataResult<?> result) {
		this.builder = this.builder.flatMap(object -> result.map(object2 -> object));
		return this;
	}

	/**
	 * Map error.
	 *
	 * @param onError on error
	 *
	 * @return ListBuilder — результат операции
	 */
	public ListBuilder<T> mapError(UnaryOperator<String> onError) {
		this.builder = this.builder.mapError(onError);
		return this;
	}

	/**
	 * Build.
	 *
	 * @param prefix prefix
	 *
	 * @return DataResult — результат операции
	 */
	public DataResult<T> build(T prefix) {
		DataResult<T> dataResult = this.builder.flatMap(object2 -> this.build((B) object2, prefix));
		this.builder = DataResult.success(this.initBuilder(), Lifecycle.stable());
		return dataResult;
	}
}
