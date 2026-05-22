package net.minecraft.entity;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;

/**
 * Утилитарный класс для расчёта урона с учётом брони и зачарований.
 * Формулы основаны на ванильной механике Minecraft: броня снижает урон
 * пропорционально своему значению, а прочность брони (toughness) уменьшает
 * эффективность пробивания брони высоким уроном.
 */
public class DamageUtil {

	public static final float MAX_ARMOR_VALUE = 20.0F;
	public static final float ARMOR_PROTECTION_DIVISOR = 25.0F;
	public static final float ARMOR_TOUGHNESS_BASE = 2.0F;
	public static final float MIN_ARMOR_FACTOR = 0.2F;
	private static final int ARMOR_TOUGHNESS_DIVISOR = 4;

	/**
	 * Вычисляет итоговый урон после применения брони и зачарований.
	 * Сначала рассчитывается эффективная броня с учётом прочности и входящего урона,
	 * затем зачарования могут дополнительно скорректировать коэффициент защиты.
	 *
	 * @param armorWearer    сущность, носящая броню
	 * @param damageAmount   входящий урон до применения брони
	 * @param damageSource   источник урона (используется для получения оружия атакующего)
	 * @param armor          суммарное значение брони носителя
	 * @param armorToughness суммарная прочность брони носителя
	 * @return итоговый урон после всех снижений
	 */
	public static float getDamageLeft(
		LivingEntity armorWearer,
		float damageAmount,
		DamageSource damageSource,
		float armor,
		float armorToughness
	) {
		float toughnessBase = ARMOR_TOUGHNESS_BASE + armorToughness / ARMOR_TOUGHNESS_DIVISOR;
		float effectiveArmor = MathHelper.clamp(
			armor - damageAmount / toughnessBase,
			armor * MIN_ARMOR_FACTOR,
			MAX_ARMOR_VALUE
		);
		float armorFactor = effectiveArmor / ARMOR_PROTECTION_DIVISOR;

		ItemStack weaponStack = damageSource.getWeaponStack();

		if (weaponStack == null || !(armorWearer.getEntityWorld() instanceof ServerWorld serverWorld)) {
			return damageAmount * (1.0F - armorFactor);
		}

		float protectionFactor = MathHelper.clamp(
			EnchantmentHelper.getArmorEffectiveness(serverWorld, weaponStack, armorWearer, damageSource, armorFactor),
			0.0F,
			1.0F
		);

		return damageAmount * (1.0F - protectionFactor);
	}

	/**
	 * Вычисляет урон после применения зачарований защиты (без учёта брони).
	 *
	 * @param damageDealt входящий урон
	 * @param protection  суммарное значение защиты от зачарований
	 * @return урон после снижения зачарованиями
	 */
	public static float getInflictedDamage(float damageDealt, float protection) {
		float clampedProtection = MathHelper.clamp(protection, 0.0F, MAX_ARMOR_VALUE);
		return damageDealt * (1.0F - clampedProtection / ARMOR_PROTECTION_DIVISOR);
	}
}
