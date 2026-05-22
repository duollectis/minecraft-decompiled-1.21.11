package net.minecraft.world.debug.data;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;

/**
 * Отладочные данные улья: тип блока, количество жильцов, уровень мёда и признак задымления.
 */
public record BeeHiveDebugData(Block type, int occupantCount, int honeyLevel, boolean sedated) {

	public static final PacketCodec<RegistryByteBuf, BeeHiveDebugData> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.registryValue(RegistryKeys.BLOCK),
			BeeHiveDebugData::type,
			PacketCodecs.VAR_INT,
			BeeHiveDebugData::occupantCount,
			PacketCodecs.VAR_INT,
			BeeHiveDebugData::honeyLevel,
			PacketCodecs.BOOLEAN,
			BeeHiveDebugData::sedated,
			BeeHiveDebugData::new
	);

	/**
	 * Создаёт снимок отладочных данных из блок-сущности улья.
	 *
	 * @param beehive блок-сущность улья
	 * @return снимок текущего состояния улья
	 */
	public static BeeHiveDebugData fromBeehive(BeehiveBlockEntity beehive) {
		return new BeeHiveDebugData(
				beehive.getCachedState().getBlock(),
				beehive.getBeeCount(),
				BeehiveBlockEntity.getHoneyLevel(beehive.getCachedState()),
				beehive.isSmoked()
		);
	}
}
