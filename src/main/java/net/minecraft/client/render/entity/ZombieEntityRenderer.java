package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.entity.mob.ZombieEntity;

@Environment(EnvType.CLIENT)
/**
 * {@code ZombieEntityRenderer}.
 */
public class ZombieEntityRenderer extends ZombieBaseEntityRenderer<ZombieEntity, ZombieEntityRenderState, ZombieEntityModel<ZombieEntityRenderState>> {

	public ZombieEntityRenderer(EntityRendererFactory.Context context) {
		this(
				context,
				EntityModelLayers.ZOMBIE,
				EntityModelLayers.ZOMBIE_BABY,
				EntityModelLayers.ZOMBIE_EQUIPMENT,
				EntityModelLayers.ZOMBIE_BABY_EQUIPMENT
		);
	}

	public ZombieEntityRenderState createRenderState() {
		return new ZombieEntityRenderState();
	}

	public ZombieEntityRenderer(
			EntityRendererFactory.Context ctx,
			EntityModelLayer layer,
			EntityModelLayer legsArmorLayer,
			EquipmentModelData<EntityModelLayer> equipmentModelData,
			EquipmentModelData<EntityModelLayer> equipmentModelData2
	) {
		super(
				ctx,
				new ZombieEntityModel<>(ctx.getPart(layer)),
				new ZombieEntityModel<>(ctx.getPart(legsArmorLayer)),
				EquipmentModelData.mapToEntityModel(equipmentModelData, ctx.getEntityModels(), ZombieEntityModel::new),
				EquipmentModelData.mapToEntityModel(equipmentModelData2, ctx.getEntityModels(), ZombieEntityModel::new)
		);
	}
}
