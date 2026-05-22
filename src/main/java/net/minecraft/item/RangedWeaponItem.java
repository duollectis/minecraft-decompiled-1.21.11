package net.minecraft.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Unit;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Базовый класс для дальнобойного оружия (лук, арбалет). Содержит общую логику
 * загрузки снарядов, расчёта разброса при мультивыстреле и создания сущностей стрел.
 */
public abstract class RangedWeaponItem extends Item {

	public static final Predicate<ItemStack> BOW_PROJECTILES = stack -> stack.isIn(ItemTags.ARROWS);
	public static final Predicate<ItemStack> CROSSBOW_HELD_PROJECTILES =
		BOW_PROJECTILES.or(stack -> stack.isOf(Items.FIREWORK_ROCKET));

	public RangedWeaponItem(Item.Settings settings) {
		super(settings);
	}

	public Predicate<ItemStack> getHeldProjectiles() {
		return getProjectiles();
	}

	public abstract Predicate<ItemStack> getProjectiles();

	/**
	 * Возвращает снаряд, который держит сущность в руке. Приоритет — второстепенная рука,
	 * затем основная. Если ни в одной руке нет подходящего снаряда — возвращает {@link ItemStack#EMPTY}.
	 */
	public static ItemStack getHeldProjectile(LivingEntity entity, Predicate<ItemStack> predicate) {
		if (predicate.test(entity.getStackInHand(Hand.OFF_HAND))) {
			return entity.getStackInHand(Hand.OFF_HAND);
		}

		return predicate.test(entity.getStackInHand(Hand.MAIN_HAND))
			? entity.getStackInHand(Hand.MAIN_HAND)
			: ItemStack.EMPTY;
	}

	public abstract int getRange();

	/**
	 * Выпускает все снаряды из списка с учётом чар на разброс (multishot).
	 * Снаряды равномерно распределяются по углу вокруг центральной оси.
	 *
	 * @param world серверный мир
	 * @param shooter стреляющая сущность
	 * @param hand рука, в которой оружие
	 * @param stack стек оружия
	 * @param projectiles список снарядов для выстрела
	 * @param speed скорость снарядов
	 * @param divergence базовый разброс
	 * @param critical является ли выстрел критическим
	 * @param target цель (может быть null)
	 */
	protected void shootAll(
		ServerWorld world,
		LivingEntity shooter,
		Hand hand,
		ItemStack stack,
		List<ItemStack> projectiles,
		float speed,
		float divergence,
		boolean critical,
		@Nullable LivingEntity target
	) {
		float spread = EnchantmentHelper.getProjectileSpread(world, stack, shooter, 0.0F);
		float spreadStep = projectiles.size() == 1 ? 0.0F : 2.0F * spread / (projectiles.size() - 1);
		float spreadOffset = (projectiles.size() - 1) % 2 * spreadStep / 2.0F;
		float sign = 1.0F;

		for (int index = 0; index < projectiles.size(); index++) {
			ItemStack projectileStack = projectiles.get(index);

			if (projectileStack.isEmpty()) {
				continue;
			}

			float yaw = spreadOffset + sign * ((index + 1) / 2) * spreadStep;
			sign = -sign;

			int capturedIndex = index;
			ProjectileEntity.spawn(
				createArrowEntity(world, shooter, stack, projectileStack, critical),
				world,
				projectileStack,
				projectile -> shoot(shooter, projectile, capturedIndex, speed, divergence, yaw, target)
			);

			stack.damage(getWeaponStackDamage(projectileStack), shooter, hand.getEquipmentSlot());

			if (stack.isEmpty()) {
				break;
			}
		}
	}

	protected int getWeaponStackDamage(ItemStack projectile) {
		return 1;
	}

	protected abstract void shoot(
		LivingEntity shooter,
		ProjectileEntity projectile,
		int index,
		float speed,
		float divergence,
		float yaw,
		@Nullable LivingEntity target
	);

	protected ProjectileEntity createArrowEntity(
		World world,
		LivingEntity shooter,
		ItemStack weaponStack,
		ItemStack projectileStack,
		boolean critical
	) {
		ArrowItem arrowItem = projectileStack.getItem() instanceof ArrowItem arrow
			? arrow
			: (ArrowItem) Items.ARROW;

		PersistentProjectileEntity arrow = arrowItem.createArrow(world, projectileStack, shooter, weaponStack);

		if (critical) {
			arrow.setCritical(true);
		}

		return arrow;
	}

	/**
	 * Формирует список снарядов для выстрела с учётом чар на количество снарядов.
	 * На клиенте всегда возвращает ровно 1 снаряд.
	 *
	 * @param stack стек оружия
	 * @param projectileStack стек снаряда
	 * @param shooter стреляющая сущность
	 * @return список снарядов для выстрела
	 */
	protected static List<ItemStack> load(ItemStack stack, ItemStack projectileStack, LivingEntity shooter) {
		if (projectileStack.isEmpty()) {
			return List.of();
		}

		int count = shooter.getEntityWorld() instanceof ServerWorld serverWorld
			? EnchantmentHelper.getProjectileCount(serverWorld, stack, shooter, 1)
			: 1;

		List<ItemStack> result = new ArrayList<>(count);
		ItemStack projectileCopy = projectileStack.copy();

		for (int i = 0; i < count; i++) {
			ItemStack projectile = getProjectile(stack, i == 0 ? projectileStack : projectileCopy, shooter, i > 0);

			if (!projectile.isEmpty()) {
				result.add(projectile);
			}
		}

		return result;
	}

	/**
	 * Извлекает один снаряд из инвентаря или создаёт «нематериальный» (intangible) снаряд
	 * для мультивыстрела и творческого режима.
	 *
	 * @param stack стек оружия
	 * @param projectileStack стек снаряда
	 * @param shooter стреляющая сущность
	 * @param multishot является ли это дополнительным снарядом мультивыстрела
	 * @return снаряд для выстрела или {@link ItemStack#EMPTY} если снарядов недостаточно
	 */
	protected static ItemStack getProjectile(
		ItemStack stack,
		ItemStack projectileStack,
		LivingEntity shooter,
		boolean multishot
	) {
		int ammoUse = !multishot && !shooter.isInCreativeMode() && shooter.getEntityWorld() instanceof ServerWorld serverWorld
			? EnchantmentHelper.getAmmoUse(serverWorld, stack, projectileStack, 1)
			: 0;

		if (ammoUse > projectileStack.getCount()) {
			return ItemStack.EMPTY;
		}

		if (ammoUse == 0) {
			ItemStack intangible = projectileStack.copyWithCount(1);
			intangible.set(DataComponentTypes.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
			return intangible;
		}

		ItemStack consumed = projectileStack.split(ammoUse);

		if (projectileStack.isEmpty() && shooter instanceof PlayerEntity player) {
			player.getInventory().removeOne(projectileStack);
		}

		return consumed;
	}
}
