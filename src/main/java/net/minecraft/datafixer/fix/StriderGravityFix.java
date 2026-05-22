package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет данные в формате DataFixer.
 */
public class StriderGravityFix extends ChoiceFix {

	public StriderGravityFix(Schema schema, boolean bl) {
		super(schema, bl, "StriderGravityFix", TypeReferences.ENTITY, "minecraft:strider");
	}

	public Dynamic<?> updateNoGravityNbt(Dynamic<?> striderDynamic) {
		return striderDynamic.get("NoGravity").asBoolean(false) ? striderDynamic.set(
				"NoGravity",
				striderDynamic.createBoolean(false)
		) : striderDynamic;
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::updateNoGravityNbt);
	}
}
