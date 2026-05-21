package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;

/**
 * {@code MapBannerBlockPosFormatFix}.
 */
public class MapBannerBlockPosFormatFix extends DataFix {

	public MapBannerBlockPosFormatFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.SAVED_DATA_MAP_DATA);
		OpticFinder<?> opticFinder = type.findField("data");
		OpticFinder<?> opticFinder2 = opticFinder.type().findField("banners");
		OpticFinder<?> opticFinder3 = DSL.typeFinder(((ListType) opticFinder2.type()).getElement());
		return this.fixTypeEverywhereTyped(
				"MapBannerBlockPosFormatFix",
				type,
				typed -> typed.updateTyped(
						opticFinder,
						typedx -> typedx.updateTyped(
								opticFinder2,
								typedxx -> typedxx.updateTyped(
										opticFinder3,
										typedxxx -> typedxxx.update(
												DSL.remainderFinder(),
												dynamic -> dynamic.update("Pos", FixUtil::fixBlockPos)
										)
								)
						)
				)
		);
	}
}
