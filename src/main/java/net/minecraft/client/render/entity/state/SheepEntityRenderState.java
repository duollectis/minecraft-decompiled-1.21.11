package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.ColorLerper;
import net.minecraft.util.DyeColor;

@Environment(EnvType.CLIENT)
public class SheepEntityRenderState extends LivingEntityRenderState {
   public float neckAngle;
   public float headAngle;
   public boolean sheared;
   public DyeColor color = DyeColor.WHITE;
   public boolean rainbow;

   public int getRgbColor() {
      return this.rainbow ? ColorLerper.lerpColor(ColorLerper.Type.SHEEP, this.age) : ColorLerper.Type.SHEEP.getArgb(this.color);
   }
}
