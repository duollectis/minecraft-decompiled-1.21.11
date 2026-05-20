package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.block.entity.Sherds;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DecoratedPotBlockEntityRenderState extends BlockEntityRenderState {
   public float field_62713;
   public DecoratedPotBlockEntity.@Nullable WobbleType wobbleType;
   public float wobbleAnimationProgress;
   public Sherds sherds = Sherds.DEFAULT;
   public Direction facing = Direction.NORTH;
}
