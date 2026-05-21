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

@Environment(EnvType.CLIENT)
/**
 * {@code BannerModelRenderer}.
 */
public class BannerModelRenderer implements SpecialModelRenderer<BannerPatternsComponent> {

	private final BannerBlockEntityRenderer blockEntityRenderer;
	private final DyeColor baseColor;

	public BannerModelRenderer(DyeColor baseColor, BannerBlockEntityRenderer blockEntityRenderer) {
		this.blockEntityRenderer = blockEntityRenderer;
		this.baseColor = baseColor;
	}

	public @Nullable BannerPatternsComponent getData(ItemStack itemStack) {
		return itemStack.get(DataComponentTypes.BANNER_PATTERNS);
	}

	public void render(
			@Nullable BannerPatternsComponent bannerPatternsComponent,
			ItemDisplayContext itemDisplayContext,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			int j,
			boolean bl,
			int k
	) {
		this.blockEntityRenderer
				.renderAsItem(
						matrixStack,
						orderedRenderCommandQueue,
						i,
						j,
						this.baseColor,
						Objects.requireNonNullElse(bannerPatternsComponent, BannerPatternsComponent.DEFAULT),
						k
				);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		this.blockEntityRenderer.collectVertices(consumer);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
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
			return new BannerModelRenderer(this.baseColor, new BannerBlockEntityRenderer(context));
		}
	}
}
