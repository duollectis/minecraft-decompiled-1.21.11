package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class MinecartEntityRenderState extends EntityRenderState {
   public float lerpedPitch;
   public float lerpedYaw;
   public long hash;
   public int damageWobbleSide;
   public float damageWobbleTicks;
   public float damageWobbleStrength;
   public int blockOffset;
   public BlockState containedBlock = Blocks.AIR.getDefaultState();
   public boolean usesExperimentalController;
   public @Nullable Vec3d lerpedPos;
   public @Nullable Vec3d presentPos;
   public @Nullable Vec3d futurePos;
   public @Nullable Vec3d pastPos;
}
