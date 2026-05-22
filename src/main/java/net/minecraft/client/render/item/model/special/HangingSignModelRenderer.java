package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.WoodType;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.HangingSignBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import org.joml.Vector3fc;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Специализированный рендерер подвесной таблички как предмета.
 * Использует модель типа {@link HangingSignBlockEntityRenderer.AttachmentType#CEILING_MIDDLE}
 * для корректного отображения в инвентаре. Текстура может быть переопределена
 * через опциональный идентификатор, иначе используется стандартная текстура типа дерева.
 */
@Environment(EnvType.CLIENT)
public class HangingSignModelRenderer implements SimpleSpecialModelRenderer {

	private static final float SCALE_FLIP_Y = -1.0F;
	private static final float SCALE_FLIP_Z = -1.0F;

	private final SpriteHolder spriteHolder;
	private final Model.SinglePartModel model;
	private final SpriteIdentifier texture;

	public HangingSignModelRenderer(SpriteHolder spriteHolder, Model.SinglePartModel model, SpriteIdentifier texture) {
		this.spriteHolder = spriteHolder;
		this.model = model;
		this.texture = texture;
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
		HangingSignBlockEntityRenderer.renderAsItem(spriteHolder, matrices, queue, light, overlay, model, texture);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrices = new MatrixStack();
		HangingSignBlockEntityRenderer.setAngles(matrices, 0.0F);
		matrices.scale(1.0F, SCALE_FLIP_Y, SCALE_FLIP_Z);
		model.getRootPart().collectVertices(matrices, consumer);
	}

	/**
	 * Несериализованная форма рендерера подвесной таблички.
	 * Хранит тип дерева и опциональный идентификатор текстуры.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked(WoodType woodType, Optional<Identifier> texture) implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<HangingSignModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						WoodType.CODEC.fieldOf("wood_type").forGetter(HangingSignModelRenderer.Unbaked::woodType),
						Identifier.CODEC.optionalFieldOf("texture").forGetter(HangingSignModelRenderer.Unbaked::texture)
				).apply(instance, HangingSignModelRenderer.Unbaked::new)
		);

		public Unbaked(WoodType woodType) {
			this(woodType, Optional.empty());
		}

		@Override
		public MapCodec<HangingSignModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			Model.SinglePartModel signModel = HangingSignBlockEntityRenderer.createModel(
					context.entityModelSet(),
					woodType,
					HangingSignBlockEntityRenderer.AttachmentType.CEILING_MIDDLE
			);
			SpriteIdentifier spriteId = texture
					.map(TexturedRenderLayers.HANGING_SIGN_SPRITE_MAPPER::map)
					.orElseGet(() -> TexturedRenderLayers.getHangingSignTextureId(woodType));

			return new HangingSignModelRenderer(context.spriteHolder(), signModel, spriteId);
		}
	}
}
