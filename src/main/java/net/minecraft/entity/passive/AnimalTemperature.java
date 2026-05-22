package net.minecraft.entity.passive;

import net.minecraft.util.Identifier;

/**
 * Идентификаторы климатических зон для выбора вариантов животных при спавне.
 * Используются как ключи в реестрах вариантов (коровы, свиньи, куры и т.д.).
 */
public interface AnimalTemperature {

	Identifier TEMPERATE = Identifier.ofVanilla("temperate");
	Identifier WARM = Identifier.ofVanilla("warm");
	Identifier COLD = Identifier.ofVanilla("cold");
}
