package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Цель выпрашивания у волка: волк смотрит на ближайшего игрока с едой или костью
 * и принимает позу попрошайки на случайное время.
 */
public class WolfBegGoal extends Goal {

	private static final int BEG_TIME_BASE = 40;
	private static final int BEG_TIME_JITTER = 40;
	private static final float LOOK_ANGLE = 10.0F;

	private final WolfEntity wolf;
	private @Nullable PlayerEntity begFrom;
	private final ServerWorld world;
	private final float begDistance;
	private int timer;
	private final TargetPredicate validPlayerPredicate;

	public WolfBegGoal(WolfEntity wolf, float begDistance) {
		this.wolf = wolf;
		this.world = getServerWorld(wolf);
		this.begDistance = begDistance;
		this.validPlayerPredicate = TargetPredicate.createNonAttackable().setBaseMaxDistance(begDistance);
		setControls(EnumSet.of(Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		begFrom = world.getClosestPlayer(validPlayerPredicate, wolf);
		return begFrom != null && isAttractive(begFrom);
	}

	@Override
	public boolean shouldContinue() {
		if (begFrom == null || !begFrom.isAlive()) {
			return false;
		}

		return wolf.squaredDistanceTo(begFrom) <= begDistance * begDistance
				&& timer > 0
				&& isAttractive(begFrom);
	}

	@Override
	public void start() {
		wolf.setBegging(true);
		timer = getTickCount(BEG_TIME_BASE + wolf.getRandom().nextInt(BEG_TIME_JITTER));
	}

	@Override
	public void stop() {
		wolf.setBegging(false);
		begFrom = null;
	}

	@Override
	public void tick() {
		if (begFrom == null) {
			return;
		}

		wolf.getLookControl().lookAt(begFrom.getX(), begFrom.getEyeY(), begFrom.getZ(), LOOK_ANGLE, wolf.getMaxLookPitchChange());
		timer--;
	}

	private boolean isAttractive(PlayerEntity player) {
		for (Hand hand : Hand.values()) {
			ItemStack itemStack = player.getStackInHand(hand);
			if (itemStack.isOf(Items.BONE) || wolf.isBreedingItem(itemStack)) {
				return true;
			}
		}

		return false;
	}
}
