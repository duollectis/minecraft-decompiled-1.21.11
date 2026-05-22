package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.datafixer.TypeReferences;

/**
 * Переименовывает поле {@code model} в {@code asset_id} внутри компонента
 * {@code minecraft:equippable}, приводя его к новому соглашению об именовании.
 */
public class EquippableAssetRenameFix extends DataFix {

	public EquippableAssetRenameFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> dataComponentsType = getInputSchema().getType(TypeReferences.DATA_COMPONENTS);
		OpticFinder<?> equippableFinder = dataComponentsType.findField("minecraft:equippable");

		return fixTypeEverywhereTyped(
			"equippable asset rename fix",
			dataComponentsType,
			typed -> typed.updateTyped(
				equippableFinder,
				equippable -> equippable.update(
					DSL.remainderFinder(),
					dynamic -> dynamic.renameField("model", "asset_id")
				)
			)
		);
	}
}
