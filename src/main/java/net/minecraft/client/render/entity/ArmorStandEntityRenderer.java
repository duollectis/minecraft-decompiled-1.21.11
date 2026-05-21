package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.ArmorStandArmorEntityModel;
import net.minecraft.client.render.entity.model.ArmorStandEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ArmorStandEntityRenderer}.
 */
public class ArmorStandEntityRenderer extends LivingEntityRenderer<ArmorStandEntity, ArmorStandEntityRenderState, ArmorStandArmorEntityModel> {

	public static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/armorstand/wood.png");
	private final ArmorStandArmorEntityModel mainModel = this.getModel();
	private final ArmorStandArmorEntityModel smallModel;

	public ArmorStandEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new ArmorStandEntityModel(context.getPart(EntityModelLayers.ARMOR_STAND)), 0.0F);
		this.smallModel = new ArmorStandEntityModel(context.getPart(EntityModelLayers.ARMOR_STAND_SMALL));
		this.addFeature(
				new ArmorFeatureRenderer<>(
						this,
						EquipmentModelData.mapToEntityModel(
								EntityModelLayers.ARMOR_STAND_EQUIPMENT,
								context.getEntityModels(),
								ArmorStandArmorEntityModel::new
						),
						EquipmentModelData.mapToEntityModel(
								EntityModelLayers.SMALL_ARMOR_STAND_EQUIPMENT,
								context.getEntityModels(),
								ArmorStandArmorEntityModel::new
						),
						context.getEquipmentRenderer()
				)
		);
		this.addFeature(new HeldItemFeatureRenderer<>(this));
		this.addFeature(new ElytraFeatureRenderer<>(this, context.getEntityModels(), context.getEquipmentRenderer()));
		this.addFeature(new HeadFeatureRenderer<>(this, context.getEntityModels(), context.getPlayerSkinCache()));
	}

	public Identifier getTexture(ArmorStandEntityRenderState armorStandEntityRenderState) {
		return TEXTURE;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return ArmorStandEntityRenderState — результат операции
	 */
	public ArmorStandEntityRenderState createRenderState() {
		return new ArmorStandEntityRenderState();
	}

	public void updateRenderState(
			ArmorStandEntity armorStandEntity,
			ArmorStandEntityRenderState armorStandEntityRenderState,
			float f
	) {
		super.updateRenderState(armorStandEntity, armorStandEntityRenderState, f);
		BipedEntityRenderer.updateBipedRenderState(
				armorStandEntity,
				armorStandEntityRenderState,
				f,
				this.itemModelResolver
		);
		armorStandEntityRenderState.yaw =
				MathHelper.lerpAngleDegrees(f, armorStandEntity.lastYaw, armorStandEntity.getYaw());
		armorStandEntityRenderState.marker = armorStandEntity.isMarker();
		armorStandEntityRenderState.small = armorStandEntity.isSmall();
		armorStandEntityRenderState.showArms = armorStandEntity.shouldShowArms();
		armorStandEntityRenderState.showBasePlate = armorStandEntity.shouldShowBasePlate();
		armorStandEntityRenderState.bodyRotation = armorStandEntity.getBodyRotation();
		armorStandEntityRenderState.headRotation = armorStandEntity.getHeadRotation();
		armorStandEntityRenderState.leftArmRotation = armorStandEntity.getLeftArmRotation();
		armorStandEntityRenderState.rightArmRotation = armorStandEntity.getRightArmRotation();
		armorStandEntityRenderState.leftLegRotation = armorStandEntity.getLeftLegRotation();
		armorStandEntityRenderState.rightLegRotation = armorStandEntity.getRightLegRotation();
		armorStandEntityRenderState.timeSinceLastHit =
				(float) (armorStandEntity.getEntityWorld().getTime() - armorStandEntity.lastHitTime) + f;
	}

	public void render(
			ArmorStandEntityRenderState armorStandEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		this.model = armorStandEntityRenderState.small ? this.smallModel : this.mainModel;
		super.render(armorStandEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
	}

	protected void setupTransforms(
			ArmorStandEntityRenderState armorStandEntityRenderState,
			MatrixStack matrixStack,
			float f,
			float g
	) {
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - f));
		if (armorStandEntityRenderState.timeSinceLastHit < 5.0F) {
			matrixStack.multiply(
					RotationAxis.POSITIVE_Y.rotationDegrees(
							MathHelper.sin(armorStandEntityRenderState.timeSinceLastHit / 1.5F * (float) Math.PI)
									* 3.0F)
			);
		}
	}

	protected boolean hasLabel(ArmorStandEntity armorStandEntity, double d) {
		return armorStandEntity.isCustomNameVisible();
	}

	protected @Nullable RenderLayer getRenderLayer(
			ArmorStandEntityRenderState armorStandEntityRenderState,
			boolean bl,
			boolean bl2,
			boolean bl3
	) {
		if (!armorStandEntityRenderState.marker) {
			return super.getRenderLayer(armorStandEntityRenderState, bl, bl2, bl3);
		}
		else {
			Identifier identifier = this.getTexture(armorStandEntityRenderState);
			if (bl2) {
				return RenderLayers.entityTranslucent(identifier, false);
			}
			else {
				return bl ? RenderLayers.entityCutoutNoCull(identifier, false) : null;
			}
		}
	}
}
