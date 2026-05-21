package net.minecraft.entity.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.List;

/**
 * {@code ElderGuardianEntity}.
 */
public class ElderGuardianEntity extends GuardianEntity {

	public static final float SCALE = EntityType.ELDER_GUARDIAN.getWidth() / EntityType.GUARDIAN.getWidth();
	private static final int EFFECT_APPLY_INTERVAL = 1200;
	private static final int AFFECTED_PLAYER_RANGE = 50;
	private static final int MINING_FATIGUE_DURATION = 6000;
	private static final int MINING_FATIGUE_AMPLIFIER = 2;
	private static final int EFFECT_DURATION_TICKS = 1200;

	public ElderGuardianEntity(EntityType<? extends ElderGuardianEntity> entityType, World world) {
		super(entityType, world);
		this.setPersistent();
		if (this.wanderGoal != null) {
			this.wanderGoal.setChance(400);
		}
	}

	public static DefaultAttributeContainer.Builder createElderGuardianAttributes() {
		return GuardianEntity.createGuardianAttributes()
		                     .add(EntityAttributes.MOVEMENT_SPEED, 0.3F)
		                     .add(EntityAttributes.ATTACK_DAMAGE, 8.0)
		                     .add(EntityAttributes.MAX_HEALTH, 80.0);
	}

	@Override
	public int getWarmupTime() {
		return 60;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return this.isTouchingWater() ? SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT
		                              : SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT_LAND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return this.isTouchingWater() ? SoundEvents.ENTITY_ELDER_GUARDIAN_HURT
		                              : SoundEvents.ENTITY_ELDER_GUARDIAN_HURT_LAND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return this.isTouchingWater() ? SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH
		                              : SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH_LAND;
	}

	@Override
	protected SoundEvent getFlopSound() {
		return SoundEvents.ENTITY_ELDER_GUARDIAN_FLOP;
	}

	@Override
	protected void mobTick(ServerWorld world) {
		super.mobTick(world);
		if ((this.age + this.getId()) % 1200 == 0) {
			StatusEffectInstance statusEffectInstance = new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 6000, 2);
			List<ServerPlayerEntity>
					list =
					StatusEffectUtil.addEffectToPlayersWithinDistance(
							world,
							this,
							this.getEntityPos(),
							50.0,
							statusEffectInstance,
							1200
					);
			list.forEach(
					player -> player.networkHandler
							.sendPacket(new GameStateChangeS2CPacket(
									GameStateChangeS2CPacket.ELDER_GUARDIAN_EFFECT,
									this.isSilent() ? 0.0F : 1.0F
							))
			);
		}

		if (!this.hasPositionTarget()) {
			this.setPositionTarget(this.getBlockPos(), 16);
		}
	}
}
