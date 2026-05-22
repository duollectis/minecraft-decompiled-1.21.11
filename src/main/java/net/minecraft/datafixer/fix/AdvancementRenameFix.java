package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.function.Function;

/**
 * Переименовывает ключи достижений в данных игрока, применяя заданную функцию переименования.
 */
public class AdvancementRenameFix extends DataFix {

	private final String name;
	private final Function<String, String> renamer;

	public AdvancementRenameFix(
		Schema outputSchema,
		boolean changesType,
		String name,
		Function<String, String> renamer
	) {
		super(outputSchema, changesType);
		this.name = name;
		this.renamer = renamer;
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
			name,
			getInputSchema().getType(TypeReferences.ADVANCEMENTS),
			typed -> typed.update(
				DSL.remainderFinder(),
				dynamic -> dynamic.updateMapValues(pair -> {
					String key = ((Dynamic<?>) pair.getFirst()).asString("");
					return pair.mapFirst(d -> dynamic.createString(renamer.apply(key)));
				})
			)
		);
	}
}
