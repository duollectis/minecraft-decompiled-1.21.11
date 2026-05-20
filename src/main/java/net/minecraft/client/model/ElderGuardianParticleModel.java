package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Unit;

@Environment(EnvType.CLIENT)
public class ElderGuardianParticleModel extends Model<Unit> {
   public ElderGuardianParticleModel(ModelPart part) {
      super(part, RenderLayers::entityCutoutNoCull);
   }
}
