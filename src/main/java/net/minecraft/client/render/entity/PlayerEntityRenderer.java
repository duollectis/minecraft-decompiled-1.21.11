package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.ClientPlayerLikeEntity;
import net.minecraft.client.network.ClientPlayerLikeState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.SwingAnimationComponent;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.SwingAnimationType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
/**
 * {@code PlayerEntityRenderer}.
 */
public class PlayerEntityRenderer<AvatarlikeEntity extends PlayerLikeEntity & ClientPlayerLikeEntity>
		extends LivingEntityRenderer<AvatarlikeEntity, PlayerEntityRenderState, PlayerEntityModel> {

	public PlayerEntityRenderer(EntityRendererFactory.Context ctx, boolean slim) {
		super(
				ctx,
				new PlayerEntityModel(
						ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER),
						slim
				),
				0.5F
		);
		EquipmentModelData<EntityModelLayer>
				equipmentLayer =
				slim ? EntityModelLayers.PLAYER_SLIM_EQUIPMENT : EntityModelLayers.PLAYER_EQUIPMENT;
		this.addFeature(
				new ArmorFeatureRenderer<>(
						this,
						EquipmentModelData.mapToEntityModel(
								equipmentLayer,
								ctx.getEntityModels(),
								modelPart -> new PlayerEntityModel(modelPart, slim)
						),
						ctx.getEquipmentRenderer()
				)
		);
		this.addFeature(new PlayerHeldItemFeatureRenderer<>(this));
		this.addFeature(new StuckArrowsFeatureRenderer<>(this, ctx));
		this.addFeature(new Deadmau5FeatureRenderer(this, ctx.getEntityModels()));
		this.addFeature(new CapeFeatureRenderer(this, ctx.getEntityModels(), ctx.getEquipmentModelLoader()));
		this.addFeature(new HeadFeatureRenderer<>(this, ctx.getEntityModels(), ctx.getPlayerSkinCache()));
		this.addFeature(new ElytraFeatureRenderer<>(this, ctx.getEntityModels(), ctx.getEquipmentRenderer()));
		this.addFeature(new ShoulderParrotFeatureRenderer(this, ctx.getEntityModels()));
		this.addFeature(new TridentRiptideFeatureRenderer(this, ctx.getEntityModels()));
		this.addFeature(new StuckStingersFeatureRenderer<>(this, ctx));
	}

	protected boolean shouldRenderFeatures(PlayerEntityRenderState playerEntityRenderState) {
		return !playerEntityRenderState.spectator;
	}

	public Vec3d getPositionOffset(PlayerEntityRenderState playerEntityRenderState) {
		Vec3d vec3d = super.getPositionOffset(playerEntityRenderState);
		return playerEntityRenderState.isInSneakingPose ? vec3d.add(
				0.0,
				playerEntityRenderState.baseScale * -2.0F / 16.0,
				0.0
		) : vec3d;
	}

	private static BipedEntityModel.ArmPose getArmPose(PlayerLikeEntity player, Arm arm) {
		ItemStack itemStack = player.getStackInHand(Hand.MAIN_HAND);
		ItemStack itemStack2 = player.getStackInHand(Hand.OFF_HAND);
		BipedEntityModel.ArmPose armPose = getArmPose(player, itemStack, Hand.MAIN_HAND);
		BipedEntityModel.ArmPose armPose2 = getArmPose(player, itemStack2, Hand.OFF_HAND);
		if (armPose.isTwoHanded()) {
			armPose2 = itemStack2.isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
		}

		return player.getMainArm() == arm ? armPose : armPose2;
	}

	private static BipedEntityModel.ArmPose getArmPose(PlayerLikeEntity player, ItemStack stack, Hand hand) {
		if (stack.isEmpty()) {
			return BipedEntityModel.ArmPose.EMPTY;
		}
		else if (!player.handSwinging && stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
			return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
		}
		else {
			if (player.getActiveHand() == hand && player.getItemUseTimeLeft() > 0) {
				UseAction useAction = stack.getUseAction();
				if (useAction == UseAction.BLOCK) {
					return BipedEntityModel.ArmPose.BLOCK;
				}

				if (useAction == UseAction.BOW) {
					return BipedEntityModel.ArmPose.BOW_AND_ARROW;
				}

				if (useAction == UseAction.TRIDENT) {
					return BipedEntityModel.ArmPose.THROW_TRIDENT;
				}

				if (useAction == UseAction.CROSSBOW) {
					return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
				}

				if (useAction == UseAction.SPYGLASS) {
					return BipedEntityModel.ArmPose.SPYGLASS;
				}

				if (useAction == UseAction.TOOT_HORN) {
					return BipedEntityModel.ArmPose.TOOT_HORN;
				}

				if (useAction == UseAction.BRUSH) {
					return BipedEntityModel.ArmPose.BRUSH;
				}

				if (useAction == UseAction.SPEAR) {
					return BipedEntityModel.ArmPose.SPEAR;
				}
			}

			SwingAnimationComponent swingAnimationComponent = stack.get(DataComponentTypes.SWING_ANIMATION);
			if (swingAnimationComponent != null && swingAnimationComponent.type() == SwingAnimationType.STAB
					&& player.handSwinging) {
				return BipedEntityModel.ArmPose.SPEAR;
			}
			else {
				return stack.isIn(ItemTags.SPEARS) ? BipedEntityModel.ArmPose.SPEAR : BipedEntityModel.ArmPose.ITEM;
			}
		}
	}

	public Identifier getTexture(PlayerEntityRenderState playerEntityRenderState) {
		return playerEntityRenderState.skinTextures.body().texturePath();
	}

	protected void scale(PlayerEntityRenderState playerEntityRenderState, MatrixStack matrixStack) {
		float f = 0.9375F;
		matrixStack.scale(0.9375F, 0.9375F, 0.9375F);
	}

	protected void renderLabelIfPresent(
			PlayerEntityRenderState playerEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		matrixStack.push();
		int i = playerEntityRenderState.extraEars ? -10 : 0;
		if (playerEntityRenderState.playerName != null) {
			orderedRenderCommandQueue.submitLabel(
					matrixStack,
					playerEntityRenderState.nameLabelPos,
					i,
					playerEntityRenderState.playerName,
					!playerEntityRenderState.sneaking,
					playerEntityRenderState.light,
					playerEntityRenderState.squaredDistanceToCamera,
					cameraRenderState
			);
			matrixStack.translate(0.0F, 9.0F * 1.15F * 0.025F, 0.0F);
		}

		if (playerEntityRenderState.displayName != null) {
			orderedRenderCommandQueue.submitLabel(
					matrixStack,
					playerEntityRenderState.nameLabelPos,
					i,
					playerEntityRenderState.displayName,
					!playerEntityRenderState.sneaking,
					playerEntityRenderState.light,
					playerEntityRenderState.squaredDistanceToCamera,
					cameraRenderState
			);
		}

		matrixStack.pop();
	}

	public PlayerEntityRenderState createRenderState() {
		return new PlayerEntityRenderState();
	}

	public void updateRenderState(
			AvatarlikeEntity playerLikeEntity,
			PlayerEntityRenderState playerEntityRenderState,
			float f
	) {
		super.updateRenderState(playerLikeEntity, playerEntityRenderState, f);
		BipedEntityRenderer.updateBipedRenderState(
				playerLikeEntity,
				playerEntityRenderState,
				f,
				this.itemModelResolver
		);
		playerEntityRenderState.leftArmPose = getArmPose(playerLikeEntity, Arm.LEFT);
		playerEntityRenderState.rightArmPose = getArmPose(playerLikeEntity, Arm.RIGHT);
		playerEntityRenderState.skinTextures = playerLikeEntity.getSkin();
		playerEntityRenderState.stuckArrowCount = playerLikeEntity.getStuckArrowCount();
		playerEntityRenderState.stingerCount = playerLikeEntity.getStingerCount();
		playerEntityRenderState.spectator = playerLikeEntity.isSpectator();
		playerEntityRenderState.hatVisible = playerLikeEntity.isModelPartVisible(PlayerModelPart.HAT);
		playerEntityRenderState.jacketVisible = playerLikeEntity.isModelPartVisible(PlayerModelPart.JACKET);
		playerEntityRenderState.leftPantsLegVisible =
				playerLikeEntity.isModelPartVisible(PlayerModelPart.LEFT_PANTS_LEG);
		playerEntityRenderState.rightPantsLegVisible =
				playerLikeEntity.isModelPartVisible(PlayerModelPart.RIGHT_PANTS_LEG);
		playerEntityRenderState.leftSleeveVisible = playerLikeEntity.isModelPartVisible(PlayerModelPart.LEFT_SLEEVE);
		playerEntityRenderState.rightSleeveVisible = playerLikeEntity.isModelPartVisible(PlayerModelPart.RIGHT_SLEEVE);
		playerEntityRenderState.capeVisible = playerLikeEntity.isModelPartVisible(PlayerModelPart.CAPE);
		this.updateGliding(playerLikeEntity, playerEntityRenderState, f);
		this.updateCape(playerLikeEntity, playerEntityRenderState, f);
		if (playerEntityRenderState.squaredDistanceToCamera < 100.0) {
			playerEntityRenderState.playerName = playerLikeEntity.getMannequinName();
		}
		else {
			playerEntityRenderState.playerName = null;
		}

		playerEntityRenderState.leftShoulderParrotVariant = playerLikeEntity.getShoulderParrotVariant(true);
		playerEntityRenderState.rightShoulderParrotVariant = playerLikeEntity.getShoulderParrotVariant(false);
		playerEntityRenderState.id = playerLikeEntity.getId();
		playerEntityRenderState.extraEars = playerLikeEntity.hasExtraEars();
		playerEntityRenderState.spyglassState.clear();
		if (playerEntityRenderState.isUsingItem) {
			ItemStack itemStack = playerLikeEntity.getStackInHand(playerEntityRenderState.activeHand);
			if (itemStack.isOf(Items.SPYGLASS)) {
				this.itemModelResolver.updateForLivingEntity(
						playerEntityRenderState.spyglassState,
						itemStack,
						ItemDisplayContext.HEAD,
						playerLikeEntity
				);
			}
		}
	}

	protected boolean hasLabel(AvatarlikeEntity playerLikeEntity, double d) {
		return super.hasLabel(playerLikeEntity, d)
				&& (playerLikeEntity.shouldRenderName()
				|| playerLikeEntity.hasCustomName() && playerLikeEntity == this.dispatcher.targetedEntity
		);
	}

	private void updateGliding(AvatarlikeEntity player, PlayerEntityRenderState state, float tickProgress) {
		state.glidingTicks = player.getGlidingTicks() + tickProgress;
		Vec3d vec3d = player.getRotationVec(tickProgress);
		Vec3d vec3d2 = player.getState().getVelocity().lerp(player.getVelocity(), tickProgress);
		if (vec3d2.horizontalLengthSquared() > 1.0E-5F && vec3d.horizontalLengthSquared() > 1.0E-5F) {
			state.applyFlyingRotation = true;
			double d = vec3d2.getHorizontal().normalize().dotProduct(vec3d.getHorizontal().normalize());
			double e = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
			state.flyingRotation = (float) (Math.signum(e) * Math.acos(Math.min(1.0, Math.abs(d))));
		}
		else {
			state.applyFlyingRotation = false;
			state.flyingRotation = 0.0F;
		}
	}

	private void updateCape(AvatarlikeEntity player, PlayerEntityRenderState state, float tickProgress) {
		ClientPlayerLikeState clientPlayerLikeState = player.getState();
		double
				d =
				clientPlayerLikeState.lerpX(tickProgress) - MathHelper.lerp(
						(double) tickProgress,
						player.lastX,
						player.getX()
				);
		double
				e =
				clientPlayerLikeState.lerpY(tickProgress) - MathHelper.lerp(
						(double) tickProgress,
						player.lastY,
						player.getY()
				);
		double
				f =
				clientPlayerLikeState.lerpZ(tickProgress) - MathHelper.lerp(
						(double) tickProgress,
						player.lastZ,
						player.getZ()
				);
		float g = MathHelper.lerpAngleDegrees(tickProgress, player.lastBodyYaw, player.bodyYaw);
		double h = MathHelper.sin(g * (float) (Math.PI / 180.0));
		double i = -MathHelper.cos(g * (float) (Math.PI / 180.0));
		state.capeFlap = (float) e * 10.0F;
		state.capeFlap = MathHelper.clamp(state.capeFlap, -6.0F, 32.0F);
		state.capeForwardTilt = (float) (d * h + f * i) * 100.0F;
		state.capeForwardTilt = state.capeForwardTilt * (1.0F - state.getGlidingProgress());
		state.capeForwardTilt = MathHelper.clamp(state.capeForwardTilt, 0.0F, 150.0F);
		state.capeSideTilt = (float) (d * i - f * h) * 100.0F;
		state.capeSideTilt = MathHelper.clamp(state.capeSideTilt, -20.0F, 20.0F);
		float j = clientPlayerLikeState.lerpMovement(tickProgress);
		float k = clientPlayerLikeState.getLerpedDistanceMoved(tickProgress);
		state.capeFlap = state.capeFlap + MathHelper.sin(k * 6.0F) * 32.0F * j;
	}

	public void renderRightArm(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			Identifier skinTexture,
			boolean sleeveVisible
	) {
		this.renderArm(matrices, queue, light, skinTexture, this.model.rightArm, sleeveVisible);
	}

	public void renderLeftArm(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			Identifier skinTexture,
			boolean sleeveVisible
	) {
		this.renderArm(matrices, queue, light, skinTexture, this.model.leftArm, sleeveVisible);
	}

	private void renderArm(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			Identifier skinTexture,
			ModelPart arm,
			boolean sleeveVisible
	) {
		PlayerEntityModel playerEntityModel = this.getModel();
		arm.resetTransform();
		arm.visible = true;
		playerEntityModel.leftSleeve.visible = sleeveVisible;
		playerEntityModel.rightSleeve.visible = sleeveVisible;
		playerEntityModel.leftArm.roll = -0.1F;
		playerEntityModel.rightArm.roll = 0.1F;
		queue.submitModelPart(
				arm,
				matrices,
				RenderLayers.entityTranslucent(skinTexture),
				light,
				OverlayTexture.DEFAULT_UV,
				null
		);
	}

	protected void setupTransforms(
			PlayerEntityRenderState playerEntityRenderState,
			MatrixStack matrixStack,
			float f,
			float g
	) {
		float h = playerEntityRenderState.leaningPitch;
		float i = playerEntityRenderState.pitch;
		if (playerEntityRenderState.isGliding) {
			super.setupTransforms(playerEntityRenderState, matrixStack, f, g);
			float j = playerEntityRenderState.getGlidingProgress();
			if (!playerEntityRenderState.usingRiptide) {
				matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(j * (-90.0F - i)));
			}

			if (playerEntityRenderState.applyFlyingRotation) {
				matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(playerEntityRenderState.flyingRotation));
			}
		}
		else if (h > 0.0F) {
			super.setupTransforms(playerEntityRenderState, matrixStack, f, g);
			float jx = playerEntityRenderState.touchingWater ? -90.0F - i : -90.0F;
			float k = MathHelper.lerp(h, 0.0F, jx);
			matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(k));
			if (playerEntityRenderState.isSwimming) {
				matrixStack.translate(0.0F, -1.0F, 0.3F);
			}
		}
		else {
			super.setupTransforms(playerEntityRenderState, matrixStack, f, g);
		}
	}

	public boolean shouldFlipUpsideDown(AvatarlikeEntity playerLikeEntity) {
		if (playerLikeEntity.isModelPartVisible(PlayerModelPart.CAPE)) {
			return playerLikeEntity instanceof PlayerEntity playerEntity ? shouldFlipUpsideDown(playerEntity)
			                                                             : super.shouldFlipUpsideDown(playerLikeEntity);
		}
		else {
			return false;
		}
	}

	public static boolean shouldFlipUpsideDown(PlayerEntity player) {
		return shouldFlipUpsideDown(player.getGameProfile().name());
	}
}
