package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Базовая реализация {@link Phase} с разумными значениями по умолчанию.
 * Все методы-заглушки переопределяются конкретными фазами по необходимости.
 */
public abstract class AbstractPhase implements Phase {

	protected final EnderDragonEntity dragon;

	public AbstractPhase(EnderDragonEntity dragon) {
		this.dragon = dragon;
	}

	@Override
	public boolean isSittingOrHovering() {
		return false;
	}

	@Override
	public void clientTick() {
	}

	@Override
	public void serverTick(ServerWorld world) {
	}

	@Override
	public void crystalDestroyed(
			EndCrystalEntity crystal,
			BlockPos pos,
			DamageSource source,
			@Nullable PlayerEntity player
	) {
	}

	@Override
	public void beginPhase() {
	}

	@Override
	public void endPhase() {
	}

	@Override
	public float getMaxYAcceleration() {
		return 0.6F;
	}

	@Override
	public @Nullable Vec3d getPathTarget() {
		return null;
	}

	@Override
	public float modifyDamageTaken(DamageSource damageSource, float damage) {
		return damage;
	}

	@Override
	public float getYawAcceleration() {
		float speed = (float) dragon.getVelocity().horizontalLength() + 1.0F;
		float capped = Math.min(speed, 40.0F);
		return 0.7F / capped / speed;
	}
}
