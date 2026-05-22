package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.ShieldEntityModel;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Unit;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Рендерер щита как предмета инвентаря.
 * Если щит имеет баннерный узор или цвет основания — рисует полотно с паттернами через
 * {@link BannerBlockEntityRenderer#renderCanvas}; иначе рисует пластину без узора.
 */
@Environment(EnvType.CLIENT)
public class ShieldModelRenderer implements SpecialModelRenderer<ComponentMap> {

	private static final int NO_TINT = -1;

	private final SpriteHolder spriteHolder;
	private final ShieldEntityModel model;

	public ShieldModelRenderer(SpriteHolder spriteHolder, ShieldEntityModel model) {
		this.spriteHolder = spriteHolder;
		this.model = model;
	}

	public @Nullable ComponentMap getData(ItemStack itemStack) {
		return itemStack.getImmutableComponents();
	}

	public void render(
			@Nullable ComponentMap components,
			ItemDisplayContext displayContext,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			boolean glint,
			int seed
	) {
		BannerPatternsComponent patterns = components != null
				? components.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT)
				: BannerPatternsComponent.DEFAULT;
		DyeColor baseColor = components != null ? components.get(DataComponentTypes.BASE_COLOR) : null;
		boolean hasDecoration = !patterns.layers().isEmpty() || baseColor != null;

		matrices.push();
		matrices.scale(1.0F, -1.0F, -1.0F);

		SpriteIdentifier spriteId = hasDecoration ? ModelBaker.SHIELD_BASE : ModelBaker.SHIELD_BASE_NO_PATTERN;

		queue.submitModelPart(
				model.getHandle(),
				matrices,
				model.getLayer(spriteId.getAtlasId()),
				light,
				overlay,
				spriteHolder.getSprite(spriteId),
				false,
				false,
				NO_TINT,
				null,
				seed
		);

		if (hasDecoration) {
			BannerBlockEntityRenderer.renderCanvas(
					spriteHolder,
					matrices,
					queue,
					light,
					overlay,
					model,
					Unit.INSTANCE,
					spriteId,
					false,
					Objects.requireNonNullElse(baseColor, DyeColor.WHITE),
					patterns,
					glint,
					null,
					seed
			);
		} else {
			queue.submitModelPart(
					model.getPlate(),
					matrices,
					model.getLayer(spriteId.getAtlasId()),
					light,
					overlay,
					spriteHolder.getSprite(spriteId),
					false,
					glint,
					NO_TINT,
					null,
					seed
			);
		}

		matrices.pop();
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrices = new MatrixStack();
		matrices.scale(1.0F, -1.0F, -1.0F);
		model.getRootPart().collectVertices(matrices, consumer);
	}

	/**
	 * Несериализованный дескриптор рендерера щита. Не содержит параметров.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked() implements SpecialModelRenderer.Unbaked {

		public static final ShieldModelRenderer.Unbaked INSTANCE = new ShieldModelRenderer.Unbaked();
		public static final MapCodec<ShieldModelRenderer.Unbaked> CODEC = MapCodec.unit(INSTANCE);

		@Override
		public MapCodec<ShieldModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			return new ShieldModelRenderer(
					context.spriteHolder(),
					new ShieldEntityModel(context.entityModelSet().getModelPart(EntityModelLayers.SHIELD))
			);
		}
	}
}
