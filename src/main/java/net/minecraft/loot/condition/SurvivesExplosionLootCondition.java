package net.minecraft.loot.condition;

import com.mojang.serialization.MapCodec;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.math.random.Random;

import java.util.Set;

/**
 * Условие лута: предмет выживает при взрыве с вероятностью {@code 1 / радиус_взрыва}.
 * Если радиус взрыва не задан в контексте — условие всегда истинно.
 */
public class SurvivesExplosionLootCondition implements LootCondition {

	private static final SurvivesExplosionLootCondition INSTANCE = new SurvivesExplosionLootCondition();
	public static final MapCodec<SurvivesExplosionLootCondition> CODEC = MapCodec.unit(INSTANCE);

	private SurvivesExplosionLootCondition() {
	}

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.SURVIVES_EXPLOSION;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.EXPLOSION_RADIUS);
	}

	@Override
	public boolean test(LootContext lootContext) {
		Float radius = lootContext.get(LootContextParameters.EXPLOSION_RADIUS);

		if (radius == null) {
			return true;
		}

		Random random = lootContext.getRandom();
		float survivalChance = 1.0F / radius;
		return random.nextFloat() <= survivalChance;
	}

	public static LootCondition.Builder builder() {
		return () -> INSTANCE;
	}
}
