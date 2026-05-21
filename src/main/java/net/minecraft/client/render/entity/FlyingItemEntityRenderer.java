package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.FlyingItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
/**
 * {@code FlyingItemEntityRenderer}.
 */
public class FlyingItemEntityRenderer<T extends Entity & FlyingItemEntity> extends EntityRenderer<T, FlyingItemEntityRenderState> {

	private final ItemModelManager itemModelManager;
	private final float scale;
	private final boolean lit;

	public FlyingItemEntityRenderer(EntityRendererFactory.Context ctx, float scale, boolean lit) {
		super(ctx);
		this.itemModelManager = ctx.getItemModelManager();
		this.scale = scale;
		this.lit = lit;
	}

	public FlyingItemEntityRenderer(EntityRendererFactory.Context context) {
		this(context, 1.0F, false);
	}

	@Override
	protected int getBlockLight(T entity, BlockPos pos) {
		return this.lit ? 15 : super.getBlockLight(entity, pos);
	}

	public void render(
			FlyingItemEntityRenderState flyingItemEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		matrixStack.push();
		matrixStack.scale(this.scale, this.scale, this.scale);
		matrixStack.multiply(cameraRenderState.orientation);
		flyingItemEntityRenderState.itemRenderState
				.render(
						matrixStack,
						orderedRenderCommandQueue,
						flyingItemEntityRenderState.light,
						OverlayTexture.DEFAULT_UV,
						flyingItemEntityRenderState.outlineColor
				);
		matrixStack.pop();
		super.render(flyingItemEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
	}

	/**
	 * Создаёт render state.
	 *
	 * @return FlyingItemEntityRenderState — результат операции
	 */
	public FlyingItemEntityRenderState createRenderState() {
		return new FlyingItemEntityRenderState();
	}

	/**
	 * Обновляет render state.
	 *
	 * @param entity entity
	 * @param flyingItemEntityRenderState flying item entity render state
	 * @param f f
	 */
	public void updateRenderState(T entity, FlyingItemEntityRenderState flyingItemEntityRenderState, float f) {
		super.updateRenderState(entity, flyingItemEntityRenderState, f);
		this.itemModelManager.updateForNonLivingEntity(
				flyingItemEntityRenderState.itemRenderState,
				entity.getStack(),
				ItemDisplayContext.GROUND,
				entity
		);
	}
}
