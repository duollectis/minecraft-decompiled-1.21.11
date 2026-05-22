package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.TridentEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Рендерер трезубца как предмета инвентаря.
 * Переворачивает модель по осям Y и Z для корректного отображения в руке.
 */
@Environment(EnvType.CLIENT)
public class TridentModelRenderer implements SimpleSpecialModelRenderer {

	private static final int NO_TINT = -1;

	private final TridentEntityModel model;

	public TridentModelRenderer(TridentEntityModel model) {
		this.model = model;
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
		matrices.push();
		matrices.scale(1.0F, -1.0F, -1.0F);
		queue.submitModelPart(
				model.getRootPart(),
				matrices,
				model.getLayer(TridentEntityModel.TEXTURE),
				light,
				overlay,
				null,
				false,
				glint,
				NO_TINT,
				null,
				seed
		);
		matrices.pop();
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrices = new MatrixStack();
		matrices.scale(1.0F, -1.0F, -1.0F);
		model.getRootPart().collectVertices(matrices, consumer);
	}

	/**
	 * Несериализованный дескриптор рендерера трезубца.
	 * Не требует параметров — модель берётся из {@code EntityModelLayers.TRIDENT}.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked() implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<TridentModelRenderer.Unbaked> CODEC = MapCodec.unit(new TridentModelRenderer.Unbaked());

		@Override
		public MapCodec<TridentModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			return new TridentModelRenderer(
					new TridentEntityModel(context.entityModelSet().getModelPart(EntityModelLayers.TRIDENT))
			);
		}
	}
}
