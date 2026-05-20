package net.minecraft.client.render.block.entity.state;

import java.util.EnumSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public class EndPortalBlockEntityRenderState extends BlockEntityRenderState {
   public EnumSet<Direction> sides = EnumSet.noneOf(Direction.class);
}
