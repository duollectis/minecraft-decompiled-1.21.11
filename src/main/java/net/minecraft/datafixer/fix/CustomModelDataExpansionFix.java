package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Расширяет формат компонента {@code minecraft:custom_model_data}: конвертирует
 * одиночное числовое значение в объект с полем {@code floats} — списком из одного float.
 */
public class CustomModelDataExpansionFix extends DataFix {

	public CustomModelDataExpansionFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> dataComponentsType = getInputSchema().getType(TypeReferences.DATA_COMPONENTS);

		return fixTypeEverywhereTyped(
				"Custom Model Data expansion",
				dataComponentsType,
				typed -> typed.update(
						DSL.remainderFinder(),
						dynamic -> dynamic.update(
								"minecraft:custom_model_data",
								customModelData -> {
									float value = customModelData.asNumber(0.0F).floatValue();
									return customModelData.createMap(Map.of(
											customModelData.createString("floats"),
											customModelData.createList(Stream.of(customModelData.createFloat(value)))
									));
								}
						)
				)
		);
	}
}
