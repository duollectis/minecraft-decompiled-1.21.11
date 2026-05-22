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
 * Старший страж — усиленная версия стража. Постоянный моб (не деспавнится).
 * Каждые {@value #EFFECT_APPLY_INTERVAL} тиков накладывает усталость горняка III
 * на всех игроков в радиусе {@value #AFFECTED_PLAYER_RANGE} блоков.
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
		setPersistent();
		if (wanderGoal != null) {
			wanderGoal.setChance(400);
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
		return isTouchingWater()
				? SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT
				: SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT_LAND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return isTouchingWater()
				? SoundEvents.ENTITY_ELDER_GUARDIAN_HURT
				: SoundEvents.ENTITY_ELDER_GUARDIAN_HURT_LAND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return isTouchingWater()
				? SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH
				: SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH_LAND;
	}

	@Override
	protected SoundEvent getFlopSound() {
		return SoundEvents.ENTITY_ELDER_GUARDIAN_FLOP;
	}

	/**
	 * Периодически накладывает эффект усталости горняка на всех игроков в радиусе {@code AFFECTED_PLAYER_RANGE} блоков.
	 * Также отправляет клиентский пакет для отображения анимации появления старшего стража.
	 */
	@Override
	protected void mobTick(ServerWorld world) {
		super.mobTick(world);

		if ((age + getId()) % EFFECT_APPLY_INTERVAL == 0) {
			StatusEffectInstance miningFatigue = new StatusEffectInstance(
					StatusEffects.MINING_FATIGUE,
					MINING_FATIGUE_DURATION,
					MINING_FATIGUE_AMPLIFIER
			);
			List<ServerPlayerEntity> affectedPlayers = StatusEffectUtil.addEffectToPlayersWithinDistance(
					world,
					this,
					getEntityPos(),
					AFFECTED_PLAYER_RANGE,
					miningFatigue,
					EFFECT_APPLY_INTERVAL
			);
			affectedPlayers.forEach(
					player -> player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
							GameStateChangeS2CPacket.ELDER_GUARDIAN_EFFECT,
							isSilent() ? 0.0F : 1.0F
					))
			);
		}

		if (!hasPositionTarget()) {
			setPositionTarget(getBlockPos(), 16);
		}
	}
}
