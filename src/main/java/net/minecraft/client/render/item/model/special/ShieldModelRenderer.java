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

@Environment(EnvType.CLIENT)
/**
 * {@code ShieldModelRenderer}.
 */
public class ShieldModelRenderer implements SpecialModelRenderer<ComponentMap> {

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
			@Nullable ComponentMap componentMap,
			ItemDisplayContext itemDisplayContext,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			int j,
			boolean bl,
			int k
	) {
		BannerPatternsComponent bannerPatternsComponent = componentMap != null
		                                                  ? componentMap.getOrDefault(
				DataComponentTypes.BANNER_PATTERNS,
				BannerPatternsComponent.DEFAULT
		)
		                                                  : BannerPatternsComponent.DEFAULT;
		DyeColor dyeColor = componentMap != null ? componentMap.get(DataComponentTypes.BASE_COLOR) : null;
		boolean bl2 = !bannerPatternsComponent.layers().isEmpty() || dyeColor != null;
		matrixStack.push();
		matrixStack.scale(1.0F, -1.0F, -1.0F);
		SpriteIdentifier spriteIdentifier = bl2 ? ModelBaker.SHIELD_BASE : ModelBaker.SHIELD_BASE_NO_PATTERN;
		orderedRenderCommandQueue.submitModelPart(
				this.model.getHandle(),
				matrixStack,
				this.model.getLayer(spriteIdentifier.getAtlasId()),
				i,
				j,
				this.spriteHolder.getSprite(spriteIdentifier),
				false,
				false,
				-1,
				null,
				k
		);
		if (bl2) {
			BannerBlockEntityRenderer.renderCanvas(
					this.spriteHolder,
					matrixStack,
					orderedRenderCommandQueue,
					i,
					j,
					this.model,
					Unit.INSTANCE,
					spriteIdentifier,
					false,
					Objects.requireNonNullElse(dyeColor, DyeColor.WHITE),
					bannerPatternsComponent,
					bl,
					null,
					k
			);
		}
		else {
			orderedRenderCommandQueue.submitModelPart(
					this.model.getPlate(),
					matrixStack,
					this.model.getLayer(spriteIdentifier.getAtlasId()),
					i,
					j,
					this.spriteHolder.getSprite(spriteIdentifier),
					false,
					bl,
					-1,
					null,
					k
			);
		}

		matrixStack.pop();
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrixStack = new MatrixStack();
		matrixStack.scale(1.0F, -1.0F, -1.0F);
		this.model.getRootPart().collectVertices(matrixStack, consumer);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
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
