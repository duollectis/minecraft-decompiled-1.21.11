package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class EndGatewayBlockEntityRenderState extends EndPortalBlockEntityRenderState {
   public int beamSpan;
   public float beamHeight;
   public int beamColor;
   public float beamRotationDegrees;
}
