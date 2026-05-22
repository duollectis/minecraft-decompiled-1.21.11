package net.minecraft.entity.passive;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Осёл — вьючное животное, способное нести сундук.
 * Может скрещиваться с лошадью (порождает мула) или другим ослом.
 */
public class DonkeyEntity extends AbstractDonkeyEntity {

	public DonkeyEntity(EntityType<? extends DonkeyEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_DONKEY_AMBIENT;
	}

	@Override
	protected SoundEvent getAngrySound() {
		return SoundEvents.ENTITY_DONKEY_ANGRY;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_DONKEY_DEATH;
	}

	@Override
	protected SoundEvent getEatSound() {
		return SoundEvents.ENTITY_DONKEY_EAT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_DONKEY_HURT;
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		if (other == this) {
			return false;
		}

		if (other instanceof DonkeyEntity donkey) {
			return canBreed() && donkey.canBreed();
		}

		if (other instanceof HorseEntity horse) {
			return canBreed() && horse.canBreed();
		}

		return false;
	}

	@Override
	protected void playJumpSound() {
		playSound(SoundEvents.ENTITY_DONKEY_JUMP, 0.4F, 1.0F);
	}

	/**
	 * При скрещивании с лошадью создаёт мула, при скрещивании с ослом — осла.
	 */
	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		EntityType<? extends AbstractHorseEntity> childType = entity instanceof HorseEntity
			? EntityType.MULE
			: EntityType.DONKEY;
		AbstractHorseEntity child = childType.create(world, SpawnReason.BREEDING);
		if (child != null) {
			setChildAttributes(entity, child);
		}

		return child;
	}
}
