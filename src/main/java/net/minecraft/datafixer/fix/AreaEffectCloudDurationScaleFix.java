package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Устанавливает поле {@code potion_duration_scale} для облака эффекта зелья.
 * Значение 0.25 соответствует стандартному масштабу длительности зелья.
 */
public class AreaEffectCloudDurationScaleFix extends ChoiceFix {

	private static final float DEFAULT_POTION_DURATION_SCALE = 0.25F;

	public AreaEffectCloudDurationScaleFix(Schema outputSchema) {
		super(outputSchema, false, "AreaEffectCloudDurationScaleFix", TypeReferences.ENTITY, "minecraft:area_effect_cloud");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(
			DSL.remainderFinder(),
			dynamic -> dynamic.set("potion_duration_scale", dynamic.createFloat(DEFAULT_POTION_DURATION_SCALE))
		);
	}
}
