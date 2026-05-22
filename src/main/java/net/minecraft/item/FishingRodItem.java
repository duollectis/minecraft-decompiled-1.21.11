package net.minecraft.item;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Предмет «Удочка». При использовании забрасывает поплавок {@link FishingBobberEntity}.
 * Повторное использование при активном поплавке — вытаскивает его и наносит урон удочке.
 */
public class FishingRodItem extends Item {

	public FishingRodItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (user.fishHook != null) {
			if (!world.isClient()) {
				int durabilityDamage = user.fishHook.use(stack);
				stack.damage(durabilityDamage, user, hand.getEquipmentSlot());
			}

			world.playSound(
					null,
					user.getX(),
					user.getY(),
					user.getZ(),
					SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE,
					SoundCategory.NEUTRAL,
					1.0F,
					0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F)
			);

			stack.emitUseGameEvent(user, GameEvent.ITEM_INTERACT_FINISH);
		} else {
			world.playSound(
					null,
					user.getX(),
					user.getY(),
					user.getZ(),
					SoundEvents.ENTITY_FISHING_BOBBER_THROW,
					SoundCategory.NEUTRAL,
					0.5F,
					0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F)
			);

			if (world instanceof ServerWorld serverWorld) {
				int timeReductionTicks = (int) (EnchantmentHelper.getFishingTimeReduction(serverWorld, stack, user) * 20.0F);
				int luckBonus = EnchantmentHelper.getFishingLuckBonus(serverWorld, stack, user);
				ProjectileEntity.spawn(new FishingBobberEntity(user, world, luckBonus, timeReductionTicks), serverWorld, stack);
			}

			user.incrementStat(Stats.USED.getOrCreateStat(this));
			stack.emitUseGameEvent(user, GameEvent.ITEM_INTERACT_START);
		}

		return ActionResult.SUCCESS;
	}
}
