package net.minecraft.datafixer.fix;

import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.slf4j.Logger;

/**
 * Исправляет данные в формате DataFixer.
 */
public class UnflattenTextComponentFix extends DataFix {

	private static final Logger LOGGER = LogUtils.getLogger();

	public UnflattenTextComponentFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	@SuppressWarnings("unchecked")
	protected TypeRewriteRule makeRule() {
		Type<Pair<String, String>>
				type =
				(Type<Pair<String, String>>) getInputSchema().getType(TypeReferences.TEXT_COMPONENT);
		Type<?> type2 = getOutputSchema().getType(TypeReferences.TEXT_COMPONENT);
		return this.createRewriteRule(type, type2);
	}

	private <T> TypeRewriteRule createRewriteRule(Type<Pair<String, String>> type, Type<T> type2) {
		return fixTypeEverywhere(
				"UnflattenTextComponentFix",
				type,
				type2,
				dynamicOps -> pair -> Util
						.readTyped(type2, parseJsonToDynamic(dynamicOps, (String) pair.getSecond()), true)
						.getValue()
		);
	}

	private static <T> Dynamic<T> parseJsonToDynamic(DynamicOps<T> dynamicOps, String string) {
		try {
			JsonElement jsonElement = LenientJsonParser.parse(string);
			if (!jsonElement.isJsonNull()) {
				return new Dynamic(dynamicOps, JsonOps.INSTANCE.convertTo(dynamicOps, jsonElement));
			}
		}
		catch (Exception var3) {
			LOGGER.error("Failed to unflatten text component json: {}", string, var3);
		}

		return new Dynamic(dynamicOps, dynamicOps.createString(string));
	}
}
