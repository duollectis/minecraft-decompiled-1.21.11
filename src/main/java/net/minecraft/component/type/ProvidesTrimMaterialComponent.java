package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.LazyRegistryEntryReference;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Optional;

/**
	 * Компонент материала отделки брони. Указывает, что предмет может использоваться
	 * как материал для нанесения отделки ({@link ArmorTrimMaterial}) на кузнечном столе.
	 */
public record ProvidesTrimMaterialComponent(LazyRegistryEntryReference<ArmorTrimMaterial> material) {

	public static final Codec<ProvidesTrimMaterialComponent> CODEC = LazyRegistryEntryReference
			.createCodec(RegistryKeys.TRIM_MATERIAL, ArmorTrimMaterial.ENTRY_CODEC)
			.xmap(ProvidesTrimMaterialComponent::new, ProvidesTrimMaterialComponent::material);
	public static final PacketCodec<RegistryByteBuf, ProvidesTrimMaterialComponent> PACKET_CODEC = LazyRegistryEntryReference
			.createPacketCodec(RegistryKeys.TRIM_MATERIAL, ArmorTrimMaterial.ENTRY_PACKET_CODEC)
			.xmap(ProvidesTrimMaterialComponent::new, ProvidesTrimMaterialComponent::material);

	public ProvidesTrimMaterialComponent(RegistryEntry<ArmorTrimMaterial> material) {
		this(new LazyRegistryEntryReference<>(material));
	}

	@Deprecated
	public ProvidesTrimMaterialComponent(RegistryKey<ArmorTrimMaterial> material) {
		this(new LazyRegistryEntryReference<>(material));
	}

	public Optional<RegistryEntry<ArmorTrimMaterial>> getMaterial(RegistryWrapper.WrapperLookup registries) {
		return material.resolveEntry(registries);
	}
}
