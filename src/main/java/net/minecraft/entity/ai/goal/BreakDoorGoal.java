package net.minecraft.entity.ai.goal;

import net.minecraft.block.Block;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Difficulty;
import net.minecraft.world.rule.GameRules;

import java.util.function.Predicate;

/**
 * Цель взлома двери: постепенно разрушает дверь, если сложность и правила мира позволяют.
 * Прогресс взлома отображается анимацией разрушения блока.
 */
public class BreakDoorGoal extends DoorInteractGoal {

	private static final int MIN_MAX_PROGRESS = 240;
	private static final int SWING_CHANCE = 20;
	private static final int BREAK_STAGES = 10;
	private static final int DOOR_SWING_EVENT = 1019;
	private static final int DOOR_BREAK_EVENT = 1021;
	private static final int BLOCK_BREAK_EVENT = 2001;
	private static final int BREAK_STAGE_RESET = -1;

	private final Predicate<Difficulty> difficultySufficientPredicate;
	protected int breakProgress;
	protected int lastBreakProgress = BREAK_STAGE_RESET;
	protected int maxProgress = BREAK_STAGE_RESET;

	public BreakDoorGoal(MobEntity mob, Predicate<Difficulty> difficultySufficientPredicate) {
		super(mob);
		this.difficultySufficientPredicate = difficultySufficientPredicate;
	}

	public BreakDoorGoal(MobEntity mob, int maxProgress, Predicate<Difficulty> difficultySufficientPredicate) {
		this(mob, difficultySufficientPredicate);
		this.maxProgress = maxProgress;
	}

	protected int getMaxProgress() {
		return Math.max(MIN_MAX_PROGRESS, maxProgress);
	}

	@Override
	public boolean canStart() {
		if (!super.canStart()) {
			return false;
		}

		if (!getServerWorld(mob).getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
			return false;
		}

		return isDifficultySufficient(mob.getEntityWorld().getDifficulty()) && !isDoorOpen();
	}

	@Override
	public void start() {
		super.start();
		breakProgress = 0;
	}

	@Override
	public boolean shouldContinue() {
		return breakProgress <= getMaxProgress()
				&& !isDoorOpen()
				&& doorPos.isWithinDistance(mob.getEntityPos(), 2.0)
				&& isDifficultySufficient(mob.getEntityWorld().getDifficulty());
	}

	@Override
	public void stop() {
		super.stop();
		mob.getEntityWorld().setBlockBreakingInfo(mob.getId(), doorPos, BREAK_STAGE_RESET);
	}

	@Override
	public void tick() {
		super.tick();
		if (mob.getRandom().nextInt(SWING_CHANCE) == 0) {
			mob.getEntityWorld().syncWorldEvent(DOOR_SWING_EVENT, doorPos, 0);
			if (!mob.handSwinging) {
				mob.swingHand(mob.getActiveHand());
			}
		}

		breakProgress++;
		int stage = (int) ((float) breakProgress / getMaxProgress() * BREAK_STAGES);
		if (stage != lastBreakProgress) {
			mob.getEntityWorld().setBlockBreakingInfo(mob.getId(), doorPos, stage);
			lastBreakProgress = stage;
		}

		if (breakProgress == getMaxProgress() && isDifficultySufficient(mob.getEntityWorld().getDifficulty())) {
			mob.getEntityWorld().removeBlock(doorPos, false);
			mob.getEntityWorld().syncWorldEvent(DOOR_BREAK_EVENT, doorPos, 0);
			mob.getEntityWorld().syncWorldEvent(
					BLOCK_BREAK_EVENT,
					doorPos,
					Block.getRawIdFromState(mob.getEntityWorld().getBlockState(doorPos))
			);
		}
	}

	private boolean isDifficultySufficient(Difficulty difficulty) {
		return difficultySufficientPredicate.test(difficulty);
	}
}
