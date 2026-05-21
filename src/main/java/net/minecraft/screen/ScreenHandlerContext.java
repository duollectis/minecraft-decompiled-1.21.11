package net.minecraft.screen;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * {@code ScreenHandlerContext}.
 */
public interface ScreenHandlerContext {

	ScreenHandlerContext EMPTY = new ScreenHandlerContext() {
		@Override
		public <T> Optional<T> get(BiFunction<World, BlockPos, T> getter) {
			return Optional.empty();
		}
	};

	static ScreenHandlerContext create(World world, BlockPos pos) {
		return new ScreenHandlerContext() {
			@Override
			public <T> Optional<T> get(BiFunction<World, BlockPos, T> getter) {
				return Optional.of(getter.apply(world, pos));
			}
		};
	}

	<T> Optional<T> get(BiFunction<World, BlockPos, T> getter);

	default <T> T get(BiFunction<World, BlockPos, T> getter, T defaultValue) {
		return this.get(getter).orElse(defaultValue);
	}

	default void run(BiConsumer<World, BlockPos> function) {
		this.get((world, pos) -> {
			function.accept(world, pos);
			return Optional.empty();
		});
	}
}
