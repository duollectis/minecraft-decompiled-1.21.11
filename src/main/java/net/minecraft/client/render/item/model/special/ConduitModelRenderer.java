package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.ConduitBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Специализированный рендерер кондуита как предмета.
 * Рендерит только внешнюю оболочку кондуита (shell), центрируя её
 * в точке (0.5, 0.5, 0.5) для корректного отображения в инвентаре.
 */
@Environment(EnvType.CLIENT)
public class ConduitModelRenderer implements SimpleSpecialModelRenderer {

	private static final float CENTER_OFFSET = 0.5F;

	private final SpriteHolder spriteHolder;
	private final ModelPart shell;

	public ConduitModelRenderer(SpriteHolder spriteHolder, ModelPart shell) {
		this.spriteHolder = spriteHolder;
		this.shell = shell;
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
		matrices.translate(CENTER_OFFSET, CENTER_OFFSET, CENTER_OFFSET);

		queue.submitModelPart(
				shell,
				matrices,
				ConduitBlockEntityRenderer.BASE_TEXTURE.getRenderLayer(RenderLayers::entitySolid),
				light,
				overlay,
				spriteHolder.getSprite(ConduitBlockEntityRenderer.BASE_TEXTURE),
				false,
				false,
				-1,
				null,
				seed
		);

		matrices.pop();
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrices = new MatrixStack();
		matrices.translate(CENTER_OFFSET, CENTER_OFFSET, CENTER_OFFSET);
		shell.collectVertices(matrices, consumer);
	}

	/**
	 * Несериализованная форма рендерера кондуита.
	 * Не требует параметров — всегда использует стандартную модель оболочки.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked() implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<ConduitModelRenderer.Unbaked> CODEC =
				MapCodec.unit(new ConduitModelRenderer.Unbaked());

		@Override
		public MapCodec<ConduitModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			return new ConduitModelRenderer(
					context.spriteHolder(),
					context.entityModelSet().getModelPart(EntityModelLayers.CONDUIT_SHELL)
			);
		}
	}
}
