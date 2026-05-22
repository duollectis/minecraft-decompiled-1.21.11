package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.BedBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Специализированный рендерер кровати как предмета.
 * Делегирует рендер {@link BedBlockEntityRenderer}, используя текстуру,
 * определённую идентификатором спрайта. Поддерживает все 16 цветов кровати
 * через вспомогательный конструктор {@link Unbaked#Unbaked(DyeColor)}.
 */
@Environment(EnvType.CLIENT)
public class BedModelRenderer implements SimpleSpecialModelRenderer {

	private final BedBlockEntityRenderer blockEntityRenderer;
	private final SpriteIdentifier textureId;

	public BedModelRenderer(BedBlockEntityRenderer blockEntityRenderer, SpriteIdentifier textureId) {
		this.blockEntityRenderer = blockEntityRenderer;
		this.textureId = textureId;
	}

	@Override
	public void render(
			ItemDisplayContext displayContext,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			boolean glint,
			int seed
	) {
		blockEntityRenderer.renderAsItem(matrices, queue, light, overlay, textureId, seed);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		blockEntityRenderer.collectVertices(consumer);
	}

	/**
	 * Несериализованная форма рендерера кровати.
	 * Хранит идентификатор текстуры кровати.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked(Identifier texture) implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<BedModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(Identifier.CODEC.fieldOf("texture").forGetter(BedModelRenderer.Unbaked::texture))
						.apply(instance, BedModelRenderer.Unbaked::new)
		);

		public Unbaked(DyeColor color) {
			this(TexturedRenderLayers.createColorId(color));
		}

		@Override
		public MapCodec<BedModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			return new BedModelRenderer(
					new BedBlockEntityRenderer(context),
					TexturedRenderLayers.BED_SPRITE_MAPPER.map(texture)
			);
		}
	}
}
