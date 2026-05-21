package net.minecraft.item.equipment.trim;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.Map;

/**
 * {@code ArmorTrimAssets}.
 */
public record ArmorTrimAssets(
		ArmorTrimAssets.AssetId base,
		Map<RegistryKey<EquipmentAsset>, ArmorTrimAssets.AssetId> overrides
) {

	public static final String MATERIAL_SEPARATOR = "_";
	public static final MapCodec<ArmorTrimAssets> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    ArmorTrimAssets.AssetId.CODEC.fieldOf("asset_name").forGetter(ArmorTrimAssets::base),
					                    Codec
							                    .unboundedMap(
									                    RegistryKey.createCodec(EquipmentAssetKeys.REGISTRY_KEY),
									                    ArmorTrimAssets.AssetId.CODEC
							                    )
							                    .optionalFieldOf("override_armor_assets", Map.of())
							                    .forGetter(ArmorTrimAssets::overrides)
			                    )
			                    .apply(instance, ArmorTrimAssets::new)
	);
	public static final PacketCodec<ByteBuf, ArmorTrimAssets> PACKET_CODEC = PacketCodec.tuple(
			ArmorTrimAssets.AssetId.PACKET_CODEC,
			ArmorTrimAssets::base,
			PacketCodecs.map(
					Object2ObjectOpenHashMap::new,
					RegistryKey.createPacketCodec(EquipmentAssetKeys.REGISTRY_KEY),
					ArmorTrimAssets.AssetId.PACKET_CODEC
			),
			ArmorTrimAssets::overrides,
			ArmorTrimAssets::new
	);
	public static final ArmorTrimAssets QUARTZ = of("quartz");
	public static final ArmorTrimAssets IRON = of("iron", Map.of(EquipmentAssetKeys.IRON, "iron_darker"));
	public static final ArmorTrimAssets
			NETHERITE =
			of("netherite", Map.of(EquipmentAssetKeys.NETHERITE, "netherite_darker"));
	public static final ArmorTrimAssets REDSTONE = of("redstone");
	public static final ArmorTrimAssets COPPER = of("copper", Map.of(EquipmentAssetKeys.COPPER, "copper_darker"));
	public static final ArmorTrimAssets GOLD = of("gold", Map.of(EquipmentAssetKeys.GOLD, "gold_darker"));
	public static final ArmorTrimAssets EMERALD = of("emerald");
	public static final ArmorTrimAssets DIAMOND = of("diamond", Map.of(EquipmentAssetKeys.DIAMOND, "diamond_darker"));
	public static final ArmorTrimAssets LAPIS = of("lapis");
	public static final ArmorTrimAssets AMETHYST = of("amethyst");
	public static final ArmorTrimAssets RESIN = of("resin");

	public static ArmorTrimAssets of(String suffix) {
		return new ArmorTrimAssets(new ArmorTrimAssets.AssetId(suffix), Map.of());
	}

	public static ArmorTrimAssets of(String suffix, Map<RegistryKey<EquipmentAsset>, String> overrides) {
		return new ArmorTrimAssets(
				new ArmorTrimAssets.AssetId(suffix),
				Map.copyOf(Maps.transformValues(overrides, ArmorTrimAssets.AssetId::new))
		);
	}

	public ArmorTrimAssets.AssetId getAssetId(RegistryKey<EquipmentAsset> equipmentAsset) {
		return this.overrides.getOrDefault(equipmentAsset, this.base);
	}

	/**
	 * {@code AssetId}.
	 */
	public record AssetId(String suffix) {

		public static final Codec<ArmorTrimAssets.AssetId>
				CODEC =
				Codecs.IDENTIFIER_PATH.xmap(ArmorTrimAssets.AssetId::new, ArmorTrimAssets.AssetId::suffix);
		public static final PacketCodec<ByteBuf, ArmorTrimAssets.AssetId> PACKET_CODEC = PacketCodecs.STRING
				.xmap(ArmorTrimAssets.AssetId::new, ArmorTrimAssets.AssetId::suffix);

		public AssetId(String suffix) {
			if (!Identifier.isPathValid(suffix)) {
				throw new IllegalArgumentException("Invalid string to use as a resource path element: " + suffix);
			}
			else {
				this.suffix = suffix;
			}
		}
	}
}
