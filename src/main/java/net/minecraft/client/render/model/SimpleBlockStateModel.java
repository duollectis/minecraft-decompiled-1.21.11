package net.minecraft.client.render.model;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelVariant;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Простая модель состояния блока с единственной частью {@link BlockModelPart}.
 * Используется для блоков с одним вариантом отображения без весовой выборки.
 */
@Environment(EnvType.CLIENT)
public class SimpleBlockStateModel implements BlockStateModel {

	private final BlockModelPart part;

	public SimpleBlockStateModel(BlockModelPart part) {
		this.part = part;
	}

	@Override
	public void addParts(Random random, List<BlockModelPart> parts) {
		parts.add(part);
	}

	@Override
	public Sprite particleSprite() {
		return part.particleSprite();
	}

	/**
	 * Незапечённый вариант простой модели состояния блока.
	 * Хранит единственный {@link ModelVariant} и запекает его через {@link Baker}.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked(ModelVariant variant) implements BlockStateModel.Unbaked {

		public static final Codec<SimpleBlockStateModel.Unbaked> CODEC = ModelVariant.CODEC
				.xmap(SimpleBlockStateModel.Unbaked::new, SimpleBlockStateModel.Unbaked::variant);

		@Override
		public BlockStateModel bake(Baker baker) {
			return new SimpleBlockStateModel(variant.bake(baker));
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			variant.resolve(resolver);
		}
	}
}
