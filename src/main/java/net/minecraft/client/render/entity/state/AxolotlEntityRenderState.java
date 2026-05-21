package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.passive.AxolotlEntity;

@Environment(EnvType.CLIENT)
/**
 * {@code AxolotlEntityRenderState}.
 */
public class AxolotlEntityRenderState extends LivingEntityRenderState {

	public AxolotlEntity.Variant variant = AxolotlEntity.Variant.DEFAULT;
	public float playingDeadValue;
	public float isMovingValue;
	public float inWaterValue = 1.0F;
	public float onGroundValue;
}
