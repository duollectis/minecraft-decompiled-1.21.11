package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ConduitBlockEntityRenderState extends BlockEntityRenderState {
   public float ticks;
   public boolean active;
   public float rotation;
   public int rotationPhase;
   public boolean eyeOpen;
}
