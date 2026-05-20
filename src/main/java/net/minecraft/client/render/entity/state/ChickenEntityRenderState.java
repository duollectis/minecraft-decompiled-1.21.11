package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.passive.ChickenVariant;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ChickenEntityRenderState extends LivingEntityRenderState {
   public float flapProgress;
   public float maxWingDeviation;
   public @Nullable ChickenVariant variant;
}
