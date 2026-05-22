package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Удаляет флаг {@code Silent} у подставки для брони, если она не является маркером.
 * Маркеры должны оставаться беззвучными по своей природе, поэтому флаг там избыточен.
 */
public class EntityArmorStandSilentFix extends ChoiceFix {

	public EntityArmorStandSilentFix(Schema schema, boolean changesType) {
		super(schema, changesType, "EntityArmorStandSilentFix", TypeReferences.ENTITY, "ArmorStand");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::fixSilent);
	}

	private Dynamic<?> fixSilent(Dynamic<?> armorStand) {
		boolean isSilent = armorStand.get("Silent").asBoolean(false);
		boolean isMarker = armorStand.get("Marker").asBoolean(false);

		return isSilent && !isMarker
				? armorStand.remove("Silent")
				: armorStand;
	}
}
