package net.minecraft.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Предмет лука. Заряжается при удержании кнопки использования,
 * выпускает стрелу при отпускании. Скорость выстрела зависит от времени натяжения.
 */
public class BowItem extends RangedWeaponItem {

	public static final int TICKS_PER_SECOND = 20;
	public static final int RANGE = 15;
	/** Максимальное время использования — фактически бесконечное удержание. */
	private static final int MAX_USE_TICKS = 72000;
	private static final float MIN_PULL_PROGRESS = 0.1F;
	private static final float MAX_PULL_PROGRESS = 1.0F;
	private static final float FULL_PULL_SPEED_MULTIPLIER = 3.0F;
	private static final float ARROW_DIVERGENCE = 1.0F;

	public BowItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (!(user instanceof PlayerEntity player)) {
			return false;
		}

		ItemStack projectileStack = player.getProjectileType(stack);

		if (projectileStack.isEmpty()) {
			return false;
		}

		int usedTicks = getMaxUseTime(stack, user) - remainingUseTicks;
		float pullProgress = getPullProgress(usedTicks);

		if (pullProgress < MIN_PULL_PROGRESS) {
			return false;
		}

		List<ItemStack> loadedProjectiles = load(stack, projectileStack, player);

		if (world instanceof ServerWorld serverWorld && !loadedProjectiles.isEmpty()) {
			shootAll(
				serverWorld,
				player,
				player.getActiveHand(),
				stack,
				loadedProjectiles,
				pullProgress * FULL_PULL_SPEED_MULTIPLIER,
				ARROW_DIVERGENCE,
				pullProgress == MAX_PULL_PROGRESS,
				null
			);
		}

		world.playSound(
			null,
			player.getX(),
			player.getY(),
			player.getZ(),
			SoundEvents.ENTITY_ARROW_SHOOT,
			SoundCategory.PLAYERS,
			1.0F,
			1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + pullProgress * 0.5F
		);
		player.incrementStat(Stats.USED.getOrCreateStat(this));
		return true;
	}

	@Override
	protected void shoot(
		LivingEntity shooter,
		ProjectileEntity projectile,
		int index,
		float speed,
		float divergence,
		float yaw,
		@Nullable LivingEntity target
	) {
		projectile.setVelocity(shooter, shooter.getPitch(), shooter.getYaw() + yaw, 0.0F, speed, divergence);
	}

	/**
	 * Вычисляет прогресс натяжения лука от 0.0 до 1.0 на основе затраченных тиков.
	 * Использует квадратичную кривую для плавного ускорения.
	 *
	 * @param useTicks количество тиков, прошедших с начала использования
	 * @return прогресс натяжения в диапазоне [0.0, 1.0]
	 */
	public static float getPullProgress(int useTicks) {
		float progress = useTicks / (float) TICKS_PER_SECOND;
		progress = (progress * progress + progress * 2.0F) / 3.0F;
		return Math.min(progress, MAX_PULL_PROGRESS);
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return MAX_USE_TICKS;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BOW;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		boolean hasProjectile = !user.getProjectileType(stack).isEmpty();

		if (!user.isInCreativeMode() && !hasProjectile) {
			return ActionResult.FAIL;
		}

		user.setCurrentHand(hand);
		return ActionResult.CONSUME;
	}

	@Override
	public Predicate<ItemStack> getProjectiles() {
		return BOW_PROJECTILES;
	}

	@Override
	public int getRange() {
		return RANGE;
	}
}
