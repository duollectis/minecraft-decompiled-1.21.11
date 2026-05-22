package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Конвертирует числовое поле {@code weather_state} медного голема в строковый идентификатор
 * стадии окисления: 0 → {@code unaffected}, 1 → {@code exposed}, 2 → {@code weathered}, 3 → {@code oxidized}.
 */
public class CopperGolemWeatherStateFix extends ChoiceFix {

	public CopperGolemWeatherStateFix(Schema outputSchema) {
		super(outputSchema, false, "CopperGolemWeatherStateFix", TypeReferences.ENTITY, "minecraft:copper_golem");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(
				DSL.remainderFinder(),
				dynamic -> dynamic.update("weather_state", CopperGolemWeatherStateFix::fixWeatherState)
		);
	}

	private static Dynamic<?> fixWeatherState(Dynamic<?> weatherState) {
		return switch (weatherState.asInt(0)) {
			case 1 -> weatherState.createString("exposed");
			case 2 -> weatherState.createString("weathered");
			case 3 -> weatherState.createString("oxidized");
			default -> weatherState.createString("unaffected");
		};
	}
}
