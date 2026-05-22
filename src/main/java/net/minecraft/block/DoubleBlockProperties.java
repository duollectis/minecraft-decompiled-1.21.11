package net.minecraft.block;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Утилитарный класс для работы с двойными блоками (сундуки, кровати и т.д.).
 * Предоставляет механизм получения свойств из одного или обоих блоков пары
 * через паттерн {@link PropertyRetriever}.
 */
public class DoubleBlockProperties {

	/**
	 * Создаёт {@link PropertySource} для двойного блока, определяя, является ли
	 * блок одиночным, первым или вторым в паре, и находя соответствующий блок-сосед.
	 * Возвращает fallback, если блок-сущность отсутствует или не прошёл проверку.
	 */
	public static <S extends BlockEntity> DoubleBlockProperties.PropertySource<S> toPropertySource(
			BlockEntityType<S> blockEntityType,
			Function<BlockState, DoubleBlockProperties.Type> typeMapper,
			Function<BlockState, Direction> directionMapper,
			Property<Direction> facingProperty,
			BlockState state,
			WorldAccess world,
			BlockPos pos,
			BiPredicate<WorldAccess, BlockPos> fallbackTester
	) {
		S blockEntity = blockEntityType.get(world, pos);

		if (blockEntity == null || fallbackTester.test(world, pos)) {
			return DoubleBlockProperties.PropertyRetriever::getFallback;
		}

		DoubleBlockProperties.Type type = typeMapper.apply(state);
		boolean isSingle = type == DoubleBlockProperties.Type.SINGLE;
		boolean isFirst = type == DoubleBlockProperties.Type.FIRST;

		if (isSingle) {
			return new DoubleBlockProperties.PropertySource.Single<>(blockEntity);
		}

		BlockPos neighborPos = pos.offset(directionMapper.apply(state));
		BlockState neighborState = world.getBlockState(neighborPos);

		if (neighborState.isOf(state.getBlock())) {
			DoubleBlockProperties.Type neighborType = typeMapper.apply(neighborState);

			if (neighborType != DoubleBlockProperties.Type.SINGLE
					&& type != neighborType
					&& neighborState.get(facingProperty) == state.get(facingProperty)
			) {
				if (fallbackTester.test(world, neighborPos)) {
					return DoubleBlockProperties.PropertyRetriever::getFallback;
				}

				S neighborEntity = blockEntityType.get(world, neighborPos);

				if (neighborEntity != null) {
					S primary = isFirst ? blockEntity : neighborEntity;
					S secondary = isFirst ? neighborEntity : blockEntity;

					return new DoubleBlockProperties.PropertySource.Pair<>(primary, secondary);
				}
			}
		}

		return new DoubleBlockProperties.PropertySource.Single<>(blockEntity);
	}

	public interface PropertyRetriever<S, T> {

		T getFromBoth(S first, S second);

		T getFrom(S single);

		T getFallback();
	}

	public interface PropertySource<S> {

		<T> T apply(DoubleBlockProperties.PropertyRetriever<? super S, T> retriever);

		public static final class Pair<S> implements DoubleBlockProperties.PropertySource<S> {

			private final S first;
			private final S second;

			public Pair(S first, S second) {
				this.first = first;
				this.second = second;
			}

			@Override
			public <T> T apply(DoubleBlockProperties.PropertyRetriever<? super S, T> propertyRetriever) {
				return propertyRetriever.getFromBoth(first, second);
			}
		}

		public static final class Single<S> implements DoubleBlockProperties.PropertySource<S> {

			private final S single;

			public Single(S single) {
				this.single = single;
			}

			@Override
			public <T> T apply(DoubleBlockProperties.PropertyRetriever<? super S, T> propertyRetriever) {
				return propertyRetriever.getFrom(single);
			}
		}
	}

	public enum Type {
		SINGLE,
		FIRST,
		SECOND;
	}
}
