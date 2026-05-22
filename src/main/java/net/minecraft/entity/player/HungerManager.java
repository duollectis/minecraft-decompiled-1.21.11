package net.minecraft.entity.player;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.rule.GameRules;

/**
 * Управляет состоянием голода, насыщения и истощения игрока.
 * Обрабатывает регенерацию здоровья от еды и урон от голодания в зависимости от сложности.
 */
public class HungerManager {

	private static final float MAX_EXHAUSTION = 40.0F;
	private static final float SATURATION_DRAIN_PER_EXHAUSTION = 1.0F;
	private static final float FOOD_DRAIN_PER_EXHAUSTION = 1.0F;
	private static final float FAST_HEAL_AMOUNT = 1.0F;
	private static final float STARVE_DAMAGE = 1.0F;
	private static final float STARVE_SAFE_HEALTH_HARD = 1.0F;
	private static final float STARVE_SAFE_HEALTH_NORMAL = 10.0F;

	private int foodLevel = HungerConstants.FULL_FOOD_LEVEL;
	private float saturationLevel = HungerConstants.INITIAL_SATURATION_LEVEL;
	private float exhaustion;
	private int foodTickTimer;

	/**
	 * Добавляет питательность и насыщение напрямую (без пересчёта через модификатор).
	 * Оба значения зажимаются в допустимых пределах.
	 */
	private void addInternal(int nutrition, float saturation) {
		foodLevel = MathHelper.clamp(nutrition + foodLevel, 0, HungerConstants.FULL_FOOD_LEVEL);
		saturationLevel = MathHelper.clamp(saturation + saturationLevel, 0.0F, (float) foodLevel);
	}

	/**
	 * Добавляет еду с пересчётом насыщения через модификатор.
	 *
	 * @param food               количество единиц еды
	 * @param saturationModifier модификатор насыщения
	 */
	public void add(int food, float saturationModifier) {
		addInternal(food, HungerConstants.calculateSaturation(food, saturationModifier));
	}

	/**
	 * Применяет эффект поедания компонента еды.
	 *
	 * @param foodComponent компонент еды предмета
	 */
	public void eat(FoodComponent foodComponent) {
		addInternal(foodComponent.nutrition(), foodComponent.saturation());
	}

	/**
	 * Выполняет тиковое обновление голода: расходует истощение, регенерирует здоровье
	 * или наносит урон от голода в зависимости от уровня еды и сложности мира.
	 *
	 * @param player игрок, для которого выполняется обновление
	 */
	public void update(ServerPlayerEntity player) {
		ServerWorld serverWorld = player.getEntityWorld();
		Difficulty difficulty = serverWorld.getDifficulty();

		if (exhaustion > HungerConstants.EXHAUSTION_UNIT) {
			exhaustion -= HungerConstants.EXHAUSTION_UNIT;

			if (saturationLevel > 0.0F) {
				saturationLevel = Math.max(saturationLevel - SATURATION_DRAIN_PER_EXHAUSTION, 0.0F);
			} else if (difficulty != Difficulty.PEACEFUL) {
				foodLevel = Math.max(foodLevel - (int) FOOD_DRAIN_PER_EXHAUSTION, 0);
			}
		}

		boolean naturalRegenEnabled = serverWorld.getGameRules().getValue(GameRules.NATURAL_HEALTH_REGENERATION);

		if (naturalRegenEnabled && saturationLevel > 0.0F && player.canFoodHeal()
			&& foodLevel >= HungerConstants.FULL_FOOD_LEVEL) {
			foodTickTimer++;

			if (foodTickTimer >= HungerConstants.FAST_HEALING_INTERVAL) {
				float healAmount = Math.min(saturationLevel, HungerConstants.HEAL_EXHAUSTION_SPRINT);
				player.heal(healAmount / HungerConstants.HEAL_EXHAUSTION_SPRINT);
				addExhaustion(healAmount);
				foodTickTimer = 0;
			}
		} else if (naturalRegenEnabled && foodLevel >= HungerConstants.SLOW_HEALING_FOOD_LEVEL
			&& player.canFoodHeal()) {
			foodTickTimer++;

			if (foodTickTimer >= HungerConstants.SLOW_HEALING_STARVING_INTERVAL) {
				player.heal(FAST_HEAL_AMOUNT);
				addExhaustion(HungerConstants.HEAL_EXHAUSTION_SPRINT);
				foodTickTimer = 0;
			}
		} else if (foodLevel <= HungerConstants.STARVING_FOOD_LEVEL) {
			foodTickTimer++;

			if (foodTickTimer >= HungerConstants.SLOW_HEALING_STARVING_INTERVAL) {
				boolean canStarveDamage = player.getHealth() > STARVE_SAFE_HEALTH_NORMAL
					|| difficulty == Difficulty.HARD
					|| player.getHealth() > STARVE_SAFE_HEALTH_HARD && difficulty == Difficulty.NORMAL;

				if (canStarveDamage) {
					player.damage(serverWorld, player.getDamageSources().starve(), STARVE_DAMAGE);
				}

				foodTickTimer = 0;
			}
		} else {
			foodTickTimer = 0;
		}
	}

	/**
	 * Читает состояние голода из NBT-хранилища.
	 *
	 * @param view источник данных
	 */
	public void readData(ReadView view) {
		foodLevel = view.getInt("foodLevel", HungerConstants.FULL_FOOD_LEVEL);
		foodTickTimer = view.getInt("foodTickTimer", 0);
		saturationLevel = view.getFloat("foodSaturationLevel", HungerConstants.INITIAL_SATURATION_LEVEL);
		exhaustion = view.getFloat("foodExhaustionLevel", 0.0F);
	}

	/**
	 * Записывает состояние голода в NBT-хранилище.
	 *
	 * @param view приёмник данных
	 */
	public void writeData(WriteView view) {
		view.putInt("foodLevel", foodLevel);
		view.putInt("foodTickTimer", foodTickTimer);
		view.putFloat("foodSaturationLevel", saturationLevel);
		view.putFloat("foodExhaustionLevel", exhaustion);
	}

	public int getFoodLevel() {
		return foodLevel;
	}

	/**
	 * Проверяет, достаточно ли еды для спринта (уровень еды должен быть выше 6).
	 */
	public boolean canSprint() {
		return foodLevel > 6;
	}

	public boolean isNotFull() {
		return foodLevel < HungerConstants.FULL_FOOD_LEVEL;
	}

	/**
	 * Добавляет истощение, ограничивая максимальное значение {@link #MAX_EXHAUSTION}.
	 *
	 * @param amount количество добавляемого истощения
	 */
	public void addExhaustion(float amount) {
		exhaustion = Math.min(exhaustion + amount, MAX_EXHAUSTION);
	}

	public float getSaturationLevel() {
		return saturationLevel;
	}

	public void setFoodLevel(int foodLevel) {
		this.foodLevel = foodLevel;
	}

	public void setSaturationLevel(float saturationLevel) {
		this.saturationLevel = saturationLevel;
	}
}
