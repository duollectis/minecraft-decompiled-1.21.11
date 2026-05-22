package net.minecraft.client.render.item;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.Lancing;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Отвечает за рендеринг предметов в руках игрока от первого лица.
 * Управляет анимациями экипировки, замаха, поедания, использования лука/арбалета/трезубца,
 * а также отображением карт в одной и двух руках.
 */
@Environment(EnvType.CLIENT)
public class HeldItemRenderer {

	private static final RenderLayer MAP_BACKGROUND =
			RenderLayers.text(Identifier.ofVanilla("textures/map/map_background.png"));
	private static final RenderLayer MAP_BACKGROUND_CHECKERBOARD =
			RenderLayers.text(Identifier.ofVanilla("textures/map/map_background_checkerboard.png"));
	private static final float SWING_TRANSLATE_X = -0.4F;
	private static final float SWING_TRANSLATE_Y = 0.2F;
	private static final float SWING_TRANSLATE_Z = -0.2F;
	private static final float SWING_EQUIP_Y_OFFSET = -0.6F;
	private static final float EQUIP_OFFSET_TRANSLATE_X = 0.56F;
	private static final float EQUIP_OFFSET_TRANSLATE_Y = -0.52F;
	private static final float EQUIP_OFFSET_TRANSLATE_Z = -0.72F;
	private static final float SWING_OFFSET_Y_ANGLE = 45.0F;
	private static final float SWING_OFFSET_X_ANGLE = -80.0F;
	private static final float SWING_OFFSET_Y_ANGLE_2 = -20.0F;
	private static final float SWING_OFFSET_Z_ANGLE = -20.0F;
	private static final float EAT_OR_DRINK_X_ANGLE_MULTIPLIER = 10.0F;
	private static final float EAT_OR_DRINK_Y_ANGLE_MULTIPLIER = 90.0F;
	private static final float EAT_OR_DRINK_Z_ANGLE_MULTIPLIER = 30.0F;
	private static final float EAT_TRANSLATE_X = 0.6F;
	private static final float EAT_TRANSLATE_Y = -0.5F;
	private static final double EAT_POWER_EXPONENT = 27.0;
	private static final float EAT_PROGRESS_THRESHOLD = 0.8F;
	private static final float EAT_WOBBLE_AMPLITUDE = 0.1F;
	private static final float EAT_WOBBLE_TRANSLATE_X = -0.3F;
	private static final float EAT_WOBBLE_TRANSLATE_Y = 0.4F;
	private static final float EAT_WOBBLE_TRANSLATE_Z = -0.4F;
	private static final float ARM_HOLDING_ITEM_SECOND_Y_ANGLE_MULTIPLIER = 70.0F;
	private static final float ARM_HOLDING_ITEM_FIRST_Z_ANGLE_MULTIPLIER = -20.0F;
	private static final float ARM_HOLD_TRANSLATE_X = -0.6F;
	private static final float ARM_HOLD_TRANSLATE_Y = 0.8F;
	private static final float ARM_HOLD_TRANSLATE_Z = 0.8F;
	private static final float ARM_HOLD_SWING_X = -0.75F;
	private static final float ARM_HOLD_SWING_Y = -0.9F;
	private static final float ARM_HOLD_TRANSLATE_UP = 3.6F;
	private static final float ARM_HOLD_TRANSLATE_FORWARD = 3.5F;
	private static final float ARM_HOLDING_ITEM_TRANSLATE_X = 5.6F;
	private static final int ARM_HOLDING_ITEM_X_ANGLE_MULTIPLIER = 200;
	private static final int ARM_HOLDING_ITEM_THIRD_Y_ANGLE_MULTIPLIER = -135;
	private static final int ARM_HOLDING_ITEM_SECOND_Z_ANGLE_MULTIPLIER = 120;
	private static final float MAP_ONE_HAND_SWING_X = -0.4F;
	private static final float MAP_ONE_HAND_SWING_Y = -0.2F;
	private static final float MAP_ONE_HAND_WOBBLE_Y = 0.04F;
	private static final float MAP_ONE_HAND_TRANSLATE_Z = -0.72F;
	private static final float MAP_ONE_HAND_EQUIP_Y = -1.2F;
	private static final float MAP_ONE_HAND_TRANSLATE_X = -0.5F;
	private static final float MAP_ONE_HAND_X_ANGLE = 45.0F;
	private static final float MAP_ONE_HAND_Y_ANGLE = -85.0F;
	private static final float ARM_X_ANGLE_MULTIPLIER = 45.0F;
	private static final float ARM_Y_ANGLE_MULTIPLIER = 92.0F;
	private static final float ARM_Z_ANGLE_MULTIPLIER = -41.0F;
	private static final float ARM_TRANSLATE_X = 0.3F;
	private static final float ARM_TRANSLATE_Y = -1.1F;
	private static final float ARM_TRANSLATE_Z = 0.45F;
	private static final float MAP_BOTH_HANDS_SWING_ANGLE = 20.0F;
	private static final float FIRST_PERSON_MAP_FIRST_SCALE = 0.38F;
	private static final float FIRST_PERSON_MAP_TRANSLATE_XY = -0.5F;
	private static final float FIRST_PERSON_MAP_SECOND_SCALE = 0.0078125F;
	private static final int MAP_BORDER_PIXELS = 7;
	private static final int MAP_TEXTURE_WIDTH = 128;
	private static final int MAP_TEXTURE_HEIGHT = 128;
	private static final float MAP_VERTEX_Z_OFFSET = 0.04F;
	private static final float MAP_VERTEX_WOBBLE = 0.004F;
	private static final float MAP_SCALE_FACTOR = 0.2F;

	private final MinecraftClient client;
	private final MapRenderState mapRenderState = new MapRenderState();
	private ItemStack mainHand = ItemStack.EMPTY;
	private ItemStack offHand = ItemStack.EMPTY;
	private float equipProgressMainHand;
	private float lastEquipProgressMainHand;
	private float equipProgressOffHand;
	private float lastEquipProgressOffHand;
	private final EntityRenderManager entityRenderDispatcher;
	private final ItemModelManager itemModelManager;

	public HeldItemRenderer(
			MinecraftClient client,
			EntityRenderManager entityRenderDispatcher,
			ItemModelManager itemModelManager
	) {
		this.client = client;
		this.entityRenderDispatcher = entityRenderDispatcher;
		this.itemModelManager = itemModelManager;
	}

	public void renderItem(
			LivingEntity entity,
			ItemStack stack,
			ItemDisplayContext renderMode,
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light
	) {
		if (stack.isEmpty()) {
			return;
		}

		ItemRenderState itemRenderState = new ItemRenderState();
		this.itemModelManager.clearAndUpdate(
				itemRenderState,
				stack,
				renderMode,
				entity.getEntityWorld(),
				entity,
				entity.getId() + renderMode.ordinal()
		);
		itemRenderState.render(matrices, orderedRenderCommandQueue, light, OverlayTexture.DEFAULT_UV, 0);
	}

	private float getMapAngle(float tickProgress) {
		float angle = 1.0F - tickProgress / 45.0F + 0.1F;
		angle = MathHelper.clamp(angle, 0.0F, 1.0F);
		return -MathHelper.cos(angle * (float) Math.PI) * 0.5F + 0.5F;
	}

	private void renderArm(
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light,
			Arm arm
	) {
		PlayerEntityRenderer<AbstractClientPlayerEntity> playerEntityRenderer =
				this.entityRenderDispatcher.getPlayerRenderer(this.client.player);
		matrices.push();
		float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ARM_Y_ANGLE_MULTIPLIER));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(SWING_OFFSET_Y_ANGLE));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * ARM_Z_ANGLE_MULTIPLIER));
		matrices.translate(side * ARM_TRANSLATE_X, ARM_TRANSLATE_Y, ARM_TRANSLATE_Z);
		Identifier skinTexture = this.client.player.getSkin().body().texturePath();

		if (arm == Arm.RIGHT) {
			playerEntityRenderer.renderRightArm(
					matrices,
					orderedRenderCommandQueue,
					light,
					skinTexture,
					this.client.player.isModelPartVisible(PlayerModelPart.RIGHT_SLEEVE)
			);
		} else {
			playerEntityRenderer.renderLeftArm(
					matrices,
					orderedRenderCommandQueue,
					light,
					skinTexture,
					this.client.player.isModelPartVisible(PlayerModelPart.LEFT_SLEEVE)
			);
		}

		matrices.pop();
	}

	private void renderMapInOneHand(
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light,
			float equipProgress,
			Arm arm,
			float swingProgress,
			ItemStack stack
	) {
		float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
		matrices.translate(side * 0.125F, -0.125F, 0.0F);

		if (!this.client.player.isInvisible()) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 10.0F));
			this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
			matrices.pop();
		}

		matrices.push();
		matrices.translate(side * 0.51F, -0.08F + equipProgress * MAP_ONE_HAND_EQUIP_Y, ARM_HOLD_SWING_X);
		float swingSqrt = MathHelper.sqrt(swingProgress);
		float swingSin = MathHelper.sin(swingSqrt * (float) Math.PI);
		float swingOffsetX = -0.5F * swingSin;
		float swingOffsetY = 0.4F * MathHelper.sin(swingSqrt * (float) (Math.PI * 2));
		float swingOffsetZ = -0.3F * MathHelper.sin(swingProgress * (float) Math.PI);
		matrices.translate(side * swingOffsetX, swingOffsetY - ARM_TRANSLATE_X * swingSin, swingOffsetZ);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingSin * -45.0F));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * swingSin * -EAT_OR_DRINK_Z_ANGLE_MULTIPLIER));
		this.renderFirstPersonMap(matrices, orderedRenderCommandQueue, light, stack);
		matrices.pop();
	}

	private void renderMapInBothHands(
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light,
			float pitch,
			float equipProgress,
			float swingProgress
	) {
		float swingSqrt = MathHelper.sqrt(swingProgress);
		float swingOffsetY = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
		float swingOffsetZ = -0.4F * MathHelper.sin(swingSqrt * (float) Math.PI);
		matrices.translate(0.0F, -swingOffsetY / 2.0F, swingOffsetZ);
		float mapAngle = this.getMapAngle(pitch);
		matrices.translate(
				0.0F,
				MAP_ONE_HAND_WOBBLE_Y + equipProgress * MAP_ONE_HAND_EQUIP_Y + mapAngle * EAT_TRANSLATE_Y,
				EQUIP_OFFSET_TRANSLATE_Z
		);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mapAngle * MAP_ONE_HAND_Y_ANGLE));

		if (!this.client.player.isInvisible()) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(EAT_OR_DRINK_Y_ANGLE_MULTIPLIER));
			this.renderArm(matrices, orderedRenderCommandQueue, light, Arm.RIGHT);
			this.renderArm(matrices, orderedRenderCommandQueue, light, Arm.LEFT);
			matrices.pop();
		}

		float swingFinalSin = MathHelper.sin(swingSqrt * (float) Math.PI);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingFinalSin * MAP_BOTH_HANDS_SWING_ANGLE));
		matrices.scale(2.0F, 2.0F, 2.0F);
		this.renderFirstPersonMap(matrices, orderedRenderCommandQueue, light, this.mainHand);
	}

	private void renderFirstPersonMap(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			ItemStack stack
	) {
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
		matrices.scale(FIRST_PERSON_MAP_FIRST_SCALE, FIRST_PERSON_MAP_FIRST_SCALE, FIRST_PERSON_MAP_FIRST_SCALE);
		matrices.translate(FIRST_PERSON_MAP_TRANSLATE_XY, FIRST_PERSON_MAP_TRANSLATE_XY, 0.0F);
		matrices.scale(FIRST_PERSON_MAP_SECOND_SCALE, FIRST_PERSON_MAP_SECOND_SCALE, FIRST_PERSON_MAP_SECOND_SCALE);
		MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
		MapState mapState = FilledMapItem.getMapState(mapIdComponent, this.client.world);
		RenderLayer renderLayer = mapState == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD;
		queue.submitCustom(
				matrices, renderLayer, (matricesEntry, vertexConsumer) -> {
					vertexConsumer
							.vertex(matricesEntry, -7.0F, 135.0F, 0.0F)
							.color(-1)
							.texture(0.0F, 1.0F)
							.light(light);
					vertexConsumer
							.vertex(matricesEntry, 135.0F, 135.0F, 0.0F)
							.color(-1)
							.texture(1.0F, 1.0F)
							.light(light);
					vertexConsumer
							.vertex(matricesEntry, 135.0F, -7.0F, 0.0F)
							.color(-1)
							.texture(1.0F, 0.0F)
							.light(light);
					vertexConsumer
							.vertex(matricesEntry, -7.0F, -7.0F, 0.0F)
							.color(-1)
							.texture(0.0F, 0.0F)
							.light(light);
				}
		);

		if (mapState == null) {
			return;
		}

		MapRenderer mapRenderer = this.client.getMapRenderer();
		mapRenderer.update(mapIdComponent, mapState, this.mapRenderState);
		mapRenderer.draw(this.mapRenderState, matrices, queue, false, light);
	}

	private void renderArmHoldingItem(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			float equipProgress,
			float swingProgress,
			Arm arm
	) {
		boolean isRightArm = arm != Arm.LEFT;
		float side = isRightArm ? 1.0F : -1.0F;
		float swingSqrt = MathHelper.sqrt(swingProgress);
		float swingOffsetX = -0.3F * MathHelper.sin(swingSqrt * (float) Math.PI);
		float swingOffsetY = 0.4F * MathHelper.sin(swingSqrt * (float) (Math.PI * 2));
		float swingOffsetZ = -0.4F * MathHelper.sin(swingProgress * (float) Math.PI);
		matrices.translate(
				side * (swingOffsetX + 0.64000005F),
				swingOffsetY + ARM_HOLD_TRANSLATE_X + equipProgress * -0.6F,
				swingOffsetZ + EQUIP_OFFSET_TRANSLATE_Z
		);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * SWING_OFFSET_Y_ANGLE));
		float swingSquaredSin = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
		float swingSin = MathHelper.sin(swingSqrt * (float) Math.PI);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * swingSin * ARM_HOLDING_ITEM_SECOND_Y_ANGLE_MULTIPLIER));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * swingSquaredSin * -MAP_BOTH_HANDS_SWING_ANGLE));
		matrices.translate(side * -1.0F, ARM_HOLD_TRANSLATE_UP, ARM_HOLD_TRANSLATE_FORWARD);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * ARM_HOLDING_ITEM_SECOND_Z_ANGLE_MULTIPLIER));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ARM_HOLDING_ITEM_X_ANGLE_MULTIPLIER));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * ARM_HOLDING_ITEM_THIRD_Y_ANGLE_MULTIPLIER));
		matrices.translate(side * ARM_HOLDING_ITEM_TRANSLATE_X, 0.0F, 0.0F);
		PlayerEntityRenderer<AbstractClientPlayerEntity> playerEntityRenderer =
				this.entityRenderDispatcher.getPlayerRenderer(this.client.player);
		Identifier skinTexture = this.client.player.getSkin().body().texturePath();

		if (isRightArm) {
			playerEntityRenderer.renderRightArm(
					matrices,
					queue,
					light,
					skinTexture,
					this.client.player.isModelPartVisible(PlayerModelPart.RIGHT_SLEEVE)
			);
		} else {
			playerEntityRenderer.renderLeftArm(
					matrices,
					queue,
					light,
					skinTexture,
					this.client.player.isModelPartVisible(PlayerModelPart.LEFT_SLEEVE)
			);
		}
	}

	private void applyEatOrDrinkTransformation(
			MatrixStack matrices,
			float tickProgress,
			Arm arm,
			ItemStack stack,
			PlayerEntity player
	) {
		float useTimeLeft = player.getItemUseTimeLeft() - tickProgress + 1.0F;
		float useProgress = useTimeLeft / stack.getMaxUseTime(player);

		if (useProgress < EAT_PROGRESS_THRESHOLD) {
			float wobble = MathHelper.abs(MathHelper.cos(useTimeLeft / 4.0F * (float) Math.PI) * 0.1F);
			matrices.translate(0.0F, wobble, 0.0F);
		}

		float eatProgress = 1.0F - (float) Math.pow(useProgress, EAT_POWER_EXPONENT);
		int side = arm == Arm.RIGHT ? 1 : -1;
		matrices.translate(eatProgress * EAT_TRANSLATE_X * side, eatProgress * EAT_TRANSLATE_Y, 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * eatProgress * EAT_OR_DRINK_Y_ANGLE_MULTIPLIER));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(eatProgress * 10.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * eatProgress * EAT_OR_DRINK_Z_ANGLE_MULTIPLIER));
	}

	private void applyBrushTransformation(
			MatrixStack matrices,
			float tickProgress,
			Arm arm,
			PlayerEntity playerEntity
	) {
		float useTimeMod = playerEntity.getItemUseTimeLeft() % 10;
		float cycleProgress = useTimeMod - tickProgress + 1.0F;
		float normalizedProgress = 1.0F - cycleProgress / 10.0F;
		float brushAngle = -15.0F + 75.0F * MathHelper.cos(normalizedProgress * 2.0F * (float) Math.PI);

		if (arm != Arm.RIGHT) {
			matrices.translate(0.1, 0.83, 0.35);
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(SWING_OFFSET_X_ANGLE));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(brushAngle));
			matrices.translate(EAT_WOBBLE_TRANSLATE_X, 0.22, 0.35);
		} else {
			matrices.translate(-0.25, 0.22, 0.35);
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(SWING_OFFSET_X_ANGLE));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(EAT_OR_DRINK_Y_ANGLE_MULTIPLIER));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(brushAngle));
		}
	}

	private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
		int side = arm == Arm.RIGHT ? 1 : -1;
		float swingSquaredSin = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (SWING_OFFSET_Y_ANGLE + swingSquaredSin * -MAP_BOTH_HANDS_SWING_ANGLE)));
		float swingSqrtSin = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * swingSqrtSin * -MAP_BOTH_HANDS_SWING_ANGLE));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingSqrtSin * SWING_OFFSET_X_ANGLE));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
	}

	private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
		int side = arm == Arm.RIGHT ? 1 : -1;
		matrices.translate(side * EQUIP_OFFSET_TRANSLATE_X, EQUIP_OFFSET_TRANSLATE_Y + equipProgress * SWING_EQUIP_Y_OFFSET, EQUIP_OFFSET_TRANSLATE_Z);
	}

	/**
	 * Рендерит предметы в обеих руках игрока от первого лица с учётом анимаций замаха,
	 * экипировки и поворота камеры. Вызывается каждый кадр из игрового рендерера.
	 */
	public void renderItem(
			float tickProgress,
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			ClientPlayerEntity player,
			int light
	) {
		float swingProgress = player.getHandSwingProgress(tickProgress);
		Hand preferredHand = (Hand) MoreObjects.firstNonNull(player.preferredHand, Hand.MAIN_HAND);
		float pitch = player.getLerpedPitch(tickProgress);
		HandRenderType handRenderType = getHandRenderType(player);
		float lerpedPitch = MathHelper.lerp(tickProgress, player.lastRenderPitch, player.renderPitch);
		float lerpedYaw = MathHelper.lerp(tickProgress, player.lastRenderYaw, player.renderYaw);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((player.getPitch(tickProgress) - lerpedPitch) * 0.1F));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((player.getYaw(tickProgress) - lerpedYaw) * 0.1F));

		if (handRenderType.renderMainHand) {
			float mainSwing = preferredHand == Hand.MAIN_HAND ? swingProgress : 0.0F;
			float mainEquip = this.itemModelManager.getSwapAnimationScale(this.mainHand)
					* (1.0F - MathHelper.lerp(tickProgress, this.lastEquipProgressMainHand, this.equipProgressMainHand));
			this.renderFirstPersonItem(
					player,
					tickProgress,
					pitch,
					Hand.MAIN_HAND,
					mainSwing,
					this.mainHand,
					mainEquip,
					matrices,
					orderedRenderCommandQueue,
					light
			);
		}

		if (handRenderType.renderOffHand) {
			float offSwing = preferredHand == Hand.OFF_HAND ? swingProgress : 0.0F;
			float offEquip = this.itemModelManager.getSwapAnimationScale(this.offHand)
					* (1.0F - MathHelper.lerp(tickProgress, this.lastEquipProgressOffHand, this.equipProgressOffHand));
			this.renderFirstPersonItem(
					player,
					tickProgress,
					pitch,
					Hand.OFF_HAND,
					offSwing,
					this.offHand,
					offEquip,
					matrices,
					orderedRenderCommandQueue,
					light
			);
		}

		this.client.gameRenderer.getEntityRenderDispatcher().render();
		this.client.getBufferBuilders().getEntityVertexConsumers().draw();
	}

	@VisibleForTesting
	static HandRenderType getHandRenderType(ClientPlayerEntity player) {
		ItemStack mainHandStack = player.getMainHandStack();
		ItemStack offHandStack = player.getOffHandStack();
		boolean hasBow = mainHandStack.isOf(Items.BOW) || offHandStack.isOf(Items.BOW);
		boolean hasCrossbow = mainHandStack.isOf(Items.CROSSBOW) || offHandStack.isOf(Items.CROSSBOW);

		if (!hasBow && !hasCrossbow) {
			return HandRenderType.RENDER_BOTH_HANDS;
		}

		if (player.isUsingItem()) {
			return getUsingItemHandRenderType(player);
		}

		return isChargedCrossbow(mainHandStack)
				? HandRenderType.RENDER_MAIN_HAND_ONLY
				: HandRenderType.RENDER_BOTH_HANDS;
	}

	private static HandRenderType getUsingItemHandRenderType(ClientPlayerEntity player) {
		ItemStack activeItem = player.getActiveItem();
		Hand activeHand = player.getActiveHand();

		if (!activeItem.isOf(Items.BOW) && !activeItem.isOf(Items.CROSSBOW)) {
			return activeHand == Hand.MAIN_HAND && isChargedCrossbow(player.getOffHandStack())
					? HandRenderType.RENDER_MAIN_HAND_ONLY
					: HandRenderType.RENDER_BOTH_HANDS;
		}

		return HandRenderType.shouldOnlyRender(activeHand);
	}

	private static boolean isChargedCrossbow(ItemStack stack) {
		return stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
	}

	/**
	 * Рендерит предмет в конкретной руке от первого лица, выбирая нужный режим анимации
	 * в зависимости от типа предмета (карта, арбалет, лук, трезубец, еда и т.д.).
	 */
	private void renderFirstPersonItem(
			AbstractClientPlayerEntity player,
			float tickProgress,
			float pitch,
			Hand hand,
			float swingProgress,
			ItemStack item,
			float equipProgress,
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light
	) {
		if (player.isUsingSpyglass()) {
			return;
		}

		boolean isMainHand = hand == Hand.MAIN_HAND;
		Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
		matrices.push();

		if (item.isEmpty()) {
			if (isMainHand && !player.isInvisible()) {
				this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
			}
		} else if (item.contains(DataComponentTypes.MAP_ID)) {
			if (isMainHand && this.offHand.isEmpty()) {
				this.renderMapInBothHands(matrices, orderedRenderCommandQueue, light, pitch, equipProgress, swingProgress);
			} else {
				this.renderMapInOneHand(matrices, orderedRenderCommandQueue, light, equipProgress, arm, swingProgress, item);
			}
		} else if (item.isOf(Items.CROSSBOW)) {
			this.applyEquipOffset(matrices, arm, equipProgress);
			boolean isCharged = CrossbowItem.isCharged(item);
			boolean isRightArm = arm == Arm.RIGHT;
			int side = isRightArm ? 1 : -1;

			if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand && !isCharged) {
				matrices.translate(side * -0.4785682F, -0.094387F, 0.05731531F);
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 65.3F));
				matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -9.785F));
				float pullTime = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickProgress + 1.0F);
				float pullProgress = pullTime / CrossbowItem.getPullTime(item, player);

				if (pullProgress > 1.0F) {
					pullProgress = 1.0F;
				}

				if (pullProgress > EAT_WOBBLE_AMPLITUDE) {
					float wobbleSin = MathHelper.sin((pullTime - 0.1F) * 1.3F);
					float wobbleScale = pullProgress - 0.1F;
					float wobble = wobbleSin * wobbleScale;
					matrices.translate(wobble * 0.0F, wobble * MAP_VERTEX_WOBBLE, wobble * 0.0F);
				}

				matrices.translate(pullProgress * 0.0F, pullProgress * 0.0F, pullProgress * MAP_ONE_HAND_WOBBLE_Y);
				matrices.scale(1.0F, 1.0F, 1.0F + pullProgress * MAP_SCALE_FACTOR);
				matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(side * SWING_OFFSET_Y_ANGLE));
			} else {
				this.swingArm(swingProgress, matrices, side, arm);

				if (isCharged && swingProgress < 0.001F && isMainHand) {
					matrices.translate(side * -0.641864F, 0.0F, 0.0F);
					matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 10.0F));
				}
			}

			this.renderItem(
					player,
					item,
					isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
					matrices,
					orderedRenderCommandQueue,
					light
			);
		} else {
			boolean isRightArm = arm == Arm.RIGHT;
			int side = isRightArm ? 1 : -1;

			if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
				UseAction useAction = item.getUseAction();

				if (!useAction.hasNoOffset()) {
					this.applyEquipOffset(matrices, arm, equipProgress);
				}

				switch (useAction) {
					case NONE:
					default:
						break;
					case EAT:
					case DRINK:
						this.applyEatOrDrinkTransformation(matrices, tickProgress, arm, item, player);
						this.applyEquipOffset(matrices, arm, equipProgress);
						break;
					case BLOCK:
						if (!(item.getItem() instanceof ShieldItem)) {
							matrices.translate(side * -0.14142136F, 0.08F, 0.14142136F);
							matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-102.25F));
							matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 13.365F));
							matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 78.05F));
						}

						break;
					case BOW:
						matrices.translate(side * -0.2785682F, 0.18344387F, 0.15731531F);
						matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
						matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 35.3F));
						matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -9.785F));
						float bowPullTime = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickProgress + 1.0F);
						float bowProgress = bowPullTime / MAP_BOTH_HANDS_SWING_ANGLE;
						bowProgress = (bowProgress * bowProgress + bowProgress * 2.0F) / 3.0F;

						if (bowProgress > 1.0F) {
							bowProgress = 1.0F;
						}

						if (bowProgress > EAT_WOBBLE_AMPLITUDE) {
							float wobbleSin = MathHelper.sin((bowPullTime - 0.1F) * 1.3F);
							float wobbleScale = bowProgress - 0.1F;
							float wobble = wobbleSin * wobbleScale;
							matrices.translate(wobble * 0.0F, wobble * MAP_VERTEX_WOBBLE, wobble * 0.0F);
						}

						matrices.translate(bowProgress * 0.0F, bowProgress * 0.0F, bowProgress * MAP_ONE_HAND_WOBBLE_Y);
						matrices.scale(1.0F, 1.0F, 1.0F + bowProgress * MAP_SCALE_FACTOR);
						matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(side * SWING_OFFSET_Y_ANGLE));
						break;
					case TRIDENT:
						matrices.translate(side * EAT_TRANSLATE_Y, 0.7F, EAT_WOBBLE_AMPLITUDE);
						matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
						matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 35.3F));
						matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -9.785F));
						float tridentPullTime = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickProgress + 1.0F);
						float tridentProgress = tridentPullTime / 10.0F;

						if (tridentProgress > 1.0F) {
							tridentProgress = 1.0F;
						}

						if (tridentProgress > EAT_WOBBLE_AMPLITUDE) {
							float wobbleSin = MathHelper.sin((tridentPullTime - 0.1F) * 1.3F);
							float wobbleScale = tridentProgress - 0.1F;
							float wobble = wobbleSin * wobbleScale;
							matrices.translate(wobble * 0.0F, wobble * MAP_VERTEX_WOBBLE, wobble * 0.0F);
						}

						matrices.translate(0.0F, 0.0F, tridentProgress * SWING_TRANSLATE_Y);
						matrices.scale(1.0F, 1.0F, 1.0F + tridentProgress * MAP_SCALE_FACTOR);
						matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(side * SWING_OFFSET_Y_ANGLE));
						break;
					case BRUSH:
						this.applyBrushTransformation(matrices, tickProgress, arm, player);
						break;
					case BUNDLE:
						this.swingArm(swingProgress, matrices, side, arm);
						break;
					case SPEAR:
						matrices.translate(side * EQUIP_OFFSET_TRANSLATE_X, EQUIP_OFFSET_TRANSLATE_Y, EQUIP_OFFSET_TRANSLATE_Z);
						float spearUseTime = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickProgress + 1.0F);
						Lancing.applyHeldItemLancingTransform(
								player.getTimeSinceLastKineticAttack(tickProgress),
								matrices,
								spearUseTime,
								arm,
								item
						);
				}
			} else if (player.isUsingRiptide()) {
				this.applyEquipOffset(matrices, arm, equipProgress);
				matrices.translate(side * SWING_TRANSLATE_X, ARM_HOLD_TRANSLATE_Y, ARM_TRANSLATE_X);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 65.0F));
				matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * MAP_ONE_HAND_Y_ANGLE));
			} else {
				this.applyEquipOffset(matrices, arm, equipProgress);

				switch (item.getSwingAnimation().type()) {
					case NONE:
					default:
						break;
					case WHACK:
						this.swingArm(swingProgress, matrices, side, arm);
						break;
					case STAB:
						Lancing.applyProjectileTransform(swingProgress, matrices, side, arm);
				}
			}

			this.renderItem(
					player,
					item,
					isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
					matrices,
					orderedRenderCommandQueue,
					light
			);
		}

		matrices.pop();
	}

	private void swingArm(float swingProgress, MatrixStack matrices, int side, Arm arm) {
		float swingOffsetX = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
		float swingOffsetY = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
		float swingOffsetZ = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
		matrices.translate(side * swingOffsetX, swingOffsetY, swingOffsetZ);
		this.applySwingOffset(matrices, arm, swingProgress);
	}

	private boolean shouldSkipHandAnimationOnSwap(ItemStack from, ItemStack to) {
		return ItemStack.shouldSkipHandAnimationOnSwap(from, to, ComponentType::skipsHandAnimation)
				? true
				: !this.itemModelManager.hasHandAnimationOnSwap(to);
	}

	public void updateHeldItems() {
		this.lastEquipProgressMainHand = this.equipProgressMainHand;
		this.lastEquipProgressOffHand = this.equipProgressOffHand;
		ClientPlayerEntity player = this.client.player;
		ItemStack currentMainHand = player.getMainHandStack();
		ItemStack currentOffHand = player.getOffHandStack();

		if (this.shouldSkipHandAnimationOnSwap(this.mainHand, currentMainHand)) {
			this.mainHand = currentMainHand;
		}

		if (this.shouldSkipHandAnimationOnSwap(this.offHand, currentOffHand)) {
			this.offHand = currentOffHand;
		}

		if (player.isRiding()) {
			this.equipProgressMainHand = MathHelper.clamp(this.equipProgressMainHand - EAT_WOBBLE_TRANSLATE_Y, 0.0F, 1.0F);
			this.equipProgressOffHand = MathHelper.clamp(this.equipProgressOffHand - EAT_WOBBLE_TRANSLATE_Y, 0.0F, 1.0F);
		} else {
			float handEquipProgress = player.getHandEquippingProgress(1.0F);
			float mainHandTarget = this.mainHand != currentMainHand ? 0.0F : handEquipProgress * handEquipProgress * handEquipProgress;
			float offHandTarget = this.offHand != currentOffHand ? 0.0F : 1.0F;
			this.equipProgressMainHand =
					this.equipProgressMainHand + MathHelper.clamp(mainHandTarget - this.equipProgressMainHand, MAP_ONE_HAND_SWING_X, EAT_WOBBLE_TRANSLATE_Y);
			this.equipProgressOffHand =
					this.equipProgressOffHand + MathHelper.clamp(offHandTarget - this.equipProgressOffHand, MAP_ONE_HAND_SWING_X, EAT_WOBBLE_TRANSLATE_Y);
		}

		if (this.equipProgressMainHand < EAT_WOBBLE_AMPLITUDE) {
			this.mainHand = currentMainHand;
		}

		if (this.equipProgressOffHand < EAT_WOBBLE_AMPLITUDE) {
			this.offHand = currentOffHand;
		}
	}

	public void resetEquipProgress(Hand hand) {
		if (hand == Hand.MAIN_HAND) {
			this.equipProgressMainHand = 0.0F;
		} else {
			this.equipProgressOffHand = 0.0F;
		}
	}

	/**
	 * Определяет, какие руки нужно рендерить в зависимости от того,
	 * держит ли игрок лук или арбалет и в каком состоянии они находятся.
	 */
	@VisibleForTesting
	@Environment(EnvType.CLIENT)
	static enum HandRenderType {
		RENDER_BOTH_HANDS(true, true),
		RENDER_MAIN_HAND_ONLY(true, false),
		RENDER_OFF_HAND_ONLY(false, true);

		final boolean renderMainHand;
		final boolean renderOffHand;

		private HandRenderType(final boolean renderMainHand, final boolean renderOffHand) {
			this.renderMainHand = renderMainHand;
			this.renderOffHand = renderOffHand;
		}

		public static HandRenderType shouldOnlyRender(Hand hand) {
			return hand == Hand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
		}
	}
}
