package net.minecraft.predicate.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;

import java.util.Optional;

/**
 * Предикат для проверки наличия конкретного зачарования с заданным диапазоном уровней.
 * Если {@code enchantments} пуст — проверяет любое зачарование в указанном диапазоне уровней.
 * Если оба поля не заданы — проверяет наличие хотя бы одного зачарования.
 */
public record EnchantmentPredicate(Optional<RegistryEntryList<Enchantment>> enchantments, NumberRange.IntRange levels) {

	public static final Codec<EnchantmentPredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					RegistryCodecs.entryList(RegistryKeys.ENCHANTMENT)
							.optionalFieldOf("enchantments")
							.forGetter(EnchantmentPredicate::enchantments),
					NumberRange.IntRange.CODEC
							.optionalFieldOf("levels", NumberRange.IntRange.ANY)
							.forGetter(EnchantmentPredicate::levels)
			)
			.apply(instance, EnchantmentPredicate::new)
	);

	public EnchantmentPredicate(RegistryEntry<Enchantment> enchantment, NumberRange.IntRange levels) {
		this(Optional.of(RegistryEntryList.of(enchantment)), levels);
	}

	public EnchantmentPredicate(RegistryEntryList<Enchantment> enchantments, NumberRange.IntRange levels) {
		this(Optional.of(enchantments), levels);
	}

	public boolean test(ItemEnchantmentsComponent enchantmentsComponent) {
		if (enchantments.isPresent()) {
			for (RegistryEntry<Enchantment> enchantment : enchantments.get()) {
				if (testLevel(enchantmentsComponent, enchantment)) {
					return true;
				}
			}

			return false;
		}

		if (levels != NumberRange.IntRange.ANY) {
			for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantmentsComponent.getEnchantmentEntries()) {
				if (levels.test(entry.getIntValue())) {
					return true;
				}
			}

			return false;
		}

		return !enchantmentsComponent.isEmpty();
	}

	private boolean testLevel(ItemEnchantmentsComponent enchantmentsComponent, RegistryEntry<Enchantment> enchantment) {
		int level = enchantmentsComponent.getLevel(enchantment);

		if (level == 0) {
			return false;
		}

		return levels == NumberRange.IntRange.ANY || levels.test(level);
	}
}
