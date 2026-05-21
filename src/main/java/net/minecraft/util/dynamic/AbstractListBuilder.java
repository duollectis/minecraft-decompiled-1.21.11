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

	public DynamicOps<T> ops() {
		return this.ops;
	}

	protected abstract B initBuilder();

	protected abstract B add(B builder, T value);

	protected abstract DataResult<T> build(B builder, T prefix);

	public ListBuilder<T> add(T value) {
		this.builder = this.builder.map(object2 -> this.add((B) object2, value));
		return this;
	}

	public ListBuilder<T> add(DataResult<T> value) {
		this.builder = this.builder.apply2stable(this::add, value);
		return this;
	}

	public ListBuilder<T> withErrorsFrom(DataResult<?> result) {
		this.builder = this.builder.flatMap(object -> result.map(object2 -> object));
		return this;
	}

	public ListBuilder<T> mapError(UnaryOperator<String> onError) {
		this.builder = this.builder.mapError(onError);
		return this;
	}

	public DataResult<T> build(T prefix) {
		DataResult<T> dataResult = this.builder.flatMap(object2 -> this.build((B) object2, prefix));
		this.builder = DataResult.success(this.initBuilder(), Lifecycle.stable());
		return dataResult;
	}
}
