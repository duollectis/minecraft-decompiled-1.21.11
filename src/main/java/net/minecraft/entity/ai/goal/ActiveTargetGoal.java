package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Цель активного поиска и атаки ближайшей сущности заданного класса.
 * Поддерживает вероятностный пропуск поиска через {@code reciprocalChance}
 * для снижения нагрузки на сервер.
 */
public class ActiveTargetGoal<T extends LivingEntity> extends TrackTargetGoal {

	private static final int DEFAULT_RECIPROCAL_CHANCE = 10;

	protected final Class<T> targetClass;
	protected final int reciprocalChance;
	protected @Nullable LivingEntity targetEntity;
	protected TargetPredicate targetPredicate;

	public ActiveTargetGoal(MobEntity mob, Class<T> targetClass, boolean checkVisibility) {
		this(mob, targetClass, DEFAULT_RECIPROCAL_CHANCE, checkVisibility, false, null);
	}

	public ActiveTargetGoal(
			MobEntity mob,
			Class<T> targetClass,
			boolean checkVisibility,
			TargetPredicate.EntityPredicate predicate
	) {
		this(mob, targetClass, DEFAULT_RECIPROCAL_CHANCE, checkVisibility, false, predicate);
	}

	public ActiveTargetGoal(MobEntity mob, Class<T> targetClass, boolean checkVisibility, boolean checkCanNavigate) {
		this(mob, targetClass, DEFAULT_RECIPROCAL_CHANCE, checkVisibility, checkCanNavigate, null);
	}

	public ActiveTargetGoal(
			MobEntity mob,
			Class<T> targetClass,
			int reciprocalChance,
			boolean checkVisibility,
			boolean checkCanNavigate,
			TargetPredicate.@Nullable EntityPredicate targetPredicate
	) {
		super(mob, checkVisibility, checkCanNavigate);
		this.targetClass = targetClass;
		this.reciprocalChance = toGoalTicks(reciprocalChance);
		setControls(EnumSet.of(Goal.Control.TARGET));
		this.targetPredicate = TargetPredicate
				.createAttackable()
				.setBaseMaxDistance(getFollowRange())
				.setPredicate(targetPredicate);
	}

	@Override
	public boolean canStart() {
		if (reciprocalChance > 0 && mob.getRandom().nextInt(reciprocalChance) != 0) {
			return false;
		}

		findClosestTarget();
		return targetEntity != null;
	}

	protected Box getSearchBox(double distance) {
		return mob.getBoundingBox().expand(distance, distance, distance);
	}

	protected void findClosestTarget() {
		ServerWorld serverWorld = getServerWorld(mob);
		boolean isPlayerTarget = targetClass == PlayerEntity.class || targetClass == ServerPlayerEntity.class;

		if (isPlayerTarget) {
			targetEntity = serverWorld.getClosestPlayer(
					getAndUpdateTargetPredicate(),
					mob,
					mob.getX(),
					mob.getEyeY(),
					mob.getZ()
			);
		}
		else {
			targetEntity = serverWorld.getClosestEntity(
					mob.getEntityWorld().getEntitiesByClass(
							targetClass,
							getSearchBox(getFollowRange()),
							livingEntity -> true
					),
					getAndUpdateTargetPredicate(),
					mob,
					mob.getX(),
					mob.getEyeY(),
					mob.getZ()
			);
		}
	}

	@Override
	public void start() {
		mob.setTarget(targetEntity);
		super.start();
	}

	public void setTargetEntity(@Nullable LivingEntity targetEntity) {
		this.targetEntity = targetEntity;
	}

	private TargetPredicate getAndUpdateTargetPredicate() {
		return targetPredicate.setBaseMaxDistance(getFollowRange());
	}
}
