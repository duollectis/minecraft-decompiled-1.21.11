package net.minecraft.predicate.item;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.potion.Potion;
import net.minecraft.predicate.component.ComponentPredicate;
import net.minecraft.predicate.component.ComponentSubPredicate;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;

import java.util.Optional;

/**
 * Предикат для проверки содержимого зелья: входит ли зелье предмета в заданный список.
 */
public record PotionContentsPredicate(RegistryEntryList<Potion> potions) implements ComponentSubPredicate<PotionContentsComponent> {

	public static final Codec<PotionContentsPredicate> CODEC = RegistryCodecs
			.entryList(RegistryKeys.POTION)
			.xmap(PotionContentsPredicate::new, PotionContentsPredicate::potions);

	public static ComponentPredicate potionContents(RegistryEntryList<Potion> potions) {
		return new PotionContentsPredicate(potions);
	}

	@Override
	public ComponentType<PotionContentsComponent> getComponentType() {
		return DataComponentTypes.POTION_CONTENTS;
	}

	public boolean test(PotionContentsComponent component) {
		Optional<RegistryEntry<Potion>> potion = component.potion();
		return potion.isPresent() && potions.contains(potion.get());
	}
}
