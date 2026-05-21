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

@Environment(EnvType.CLIENT)
/**
 * {@code HeldItemRenderer}.
 */
public class HeldItemRenderer {

	private static final RenderLayer
			MAP_BACKGROUND =
			RenderLayers.text(Identifier.ofVanilla("textures/map/map_background.png"));
	private static final RenderLayer
			MAP_BACKGROUND_CHECKERBOARD =
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
	private static final float EAT_TRANSLATE_Z = 0.0F;
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
	private static final float ARM_HOLD_Y_ANGLE = 45.0F;
	private static final float ARM_HOLD_TRANSLATE_SIDE = -1.0F;
	private static final float ARM_HOLD_TRANSLATE_UP = 3.6F;
	private static final float ARM_HOLD_TRANSLATE_FORWARD = 3.5F;
	private static final float ARM_HOLDING_ITEM_TRANSLATE_X = 5.6F;
	private static final int ARM_HOLDING_ITEM_X_ANGLE_MULTIPLIER = 200;
	private static final int ARM_HOLDING_ITEM_THIRD_Y_ANGLE_MULTIPLIER = -135;
	private static final int ARM_HOLDING_ITEM_SECOND_Z_ANGLE_MULTIPLIER = 120;
	private static final float MAP_ONE_HAND_SWING_X = -0.4F;
	private static final float MAP_ONE_HAND_SWING_Y = -0.2F;
	private static final float MAP_ONE_HAND_SWING_Z = 0.0F;
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
	private static final float FIRST_PERSON_MAP_TRANSLATE_X = -0.5F;
	private static final float FIRST_PERSON_MAP_TRANSLATE_Y = -0.5F;
	private static final float FIRST_PERSON_MAP_TRANSLATE_Z = 0.0F;
	private static final float FIRST_PERSON_MAP_SECOND_SCALE = 0.0078125F;
	private static final int MAP_BORDER_PIXELS = 7;
	private static final int MAP_TEXTURE_WIDTH = 128;
	private static final int MAP_TEXTURE_HEIGHT = 128;
	private static final float MAP_VERTEX_X_MIN = 0.0F;
	private static final float MAP_VERTEX_Y_MIN = 0.0F;
	private static final float MAP_VERTEX_Z_OFFSET = 0.04F;
	private static final float MAP_VERTEX_X_MAX = 0.0F;
	private static final float MAP_VERTEX_WOBBLE = 0.004F;
	private static final float MAP_VERTEX_Z_MAX = 0.0F;
	private static final float MAP_SCALE_FACTOR = 0.2F;
	private static final float MAP_SCALE_FACTOR_2 = 0.1F;
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
		if (!stack.isEmpty()) {
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
	}

	private float getMapAngle(float tickProgress) {
		float f = 1.0F - tickProgress / 45.0F + 0.1F;
		f = MathHelper.clamp(f, 0.0F, 1.0F);
		return -MathHelper.cos(f * (float) Math.PI) * 0.5F + 0.5F;
	}

	private void renderArm(
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light,
			Arm arm
	) {
		PlayerEntityRenderer<AbstractClientPlayerEntity>
				playerEntityRenderer =
				this.entityRenderDispatcher.getPlayerRenderer(this.client.player);
		matrices.push();
		float f = arm == Arm.RIGHT ? 1.0F : -1.0F;
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(92.0F));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * -41.0F));
		matrices.translate(f * 0.3F, -1.1F, 0.45F);
		Identifier identifier = this.client.player.getSkin().body().texturePath();
		if (arm == Arm.RIGHT) {
			playerEntityRenderer.renderRightArm(
					matrices,
					orderedRenderCommandQueue,
					light,
					identifier,
					this.client.player.isModelPartVisible(PlayerModelPart.RIGHT_SLEEVE)
			);
		}
		else {
			playerEntityRenderer.renderLeftArm(
					matrices,
					orderedRenderCommandQueue,
					light,
					identifier,
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
		float f = arm == Arm.RIGHT ? 1.0F : -1.0F;
		matrices.translate(f * 0.125F, -0.125F, 0.0F);
		if (!this.client.player.isInvisible()) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 10.0F));
			this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
			matrices.pop();
		}

		matrices.push();
		matrices.translate(f * 0.51F, -0.08F + equipProgress * -1.2F, -0.75F);
		float g = MathHelper.sqrt(swingProgress);
		float h = MathHelper.sin(g * (float) Math.PI);
		float i = -0.5F * h;
		float j = 0.4F * MathHelper.sin(g * (float) (Math.PI * 2));
		float k = -0.3F * MathHelper.sin(swingProgress * (float) Math.PI);
		matrices.translate(f * i, j - 0.3F * h, k);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * -45.0F));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * h * -30.0F));
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
		float f = MathHelper.sqrt(swingProgress);
		float g = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
		float h = -0.4F * MathHelper.sin(f * (float) Math.PI);
		matrices.translate(0.0F, -g / 2.0F, h);
		float i = this.getMapAngle(pitch);
		matrices.translate(0.0F, 0.04F + equipProgress * -1.2F + i * -0.5F, -0.72F);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(i * -85.0F));
		if (!this.client.player.isInvisible()) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
			this.renderArm(matrices, orderedRenderCommandQueue, light, Arm.RIGHT);
			this.renderArm(matrices, orderedRenderCommandQueue, light, Arm.LEFT);
			matrices.pop();
		}

		float j = MathHelper.sin(f * (float) Math.PI);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(j * 20.0F));
		matrices.scale(2.0F, 2.0F, 2.0F);
		this.renderFirstPersonMap(matrices, orderedRenderCommandQueue, light, this.mainHand);
	}

	private void renderFirstPersonMap(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int swingProgress,
			ItemStack stack
	) {
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
		matrices.scale(0.38F, 0.38F, 0.38F);
		matrices.translate(-0.5F, -0.5F, 0.0F);
		matrices.scale(0.0078125F, 0.0078125F, 0.0078125F);
		MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
		MapState mapState = FilledMapItem.getMapState(mapIdComponent, this.client.world);
		RenderLayer renderLayer = mapState == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD;
		queue.submitCustom(
				matrices, renderLayer, (matricesEntry, vertexConsumer) -> {
					vertexConsumer
							.vertex(matricesEntry, -7.0F, 135.0F, 0.0F)
							.color(-1)
							.texture(0.0F, 1.0F)
							.light(swingProgress);
					vertexConsumer
							.vertex(matricesEntry, 135.0F, 135.0F, 0.0F)
							.color(-1)
							.texture(1.0F, 1.0F)
							.light(swingProgress);
					vertexConsumer
							.vertex(matricesEntry, 135.0F, -7.0F, 0.0F)
							.color(-1)
							.texture(1.0F, 0.0F)
							.light(swingProgress);
					vertexConsumer
							.vertex(matricesEntry, -7.0F, -7.0F, 0.0F)
							.color(-1)
							.texture(0.0F, 0.0F)
							.light(swingProgress);
				}
		);
		if (mapState != null) {
			MapRenderer mapRenderer = this.client.getMapRenderer();
			mapRenderer.update(mapIdComponent, mapState, this.mapRenderState);
			mapRenderer.draw(this.mapRenderState, matrices, queue, false, swingProgress);
		}
	}

	private void renderArmHoldingItem(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			float equipProgress,
			float swingProgress,
			Arm arm
	) {
		boolean bl = arm != Arm.LEFT;
		float f = bl ? 1.0F : -1.0F;
		float g = MathHelper.sqrt(swingProgress);
		float h = -0.3F * MathHelper.sin(g * (float) Math.PI);
		float i = 0.4F * MathHelper.sin(g * (float) (Math.PI * 2));
		float j = -0.4F * MathHelper.sin(swingProgress * (float) Math.PI);
		matrices.translate(f * (h + 0.64000005F), i + -0.6F + equipProgress * -0.6F, j + -0.71999997F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * 45.0F));
		float k = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
		float l = MathHelper.sin(g * (float) Math.PI);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * l * 70.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * k * -20.0F));
		AbstractClientPlayerEntity abstractClientPlayerEntity = this.client.player;
		matrices.translate(f * -1.0F, 3.6F, 3.5F);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 120.0F));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200.0F));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * -135.0F));
		matrices.translate(f * 5.6F, 0.0F, 0.0F);
		PlayerEntityRenderer<AbstractClientPlayerEntity>
				playerEntityRenderer =
				this.entityRenderDispatcher.getPlayerRenderer(abstractClientPlayerEntity);
		Identifier identifier = abstractClientPlayerEntity.getSkin().body().texturePath();
		if (bl) {
			playerEntityRenderer.renderRightArm(
					matrices,
					queue,
					light,
					identifier,
					abstractClientPlayerEntity.isModelPartVisible(PlayerModelPart.RIGHT_SLEEVE)
			);
		}
		else {
			playerEntityRenderer.renderLeftArm(
					matrices,
					queue,
					light,
					identifier,
					abstractClientPlayerEntity.isModelPartVisible(PlayerModelPart.LEFT_SLEEVE)
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
		float f = player.getItemUseTimeLeft() - tickProgress + 1.0F;
		float g = f / stack.getMaxUseTime(player);
		if (g < 0.8F) {
			float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
			matrices.translate(0.0F, h, 0.0F);
		}

		float h = 1.0F - (float) Math.pow(g, 27.0);
		int i = arm == Arm.RIGHT ? 1 : -1;
		matrices.translate(h * 0.6F * i, h * -0.5F, h * 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * h * 90.0F));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * h * 30.0F));
	}

	private void applyBrushTransformation(
			MatrixStack matrices,
			float tickProgress,
			Arm arm,
			PlayerEntity playerEntity
	) {
		float f = playerEntity.getItemUseTimeLeft() % 10;
		float g = f - tickProgress + 1.0F;
		float h = 1.0F - g / 10.0F;
		float i = -90.0F;
		float j = 60.0F;
		float k = 150.0F;
		float l = -15.0F;
		int m = 2;
		float n = -15.0F + 75.0F * MathHelper.cos(h * 2.0F * (float) Math.PI);
		if (arm != Arm.RIGHT) {
			matrices.translate(0.1, 0.83, 0.35);
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
			matrices.translate(-0.3, 0.22, 0.35);
		}
		else {
			matrices.translate(-0.25, 0.22, 0.35);
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
		}
	}

	private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
		int i = arm == Arm.RIGHT ? 1 : -1;
		float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * -20.0F)));
		float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * g * -20.0F));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
	}

	private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
		int i = arm == Arm.RIGHT ? 1 : -1;
		matrices.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
	}

	public void renderItem(
			float tickProgress,
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			ClientPlayerEntity player,
			int light
	) {
		float f = player.getHandSwingProgress(tickProgress);
		Hand hand = (Hand) MoreObjects.firstNonNull(player.preferredHand, Hand.MAIN_HAND);
		float g = player.getLerpedPitch(tickProgress);
		HeldItemRenderer.HandRenderType handRenderType = getHandRenderType(player);
		float h = MathHelper.lerp(tickProgress, player.lastRenderPitch, player.renderPitch);
		float i = MathHelper.lerp(tickProgress, player.lastRenderYaw, player.renderYaw);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((player.getPitch(tickProgress) - h) * 0.1F));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((player.getYaw(tickProgress) - i) * 0.1F));
		if (handRenderType.renderMainHand) {
			float j = hand == Hand.MAIN_HAND ? f : 0.0F;
			float k = this.itemModelManager.getSwapAnimationScale(this.mainHand)
					* (1.0F - MathHelper.lerp(
					tickProgress,
					this.lastEquipProgressMainHand,
					this.equipProgressMainHand
			)
			);
			this.renderFirstPersonItem(
					player,
					tickProgress,
					g,
					Hand.MAIN_HAND,
					j,
					this.mainHand,
					k,
					matrices,
					orderedRenderCommandQueue,
					light
			);
		}

		if (handRenderType.renderOffHand) {
			float j = hand == Hand.OFF_HAND ? f : 0.0F;
			float k = this.itemModelManager.getSwapAnimationScale(this.offHand)
					* (1.0F - MathHelper.lerp(tickProgress, this.lastEquipProgressOffHand, this.equipProgressOffHand));
			this.renderFirstPersonItem(
					player,
					tickProgress,
					g,
					Hand.OFF_HAND,
					j,
					this.offHand,
					k,
					matrices,
					orderedRenderCommandQueue,
					light
			);
		}

		this.client.gameRenderer.getEntityRenderDispatcher().render();
		this.client.getBufferBuilders().getEntityVertexConsumers().draw();
	}

	@VisibleForTesting
	static HeldItemRenderer.HandRenderType getHandRenderType(ClientPlayerEntity player) {
		ItemStack itemStack = player.getMainHandStack();
		ItemStack itemStack2 = player.getOffHandStack();
		boolean bl = itemStack.isOf(Items.BOW) || itemStack2.isOf(Items.BOW);
		boolean bl2 = itemStack.isOf(Items.CROSSBOW) || itemStack2.isOf(Items.CROSSBOW);
		if (!bl && !bl2) {
			return HeldItemRenderer.HandRenderType.RENDER_BOTH_HANDS;
		}
		else if (player.isUsingItem()) {
			return getUsingItemHandRenderType(player);
		}
		else {
			return isChargedCrossbow(itemStack) ? HeldItemRenderer.HandRenderType.RENDER_MAIN_HAND_ONLY
			                                    : HeldItemRenderer.HandRenderType.RENDER_BOTH_HANDS;
		}
	}

	private static HeldItemRenderer.HandRenderType getUsingItemHandRenderType(ClientPlayerEntity player) {
		ItemStack itemStack = player.getActiveItem();
		Hand hand = player.getActiveHand();
		if (!itemStack.isOf(Items.BOW) && !itemStack.isOf(Items.CROSSBOW)) {
			return hand == Hand.MAIN_HAND && isChargedCrossbow(player.getOffHandStack())
			       ? HeldItemRenderer.HandRenderType.RENDER_MAIN_HAND_ONLY
			       : HeldItemRenderer.HandRenderType.RENDER_BOTH_HANDS;
		}
		else {
			return HeldItemRenderer.HandRenderType.shouldOnlyRender(hand);
		}
	}

	private static boolean isChargedCrossbow(ItemStack stack) {
		return stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
	}

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
		if (!player.isUsingSpyglass()) {
			boolean bl = hand == Hand.MAIN_HAND;
			Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
			matrices.push();
			if (item.isEmpty()) {
				if (bl && !player.isInvisible()) {
					this.renderArmHoldingItem(
							matrices,
							orderedRenderCommandQueue,
							light,
							equipProgress,
							swingProgress,
							arm
					);
				}
			}
			else if (item.contains(DataComponentTypes.MAP_ID)) {
				if (bl && this.offHand.isEmpty()) {
					this.renderMapInBothHands(
							matrices,
							orderedRenderCommandQueue,
							light,
							pitch,
							equipProgress,
							swingProgress
					);
				}
				else {
					this.renderMapInOneHand(
							matrices,
							orderedRenderCommandQueue,
							light,
							equipProgress,
							arm,
							swingProgress,
							item
					);
				}
			}
			else if (item.isOf(Items.CROSSBOW)) {
				this.applyEquipOffset(matrices, arm, equipProgress);
				boolean bl2 = CrossbowItem.isCharged(item);
				boolean bl3 = arm == Arm.RIGHT;
				int i = bl3 ? 1 : -1;
				if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand && !bl2) {
					matrices.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
					matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
					matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 65.3F));
					matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -9.785F));
					float f = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickProgress + 1.0F);
					float g = f / CrossbowItem.getPullTime(item, player);
					if (g > 1.0F) {
						g = 1.0F;
					}

					if (g > 0.1F) {
						float h = MathHelper.sin((f - 0.1F) * 1.3F);
						float j = g - 0.1F;
						float k = h * j;
						matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
					}

					matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
					matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
					matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(i * 45.0F));
				}
				else {
					this.swingArm(swingProgress, matrices, i, arm);
					if (bl2 && swingProgress < 0.001F && bl) {
						matrices.translate(i * -0.641864F, 0.0F, 0.0F);
						matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 10.0F));
					}
				}

				this.renderItem(
						player,
						item,
						bl3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
						matrices,
						orderedRenderCommandQueue,
						light
				);
			}
			else {
				boolean bl2 = arm == Arm.RIGHT;
				int l = bl2 ? 1 : -1;
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
								matrices.translate(l * -0.14142136F, 0.08F, 0.14142136F);
								matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-102.25F));
								matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(l * 13.365F));
								matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(l * 78.05F));
							}
							break;
						case BOW:
							matrices.translate(l * -0.2785682F, 0.18344387F, 0.15731531F);
							matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
							matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(l * 35.3F));
							matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(l * -9.785F));
							float
									fxxx =
									item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickProgress + 1.0F);
							float gxx = fxxx / 20.0F;
							gxx = (gxx * gxx + gxx * 2.0F) / 3.0F;
							if (gxx > 1.0F) {
								gxx = 1.0F;
							}

							if (gxx > 0.1F) {
								float h = MathHelper.sin((fxxx - 0.1F) * 1.3F);
								float j = gxx - 0.1F;
								float k = h * j;
								matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
							}

							matrices.translate(gxx * 0.0F, gxx * 0.0F, gxx * 0.04F);
							matrices.scale(1.0F, 1.0F, 1.0F + gxx * 0.2F);
							matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(l * 45.0F));
							break;
						case TRIDENT:
							matrices.translate(l * -0.5F, 0.7F, 0.1F);
							matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
							matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(l * 35.3F));
							matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(l * -9.785F));
							float
									fxx =
									item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickProgress + 1.0F);
							float gx = fxx / 10.0F;
							if (gx > 1.0F) {
								gx = 1.0F;
							}

							if (gx > 0.1F) {
								float h = MathHelper.sin((fxx - 0.1F) * 1.3F);
								float j = gx - 0.1F;
								float k = h * j;
								matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
							}

							matrices.translate(0.0F, 0.0F, gx * 0.2F);
							matrices.scale(1.0F, 1.0F, 1.0F + gx * 0.2F);
							matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(l * 45.0F));
							break;
						case BRUSH:
							this.applyBrushTransformation(matrices, tickProgress, arm, player);
							break;
						case BUNDLE:
							this.swingArm(swingProgress, matrices, l, arm);
							break;
						case SPEAR:
							matrices.translate(l * 0.56F, -0.52F, -0.72F);
							float fx = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickProgress + 1.0F);
							Lancing.applyHeldItemLancingTransform(
									player.getTimeSinceLastKineticAttack(tickProgress),
									matrices,
									fx,
									arm,
									item
							);
					}
				}
				else if (player.isUsingRiptide()) {
					this.applyEquipOffset(matrices, arm, equipProgress);
					matrices.translate(l * -0.4F, 0.8F, 0.3F);
					matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(l * 65.0F));
					matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(l * -85.0F));
				}
				else {
					this.applyEquipOffset(matrices, arm, equipProgress);
					switch (item.getSwingAnimation().type()) {
						case NONE:
						default:
							break;
						case WHACK:
							this.swingArm(swingProgress, matrices, l, arm);
							break;
						case STAB:
							Lancing.applyProjectileTransform(swingProgress, matrices, l, arm);
					}
				}

				this.renderItem(
						player,
						item,
						bl2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
						matrices,
						orderedRenderCommandQueue,
						light
				);
			}

			matrices.pop();
		}
	}

	private void swingArm(float swingProgress, MatrixStack matrixStack, int i, Arm arm) {
		float f = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
		float g = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
		float h = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
		matrixStack.translate(i * f, g, h);
		this.applySwingOffset(matrixStack, arm, swingProgress);
	}

	private boolean shouldSkipHandAnimationOnSwap(ItemStack from, ItemStack to) {
		return ItemStack.shouldSkipHandAnimationOnSwap(from, to, ComponentType::skipsHandAnimation) ? true
		                                                                                            : !this.itemModelManager.hasHandAnimationOnSwap(
				                                                                                            to);
	}

	public void updateHeldItems() {
		this.lastEquipProgressMainHand = this.equipProgressMainHand;
		this.lastEquipProgressOffHand = this.equipProgressOffHand;
		ClientPlayerEntity clientPlayerEntity = this.client.player;
		ItemStack itemStack = clientPlayerEntity.getMainHandStack();
		ItemStack itemStack2 = clientPlayerEntity.getOffHandStack();
		if (this.shouldSkipHandAnimationOnSwap(this.mainHand, itemStack)) {
			this.mainHand = itemStack;
		}

		if (this.shouldSkipHandAnimationOnSwap(this.offHand, itemStack2)) {
			this.offHand = itemStack2;
		}

		if (clientPlayerEntity.isRiding()) {
			this.equipProgressMainHand = MathHelper.clamp(this.equipProgressMainHand - 0.4F, 0.0F, 1.0F);
			this.equipProgressOffHand = MathHelper.clamp(this.equipProgressOffHand - 0.4F, 0.0F, 1.0F);
		}
		else {
			float f = clientPlayerEntity.getHandEquippingProgress(1.0F);
			float g = this.mainHand != itemStack ? 0.0F : f * f * f;
			float h = this.offHand != itemStack2 ? 0.0F : 1.0F;
			this.equipProgressMainHand =
					this.equipProgressMainHand + MathHelper.clamp(g - this.equipProgressMainHand, -0.4F, 0.4F);
			this.equipProgressOffHand =
					this.equipProgressOffHand + MathHelper.clamp(h - this.equipProgressOffHand, -0.4F, 0.4F);
		}

		if (this.equipProgressMainHand < 0.1F) {
			this.mainHand = itemStack;
		}

		if (this.equipProgressOffHand < 0.1F) {
			this.offHand = itemStack2;
		}
	}

	public void resetEquipProgress(Hand hand) {
		if (hand == Hand.MAIN_HAND) {
			this.equipProgressMainHand = 0.0F;
		}
		else {
			this.equipProgressOffHand = 0.0F;
		}
	}

	@Environment(EnvType.CLIENT)
	@VisibleForTesting
	/**
	 * {@code HandRenderType}.
	 */
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

		public static HeldItemRenderer.HandRenderType shouldOnlyRender(Hand hand) {
			return hand == Hand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
		}
	}
}
