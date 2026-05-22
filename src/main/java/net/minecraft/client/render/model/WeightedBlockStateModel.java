package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Модель состояния блока с весовой выборкой варианта отображения.
 * При каждом вызове {@link #addParts} выбирает один из вариантов из {@link Pool}
 * пропорционально их весам, используя переданный генератор случайных чисел.
 */
@Environment(EnvType.CLIENT)
public class WeightedBlockStateModel implements BlockStateModel {

	private final Pool<BlockStateModel> models;
	private final Sprite particleSprite;

	public WeightedBlockStateModel(Pool<BlockStateModel> models) {
		this.models = models;
		this.particleSprite = models.getEntries().getFirst().value().particleSprite();
	}

	@Override
	public Sprite particleSprite() {
		return particleSprite;
	}

	@Override
	public void addParts(Random random, List<BlockModelPart> parts) {
		models.get(random).addParts(random, parts);
	}

	/**
	 * Незапечённый вариант взвешенной модели состояния блока.
	 * Запекает каждый вариант из пула через {@link Baker} и собирает новый {@link Pool}.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked(Pool<BlockStateModel.Unbaked> entries) implements BlockStateModel.Unbaked {

		@Override
		public BlockStateModel bake(Baker baker) {
			return new WeightedBlockStateModel(entries.transform(model -> model.bake(baker)));
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			entries.getEntries().forEach(entry -> entry.value().resolve(resolver));
		}
	}
}
