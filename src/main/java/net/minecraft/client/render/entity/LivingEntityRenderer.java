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

/**
 * Базовый рендерер живых существ. Управляет трансформациями тела (сон, смерть, рябь),
 * рендерингом слоёв брони/предметов и видимостью имени над головой.
 */
@Environment(EnvType.CLIENT)
public abstract class LivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
		extends EntityRenderer<T, S>
		implements FeatureRendererContext<S, M> {

	private static final float OVERLAY_ALPHA = 0.1F;
	private static final float DRAGON_HEAD_EXPAND = 0.5F;
	private static final float DEATH_ROTATION_SCALE = 1.6F;
	private static final float DEATH_ROTATION_MAX = 1.0F;
	private static final float RIPTIDE_PITCH_OFFSET = -90.0F;
	private static final float RIPTIDE_YAW_MULTIPLIER = -75.0F;
	private static final float SNEAKY_LABEL_MAX_DISTANCE = 1024.0F;
	private static final int TRANSLUCENT_COLOR = 654311423;

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

	@Override
	protected Box getBoundingBox(T livingEntity) {
		Box box = super.getBoundingBox(livingEntity);
		return livingEntity.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.DRAGON_HEAD)
				? box.expand(DRAGON_HEAD_EXPAND, DRAGON_HEAD_EXPAND, DRAGON_HEAD_EXPAND)
				: box;
	}

	@Override
	public void render(
			S livingEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		matrixStack.push();

		if (livingEntityRenderState.isInPose(EntityPose.SLEEPING)) {
			Direction sleepDir = livingEntityRenderState.sleepingDirection;
			if (sleepDir != null) {
				float eyeHeight = livingEntityRenderState.standingEyeHeight - 0.1F;
				matrixStack.translate(-sleepDir.getOffsetX() * eyeHeight, 0.0F, -sleepDir.getOffsetZ() * eyeHeight);
			}
		}

		float baseScale = livingEntityRenderState.baseScale;
		matrixStack.scale(baseScale, baseScale, baseScale);
		this.setupTransforms(livingEntityRenderState, matrixStack, livingEntityRenderState.bodyYaw, baseScale);
		matrixStack.scale(-1.0F, -1.0F, 1.0F);
		this.scale(livingEntityRenderState, matrixStack);
		matrixStack.translate(0.0F, -1.501F, 0.0F);

		boolean visible = this.isVisible(livingEntityRenderState);
		boolean translucent = !visible && !livingEntityRenderState.invisibleToPlayer;
		RenderLayer renderLayer = this.getRenderLayer(livingEntityRenderState, visible, translucent, livingEntityRenderState.hasOutline());

		if (renderLayer != null) {
			int overlay = getOverlay(livingEntityRenderState, this.getAnimationCounter(livingEntityRenderState));
			int baseColor = translucent ? TRANSLUCENT_COLOR : -1;
			int mixColor = ColorHelper.mix(baseColor, this.getMixColor(livingEntityRenderState));
			orderedRenderCommandQueue.submitModel(
					this.model,
					livingEntityRenderState,
					matrixStack,
					renderLayer,
					livingEntityRenderState.light,
					overlay,
					mixColor,
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

	protected @Nullable RenderLayer getRenderLayer(S state, boolean showBody, boolean translucent, boolean showOutline) {
		Identifier texture = this.getTexture(state);
		if (translucent) {
			return RenderLayers.itemEntityTranslucentCull(texture);
		}

		if (showBody) {
			return this.model.getLayer(texture);
		}

		return showOutline ? RenderLayers.outlineNoCull(texture) : null;
	}

	public static int getOverlay(LivingEntityRenderState state, float whiteOverlayProgress) {
		return OverlayTexture.packUv(OverlayTexture.getU(whiteOverlayProgress), OverlayTexture.getV(state.hurt));
	}

	protected boolean isVisible(S state) {
		return !state.invisible;
	}

	private static float getYaw(Direction direction) {
		return switch (direction) {
			case SOUTH -> 90.0F;
			case WEST -> 0.0F;
			case NORTH -> 270.0F;
			case EAST -> 180.0F;
			default -> 0.0F;
		};
	}

	protected boolean isShaking(S state) {
		return state.shaking;
	}

	/**
	 * Применяет трансформации тела: поворот при смерти, riptide-вращение, поза сна, переворот вверх ногами.
	 * Вызывается перед рендерингом модели.
	 */
	protected void setupTransforms(S state, MatrixStack matrices, float bodyYaw, float baseHeight) {
		if (this.isShaking(state)) {
			bodyYaw += (float) (Math.cos(MathHelper.floor(state.age) * 3.25F) * Math.PI * 0.4F);
		}

		if (!state.isInPose(EntityPose.SLEEPING)) {
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
		}

		if (state.deathTime > 0.0F) {
			float deathProgress = (state.deathTime - 1.0F) / 20.0F * DEATH_ROTATION_SCALE;
			deathProgress = MathHelper.sqrt(deathProgress);
			if (deathProgress > DEATH_ROTATION_MAX) {
				deathProgress = DEATH_ROTATION_MAX;
			}

			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(deathProgress * this.getLyingPositionRotationDegrees()));
		} else if (state.usingRiptide) {
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(RIPTIDE_PITCH_OFFSET - state.pitch));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.age * RIPTIDE_YAW_MULTIPLIER));
		} else if (state.isInPose(EntityPose.SLEEPING)) {
			Direction sleepDir = state.sleepingDirection;
			float sleepYaw = sleepDir != null ? getYaw(sleepDir) : bodyYaw;
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sleepYaw));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.getLyingPositionRotationDegrees()));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270.0F));
		} else if (state.flipUpsideDown) {
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

	@Override
	protected boolean hasLabel(T livingEntity, double distance) {
		if (livingEntity.isSneaky() && distance >= SNEAKY_LABEL_MAX_DISTANCE) {
			return false;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity localPlayer = client.player;
		boolean notInvisible = !livingEntity.isInvisibleTo(localPlayer);

		if (livingEntity == localPlayer) {
			return MinecraftClient.isHudEnabled()
					&& livingEntity != client.getCameraEntity()
					&& notInvisible
					&& !livingEntity.hasPassengers();
		}

		AbstractTeam entityTeam = livingEntity.getScoreboardTeam();
		if (entityTeam == null) {
			return MinecraftClient.isHudEnabled()
					&& livingEntity != client.getCameraEntity()
					&& notInvisible
					&& !livingEntity.hasPassengers();
		}

		AbstractTeam playerTeam = localPlayer.getScoreboardTeam();
		return switch (entityTeam.getNameTagVisibilityRule()) {
			case ALWAYS -> notInvisible;
			case NEVER -> false;
			case HIDE_FOR_OTHER_TEAMS -> playerTeam == null
					? notInvisible
					: entityTeam.isEqual(playerTeam) && (entityTeam.shouldShowFriendlyInvisibles() || notInvisible);
			case HIDE_FOR_OWN_TEAM -> playerTeam == null ? notInvisible : !entityTeam.isEqual(playerTeam) && notInvisible;
		};
	}

	/**
	 * Проверяет, нужно ли перевернуть существо вверх ногами по кастомному имени (Dinnerbone/Grumm).
	 */
	public boolean shouldFlipUpsideDown(T entity) {
		Text customName = entity.getCustomName();
		return customName != null && shouldFlipUpsideDown(customName.getString());
	}

	protected static boolean shouldFlipUpsideDown(String name) {
		return "Dinnerbone".equals(name) || "Grumm".equals(name);
	}

	@Override
	protected float getShadowRadius(S livingEntityRenderState) {
		return super.getShadowRadius(livingEntityRenderState) * livingEntityRenderState.baseScale;
	}

	@Override
	public void updateRenderState(T livingEntity, S livingEntityRenderState, float tickProgress) {
		super.updateRenderState(livingEntity, livingEntityRenderState, tickProgress);

		float headYaw = MathHelper.lerpAngleDegrees(tickProgress, livingEntity.lastHeadYaw, livingEntity.headYaw);
		livingEntityRenderState.bodyYaw = clampBodyYaw(livingEntity, headYaw, tickProgress);
		livingEntityRenderState.relativeHeadYaw = MathHelper.wrapDegrees(headYaw - livingEntityRenderState.bodyYaw);
		livingEntityRenderState.pitch = livingEntity.getLerpedPitch(tickProgress);
		livingEntityRenderState.flipUpsideDown = this.shouldFlipUpsideDown(livingEntity);

		if (livingEntityRenderState.flipUpsideDown) {
			livingEntityRenderState.pitch *= -1.0F;
			livingEntityRenderState.relativeHeadYaw *= -1.0F;
		}

		if (!livingEntity.hasVehicle() && livingEntity.isAlive()) {
			livingEntityRenderState.limbSwingAnimationProgress = livingEntity.limbAnimator.getAnimationProgress(tickProgress);
			livingEntityRenderState.limbSwingAmplitude = livingEntity.limbAnimator.getAmplitude(tickProgress);
		} else {
			livingEntityRenderState.limbSwingAnimationProgress = 0.0F;
			livingEntityRenderState.limbSwingAmplitude = 0.0F;
		}

		if (livingEntity.getVehicle() instanceof LivingEntity vehicle) {
			livingEntityRenderState.headItemAnimationProgress = vehicle.limbAnimator.getAnimationProgress(tickProgress);
		} else {
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
		livingEntityRenderState.timeSinceLastKineticAttack = livingEntity.getTimeSinceLastKineticAttack(tickProgress);
		livingEntityRenderState.hurt = livingEntity.hurtTime > 0 || livingEntity.deathTime > 0;

		ItemStack headStack = livingEntity.getEquippedStack(EquipmentSlot.HEAD);
		if (headStack.getItem() instanceof BlockItem blockItem
				&& blockItem.getBlock() instanceof AbstractSkullBlock skullBlock) {
			livingEntityRenderState.wearingSkullType = skullBlock.getSkullType();
			livingEntityRenderState.wearingSkullProfile = headStack.get(DataComponentTypes.PROFILE);
			livingEntityRenderState.headItemRenderState.clear();
		} else {
			livingEntityRenderState.wearingSkullType = null;
			livingEntityRenderState.wearingSkullProfile = null;
			if (!ArmorFeatureRenderer.hasModel(headStack, EquipmentSlot.HEAD)) {
				this.itemModelResolver.updateForLivingEntity(
						livingEntityRenderState.headItemRenderState,
						headStack,
						ItemDisplayContext.HEAD,
						livingEntity
				);
			} else {
				livingEntityRenderState.headItemRenderState.clear();
			}
		}

		livingEntityRenderState.deathTime = livingEntity.deathTime > 0 ? livingEntity.deathTime + tickProgress : 0.0F;

		MinecraftClient client = MinecraftClient.getInstance();
		livingEntityRenderState.invisibleToPlayer =
				livingEntityRenderState.invisible && livingEntity.isInvisibleTo(client.player);
	}

	private static float clampBodyYaw(LivingEntity entity, float headYawDegrees, float tickProgress) {
		if (!(entity.getVehicle() instanceof LivingEntity vehicle)) {
			return MathHelper.lerpAngleDegrees(tickProgress, entity.lastBodyYaw, entity.bodyYaw);
		}

		float vehicleBodyYaw = MathHelper.lerpAngleDegrees(tickProgress, vehicle.lastBodyYaw, vehicle.bodyYaw);
		float maxYawOffset = 85.0F;
		float clampedOffset = MathHelper.clamp(MathHelper.wrapDegrees(headYawDegrees - vehicleBodyYaw), -maxYawOffset, maxYawOffset);
		float bodyYaw = headYawDegrees - clampedOffset;

		if (Math.abs(clampedOffset) > 50.0F) {
			bodyYaw += clampedOffset * 0.2F;
		}

		return bodyYaw;
	}
}
