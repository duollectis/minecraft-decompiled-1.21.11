package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Цель железного голема: атаковать игрока, чья репутация у любого жителя
 * в радиусе 10 блоков опустилась ниже {@link #MIN_REPUTATION}.
 */
public class TrackIronGolemTargetGoal extends TrackTargetGoal {

	private static final double SEARCH_RANGE_XZ = 10.0;
	private static final double SEARCH_RANGE_Y = 8.0;
	private static final int MIN_REPUTATION = -100;
	private static final double TARGET_RANGE = 64.0;

	private final IronGolemEntity golem;
	private @Nullable LivingEntity target;
	private final TargetPredicate targetPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(TARGET_RANGE);

	public TrackIronGolemTargetGoal(IronGolemEntity golem) {
		super(golem, false, true);
		this.golem = golem;
		this.setControls(EnumSet.of(Goal.Control.TARGET));
	}

	@Override
	public boolean canStart() {
		Box searchBox = golem.getBoundingBox().expand(SEARCH_RANGE_XZ, SEARCH_RANGE_Y, SEARCH_RANGE_XZ);
		ServerWorld serverWorld = getServerWorld(golem);
		List<? extends LivingEntity> villagers = serverWorld.getTargets(VillagerEntity.class, targetPredicate, golem, searchBox);
		List<PlayerEntity> players = serverWorld.getPlayers(targetPredicate, golem, searchBox);

		for (LivingEntity entity : villagers) {
			VillagerEntity villager = (VillagerEntity) entity;

			for (PlayerEntity player : players) {
				if (villager.getReputation(player) <= MIN_REPUTATION) {
					target = player;
				}
			}
		}

		if (target == null) {
			return false;
		}

		return !(target instanceof PlayerEntity player && (player.isSpectator() || player.isCreative()));
	}

	@Override
	public void start() {
		golem.setTarget(target);
		super.start();
	}
}
