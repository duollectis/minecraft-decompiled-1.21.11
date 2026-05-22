package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет данные в формате DataFixer.
 */
public class OminousBannerBlockEntityRenameFix extends ChoiceFix {

	public OminousBannerBlockEntityRenameFix(Schema schema, boolean bl) {
		super(schema, bl, "OminousBannerBlockEntityRenameFix", TypeReferences.BLOCK_ENTITY, "minecraft:banner");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		OpticFinder<?> opticFinder = inputTyped.getType().findField("CustomName");
		OpticFinder<Pair<String, String>>
				opticFinder2 =
				(OpticFinder<Pair<String, String>>) DSL.typeFinder(this
						.getInputSchema()
						.getType(TypeReferences.TEXT_COMPONENT));
		return inputTyped.updateTyped(
				opticFinder,
				typed -> typed.update(
						opticFinder2,
						pair -> pair.mapSecond(
								string -> string.replace(
										"\"translate\":\"block.minecraft.illager_banner\"",
										"\"translate\":\"block.minecraft.ominous_banner\""
								)
						)
				)
		);
	}
}
