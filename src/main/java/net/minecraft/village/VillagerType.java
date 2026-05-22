package net.minecraft.village;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.Map;

/**
 * Тип жителя деревни, определяющий его внешний вид в зависимости от биома.
 * <p>
 * Каждый биом отображается на конкретный тип через {@link #BIOME_TO_TYPE}.
 * Если биом не найден в карте, используется тип {@link #PLAINS} по умолчанию.
 */
public final class VillagerType {

	public static final RegistryKey<VillagerType> DESERT = of("desert");
	public static final RegistryKey<VillagerType> JUNGLE = of("jungle");
	public static final RegistryKey<VillagerType> PLAINS = of("plains");
	public static final RegistryKey<VillagerType> SAVANNA = of("savanna");
	public static final RegistryKey<VillagerType> SNOW = of("snow");
	public static final RegistryKey<VillagerType> SWAMP = of("swamp");
	public static final RegistryKey<VillagerType> TAIGA = of("taiga");

	public static final Codec<RegistryEntry<VillagerType>> CODEC = RegistryFixedCodec.of(RegistryKeys.VILLAGER_TYPE);
	public static final PacketCodec<RegistryByteBuf, RegistryEntry<VillagerType>> PACKET_CODEC =
			PacketCodecs.registryEntry(RegistryKeys.VILLAGER_TYPE);

	public static final Map<RegistryKey<Biome>, RegistryKey<VillagerType>> BIOME_TO_TYPE = Util.make(
			Maps.newHashMap(), map -> {
				map.put(BiomeKeys.BADLANDS, DESERT);
				map.put(BiomeKeys.DESERT, DESERT);
				map.put(BiomeKeys.ERODED_BADLANDS, DESERT);
				map.put(BiomeKeys.WOODED_BADLANDS, DESERT);
				map.put(BiomeKeys.BAMBOO_JUNGLE, JUNGLE);
				map.put(BiomeKeys.JUNGLE, JUNGLE);
				map.put(BiomeKeys.SPARSE_JUNGLE, JUNGLE);
				map.put(BiomeKeys.SAVANNA_PLATEAU, SAVANNA);
				map.put(BiomeKeys.SAVANNA, SAVANNA);
				map.put(BiomeKeys.WINDSWEPT_SAVANNA, SAVANNA);
				map.put(BiomeKeys.DEEP_FROZEN_OCEAN, SNOW);
				map.put(BiomeKeys.FROZEN_OCEAN, SNOW);
				map.put(BiomeKeys.FROZEN_RIVER, SNOW);
				map.put(BiomeKeys.ICE_SPIKES, SNOW);
				map.put(BiomeKeys.SNOWY_BEACH, SNOW);
				map.put(BiomeKeys.SNOWY_TAIGA, SNOW);
				map.put(BiomeKeys.SNOWY_PLAINS, SNOW);
				map.put(BiomeKeys.GROVE, SNOW);
				map.put(BiomeKeys.SNOWY_SLOPES, SNOW);
				map.put(BiomeKeys.FROZEN_PEAKS, SNOW);
				map.put(BiomeKeys.JAGGED_PEAKS, SNOW);
				map.put(BiomeKeys.SWAMP, SWAMP);
				map.put(BiomeKeys.MANGROVE_SWAMP, SWAMP);
				map.put(BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA, TAIGA);
				map.put(BiomeKeys.OLD_GROWTH_PINE_TAIGA, TAIGA);
				map.put(BiomeKeys.WINDSWEPT_GRAVELLY_HILLS, TAIGA);
				map.put(BiomeKeys.WINDSWEPT_HILLS, TAIGA);
				map.put(BiomeKeys.TAIGA, TAIGA);
				map.put(BiomeKeys.WINDSWEPT_FOREST, TAIGA);
			}
	);

	private static RegistryKey<VillagerType> of(String id) {
		return RegistryKey.of(RegistryKeys.VILLAGER_TYPE, Identifier.ofVanilla(id));
	}

	private static VillagerType create(Registry<VillagerType> registry, RegistryKey<VillagerType> key) {
		return Registry.register(registry, key, new VillagerType());
	}

	public static VillagerType registerAndGetDefault(Registry<VillagerType> registry) {
		create(registry, DESERT);
		create(registry, JUNGLE);
		create(registry, PLAINS);
		create(registry, SAVANNA);
		create(registry, SNOW);
		create(registry, SWAMP);
		return create(registry, TAIGA);
	}

	/**
	 * Определяет тип жителя по биому. Если биом не имеет специального типа — возвращает {@link #PLAINS}.
	 *
	 * @param biomeEntry запись реестра биома
	 * @return ключ типа жителя для данного биома
	 */
	public static RegistryKey<VillagerType> forBiome(RegistryEntry<Biome> biomeEntry) {
		return biomeEntry.getKey().map(BIOME_TO_TYPE::get).orElse(PLAINS);
	}
}
