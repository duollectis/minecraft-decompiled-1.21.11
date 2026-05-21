package net.minecraft.client.render.model;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelVariant;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.random.Random;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code SimpleBlockStateModel}.
 */
public class SimpleBlockStateModel implements BlockStateModel {

	private final BlockModelPart part;

	public SimpleBlockStateModel(BlockModelPart part) {
		this.part = part;
	}

	@Override
	public void addParts(Random random, List<BlockModelPart> parts) {
		parts.add(this.part);
	}

	@Override
	public Sprite particleSprite() {
		return this.part.particleSprite();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked(ModelVariant variant) implements BlockStateModel.Unbaked {

		public static final Codec<SimpleBlockStateModel.Unbaked> CODEC = ModelVariant.CODEC
				.xmap(SimpleBlockStateModel.Unbaked::new, SimpleBlockStateModel.Unbaked::variant);

		@Override
		public BlockStateModel bake(Baker baker) {
			return new SimpleBlockStateModel(this.variant.bake(baker));
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			this.variant.resolve(resolver);
		}
	}
}
