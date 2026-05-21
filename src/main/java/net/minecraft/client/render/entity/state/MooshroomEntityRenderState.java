package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.passive.MooshroomEntity;

@Environment(EnvType.CLIENT)
/**
 * {@code MooshroomEntityRenderState}.
 */
public class MooshroomEntityRenderState extends LivingEntityRenderState {

	public MooshroomEntity.Variant type = MooshroomEntity.Variant.RED;
}
