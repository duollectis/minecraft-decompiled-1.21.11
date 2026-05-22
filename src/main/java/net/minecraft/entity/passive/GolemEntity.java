package net.minecraft.entity.passive;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для всех големов (железный, снежный, медный).
 * Големы не издают звуков по умолчанию и не деспавнятся от расстояния.
 */
public abstract class GolemEntity extends PathAwareEntity {

	private static final int AMBIENT_SOUND_DELAY = 120;

	protected GolemEntity(EntityType<? extends GolemEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return null;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return null;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return AMBIENT_SOUND_DELAY;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}
}
