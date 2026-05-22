package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

/**
 * Задача мозга для атаки арбалетом: управляет циклом зарядки, ожидания и выстрела.
 * Состояния: UNCHARGED → CHARGING → CHARGED → READY_TO_ATTACK → UNCHARGED.
 */
public class CrossbowAttackTask<E extends MobEntity & CrossbowUser, T extends LivingEntity> extends MultiTickTask<E> {

	private static final int RUN_TIME = 1200;
	private static final int CHARGED_COOLDOWN_BASE = 20;
	private static final int CHARGED_COOLDOWN_VARIANCE = 20;
	private int chargingCooldown;
	private CrossbowAttackTask.CrossbowState state = CrossbowAttackTask.CrossbowState.UNCHARGED;

	public CrossbowAttackTask() {
		super(
				ImmutableMap.of(
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_PRESENT
				), RUN_TIME
		);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, E entity) {
		LivingEntity target = getAttackTarget(entity);
		return entity.isHolding(Items.CROSSBOW)
				&& TargetUtil.isVisibleInMemory(entity, target)
				&& TargetUtil.isTargetWithinAttackRange(entity, target, 0);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, E entity, long time) {
		return entity.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET) && shouldRun(world, entity);
	}

	@Override
	protected void keepRunning(ServerWorld world, E entity, long time) {
		LivingEntity target = getAttackTarget(entity);
		setLookTarget(entity, target);
		tickState(entity, target);
	}

	@Override
	protected void finishRunning(ServerWorld world, E entity, long time) {
		if (entity.isUsingItem()) {
			entity.clearActiveItem();
		}

		if (entity.isHolding(Items.CROSSBOW)) {
			entity.setCharging(false);
			entity.getActiveItem().set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
		}
	}

	private void tickState(E entity, LivingEntity target) {
		if (state == CrossbowState.UNCHARGED) {
			entity.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(entity, Items.CROSSBOW));
			state = CrossbowState.CHARGING;
			entity.setCharging(true);
		} else if (state == CrossbowState.CHARGING) {
			if (!entity.isUsingItem()) {
				state = CrossbowState.UNCHARGED;
			}

			ItemStack activeItem = entity.getActiveItem();
			if (entity.getItemUseTime() >= CrossbowItem.getPullTime(activeItem, entity)) {
				entity.stopUsingItem();
				state = CrossbowState.CHARGED;
				chargingCooldown = CHARGED_COOLDOWN_BASE + entity.getRandom().nextInt(CHARGED_COOLDOWN_VARIANCE);
				entity.setCharging(false);
			}
		} else if (state == CrossbowState.CHARGED) {
			chargingCooldown--;
			if (chargingCooldown == 0) {
				state = CrossbowState.READY_TO_ATTACK;
			}
		} else if (state == CrossbowState.READY_TO_ATTACK) {
			entity.shootAt(target, 1.0F);
			state = CrossbowState.UNCHARGED;
		}
	}

	private void setLookTarget(MobEntity entity, LivingEntity target) {
		entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(target, true));
	}

	private static LivingEntity getAttackTarget(LivingEntity entity) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).get();
	}

	enum CrossbowState {
		UNCHARGED,
		CHARGING,
		CHARGED,
		READY_TO_ATTACK;
	}
}
