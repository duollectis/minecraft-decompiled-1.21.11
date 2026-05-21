package net.minecraft.client.render.entity;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code LivingEntityRenderer}.
 */
public abstract class LivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
		extends EntityRenderer<T, S>
		implements FeatureRendererContext<S, M> {

	private static final float OVERLAY_ALPHA = 0.1F;
	protected M model;
	protected final ItemModelManager itemModelResolver;
	protected final List<FeatureRenderer<S, M>> features = Lists.newArrayList();

	public LivingEntityRenderer(EntityRendererFactory.Context ctx, M model, float shadowRadius) {
		super(ctx);
		this.itemModelResolver = ctx.getItemModelManager();
		this.model = model;
		this.shadowRadius = shadowRadius;
	}

	protected final boolean addFeature(FeatureRenderer<S, M> feature) {
		return this.features.add(feature);
	}

	@Override
	public M getModel() {
		return this.model;
	}

	protected Box getBoundingBox(T livingEntity) {
		Box box = super.getBoundingBox(livingEntity);
		if (livingEntity.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.DRAGON_HEAD)) {
			float f = 0.5F;
			return box.expand(0.5, 0.5, 0.5);
		}
		else {
			return box;
		}
	}

	public void render(
			S livingEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		matrixStack.push();
		if (livingEntityRenderState.isInPose(EntityPose.SLEEPING)) {
			Direction direction = livingEntityRenderState.sleepingDirection;
			if (direction != null) {
				float f = livingEntityRenderState.standingEyeHeight - 0.1F;
				matrixStack.translate(-direction.getOffsetX() * f, 0.0F, -direction.getOffsetZ() * f);
			}
		}

		float g = livingEntityRenderState.baseScale;
		matrixStack.scale(g, g, g);
		this.setupTransforms(livingEntityRenderState, matrixStack, livingEntityRenderState.bodyYaw, g);
		matrixStack.scale(-1.0F, -1.0F, 1.0F);
		this.scale(livingEntityRenderState, matrixStack);
		matrixStack.translate(0.0F, -1.501F, 0.0F);
		boolean bl = this.isVisible(livingEntityRenderState);
		boolean bl2 = !bl && !livingEntityRenderState.invisibleToPlayer;
		RenderLayer
				renderLayer =
				this.getRenderLayer(livingEntityRenderState, bl, bl2, livingEntityRenderState.hasOutline());
		if (renderLayer != null) {
			int i = getOverlay(livingEntityRenderState, this.getAnimationCounter(livingEntityRenderState));
			int j = bl2 ? 654311423 : -1;
			int k = ColorHelper.mix(j, this.getMixColor(livingEntityRenderState));
			orderedRenderCommandQueue.submitModel(
					this.model,
					livingEntityRenderState,
					matrixStack,
					renderLayer,
					livingEntityRenderState.light,
					i,
					k,
					null,
					livingEntityRenderState.outlineColor,
					null
			);
		}

		if (this.shouldRenderFeatures(livingEntityRenderState) && !this.features.isEmpty()) {
			this.model.setAngles(livingEntityRenderState);

			for (FeatureRenderer<S, M> featureRenderer : this.features) {
				featureRenderer.render(
						matrixStack,
						orderedRenderCommandQueue,
						livingEntityRenderState.light,
						livingEntityRenderState,
						livingEntityRenderState.relativeHeadYaw,
						livingEntityRenderState.pitch
				);
			}
		}

		matrixStack.pop();
		super.render(livingEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
	}

	protected boolean shouldRenderFeatures(S state) {
		return true;
	}

	protected int getMixColor(S state) {
		return -1;
	}

	public abstract Identifier getTexture(S state);

	protected @Nullable RenderLayer getRenderLayer(
			S state,
			boolean showBody,
			boolean translucent,
			boolean showOutline
	) {
		Identifier identifier = this.getTexture(state);
		if (translucent) {
			return RenderLayers.itemEntityTranslucentCull(identifier);
		}
		else if (showBody) {
			return this.model.getLayer(identifier);
		}
		else {
			return showOutline ? RenderLayers.outlineNoCull(identifier) : null;
		}
	}

	public static int getOverlay(LivingEntityRenderState state, float whiteOverlayProgress) {
		return OverlayTexture.packUv(OverlayTexture.getU(whiteOverlayProgress), OverlayTexture.getV(state.hurt));
	}

	protected boolean isVisible(S state) {
		return !state.invisible;
	}

	private static float getYaw(Direction direction) {
		switch (direction) {
			case SOUTH:
				return 90.0F;
			case WEST:
				return 0.0F;
			case NORTH:
				return 270.0F;
			case EAST:
				return 180.0F;
			default:
				return 0.0F;
		}
	}

	protected boolean isShaking(S state) {
		return state.shaking;
	}

	protected void setupTransforms(S state, MatrixStack matrices, float bodyYaw, float baseHeight) {
		if (this.isShaking(state)) {
			bodyYaw += (float) (Math.cos(MathHelper.floor(state.age) * 3.25F) * Math.PI * 0.4F);
		}

		if (!state.isInPose(EntityPose.SLEEPING)) {
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
		}

		if (state.deathTime > 0.0F) {
			float f = (state.deathTime - 1.0F) / 20.0F * 1.6F;
			f = MathHelper.sqrt(f);
			if (f > 1.0F) {
				f = 1.0F;
			}

			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * this.getLyingPositionRotationDegrees()));
		}
		else if (state.usingRiptide) {
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - state.pitch));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.age * -75.0F));
		}
		else if (state.isInPose(EntityPose.SLEEPING)) {
			Direction direction = state.sleepingDirection;
			float g = direction != null ? getYaw(direction) : bodyYaw;
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.getLyingPositionRotationDegrees()));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270.0F));
		}
		else if (state.flipUpsideDown) {
			matrices.translate(0.0F, (state.height + 0.1F) / baseHeight, 0.0F);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
		}
	}

	protected float getLyingPositionRotationDegrees() {
		return 90.0F;
	}

	protected float getAnimationCounter(S state) {
		return 0.0F;
	}

	protected void scale(S state, MatrixStack matrices) {
	}

	protected boolean hasLabel(T livingEntity, double d) {
		if (livingEntity.isSneaky()) {
			float f = 32.0F;
			if (d >= 1024.0) {
				return false;
			}
		}

		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
		boolean bl = !livingEntity.isInvisibleTo(clientPlayerEntity);
		if (livingEntity != clientPlayerEntity) {
			AbstractTeam abstractTeam = livingEntity.getScoreboardTeam();
			AbstractTeam abstractTeam2 = clientPlayerEntity.getScoreboardTeam();
			if (abstractTeam != null) {
				AbstractTeam.VisibilityRule visibilityRule = abstractTeam.getNameTagVisibilityRule();
				switch (visibilityRule) {
					case ALWAYS:
						return bl;
					case NEVER:
						return false;
					case HIDE_FOR_OTHER_TEAMS:
						return abstractTeam2 == null ? bl : abstractTeam.isEqual(abstractTeam2) && (
								abstractTeam.shouldShowFriendlyInvisibles() || bl
						);
					case HIDE_FOR_OWN_TEAM:
						return abstractTeam2 == null ? bl : !abstractTeam.isEqual(abstractTeam2) && bl;
					default:
						return true;
				}
			}
		}

		return MinecraftClient.isHudEnabled() && livingEntity != minecraftClient.getCameraEntity() && bl
				&& !livingEntity.hasPassengers();
	}

	public boolean shouldFlipUpsideDown(T entity) {
		Text text = entity.getCustomName();
		return text != null && shouldFlipUpsideDown(text.getString());
	}

	protected static boolean shouldFlipUpsideDown(String name) {
		return "Dinnerbone".equals(name) || "Grumm".equals(name);
	}

	protected float getShadowRadius(S livingEntityRenderState) {
		return super.getShadowRadius(livingEntityRenderState) * livingEntityRenderState.baseScale;
	}

	public void updateRenderState(T livingEntity, S livingEntityRenderState, float f) {
		super.updateRenderState(livingEntity, livingEntityRenderState, f);
		float g = MathHelper.lerpAngleDegrees(f, livingEntity.lastHeadYaw, livingEntity.headYaw);
		livingEntityRenderState.bodyYaw = clampBodyYaw(livingEntity, g, f);
		livingEntityRenderState.relativeHeadYaw = MathHelper.wrapDegrees(g - livingEntityRenderState.bodyYaw);
		livingEntityRenderState.pitch = livingEntity.getLerpedPitch(f);
		livingEntityRenderState.flipUpsideDown = this.shouldFlipUpsideDown(livingEntity);
		if (livingEntityRenderState.flipUpsideDown) {
			livingEntityRenderState.pitch *= -1.0F;
			livingEntityRenderState.relativeHeadYaw *= -1.0F;
		}

		if (!livingEntity.hasVehicle() && livingEntity.isAlive()) {
			livingEntityRenderState.limbSwingAnimationProgress = livingEntity.limbAnimator.getAnimationProgress(f);
			livingEntityRenderState.limbSwingAmplitude = livingEntity.limbAnimator.getAmplitude(f);
		}
		else {
			livingEntityRenderState.limbSwingAnimationProgress = 0.0F;
			livingEntityRenderState.limbSwingAmplitude = 0.0F;
		}

		if (livingEntity.getVehicle() instanceof LivingEntity livingEntity2) {
			livingEntityRenderState.headItemAnimationProgress = livingEntity2.limbAnimator.getAnimationProgress(f);
		}
		else {
			livingEntityRenderState.headItemAnimationProgress = livingEntityRenderState.limbSwingAnimationProgress;
		}

		livingEntityRenderState.baseScale = livingEntity.getScale();
		livingEntityRenderState.ageScale = livingEntity.getScaleFactor();
		livingEntityRenderState.pose = livingEntity.getPose();
		livingEntityRenderState.sleepingDirection = livingEntity.getSleepingDirection();
		if (livingEntityRenderState.sleepingDirection != null) {
			livingEntityRenderState.standingEyeHeight = livingEntity.getEyeHeight(EntityPose.STANDING);
		}

		livingEntityRenderState.shaking = livingEntity.isFrozen();
		livingEntityRenderState.baby = livingEntity.isBaby();
		livingEntityRenderState.touchingWater = livingEntity.isTouchingWater();
		livingEntityRenderState.usingRiptide = livingEntity.isUsingRiptide();
		livingEntityRenderState.timeSinceLastKineticAttack = livingEntity.getTimeSinceLastKineticAttack(f);
		livingEntityRenderState.hurt = livingEntity.hurtTime > 0 || livingEntity.deathTime > 0;
		ItemStack itemStack = livingEntity.getEquippedStack(EquipmentSlot.HEAD);
		if (itemStack.getItem() instanceof BlockItem blockItem
				&& blockItem.getBlock() instanceof AbstractSkullBlock abstractSkullBlock) {
			livingEntityRenderState.wearingSkullType = abstractSkullBlock.getSkullType();
			livingEntityRenderState.wearingSkullProfile = itemStack.get(DataComponentTypes.PROFILE);
			livingEntityRenderState.headItemRenderState.clear();
		}
		else {
			livingEntityRenderState.wearingSkullType = null;
			livingEntityRenderState.wearingSkullProfile = null;
			if (!ArmorFeatureRenderer.hasModel(itemStack, EquipmentSlot.HEAD)) {
				this.itemModelResolver.updateForLivingEntity(
						livingEntityRenderState.headItemRenderState,
						itemStack,
						ItemDisplayContext.HEAD,
						livingEntity
				);
			}
			else {
				livingEntityRenderState.headItemRenderState.clear();
			}
		}

		livingEntityRenderState.deathTime = livingEntity.deathTime > 0 ? livingEntity.deathTime + f : 0.0F;
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		livingEntityRenderState.invisibleToPlayer =
				livingEntityRenderState.invisible && livingEntity.isInvisibleTo(minecraftClient.player);
	}

	private static float clampBodyYaw(LivingEntity entity, float degrees, float tickProgress) {
		if (entity.getVehicle() instanceof LivingEntity livingEntity) {
			float f = MathHelper.lerpAngleDegrees(tickProgress, livingEntity.lastBodyYaw, livingEntity.bodyYaw);
			float g = 85.0F;
			float h = MathHelper.clamp(MathHelper.wrapDegrees(degrees - f), -85.0F, 85.0F);
			f = degrees - h;
			if (Math.abs(h) > 50.0F) {
				f += h * 0.2F;
			}

			return f;
		}
		else {
			return MathHelper.lerpAngleDegrees(tickProgress, entity.lastBodyYaw, entity.bodyYaw);
		}
	}
}
