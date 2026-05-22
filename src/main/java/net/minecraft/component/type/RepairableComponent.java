package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;

/**
	 * Компонент ремонтируемости предмета. Определяет список предметов,
	 * которыми можно починить данный предмет на наковальне.
	 */
public record RepairableComponent(RegistryEntryList<Item> items) {

	public static final Codec<RepairableComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(RegistryCodecs
							.entryList(RegistryKeys.ITEM)
							.fieldOf("items")
							.forGetter(RepairableComponent::items))
					.apply(instance, RepairableComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, RepairableComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.registryEntryList(RegistryKeys.ITEM), RepairableComponent::items, RepairableComponent::new
	);

	/**
		 * Проверяет, может ли данный стек предметов использоваться для ремонта.
		 *
		 * @param stack стек предмета-материала для ремонта
		 * @return {@code true} если предмет входит в список допустимых материалов
		 */
	public boolean matches(ItemStack stack) {
		return stack.isIn(items);
	}
}
