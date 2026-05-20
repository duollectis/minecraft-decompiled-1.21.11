package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ShulkerBoxBlockEntityRenderState extends BlockEntityRenderState {
   public Direction facing = Direction.NORTH;
   public @Nullable DyeColor dyeColor;
   public float animationProgress;
}
