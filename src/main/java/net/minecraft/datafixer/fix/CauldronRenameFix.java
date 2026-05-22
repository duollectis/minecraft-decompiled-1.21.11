package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;

/**
 * Переименовывает блок {@code minecraft:cauldron} в {@code minecraft:water_cauldron},
 * если уровень воды больше нуля. Пустой котёл теряет тег {@code Properties}.
 */
public class CauldronRenameFix extends DataFix {

	private static final String CAULDRON_ID = "minecraft:cauldron";
	private static final String WATER_CAULDRON_ID = "minecraft:water_cauldron";
	private static final String EMPTY_LEVEL = "0";

	public CauldronRenameFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	private static Dynamic<?> rename(Dynamic<?> cauldronDynamic) {
		Optional<String> nameOpt = cauldronDynamic.get("Name").asString().result();

		if (nameOpt.isEmpty() || !CAULDRON_ID.equals(nameOpt.get())) {
			return cauldronDynamic;
		}

		Dynamic<?> properties = cauldronDynamic.get("Properties").orElseEmptyMap();
		return properties.get("level").asString(EMPTY_LEVEL).equals(EMPTY_LEVEL)
			? cauldronDynamic.remove("Properties")
			: cauldronDynamic.set("Name", cauldronDynamic.createString(WATER_CAULDRON_ID));
	}

	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
			"cauldron_rename_fix",
			getInputSchema().getType(TypeReferences.BLOCK_STATE),
			typed -> typed.update(DSL.remainderFinder(), CauldronRenameFix::rename)
		);
	}
}
