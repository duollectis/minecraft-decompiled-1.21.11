package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Специализированный рендерер знамени как предмета.
 * Делегирует рендер {@link BannerBlockEntityRenderer}, передавая базовый цвет
 * и паттерны из компонента {@link DataComponentTypes#BANNER_PATTERNS}.
 * При отсутствии компонента паттернов использует {@link BannerPatternsComponent#DEFAULT}.
 */
@Environment(EnvType.CLIENT)
public class BannerModelRenderer implements SpecialModelRenderer<BannerPatternsComponent> {

	private final BannerBlockEntityRenderer blockEntityRenderer;
	private final DyeColor baseColor;

	public BannerModelRenderer(DyeColor baseColor, BannerBlockEntityRenderer blockEntityRenderer) {
		this.blockEntityRenderer = blockEntityRenderer;
		this.baseColor = baseColor;
	}

	@Override
	public @Nullable BannerPatternsComponent getData(ItemStack stack) {
		return stack.get(DataComponentTypes.BANNER_PATTERNS);
	}

	@Override
	public void render(
			@Nullable BannerPatternsComponent patterns,
			ItemDisplayContext displayContext,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			boolean glint,
			int seed
	) {
		blockEntityRenderer.renderAsItem(
				matrices,
				queue,
				light,
				overlay,
				baseColor,
				Objects.requireNonNullElse(patterns, BannerPatternsComponent.DEFAULT),
				seed
		);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		blockEntityRenderer.collectVertices(consumer);
	}

	/**
	 * Несериализованная форма рендерера знамени.
	 * Хранит базовый цвет знамени, определяющий фоновый цвет полотна.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked(DyeColor baseColor) implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<BannerModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(DyeColor.CODEC.fieldOf("color").forGetter(BannerModelRenderer.Unbaked::baseColor))
						.apply(instance, BannerModelRenderer.Unbaked::new)
		);

		@Override
		public MapCodec<BannerModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			return new BannerModelRenderer(baseColor, new BannerBlockEntityRenderer(context));
		}
	}
}
