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

/**
 * Идентификатор спрайта в текстурном атласе.
 * Хранит ссылку на атлас и конкретную текстуру внутри него,
 * а также кэширует {@link RenderLayer} для повторного использования.
 */
@Environment(EnvType.CLIENT)
public class SpriteIdentifier {

	public static final Comparator<SpriteIdentifier> COMPARATOR = Comparator
		.comparing(SpriteIdentifier::getAtlasId)
		.thenComparing(SpriteIdentifier::getTextureId);

	private final Identifier atlas;
	private final Identifier texture;
	private @Nullable RenderLayer layer;

	public SpriteIdentifier(Identifier atlas, Identifier texture) {
		this.atlas = atlas;
		this.texture = texture;
	}

	public Identifier getAtlasId() {
		return atlas;
	}

	public Identifier getTextureId() {
		return texture;
	}

	public RenderLayer getRenderLayer(Function<Identifier, RenderLayer> layerFactory) {
		if (layer == null) {
			layer = layerFactory.apply(atlas);
		}

		return layer;
	}

	public VertexConsumer getVertexConsumer(
		SpriteHolder spriteHolder,
		VertexConsumerProvider vertexConsumerProvider,
		Function<Identifier, RenderLayer> layerFactory
	) {
		return spriteHolder
			.getSprite(this)
			.getTextureSpecificVertexConsumer(vertexConsumerProvider.getBuffer(getRenderLayer(layerFactory)));
	}

	public VertexConsumer getVertexConsumer(
		SpriteHolder spriteHolder,
		VertexConsumerProvider vertexConsumerProvider,
		Function<Identifier, RenderLayer> layerFactory,
		boolean glint,
		boolean foil
	) {
		return spriteHolder.getSprite(this)
			.getTextureSpecificVertexConsumer(ItemRenderer.getItemGlintConsumer(
				vertexConsumerProvider,
				getRenderLayer(layerFactory),
				glint,
				foil
			));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SpriteIdentifier other = (SpriteIdentifier) o;
		return atlas.equals(other.atlas) && texture.equals(other.texture);
	}

	@Override
	public int hashCode() {
		return Objects.hash(atlas, texture);
	}

	@Override
	public String toString() {
		return "Material{atlasLocation=" + atlas + ", texture=" + texture + "}";
	}
}
