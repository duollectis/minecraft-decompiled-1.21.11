package net.minecraft.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

/**
 * Базовый класс для бросаемых зелий. Реализует логику броска через
 * {@link ProjectileEntity#spawnWithVelocity} с углом -20° и мощностью {@link #POWER}.
 * Подклассы обязаны реализовать фабричные методы {@link #createEntity}.
 */
public abstract class ThrowablePotionItem extends PotionItem implements ProjectileItem {

	public static final float POWER = 0.5F;

	public ThrowablePotionItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (world instanceof ServerWorld serverWorld) {
			ProjectileEntity.spawnWithVelocity(this::createEntity, serverWorld, stack, user, -20.0F, POWER, 1.0F);
		}

		user.incrementStat(Stats.USED.getOrCreateStat(this));
		stack.decrementUnlessCreative(1, user);

		return ActionResult.SUCCESS;
	}

	/**
	 * Создаёт сущность зелья для броска игроком или существом.
	 *
	 * @param world серверный мир
	 * @param user бросающее существо
	 * @param stack стек зелья
	 * @return созданная сущность зелья
	 */
	protected abstract PotionEntity createEntity(ServerWorld world, LivingEntity user, ItemStack stack);

	/**
	 * Создаёт сущность зелья для выстрела из диспенсера.
	 *
	 * @param world мир
	 * @param pos позиция спавна
	 * @param stack стек зелья
	 * @return созданная сущность зелья
	 */
	protected abstract PotionEntity createEntity(World world, Position pos, ItemStack stack);

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		return createEntity(world, pos, stack);
	}

	@Override
	public ProjectileItem.Settings getProjectileSettings() {
		return ProjectileItem.Settings.builder()
			.uncertainty(ProjectileItem.Settings.DEFAULT.uncertainty() * 0.5F)
			.power(ProjectileItem.Settings.DEFAULT.power() * 1.25F)
			.build();
	}
}
