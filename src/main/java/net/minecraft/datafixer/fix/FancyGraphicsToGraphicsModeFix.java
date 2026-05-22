package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Мигрирует булево поле {@code fancyGraphics} в числовое поле {@code graphicsMode}:
 * {@code "true"} → {@code "1"} (Fancy), иначе → {@code "0"} (Fast).
 */
public class FancyGraphicsToGraphicsModeFix extends DataFix {

	public FancyGraphicsToGraphicsModeFix(Schema schema) {
		super(schema, true);
	}

	@Override
	public TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
			"fancyGraphics to graphicsMode",
			getInputSchema().getType(TypeReferences.OPTIONS),
			typed -> typed.update(
				DSL.remainderFinder(),
				options -> options.renameAndFixField("fancyGraphics", "graphicsMode", this::convertGraphicsMode)
			)
		);
	}

	private <T> Dynamic<T> convertGraphicsMode(Dynamic<T> value) {
		return "true".equals(value.asString("true"))
			? value.createString("1")
			: value.createString("0");
	}
}
