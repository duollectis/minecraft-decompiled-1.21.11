package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

/**
 * Мигрирует числовое поле {@code SoundVariant} козьего рога в строковый
 * идентификатор инструмента {@code instrument}, используя индекс в массиве
 * {@link #GOAT_HORN_IDS}. Некорректные индексы сбрасываются к {@code 0}.
 */
public class GoatHornIdFix extends SimpleItemNbtFix {

	private static final String[] GOAT_HORN_IDS = {
		"minecraft:ponder_goat_horn",
		"minecraft:sing_goat_horn",
		"minecraft:seek_goat_horn",
		"minecraft:feel_goat_horn",
		"minecraft:admire_goat_horn",
		"minecraft:call_goat_horn",
		"minecraft:yearn_goat_horn",
		"minecraft:dream_goat_horn"
	};

	public GoatHornIdFix(Schema outputSchema) {
		super(outputSchema, "GoatHornIdFix", itemId -> itemId.equals("minecraft:goat_horn"));
	}

	@Override
	protected <T> Dynamic<T> fixNbt(Dynamic<T> dynamic) {
		int variantIndex = dynamic.get("SoundVariant").asInt(0);
		int safeIndex = variantIndex >= 0 && variantIndex < GOAT_HORN_IDS.length ? variantIndex : 0;
		String instrumentId = GOAT_HORN_IDS[safeIndex];

		return dynamic.remove("SoundVariant").set("instrument", dynamic.createString(instrumentId));
	}
}
