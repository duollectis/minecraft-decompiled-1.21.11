package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;

/**
 * Мигрирует данные зелья облака эффекта из разрозненных полей ({@code Color}, {@code effects}, {@code Potion})
 * в единый объект {@code potion_contents}.
 */
public class AreaEffectCloudPotionFix extends ChoiceFix {

	public AreaEffectCloudPotionFix(Schema outputSchema) {
		super(outputSchema, false, "AreaEffectCloudPotionFix", TypeReferences.ENTITY, "minecraft:area_effect_cloud");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::update);
	}

	private <T> Dynamic<T> update(Dynamic<T> cloud) {
		Optional<Dynamic<T>> color = cloud.get("Color").result();
		Optional<Dynamic<T>> effects = cloud.get("effects").result();
		Optional<Dynamic<T>> potion = cloud.get("Potion").result();

		cloud = cloud.remove("Color").remove("effects").remove("Potion");

		if (color.isEmpty() && effects.isEmpty() && potion.isEmpty()) {
			return cloud;
		}

		Dynamic<T> potionContents = cloud.emptyMap();

		if (color.isPresent()) {
			potionContents = potionContents.set("custom_color", color.get());
		}

		if (effects.isPresent()) {
			potionContents = potionContents.set("custom_effects", effects.get());
		}

		if (potion.isPresent()) {
			potionContents = potionContents.set("potion", potion.get());
		}

		return cloud.set("potion_contents", potionContents);
	}
}
