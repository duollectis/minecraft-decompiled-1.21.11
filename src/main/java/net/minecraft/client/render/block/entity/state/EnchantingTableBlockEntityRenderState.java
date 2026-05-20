package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class EnchantingTableBlockEntityRenderState extends BlockEntityRenderState {
   public float ticks;
   public float bookRotationDegrees;
   public float pageAngle;
   public float pageTurningSpeed;
}
