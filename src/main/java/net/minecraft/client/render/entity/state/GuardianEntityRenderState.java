package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GuardianEntityRenderState extends LivingEntityRenderState {
   public float spikesExtension;
   public float tailAngle;
   public Vec3d cameraPosVec = Vec3d.ZERO;
   public @Nullable Vec3d rotationVec;
   public @Nullable Vec3d lookAtPos;
   public @Nullable Vec3d beamTargetPos;
   public float beamTicks;
   public float beamProgress;
}
