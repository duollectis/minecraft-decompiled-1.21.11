package net.minecraft.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

/**
 * Предмет «Бутылка опыта». При броске запускает снаряд {@link ExperienceBottleEntity},
 * который при приземлении рассыпает шарики опыта.
 * Имеет повышенную точность и мощность по сравнению с обычными снарядами.
 */
public class ExperienceBottleItem extends Item implements ProjectileItem {

	/** Угол наклона броска вниз (отрицательный — вверх по оси Y). */
	private static final float THROW_PITCH = -20.0F;
	private static final float THROW_POWER = 0.7F;

	public ExperienceBottleItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		world.playSound(
				null,
				user.getX(),
				user.getY(),
				user.getZ(),
				SoundEvents.ENTITY_EXPERIENCE_BOTTLE_THROW,
				SoundCategory.NEUTRAL,
				0.5F,
				0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F)
		);

		if (world instanceof ServerWorld serverWorld) {
			ProjectileEntity.spawnWithVelocity(
					ExperienceBottleEntity::new,
					serverWorld,
					stack,
					user,
					THROW_PITCH,
					THROW_POWER,
					1.0F
			);
		}

		user.incrementStat(Stats.USED.getOrCreateStat(this));
		stack.decrementUnlessCreative(1, user);

		return ActionResult.SUCCESS;
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		return new ExperienceBottleEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
	}

	@Override
	public ProjectileItem.Settings getProjectileSettings() {
		return ProjectileItem.Settings.builder()
				.uncertainty(ProjectileItem.Settings.DEFAULT.uncertainty() * 0.5F)
				.power(ProjectileItem.Settings.DEFAULT.power() * 1.25F)
				.build();
	}
}
