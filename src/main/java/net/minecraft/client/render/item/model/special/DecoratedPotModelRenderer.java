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

/**
 * Специализированный рендерер украшенного горшка как предмета.
 * Делегирует рендер {@link DecoratedPotBlockEntityRenderer}, передавая черепки
 * из компонента {@link DataComponentTypes#POT_DECORATIONS}.
 * При отсутствии компонента использует {@link Sherds#DEFAULT} (горшок без украшений).
 */
@Environment(EnvType.CLIENT)
public class DecoratedPotModelRenderer implements SpecialModelRenderer<Sherds> {

	private final DecoratedPotBlockEntityRenderer blockEntityRenderer;

	public DecoratedPotModelRenderer(DecoratedPotBlockEntityRenderer blockEntityRenderer) {
		this.blockEntityRenderer = blockEntityRenderer;
	}

	@Override
	public @Nullable Sherds getData(ItemStack stack) {
		return stack.get(DataComponentTypes.POT_DECORATIONS);
	}

	@Override
	public void render(
			@Nullable Sherds sherds,
			ItemDisplayContext displayContext,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			boolean glint,
			int seed
	) {
		blockEntityRenderer.render(
				matrices,
				queue,
				light,
				overlay,
				Objects.requireNonNullElse(sherds, Sherds.DEFAULT),
				seed
		);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		blockEntityRenderer.collectVertices(consumer);
	}

	/**
	 * Несериализованная форма рендерера украшенного горшка.
	 * Не требует параметров — всегда использует стандартную модель горшка.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked() implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<DecoratedPotModelRenderer.Unbaked> CODEC =
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
