package net.minecraft.client.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code DataCache}.
 */
public class DataCache<C extends DataCache.CacheContext<C>, D> {

	private final Function<C, D> dataFunction;
	private @Nullable C context;
	private @Nullable D data;

	public DataCache(Function<C, D> dataFunction) {
		this.dataFunction = dataFunction;
	}

	public D compute(C context) {
		if (context == this.context && this.data != null) {
			return this.data;
		}
		else {
			D object = this.dataFunction.apply(context);
			this.data = object;
			this.context = context;
			context.registerForCleaning(this);
			return object;
		}
	}

	public void clean() {
		this.data = null;
		this.context = null;
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code CacheContext}.
	 */
	public interface CacheContext<C extends DataCache.CacheContext<C>> {

		void registerForCleaning(DataCache<C, ?> dataCache);
	}
}
