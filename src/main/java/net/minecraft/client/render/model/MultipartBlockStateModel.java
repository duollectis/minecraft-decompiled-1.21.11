package net.minecraft.client.render.model;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Модель состояния блока, составленная из нескольких частей (multipart).
 * Каждая часть имеет условие {@link Selector#condition()} — предикат над {@link BlockState}.
 * При рендеринге добавляются только те части, чьё условие выполняется для текущего состояния.
 */
@Environment(EnvType.CLIENT)
public class MultipartBlockStateModel implements BlockStateModel {

	private final MultipartBlockStateModel.MultipartBakedModel bakedModels;
	private final BlockState state;
	private @Nullable List<BlockStateModel> models;

	MultipartBlockStateModel(MultipartBlockStateModel.MultipartBakedModel bakedModels, BlockState state) {
		this.bakedModels = bakedModels;
		this.state = state;
	}

	@Override
	public Sprite particleSprite() {
		return bakedModels.particleSprite;
	}

	@Override
	public void addParts(Random random, List<BlockModelPart> parts) {
		if (models == null) {
			models = bakedModels.build(state);
		}

		long seed = random.nextLong();

		for (BlockStateModel model : models) {
			random.setSeed(seed);
			model.addParts(random, parts);
		}
	}

	/**
	 * Запечённая мультипарт-модель: хранит список селекторов и кэш результатов
	 * фильтрации по {@link BitSet} активных условий для каждого состояния блока.
	 */
	@Environment(EnvType.CLIENT)
	static final class MultipartBakedModel {

		private final List<MultipartBlockStateModel.Selector<BlockStateModel>> selectors;
		final Sprite particleSprite;
		private final Map<BitSet, List<BlockStateModel>> cache = new ConcurrentHashMap<>();

		private static BlockStateModel getFirst(List<MultipartBlockStateModel.Selector<BlockStateModel>> selectors) {
			if (selectors.isEmpty()) {
				throw new IllegalArgumentException("Model must have at least one selector");
			}

			return selectors.getFirst().model();
		}

		public MultipartBakedModel(List<MultipartBlockStateModel.Selector<BlockStateModel>> selectors) {
			this.selectors = selectors;
			particleSprite = getFirst(selectors).particleSprite();
		}

		public List<BlockStateModel> build(BlockState state) {
			BitSet activeBits = new BitSet();

			for (int i = 0; i < selectors.size(); i++) {
				if (selectors.get(i).condition().test(state)) {
					activeBits.set(i);
				}
			}

			return cache.computeIfAbsent(activeBits, bits -> {
				ImmutableList.Builder<BlockStateModel> builder = ImmutableList.builder();

				for (int i = 0; i < selectors.size(); i++) {
					if (bits.get(i)) {
						builder.add(selectors.get(i).model());
					}
				}

				return builder.build();
			});
		}
	}

	/**
	 * Незапечённая мультипарт-модель: запекает каждый селектор через {@link Baker}
	 * и кэширует результат через {@link Baker.ResolvableCacheKey}.
	 */
	@Environment(EnvType.CLIENT)
	public static class MultipartUnbaked implements BlockStateModel.UnbakedGrouped {

		final List<MultipartBlockStateModel.Selector<BlockStateModel.Unbaked>> selectors;

		private final Baker.ResolvableCacheKey<MultipartBlockStateModel.MultipartBakedModel> bakerCache;

		public MultipartUnbaked(List<MultipartBlockStateModel.Selector<BlockStateModel.Unbaked>> selectors) {
			this.selectors = selectors;
			this.bakerCache = baker -> {
				ImmutableList.Builder<MultipartBlockStateModel.Selector<BlockStateModel>> builder =
						ImmutableList.builderWithExpectedSize(selectors.size());

				for (MultipartBlockStateModel.Selector<BlockStateModel.Unbaked> selector : selectors) {
					builder.add(selector.build(selector.model().bake(baker)));
				}

				return new MultipartBlockStateModel.MultipartBakedModel(builder.build());
			};
		}

		@Override
		public Object getEqualityGroup(BlockState state) {
			IntList activeIndices = new IntArrayList();

			for (int i = 0; i < selectors.size(); i++) {
				if (selectors.get(i).condition().test(state)) {
					activeIndices.add(i);
				}
			}

			@Environment(EnvType.CLIENT)
			record EqualityGroup(MultipartBlockStateModel.MultipartUnbaked model, IntList selectors) {
			}

			return new EqualityGroup(this, activeIndices);
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			selectors.forEach(selector -> selector.model().resolve(resolver));
		}

		@Override
		public BlockStateModel bake(BlockState state, Baker baker) {
			return new MultipartBlockStateModel(baker.compute(bakerCache), state);
		}
	}

	/**
	 * Пара условие + модель для мультипарт-блока.
	 * Метод {@link #build(Object)} создаёт новый селектор с тем же условием, но другой моделью.
	 */
	@Environment(EnvType.CLIENT)
	public record Selector<T>(Predicate<BlockState> condition, T model) {

		public <S> MultipartBlockStateModel.Selector<S> build(S newModel) {
			return new MultipartBlockStateModel.Selector<>(condition, newModel);
		}
	}
}
