package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Фаза зависания дракона на месте. Используется как начальное состояние
 * до первого тика сервера, когда позиция ещё не известна.
 */
public class HoverPhase extends AbstractPhase {

	private @Nullable Vec3d target;

	public HoverPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public void serverTick(ServerWorld world) {
		if (target == null) {
			target = dragon.getEntityPos();
		}
	}

	@Override
	public boolean isSittingOrHovering() {
		return true;
	}

	@Override
	public void beginPhase() {
		target = null;
	}

	@Override
	public float getMaxYAcceleration() {
		return 1.0F;
	}

	@Override
	public @Nullable Vec3d getPathTarget() {
		return target;
	}

	@Override
	public PhaseType<HoverPhase> getType() {
		return PhaseType.HOVER;
	}
}
