package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
	 * Компонент зачарований предмета. Хранит карту «зачарование → уровень».
	 * Уровень 0 означает отсутствие зачарования; уровни выше {@link Enchantment#MAX_LEVEL} обрезаются.
	 */
public class ItemEnchantmentsComponent implements TooltipAppender {

	private static final int MIN_VALID_LEVEL = 0;
	private static final int MAX_VALID_LEVEL = 255;

	public static final ItemEnchantmentsComponent DEFAULT = new ItemEnchantmentsComponent(new Object2IntOpenHashMap<>());
	private static final Codec<Integer> ENCHANTMENT_LEVEL_CODEC = Codec.intRange(1, Enchantment.MAX_LEVEL);

	public static final Codec<ItemEnchantmentsComponent> CODEC = Codec.unboundedMap(
			Enchantment.ENTRY_CODEC,
			ENCHANTMENT_LEVEL_CODEC
	).xmap(
			map -> new ItemEnchantmentsComponent(new Object2IntOpenHashMap<>(map)),
			component -> component.enchantments
	);

	public static final PacketCodec<RegistryByteBuf, ItemEnchantmentsComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.map(Object2IntOpenHashMap::new, Enchantment.ENTRY_PACKET_CODEC, PacketCodecs.VAR_INT),
			component -> component.enchantments,
			ItemEnchantmentsComponent::new
	);

	final Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantments;

	ItemEnchantmentsComponent(Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantments) {
		this.enchantments = enchantments;

		for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.object2IntEntrySet()) {
			int level = entry.getIntValue();
			if (level < MIN_VALID_LEVEL || level > MAX_VALID_LEVEL) {
				throw new IllegalArgumentException("Enchantment " + entry.getKey() + " has invalid level " + level);
			}
		}
	}

	public int getLevel(RegistryEntry<Enchantment> enchantment) {
		return enchantments.getInt(enchantment);
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		RegistryWrapper.WrapperLookup wrapperLookup = context.getRegistryLookup();
		RegistryEntryList<Enchantment> orderedList = getTooltipOrderList(
				wrapperLookup,
				RegistryKeys.ENCHANTMENT,
				EnchantmentTags.TOOLTIP_ORDER
		);

		for (RegistryEntry<Enchantment> entry : orderedList) {
			int level = enchantments.getInt(entry);
			if (level > 0) {
				textConsumer.accept(Enchantment.getName(entry, level));
			}
		}

		for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.object2IntEntrySet()) {
			RegistryEntry<Enchantment> enchantment = entry.getKey();
			if (!orderedList.contains(enchantment)) {
				textConsumer.accept(Enchantment.getName(enchantment, entry.getIntValue()));
			}
		}
	}

	private static <T> RegistryEntryList<T> getTooltipOrderList(
			RegistryWrapper.@Nullable WrapperLookup registries,
			RegistryKey<Registry<T>> registryRef,
			TagKey<T> tooltipOrderTag
	) {
		if (registries == null) {
			return RegistryEntryList.of();
		}

		Optional<RegistryEntryList.Named<T>> optional = registries.getOrThrow(registryRef).getOptional(tooltipOrderTag);
		return optional.isPresent() ? optional.get() : RegistryEntryList.of();
	}

	public Set<RegistryEntry<Enchantment>> getEnchantments() {
		return Collections.unmodifiableSet(enchantments.keySet());
	}

	public Set<Object2IntMap.Entry<RegistryEntry<Enchantment>>> getEnchantmentEntries() {
		return Collections.unmodifiableSet(enchantments.object2IntEntrySet());
	}

	public int getSize() {
		return enchantments.size();
	}

	public boolean isEmpty() {
		return enchantments.isEmpty();
	}

	@Override
	public boolean equals(Object o) {
		return this == o
			? true
			: o instanceof ItemEnchantmentsComponent other && enchantments.equals(other.enchantments);
	}

	@Override
	public int hashCode() {
		return enchantments.hashCode();
	}

	@Override
	public String toString() {
		return "ItemEnchantments{enchantments=" + enchantments + "}";
	}

	public static class Builder {

		private final Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap<>();

		public Builder(ItemEnchantmentsComponent enchantmentsComponent) {
			enchantments.putAll(enchantmentsComponent.enchantments);
		}

		public void set(RegistryEntry<Enchantment> enchantment, int level) {
			if (level <= 0) {
				enchantments.removeInt(enchantment);
			} else {
				enchantments.put(enchantment, Math.min(level, Enchantment.MAX_LEVEL));
			}
		}

		public void add(RegistryEntry<Enchantment> enchantment, int level) {
			if (level > 0) {
				enchantments.merge(enchantment, Math.min(level, Enchantment.MAX_LEVEL), Integer::max);
			}
		}

		public void remove(Predicate<RegistryEntry<Enchantment>> predicate) {
			enchantments.keySet().removeIf(predicate);
		}

		public int getLevel(RegistryEntry<Enchantment> enchantment) {
			return enchantments.getOrDefault(enchantment, 0);
		}

		public Set<RegistryEntry<Enchantment>> getEnchantments() {
			return enchantments.keySet();
		}

		public ItemEnchantmentsComponent build() {
			return new ItemEnchantmentsComponent(enchantments);
		}
	}
}
