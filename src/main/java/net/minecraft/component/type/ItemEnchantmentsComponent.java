package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
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
 * {@code ItemEnchantmentsComponent}.
 */
public class ItemEnchantmentsComponent implements TooltipAppender {

	public static final ItemEnchantmentsComponent DEFAULT = new ItemEnchantmentsComponent(new Object2IntOpenHashMap());
	private static final Codec<Integer> ENCHANTMENT_LEVEL_CODEC = Codec.intRange(1, 255);
	public static final Codec<ItemEnchantmentsComponent>
			CODEC =
			Codec.unboundedMap(Enchantment.ENTRY_CODEC, ENCHANTMENT_LEVEL_CODEC)
			     .xmap(
					     map -> new ItemEnchantmentsComponent(new Object2IntOpenHashMap(map)),
					     itemEnchantmentsComponent -> itemEnchantmentsComponent.enchantments
			     );
	public static final PacketCodec<RegistryByteBuf, ItemEnchantmentsComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.map(Object2IntOpenHashMap::new, Enchantment.ENTRY_PACKET_CODEC, PacketCodecs.VAR_INT),
			component -> component.enchantments,
			ItemEnchantmentsComponent::new
	);
	final Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantments;

	ItemEnchantmentsComponent(Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantments) {
		this.enchantments = enchantments;
		ObjectIterator var2 = enchantments.object2IntEntrySet().iterator();

		while (var2.hasNext()) {
			Entry<RegistryEntry<Enchantment>> entry = (Entry<RegistryEntry<Enchantment>>) var2.next();
			int i = entry.getIntValue();
			if (i < 0 || i > 255) {
				throw new IllegalArgumentException("Enchantment " + entry.getKey() + " has invalid level " + i);
			}
		}
	}

	public int getLevel(RegistryEntry<Enchantment> enchantment) {
		return this.enchantments.getInt(enchantment);
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		RegistryWrapper.WrapperLookup wrapperLookup = context.getRegistryLookup();
		RegistryEntryList<Enchantment>
				registryEntryList =
				getTooltipOrderList(wrapperLookup, RegistryKeys.ENCHANTMENT, EnchantmentTags.TOOLTIP_ORDER);

		for (RegistryEntry<Enchantment> registryEntry : registryEntryList) {
			int i = this.enchantments.getInt(registryEntry);
			if (i > 0) {
				textConsumer.accept(Enchantment.getName(registryEntry, i));
			}
		}

		ObjectIterator var10 = this.enchantments.object2IntEntrySet().iterator();

		while (var10.hasNext()) {
			Entry<RegistryEntry<Enchantment>> entry = (Entry<RegistryEntry<Enchantment>>) var10.next();
			RegistryEntry<Enchantment> registryEntry2 = (RegistryEntry<Enchantment>) entry.getKey();
			if (!registryEntryList.contains(registryEntry2)) {
				textConsumer.accept(Enchantment.getName(
						(RegistryEntry<Enchantment>) entry.getKey(),
						entry.getIntValue()
				));
			}
		}
	}

	private static <T> RegistryEntryList<T> getTooltipOrderList(
			RegistryWrapper.@Nullable WrapperLookup registries,
			RegistryKey<Registry<T>> registryRef,
			TagKey<T> tooltipOrderTag
	) {
		if (registries != null) {
			Optional<RegistryEntryList.Named<T>>
					optional =
					registries.getOrThrow(registryRef).getOptional(tooltipOrderTag);
			if (optional.isPresent()) {
				return optional.get();
			}
		}

		return RegistryEntryList.of();
	}

	public Set<RegistryEntry<Enchantment>> getEnchantments() {
		return Collections.unmodifiableSet(this.enchantments.keySet());
	}

	public Set<Entry<RegistryEntry<Enchantment>>> getEnchantmentEntries() {
		return Collections.unmodifiableSet(this.enchantments.object2IntEntrySet());
	}

	public int getSize() {
		return this.enchantments.size();
	}

	public boolean isEmpty() {
		return this.enchantments.isEmpty();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else {
			return o instanceof ItemEnchantmentsComponent itemEnchantmentsComponent ? this.enchantments.equals(
					itemEnchantmentsComponent.enchantments) : false;
		}
	}

	@Override
	public int hashCode() {
		return this.enchantments.hashCode();
	}

	@Override
	public String toString() {
		return "ItemEnchantments{enchantments=" + this.enchantments + "}";
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap();

		public Builder(ItemEnchantmentsComponent enchantmentsComponent) {
			this.enchantments.putAll(enchantmentsComponent.enchantments);
		}

		public void set(RegistryEntry<Enchantment> enchantment, int level) {
			if (level <= 0) {
				this.enchantments.removeInt(enchantment);
			}
			else {
				this.enchantments.put(enchantment, Math.min(level, 255));
			}
		}

		public void add(RegistryEntry<Enchantment> enchantment, int level) {
			if (level > 0) {
				this.enchantments.merge(enchantment, Math.min(level, 255), Integer::max);
			}
		}

		public void remove(Predicate<RegistryEntry<Enchantment>> predicate) {
			this.enchantments.keySet().removeIf(predicate);
		}

		public int getLevel(RegistryEntry<Enchantment> enchantment) {
			return this.enchantments.getOrDefault(enchantment, 0);
		}

		public Set<RegistryEntry<Enchantment>> getEnchantments() {
			return this.enchantments.keySet();
		}

		public ItemEnchantmentsComponent build() {
			return new ItemEnchantmentsComponent(this.enchantments);
		}
	}
}
