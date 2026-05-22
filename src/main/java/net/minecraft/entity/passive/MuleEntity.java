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
 * Мул — гибрид лошади и осла, способный нести сундук.
 * Стерилен: при «размножении» всегда создаёт нового мула без наследования атрибутов.
 */
public class MuleEntity extends AbstractDonkeyEntity {

	public MuleEntity(EntityType<? extends MuleEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_MULE_AMBIENT;
	}

	@Override
	protected SoundEvent getAngrySound() {
		return SoundEvents.ENTITY_MULE_ANGRY;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_MULE_DEATH;
	}

	@Override
	protected SoundEvent getEatSound() {
		return SoundEvents.ENTITY_MULE_EAT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_MULE_HURT;
	}

	@Override
	protected void playJumpSound() {
		playSound(SoundEvents.ENTITY_MULE_JUMP, 0.4F, 1.0F);
	}

	@Override
	protected void playAddChestSound() {
		playSound(
			SoundEvents.ENTITY_MULE_CHEST,
			1.0F,
			(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
		);
	}

	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		return EntityType.MULE.create(world, SpawnReason.BREEDING);
	}
}
