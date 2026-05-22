package net.minecraft.client.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Однозначный кэш вычисленного значения, привязанный к конкретному контексту.
 *
 * <p>Хранит последний вычисленный результат {@code data} вместе с контекстом {@code context},
 * для которого он был получен. При повторном вызове {@link #compute} с тем же контекстом
 * возвращает кэшированное значение без повторного вычисления.
 * При смене контекста или вызове {@link #clean} кэш сбрасывается.
 *
 * @param <C> тип контекста, реализующий {@link CacheContext}
 * @param <D> тип кэшируемых данных
 */
@Environment(EnvType.CLIENT)
public class DataCache<C extends DataCache.CacheContext<C>, D> {

	private final Function<C, D> dataFunction;
	private @Nullable C context;
	private @Nullable D data;

	public DataCache(Function<C, D> dataFunction) {
		this.dataFunction = dataFunction;
	}

	/**
	 * Возвращает кэшированное значение для {@code context}, либо вычисляет новое.
	 * При вычислении регистрирует себя в контексте для последующей очистки.
	 */
	public D compute(C context) {
		if (context == this.context && data != null) {
			return data;
		}

		D computed = dataFunction.apply(context);
		data = computed;
		this.context = context;
		context.registerForCleaning(this);
		return computed;
	}

	public void clean() {
		data = null;
		context = null;
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface CacheContext<C extends CacheContext<C>> {

		void registerForCleaning(DataCache<C, ?> dataCache);
	}
}
