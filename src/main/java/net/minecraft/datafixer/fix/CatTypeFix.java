package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет некорректный тип кота: {@code CatType = 9} (Siamese) был ошибочно
 * назначен двум разным породам, поэтому Siamese переносится на индекс 10.
 */
public class CatTypeFix extends ChoiceFix {

	public CatTypeFix(Schema schema, boolean changesType) {
		super(schema, changesType, "CatTypeFix", TypeReferences.ENTITY, "minecraft:cat");
	}

	private static final int SIAMESE_OLD_TYPE = 9;
	private static final int SIAMESE_NEW_TYPE = 10;

	private Dynamic<?> fixCatTypeData(Dynamic<?> catDynamic) {
		return catDynamic.get("CatType").asInt(0) == SIAMESE_OLD_TYPE
			? catDynamic.set("CatType", catDynamic.createInt(SIAMESE_NEW_TYPE))
			: catDynamic;
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::fixCatTypeData);
	}
}
