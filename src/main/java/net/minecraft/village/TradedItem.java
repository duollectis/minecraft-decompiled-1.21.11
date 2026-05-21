package net.minecraft.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.predicate.component.ComponentMapPredicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * {@code TradedItem}.
 */
public record TradedItem(RegistryEntry<Item> item, int count, ComponentMapPredicate components, ItemStack itemStack) {

	public static final Codec<TradedItem> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Item.ENTRY_CODEC.fieldOf("id").forGetter(TradedItem::item),
					                    Codecs.POSITIVE_INT.fieldOf("count").orElse(1).forGetter(TradedItem::count),
					                    ComponentMapPredicate.CODEC
							                    .optionalFieldOf("components", ComponentMapPredicate.EMPTY)
							                    .forGetter(TradedItem::components)
			                    )
			                    .apply(instance, TradedItem::new)
	);
	public static final PacketCodec<RegistryByteBuf, TradedItem> PACKET_CODEC = PacketCodec.tuple(
			Item.ENTRY_PACKET_CODEC,
			TradedItem::item,
			PacketCodecs.VAR_INT,
			TradedItem::count,
			ComponentMapPredicate.PACKET_CODEC,
			TradedItem::components,
			TradedItem::new
	);
	public static final PacketCodec<RegistryByteBuf, Optional<TradedItem>>
			OPTIONAL_PACKET_CODEC =
			PACKET_CODEC.collect(PacketCodecs::optional);

	public TradedItem(ItemConvertible item) {
		this(item, 1);
	}

	public TradedItem(ItemConvertible item, int count) {
		this(item.asItem().getRegistryEntry(), count, ComponentMapPredicate.EMPTY);
	}

	public TradedItem(RegistryEntry<Item> item, int count, ComponentMapPredicate components) {
		this(item, count, components, createDisplayStack(item, count, components));
	}

	public TradedItem withComponents(UnaryOperator<ComponentMapPredicate.Builder> builderCallback) {
		return new TradedItem(this.item, this.count, builderCallback.apply(ComponentMapPredicate.builder()).build());
	}

	private static ItemStack createDisplayStack(RegistryEntry<Item> item, int count, ComponentMapPredicate components) {
		return new ItemStack(item, count, components.toChanges());
	}

	public boolean matches(ItemStack stack) {
		return stack.itemMatches(this.item) && this.components.test((ComponentsAccess) stack);
	}
}
