package net.minecraft.entity.player;

/**
 * Константы системы голода и насыщения игрока.
 * Все значения соответствуют ванильной механике Minecraft.
 */
public class HungerConstants {

	public static final int FULL_FOOD_LEVEL = 20;
	public static final float FULL_SATURATION_LEVEL = 20.0F;
	public static final float INITIAL_SATURATION_LEVEL = 5.0F;
	public static final float HALF_SATURATION_LEVEL = 2.5F;
	public static final float EXHAUSTION_UNIT = 4.0F;
	public static final int SLOW_HEALING_STARVING_INTERVAL = 80;
	public static final int FAST_HEALING_INTERVAL = 10;
	public static final int SLOW_HEALING_FOOD_LEVEL = 18;
	public static final int EXHAUSTION_PER_HITPOINT = 6;
	public static final int STARVING_FOOD_LEVEL = 0;
	public static final float HEAL_EXHAUSTION_VERY_SLOW = 0.1F;
	public static final float HEAL_EXHAUSTION_SLOW = 0.3F;
	public static final float HEAL_EXHAUSTION_MEDIUM = 0.6F;
	public static final float HEAL_EXHAUSTION_FAST = 0.8F;
	public static final float HEAL_EXHAUSTION_NORMAL = 1.0F;
	public static final float HEAL_EXHAUSTION_QUICK = 1.2F;
	public static final float HEAL_EXHAUSTION_SPRINT = 6.0F;
	public static final float WALK_EXHAUSTION = 0.05F;
	public static final float SPRINT_EXHAUSTION = 0.2F;
	public static final float SWIM_EXHAUSTION = 0.005F;
	public static final float JUMP_EXHAUSTION = 0.1F;
	public static final float SPRINT_JUMP_EXHAUSTION_BASE = 0.0F;
	public static final float ATTACK_EXHAUSTION_BASE = 0.0F;
	public static final float SPRINT_JUMP_EXHAUSTION_BONUS = 0.1F;
	public static final float ATTACK_EXHAUSTION_BONUS = 0.01F;

	/**
	 * Вычисляет уровень насыщения по питательности и модификатору насыщения.
	 * Формула: {@code nutrition * saturationModifier * 2}.
	 *
	 * @param nutrition          питательность еды
	 * @param saturationModifier модификатор насыщения (0.0–1.0 и выше)
	 * @return итоговый уровень насыщения
	 */
	public static float calculateSaturation(int nutrition, float saturationModifier) {
		return nutrition * saturationModifier * 2.0F;
	}
}
