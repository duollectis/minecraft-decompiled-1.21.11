package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.ChestType;

@Environment(EnvType.CLIENT)
public class ChestBlockEntityRenderState extends BlockEntityRenderState {
   public ChestType chestType = ChestType.SINGLE;
   public float lidAnimationProgress;
   public float yaw;
   public ChestBlockEntityRenderState.Variant variant = ChestBlockEntityRenderState.Variant.REGULAR;

   @Environment(EnvType.CLIENT)
   public static enum Variant {
      ENDER_CHEST,
      CHRISTMAS,
      TRAPPED,
      COPPER_UNAFFECTED,
      COPPER_EXPOSED,
      COPPER_WEATHERED,
      COPPER_OXIDIZED,
      REGULAR;
   }
}
