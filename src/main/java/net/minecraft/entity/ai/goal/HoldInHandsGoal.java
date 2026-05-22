package net.minecraft.entity.ai.goal;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Цель, заставляющая моба взять предмет в руку и использовать его до завершения.
 * По окончании предмет убирается, а при наличии звука — воспроизводится.
 */
public class HoldInHandsGoal<T extends MobEntity> extends Goal {

	private final T actor;
	private final ItemStack item;
	private final Predicate<? super T> condition;
	private final @Nullable SoundEvent sound;

	public HoldInHandsGoal(T actor, ItemStack item, @Nullable SoundEvent sound, Predicate<? super T> condition) {
		this.actor = actor;
		this.item = item;
		this.sound = sound;
		this.condition = condition;
	}

	@Override
	public boolean canStart() {
		return condition.test(actor);
	}

	@Override
	public boolean shouldContinue() {
		return actor.isUsingItem();
	}

	@Override
	public void start() {
		actor.equipStack(EquipmentSlot.MAINHAND, item.copy());
		actor.setCurrentHand(Hand.MAIN_HAND);
	}

	@Override
	public void stop() {
		actor.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

		if (sound != null) {
			actor.playSound(sound, 1.0F, actor.getRandom().nextFloat() * 0.2F + 0.9F);
		}
	}
}
