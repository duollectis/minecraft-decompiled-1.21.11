package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

/**
 * Добавляет булево поле с заданным значением, если оно ещё не присутствует в данных.
 */
public class AddFlagIfNotPresentFix extends DataFix {

	private final String description;
	private final boolean flagValue;
	private final String key;
	private final TypeReference typeReference;

	public AddFlagIfNotPresentFix(Schema outputSchema, TypeReference typeReference, String key, boolean flagValue) {
		super(outputSchema, true);
		this.flagValue = flagValue;
		this.key = key;
		description = "AddFlagIfNotPresentFix_" + key + "=" + flagValue + " for " + outputSchema.getVersionKey();
		this.typeReference = typeReference;
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> type = getInputSchema().getType(typeReference);

		return fixTypeEverywhereTyped(
			description,
			type,
			typed -> typed.update(
				DSL.remainderFinder(),
				dynamic -> dynamic.set(
					key,
					(Dynamic<?>) DataFixUtils.orElseGet(
						dynamic.get(key).result(),
						() -> dynamic.createBoolean(flagValue)
					)
				)
			)
		);
	}
}
