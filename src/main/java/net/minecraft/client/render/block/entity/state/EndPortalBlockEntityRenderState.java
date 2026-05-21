package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Direction;

import java.util.EnumSet;

@Environment(EnvType.CLIENT)
/**
 * {@code EndPortalBlockEntityRenderState}.
 */
public class EndPortalBlockEntityRenderState extends BlockEntityRenderState {

	public EnumSet<Direction> sides = EnumSet.noneOf(Direction.class);
}
