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

@Environment(EnvType.CLIENT)
/**
 * {@code InGameOverlayRenderer}.
 */
public class InGameOverlayRenderer {

	private static final Identifier UNDERWATER_TEXTURE = Identifier.ofVanilla("textures/misc/underwater.png");
	private final MinecraftClient client;
	private final SpriteHolder spriteHolder;
	private final VertexConsumerProvider vertexConsumers;
	public static final int FLOATING_ITEM_DISPLAY_TICKS = 40;
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
		if (this.floatingItemTimer > 0) {
			this.floatingItemTimer--;
			if (this.floatingItemTimer == 0) {
				this.floatingItem = null;
			}
		}
	}

	public void renderOverlays(boolean sleeping, float tickProgress, OrderedRenderCommandQueue queue) {
		MatrixStack matrixStack = new MatrixStack();
		PlayerEntity playerEntity = this.client.player;
		if (this.client.options.getPerspective().isFirstPerson() && !sleeping) {
			if (!playerEntity.noClip) {
				BlockState blockState = getInWallBlockState(playerEntity);
				if (blockState != null) {
					renderInWallOverlay(
							this.client
									.getBlockRenderManager()
									.getModels()
									.getModelParticleSprite(blockState), matrixStack, this.vertexConsumers
					);
				}
			}

			if (!this.client.player.isSpectator()) {
				if (this.client.player.isSubmergedIn(FluidTags.WATER)) {
					renderUnderwaterOverlay(this.client, matrixStack, this.vertexConsumers);
				}

				if (this.client.player.isOnFire()) {
					Sprite sprite = this.spriteHolder.getSprite(ModelBaker.FIRE_1);
					renderFireOverlay(matrixStack, this.vertexConsumers, sprite);
				}
			}
		}

		if (!this.client.options.hudHidden) {
			this.renderFloatingItem(matrixStack, tickProgress, queue);
		}
	}

	private void renderFloatingItem(MatrixStack matrices, float tickProgress, OrderedRenderCommandQueue queue) {
		if (this.floatingItem != null && this.floatingItemTimer > 0) {
			int i = 40 - this.floatingItemTimer;
			float f = (i + tickProgress) / 40.0F;
			float g = f * f;
			float h = f * g;
			float j = 10.25F * h * g - 24.95F * g * g + 25.5F * h - 13.8F * g + 4.0F * f;
			float k = j * (float) Math.PI;
			float
					l =
					(float) this.client.getWindow().getFramebufferWidth() / this.client
							.getWindow()
							.getFramebufferHeight();
			float m = this.floatingItemOffsetX * 0.3F * l;
			float n = this.floatingItemOffsetY * 0.3F;
			matrices.push();
			matrices.translate(
					m * MathHelper.abs(MathHelper.sin(k * 2.0F)),
					n * MathHelper.abs(MathHelper.sin(k * 2.0F)),
					-10.0F + 9.0F * MathHelper.sin(k)
			);
			float o = 0.8F;
			matrices.scale(0.8F, 0.8F, 0.8F);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(900.0F * MathHelper.abs(MathHelper.sin(k))));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F * MathHelper.cos(f * 8.0F)));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(6.0F * MathHelper.cos(f * 8.0F)));
			this.client.gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_3D);
			ItemRenderState itemRenderState = new ItemRenderState();
			this.client
					.getItemModelManager()
					.clearAndUpdate(
							itemRenderState,
							this.floatingItem,
							ItemDisplayContext.FIXED,
							this.client.world,
							null,
							0
					);
			itemRenderState.render(matrices, queue, 15728880, OverlayTexture.DEFAULT_UV, 0);
			matrices.pop();
		}
	}

	public void clearFloatingItem() {
		this.floatingItem = null;
	}

	public void setFloatingItem(ItemStack stack, Random random) {
		this.floatingItem = stack;
		this.floatingItemTimer = 40;
		this.floatingItemOffsetX = random.nextFloat() * 2.0F - 1.0F;
		this.floatingItemOffsetY = random.nextFloat() * 2.0F - 1.0F;
	}

	private static @Nullable BlockState getInWallBlockState(PlayerEntity player) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int i = 0; i < 8; i++) {
			double d = player.getX() + ((i >> 0) % 2 - 0.5F) * player.getWidth() * 0.8F;
			double e = player.getEyeY() + ((i >> 1) % 2 - 0.5F) * 0.1F * player.getScale();
			double f = player.getZ() + ((i >> 2) % 2 - 0.5F) * player.getWidth() * 0.8F;
			mutable.set(d, e, f);
			BlockState blockState = player.getEntityWorld().getBlockState(mutable);
			if (blockState.getRenderType() != BlockRenderType.INVISIBLE
					&& blockState.shouldBlockVision(player.getEntityWorld(), mutable)) {
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
		float f = 0.1F;
		int i = ColorHelper.fromFloats(1.0F, 0.1F, 0.1F, 0.1F);
		float g = -1.0F;
		float h = 1.0F;
		float j = -1.0F;
		float k = 1.0F;
		float l = -0.5F;
		float m = sprite.getMinU();
		float n = sprite.getMaxU();
		float o = sprite.getMinV();
		float p = sprite.getMaxV();
		Matrix4f matrix4f = matrices.peek().getPositionMatrix();
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.blockScreenEffect(sprite.getAtlasId()));
		vertexConsumer.vertex(matrix4f, -1.0F, -1.0F, -0.5F).texture(n, p).color(i);
		vertexConsumer.vertex(matrix4f, 1.0F, -1.0F, -0.5F).texture(m, p).color(i);
		vertexConsumer.vertex(matrix4f, 1.0F, 1.0F, -0.5F).texture(m, o).color(i);
		vertexConsumer.vertex(matrix4f, -1.0F, 1.0F, -0.5F).texture(n, o).color(i);
	}

	private static void renderUnderwaterOverlay(
			MinecraftClient client,
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers
	) {
		BlockPos blockPos = BlockPos.ofFloored(client.player.getX(), client.player.getEyeY(), client.player.getZ());
		float
				f =
				LightmapTextureManager.getBrightness(
						client.player.getEntityWorld().getDimension(),
						client.player.getEntityWorld().getLightLevel(blockPos)
				);
		int i = ColorHelper.fromFloats(0.1F, f, f, f);
		float g = 4.0F;
		float h = -1.0F;
		float j = 1.0F;
		float k = -1.0F;
		float l = 1.0F;
		float m = -0.5F;
		float n = -client.player.getYaw() / 64.0F;
		float o = client.player.getPitch() / 64.0F;
		Matrix4f matrix4f = matrices.peek().getPositionMatrix();
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.blockScreenEffect(UNDERWATER_TEXTURE));
		vertexConsumer.vertex(matrix4f, -1.0F, -1.0F, -0.5F).texture(4.0F + n, 4.0F + o).color(i);
		vertexConsumer.vertex(matrix4f, 1.0F, -1.0F, -0.5F).texture(0.0F + n, 4.0F + o).color(i);
		vertexConsumer.vertex(matrix4f, 1.0F, 1.0F, -0.5F).texture(0.0F + n, 0.0F + o).color(i);
		vertexConsumer.vertex(matrix4f, -1.0F, 1.0F, -0.5F).texture(4.0F + n, 0.0F + o).color(i);
	}

	private static void renderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite) {
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayers.fireScreenEffect(sprite.getAtlasId()));
		float f = sprite.getMinU();
		float g = sprite.getMaxU();
		float h = sprite.getMinV();
		float i = sprite.getMaxV();
		float j = 1.0F;

		for (int k = 0; k < 2; k++) {
			matrices.push();
			float l = -0.5F;
			float m = 0.5F;
			float n = -0.5F;
			float o = 0.5F;
			float p = -0.5F;
			matrices.translate(-(k * 2 - 1) * 0.24F, -0.3F, 0.0F);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((k * 2 - 1) * 10.0F));
			Matrix4f matrix4f = matrices.peek().getPositionMatrix();
			vertexConsumer.vertex(matrix4f, -0.5F, -0.5F, -0.5F).texture(g, i).color(1.0F, 1.0F, 1.0F, 0.9F);
			vertexConsumer.vertex(matrix4f, 0.5F, -0.5F, -0.5F).texture(f, i).color(1.0F, 1.0F, 1.0F, 0.9F);
			vertexConsumer.vertex(matrix4f, 0.5F, 0.5F, -0.5F).texture(f, h).color(1.0F, 1.0F, 1.0F, 0.9F);
			vertexConsumer.vertex(matrix4f, -0.5F, 0.5F, -0.5F).texture(g, h).color(1.0F, 1.0F, 1.0F, 0.9F);
			matrices.pop();
		}
	}
}
