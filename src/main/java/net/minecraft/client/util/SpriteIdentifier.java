package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code SpriteIdentifier}.
 */
public class SpriteIdentifier {

	public static final Comparator<SpriteIdentifier> COMPARATOR = Comparator.comparing(SpriteIdentifier::getAtlasId)
	                                                                        .thenComparing(SpriteIdentifier::getTextureId);
	private final Identifier atlas;
	private final Identifier texture;
	private @Nullable RenderLayer layer;

	public SpriteIdentifier(Identifier atlas, Identifier texture) {
		this.atlas = atlas;
		this.texture = texture;
	}

	public Identifier getAtlasId() {
		return this.atlas;
	}

	public Identifier getTextureId() {
		return this.texture;
	}

	public RenderLayer getRenderLayer(Function<Identifier, RenderLayer> layerFactory) {
		if (this.layer == null) {
			this.layer = layerFactory.apply(this.atlas);
		}

		return this.layer;
	}

	public VertexConsumer getVertexConsumer(
			SpriteHolder spriteHolder,
			VertexConsumerProvider vertexConsumerProvider,
			Function<Identifier, RenderLayer> function
	) {
		return spriteHolder
				.getSprite(this)
				.getTextureSpecificVertexConsumer(vertexConsumerProvider.getBuffer(this.getRenderLayer(function)));
	}

	public VertexConsumer getVertexConsumer(
			SpriteHolder spriteHolder,
			VertexConsumerProvider vertexConsumerProvider,
			Function<Identifier, RenderLayer> function,
			boolean bl,
			boolean bl2
	) {
		return spriteHolder.getSprite(this)
		                   .getTextureSpecificVertexConsumer(ItemRenderer.getItemGlintConsumer(
				                   vertexConsumerProvider,
				                   this.getRenderLayer(function),
				                   bl,
				                   bl2
		                   ));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else if (o != null && this.getClass() == o.getClass()) {
			SpriteIdentifier spriteIdentifier = (SpriteIdentifier) o;
			return this.atlas.equals(spriteIdentifier.atlas) && this.texture.equals(spriteIdentifier.texture);
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.atlas, this.texture);
	}

	@Override
	public String toString() {
		return "Material{atlasLocation=" + this.atlas + ", texture=" + this.texture + "}";
	}
}
