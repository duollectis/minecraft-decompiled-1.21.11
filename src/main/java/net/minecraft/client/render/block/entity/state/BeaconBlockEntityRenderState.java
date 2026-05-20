package net.minecraft.client.render.block.entity.state;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BeaconBlockEntityRenderState extends BlockEntityRenderState {
   public float beamRotationDegrees;
   public float beamScale;
   public List<BeaconBlockEntityRenderState.BeamSegment> beamSegments = new ArrayList<>();

   @Environment(EnvType.CLIENT)
   public record BeamSegment(int color, int height) {
   }
}
