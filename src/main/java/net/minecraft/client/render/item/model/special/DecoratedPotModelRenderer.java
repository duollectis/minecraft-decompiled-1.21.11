package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.Sherds;
import net.minecraft.client.render.block.entity.DecoratedPotBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code DecoratedPotModelRenderer}.
 */
public class DecoratedPotModelRenderer implements SpecialModelRenderer<Sherds> {

	private final DecoratedPotBlockEntityRenderer blockEntityRenderer;

	public DecoratedPotModelRenderer(DecoratedPotBlockEntityRenderer blockEntityRenderer) {
		this.blockEntityRenderer = blockEntityRenderer;
	}

	public @Nullable Sherds getData(ItemStack itemStack) {
		return itemStack.get(DataComponentTypes.POT_DECORATIONS);
	}

	public void render(
			@Nullable Sherds sherds,
			ItemDisplayContext itemDisplayContext,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			int j,
			boolean bl,
			int k
	) {
		this.blockEntityRenderer.render(
				matrixStack,
				orderedRenderCommandQueue,
				i,
				j,
				Objects.requireNonNullElse(sherds, Sherds.DEFAULT),
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
	public record Unbaked() implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<DecoratedPotModelRenderer.Unbaked>
				CODEC =
				MapCodec.unit(new DecoratedPotModelRenderer.Unbaked());

		@Override
		public MapCodec<DecoratedPotModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			return new DecoratedPotModelRenderer(new DecoratedPotBlockEntityRenderer(context));
		}
	}
}
