package net.minecraft.entity.passive;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Лама торговца — особая разновидность ламы, привязанная к странствующему торговцу.
 * Автоматически исчезает через {@code despawnDelay} тиков, если не приручена,
 * не привязана игроком и не несёт пассажира. Таймер синхронизируется с таймером
 * торговца, пока лама находится на его поводке.
 */
public class TraderLlamaEntity extends LlamaEntity {

	private static final int DEFAULT_DESPAWN_DELAY = 47999;

	private int despawnDelay = DEFAULT_DESPAWN_DELAY;

	public TraderLlamaEntity(EntityType<? extends TraderLlamaEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public boolean isTrader() {
		return true;
	}

	@Override
	protected @Nullable LlamaEntity createChild() {
		return EntityType.TRADER_LLAMA.create(getEntityWorld(), SpawnReason.BREEDING);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("DespawnDelay", despawnDelay);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		despawnDelay = view.getInt("DespawnDelay", DEFAULT_DESPAWN_DELAY);
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(1, new EscapeDangerGoal(this, 2.0));
		targetSelector.add(1, new DefendTraderGoal(this));
		targetSelector.add(
			2,
			new ActiveTargetGoal<>(
				this,
				ZombieEntity.class,
				true,
				(entity, serverWorld) -> entity.getType() != EntityType.ZOMBIFIED_PIGLIN
			)
		);
		targetSelector.add(2, new ActiveTargetGoal<>(this, IllagerEntity.class, true));
	}

	public void setDespawnDelay(int despawnDelay) {
		this.despawnDelay = despawnDelay;
	}

	@Override
	protected void putPlayerOnBack(PlayerEntity player) {
		if (getLeashHolder() instanceof WanderingTraderEntity) {
			return;
		}

		super.putPlayerOnBack(player);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (!getEntityWorld().isClient()) {
			tryDespawn();
		}
	}

	/**
	 * Уменьшает таймер исчезновения. Если лама на поводке у торговца —
	 * синхронизирует таймер с торговцем. При достижении нуля — удаляет ламу.
	 */
	private void tryDespawn() {
		if (!canDespawn()) {
			return;
		}

		despawnDelay = heldByTrader()
			? ((WanderingTraderEntity) getLeashHolder()).getDespawnDelay() - 1
			: despawnDelay - 1;

		if (despawnDelay <= 0) {
			detachLeashWithoutDrop();
			discard();
		}
	}

	private boolean canDespawn() {
		return !isTame() && !leashedByPlayer() && !hasPlayerRider();
	}

	private boolean heldByTrader() {
		return getLeashHolder() instanceof WanderingTraderEntity;
	}

	private boolean leashedByPlayer() {
		return isLeashed() && !heldByTrader();
	}

	@Override
	public @Nullable EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		if (spawnReason == SpawnReason.EVENT) {
			setBreedingAge(0);
		}

		if (entityData == null) {
			entityData = new PassiveEntity.PassiveData(false);
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	/**
	 * Цель защиты торговца: лама атакует того, кто ударил привязанного торговца.
	 */
	protected static class DefendTraderGoal extends TrackTargetGoal {

		private final LlamaEntity llama;
		private LivingEntity offender;
		private int traderLastAttackedTime;

		public DefendTraderGoal(LlamaEntity llama) {
			super(llama, false);
			this.llama = llama;
			setControls(EnumSet.of(Goal.Control.TARGET));
		}

		@Override
		public boolean canStart() {
			if (!llama.isLeashed()) {
				return false;
			}

			if (!(llama.getLeashHolder() instanceof WanderingTraderEntity trader)) {
				return false;
			}

			offender = trader.getAttacker();
			int lastAttackedTime = trader.getLastAttackedTime();
			return lastAttackedTime != traderLastAttackedTime && canTrack(offender, TargetPredicate.DEFAULT);
		}

		@Override
		public void start() {
			mob.setTarget(offender);
			if (llama.getLeashHolder() instanceof WanderingTraderEntity trader) {
				traderLastAttackedTime = trader.getLastAttackedTime();
			}

			super.start();
		}
	}
}
