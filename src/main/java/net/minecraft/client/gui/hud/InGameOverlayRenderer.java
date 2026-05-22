package net.minecraft.client.gui.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Рендерит оверлеи от первого лица: стена, вода, огонь, а также анимацию подобранного предмета.
 */
@Environment(EnvType.CLIENT)
public class InGameOverlayRenderer {

	private static final Identifier UNDERWATER_TEXTURE = Identifier.ofVanilla("textures/misc/underwater.png");
	private static final float WALL_OVERLAY_ALPHA = 0.1F;
	private static final float UNDERWATER_ALPHA = 0.1F;
	private static final float UNDERWATER_TILE_SIZE = 4.0F;
	private static final float OVERLAY_Z = -0.5F;
	private static final float FIRE_OPACITY = 0.9F;
	private static final float FIRE_SIDE_OFFSET = 0.24F;
	private static final float FIRE_TILT_DEGREES = 10.0F;
	private static final float FIRE_Y_OFFSET = -0.3F;
	private static final int CORNER_SAMPLES = 8;
	private static final float ITEM_SCALE = 0.8F;
	private static final float ITEM_Z_NEAR = -10.0F;
	private static final float ITEM_Z_FAR = 9.0F;
	private static final float ITEM_OFFSET_FACTOR = 0.3F;
	private static final int FULL_BRIGHT = 15728880;

	public static final int FLOATING_ITEM_DISPLAY_TICKS = 40;

	private final MinecraftClient client;
	private final SpriteHolder spriteHolder;
	private final VertexConsumerProvider vertexConsumers;
	private @Nullable ItemStack floatingItem;
	private int floatingItemTimer;
	private float floatingItemOffsetX;
	private float floatingItemOffsetY;

	public InGameOverlayRenderer(
		MinecraftClient client,
		SpriteHolder spriteHolder,
		VertexConsumerProvider vertexConsumers
	) {
		this.client = client;
		this.spriteHolder = spriteHolder;
		this.vertexConsumers = vertexConsumers;
	}

	public void tickFloatingItemTimer() {
		if (floatingItemTimer <= 0) {
			return;
		}

		floatingItemTimer--;

		if (floatingItemTimer == 0) {
			floatingItem = null;
		}
	}

	public void renderOverlays(boolean sleeping, float tickProgress, OrderedRenderCommandQueue queue) {
		MatrixStack matrices = new MatrixStack();
		PlayerEntity player = client.player;

		if (client.options.getPerspective().isFirstPerson() && !sleeping) {
			if (!player.noClip) {
				BlockState wallBlock = getInWallBlockState(player);

				if (wallBlock != null) {
					renderInWallOverlay(
						client.getBlockRenderManager().getModels().getModelParticleSprite(wallBlock),
						matrices,
						vertexConsumers
					);
				}
			}

			if (!client.player.isSpectator()) {
				if (client.player.isSubmergedIn(FluidTags.WATER)) {
					renderUnderwaterOverlay(client, matrices, vertexConsumers);
				}

				if (client.player.isOnFire()) {
					renderFireOverlay(matrices, vertexConsumers, spriteHolder.getSprite(ModelBaker.FIRE_1));
				}
			}
		}

		if (!client.options.hudHidden) {
			renderFloatingItem(matrices, tickProgress, queue);
		}
	}

	private void renderFloatingItem(MatrixStack matrices, float tickProgress, OrderedRenderCommandQueue queue) {
		if (floatingItem == null || floatingItemTimer <= 0) {
			return;
		}

		int elapsed = FLOATING_ITEM_DISPLAY_TICKS - floatingItemTimer;
		float progress = (elapsed + tickProgress) / FLOATING_ITEM_DISPLAY_TICKS;
		float progressSq = progress * progress;
		float progressCb = progress * progressSq;
		// Кривая Безье для плавного выброса предмета на экран
		float swing = 10.25F * progressCb * progressSq - 24.95F * progressSq * progressSq + 25.5F * progressCb - 13.8F * progressSq + 4.0F * progress;
		float angle = swing * (float) Math.PI;
		float aspectRatio = (float) client.getWindow().getFramebufferWidth() / client.getWindow().getFramebufferHeight();
		float offsetX = floatingItemOffsetX * ITEM_OFFSET_FACTOR * aspectRatio;
		float offsetY = floatingItemOffsetY * ITEM_OFFSET_FACTOR;

		matrices.push();
		matrices.translate(
			offsetX * MathHelper.abs(MathHelper.sin(angle * 2.0F)),
			offsetY * MathHelper.abs(MathHelper.sin(angle * 2.0F)),
			ITEM_Z_NEAR + ITEM_Z_FAR * MathHelper.sin(angle)
		);
		matrices.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(900.0F * MathHelper.abs(MathHelper.sin(angle))));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F * MathHelper.cos(progress * 8.0F)));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(6.0F * MathHelper.cos(progress * 8.0F)));

		client.gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_3D);

		ItemRenderState itemRenderState = new ItemRenderState();
		client.getItemModelManager().clearAndUpdate(
			itemRenderState,
			floatingItem,
			ItemDisplayContext.FIXED,
			client.world,
			null,
			0
		);
		itemRenderState.render(matrices, queue, FULL_BRIGHT, OverlayTexture.DEFAULT_UV, 0);
		matrices.pop();
	}

	public void clearFloatingItem() {
		floatingItem = null;
	}

	public void setFloatingItem(ItemStack stack, Random random) {
		floatingItem = stack;
		floatingItemTimer = FLOATING_ITEM_DISPLAY_TICKS;
		floatingItemOffsetX = random.nextFloat() * 2.0F - 1.0F;
		floatingItemOffsetY = random.nextFloat() * 2.0F - 1.0F;
	}

	private static @Nullable BlockState getInWallBlockState(PlayerEntity player) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int corner = 0; corner < CORNER_SAMPLES; corner++) {
			double x = player.getX() + ((corner >> 0) % 2 - 0.5F) * player.getWidth() * 0.8F;
			double y = player.getEyeY() + ((corner >> 1) % 2 - 0.5F) * 0.1F * player.getScale();
			double z = player.getZ() + ((corner >> 2) % 2 - 0.5F) * player.getWidth() * 0.8F;
			mutable.set(x, y, z);
			BlockState blockState = player.getEntityWorld().getBlockState(mutable);

			if (blockState.getRenderType() != BlockRenderType.INVISIBLE
				&& blockState.shouldBlockVision(player.getEntityWorld(), mutable)
			) {
				return blockState;
			}
		}

		return null;
	}

	private static void renderInWallOverlay(
		Sprite sprite,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers
	) {
		int color = ColorHelper.fromFloats(1.0F, WALL_OVERLAY_ALPHA, WALL_OVERLAY_ALPHA, WALL_OVERLAY_ALPHA);
		float minU = sprite.getMinU();
		float maxU = sprite.getMaxU();
		float minV = sprite.getMinV();
		float maxV = sprite.getMaxV();
		Matrix4f posMatrix = matrices.peek().getPositionMatrix();
		VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayers.blockScreenEffect(sprite.getAtlasId()));
		consumer.vertex(posMatrix, -1.0F, -1.0F, OVERLAY_Z).texture(maxU, maxV).color(color);
		consumer.vertex(posMatrix, 1.0F, -1.0F, OVERLAY_Z).texture(minU, maxV).color(color);
		consumer.vertex(posMatrix, 1.0F, 1.0F, OVERLAY_Z).texture(minU, minV).color(color);
		consumer.vertex(posMatrix, -1.0F, 1.0F, OVERLAY_Z).texture(maxU, minV).color(color);
	}

	private static void renderUnderwaterOverlay(
		MinecraftClient client,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers
	) {
		BlockPos eyePos = BlockPos.ofFloored(client.player.getX(), client.player.getEyeY(), client.player.getZ());
		float brightness = LightmapTextureManager.getBrightness(
			client.player.getEntityWorld().getDimension(),
			client.player.getEntityWorld().getLightLevel(eyePos)
		);
		int color = ColorHelper.fromFloats(UNDERWATER_ALPHA, brightness, brightness, brightness);
		float yawOffset = -client.player.getYaw() / 64.0F;
		float pitchOffset = client.player.getPitch() / 64.0F;
		Matrix4f posMatrix = matrices.peek().getPositionMatrix();
		VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayers.blockScreenEffect(UNDERWATER_TEXTURE));
		consumer.vertex(posMatrix, -1.0F, -1.0F, OVERLAY_Z).texture(UNDERWATER_TILE_SIZE + yawOffset, UNDERWATER_TILE_SIZE + pitchOffset).color(color);
		consumer.vertex(posMatrix, 1.0F, -1.0F, OVERLAY_Z).texture(0.0F + yawOffset, UNDERWATER_TILE_SIZE + pitchOffset).color(color);
		consumer.vertex(posMatrix, 1.0F, 1.0F, OVERLAY_Z).texture(0.0F + yawOffset, 0.0F + pitchOffset).color(color);
		consumer.vertex(posMatrix, -1.0F, 1.0F, OVERLAY_Z).texture(UNDERWATER_TILE_SIZE + yawOffset, 0.0F + pitchOffset).color(color);
	}

	private static void renderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite) {
		VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayers.fireScreenEffect(sprite.getAtlasId()));
		float minU = sprite.getMinU();
		float maxU = sprite.getMaxU();
		float minV = sprite.getMinV();
		float maxV = sprite.getMaxV();

		for (int side = 0; side < 2; side++) {
			matrices.push();
			matrices.translate(-(side * 2 - 1) * FIRE_SIDE_OFFSET, FIRE_Y_OFFSET, 0.0F);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((side * 2 - 1) * FIRE_TILT_DEGREES));
			Matrix4f posMatrix = matrices.peek().getPositionMatrix();
			consumer.vertex(posMatrix, -0.5F, -0.5F, OVERLAY_Z).texture(maxU, maxV).color(1.0F, 1.0F, 1.0F, FIRE_OPACITY);
			consumer.vertex(posMatrix, 0.5F, -0.5F, OVERLAY_Z).texture(minU, maxV).color(1.0F, 1.0F, 1.0F, FIRE_OPACITY);
			consumer.vertex(posMatrix, 0.5F, 0.5F, OVERLAY_Z).texture(minU, minV).color(1.0F, 1.0F, 1.0F, FIRE_OPACITY);
			consumer.vertex(posMatrix, -0.5F, 0.5F, OVERLAY_Z).texture(maxU, minV).color(1.0F, 1.0F, 1.0F, FIRE_OPACITY);
			matrices.pop();
		}
	}
}
