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
 * Описывает предмет, участвующий в торговой сделке, с опциональным предикатом компонентов.
 * <p>
 * Хранит кешированный {@link ItemStack} для отображения в UI, чтобы не пересоздавать его
 * при каждом рендере. Предикат компонентов позволяет требовать конкретные NBT-данные
 * (например, зачарования или цвет кожаной брони).
 *
 * @param item       запись реестра предмета
 * @param count      количество предметов
 * @param components предикат компонентов, которым должен соответствовать предмет
 * @param itemStack  кешированный стек для отображения
 */
public record TradedItem(RegistryEntry<Item> item, int count, ComponentMapPredicate components, ItemStack itemStack) {

	public static final Codec<TradedItem> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Item.ENTRY_CODEC.fieldOf("id").forGetter(TradedItem::item),
					Codecs.POSITIVE_INT.fieldOf("count").orElse(1).forGetter(TradedItem::count),
					ComponentMapPredicate.CODEC
							.optionalFieldOf("components", ComponentMapPredicate.EMPTY)
							.forGetter(TradedItem::components)
			).apply(instance, TradedItem::new)
	);

	public static final PacketCodec<RegistryByteBuf, TradedItem> PACKET_CODEC = PacketCodec.tuple(
			Item.ENTRY_PACKET_CODEC, TradedItem::item,
			PacketCodecs.VAR_INT, TradedItem::count,
			ComponentMapPredicate.PACKET_CODEC, TradedItem::components,
			TradedItem::new
	);

	public static final PacketCodec<RegistryByteBuf, Optional<TradedItem>> OPTIONAL_PACKET_CODEC =
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
		return new TradedItem(item, count, builderCallback.apply(ComponentMapPredicate.builder()).build());
	}

	/**
	 * Проверяет, соответствует ли переданный стек этому торговому предмету
	 * по типу и предикату компонентов.
	 *
	 * @param stack стек для проверки
	 * @return {@code true}, если стек подходит для данной сделки
	 */
	public boolean matches(ItemStack stack) {
		return stack.itemMatches(item) && components.test((ComponentsAccess) stack);
	}

	private static ItemStack createDisplayStack(RegistryEntry<Item> item, int count, ComponentMapPredicate components) {
		return new ItemStack(item, count, components.toChanges());
	}
}
