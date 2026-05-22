package net.minecraft.util.dynamic;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;

import java.util.function.UnaryOperator;

/**
 * Базовая реализация {@link ListBuilder} для {@link DynamicOps}.
 * Накапливает элементы в промежуточном буфере типа {@code B},
 * объединяя ошибки через {@link DataResult}.
 *
 * @param <T> тип сериализованных данных
 * @param <B> тип внутреннего буфера
 */
abstract class AbstractListBuilder<T, B> implements ListBuilder<T> {

	private final DynamicOps<T> ops;
	protected DataResult<B> builder = DataResult.success(initBuilder(), Lifecycle.stable());

	protected AbstractListBuilder(DynamicOps<T> ops) {
		this.ops = ops;
	}

	@Override
	public DynamicOps<T> ops() {
		return ops;
	}

	protected abstract B initBuilder();

	protected abstract B add(B builder, T value);

	protected abstract DataResult<T> build(B builder, T prefix);

	@Override
	public ListBuilder<T> add(T value) {
		builder = builder.map(buf -> add(buf, value));
		return this;
	}

	@Override
	public ListBuilder<T> add(DataResult<T> value) {
		builder = builder.apply2stable(this::add, value);
		return this;
	}

	@Override
	public ListBuilder<T> withErrorsFrom(DataResult<?> result) {
		builder = builder.flatMap(buf -> result.map(ignored -> buf));
		return this;
	}

	@Override
	public ListBuilder<T> mapError(UnaryOperator<String> onError) {
		builder = builder.mapError(onError);
		return this;
	}

	@Override
	public DataResult<T> build(T prefix) {
		DataResult<T> result = builder.flatMap(buf -> build(buf, prefix));
		builder = DataResult.success(initBuilder(), Lifecycle.stable());
		return result;
	}
}
