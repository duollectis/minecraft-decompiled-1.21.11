package net.minecraft.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Неизменяемый снимок данных жителя деревни: тип внешности, профессия и уровень.
 * <p>
 * Используется как value-object — все методы {@code with*} возвращают новый экземпляр.
 * Уровень автоматически зажимается в диапазоне [{@link #MIN_LEVEL}, {@link #MAX_LEVEL}]
 * при создании через конструктор.
 *
 * @param type       тип жителя (определяет внешность в зависимости от биома)
 * @param profession профессия жителя
 * @param level      уровень торговли (1–5)
 */
public record VillagerData(RegistryEntry<VillagerType> type, RegistryEntry<VillagerProfession> profession, int level) {

	public static final int MIN_LEVEL = 1;
	public static final int MAX_LEVEL = 5;

	private static final int[] LEVEL_BASE_EXPERIENCE = {0, 10, 70, 150, 250};

	public static final Codec<VillagerData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Registries.VILLAGER_TYPE
							.getEntryCodec()
							.fieldOf("type")
							.orElseGet(() -> Registries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS))
							.forGetter(VillagerData::type),
					Registries.VILLAGER_PROFESSION
							.getEntryCodec()
							.fieldOf("profession")
							.orElseGet(() -> Registries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE))
							.forGetter(VillagerData::profession),
					Codec.INT.fieldOf("level").orElse(MIN_LEVEL).forGetter(VillagerData::level)
			).apply(instance, VillagerData::new)
	);

	public static final PacketCodec<RegistryByteBuf, VillagerData> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.registryEntry(RegistryKeys.VILLAGER_TYPE), VillagerData::type,
			PacketCodecs.registryEntry(RegistryKeys.VILLAGER_PROFESSION), VillagerData::profession,
			PacketCodecs.VAR_INT, VillagerData::level,
			VillagerData::new
	);

	public VillagerData(RegistryEntry<VillagerType> type, RegistryEntry<VillagerProfession> profession, int level) {
		this.type = type;
		this.profession = profession;
		this.level = Math.max(MIN_LEVEL, level);
	}

	public VillagerData withType(RegistryEntry<VillagerType> type) {
		return new VillagerData(type, profession, level);
	}

	public VillagerData withType(RegistryEntryLookup.RegistryLookup registries, RegistryKey<VillagerType> typeKey) {
		return withType(registries.getEntryOrThrow(typeKey));
	}

	public VillagerData withProfession(RegistryEntry<VillagerProfession> profession) {
		return new VillagerData(type, profession, level);
	}

	public VillagerData withProfession(
			RegistryEntryLookup.RegistryLookup registries,
			RegistryKey<VillagerProfession> professionKey
	) {
		return withProfession(registries.getEntryOrThrow(professionKey));
	}

	public VillagerData withLevel(int level) {
		return new VillagerData(type, profession, level);
	}

	public static int getLowerLevelExperience(int level) {
		return canLevelUp(level) ? LEVEL_BASE_EXPERIENCE[level - 1] : 0;
	}

	public static int getUpperLevelExperience(int level) {
		return canLevelUp(level) ? LEVEL_BASE_EXPERIENCE[level] : 0;
	}

	public static boolean canLevelUp(int level) {
		return level >= MIN_LEVEL && level < MAX_LEVEL;
	}
}
