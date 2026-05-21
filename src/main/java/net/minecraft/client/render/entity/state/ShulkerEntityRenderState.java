package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ShulkerEntityRenderState}.
 */
public class ShulkerEntityRenderState extends LivingEntityRenderState {

	public Vec3d renderPositionOffset = Vec3d.ZERO;
	public @Nullable DyeColor color;
	public float openProgress;
	public float headYaw;
	public float shellYaw;
	public Direction facing = Direction.DOWN;
}
