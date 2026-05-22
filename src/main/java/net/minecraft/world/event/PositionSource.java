package net.minecraft.world.event;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Источник позиции для игровых событий и вибраций.
 * Абстрагирует конкретное местоположение: блок или сущность.
 * Возвращает {@link Optional#empty()}, если источник больше не существует в мире
 * (например, сущность была удалена).
 */
public interface PositionSource {

	Codec<PositionSource> CODEC =
		Registries.POSITION_SOURCE_TYPE.getCodec().dispatch(PositionSource::getType, PositionSourceType::getCodec);

	PacketCodec<RegistryByteBuf, PositionSource> PACKET_CODEC =
		PacketCodecs.registryValue(RegistryKeys.POSITION_SOURCE_TYPE)
			.dispatch(PositionSource::getType, PositionSourceType::getPacketCodec);

	/**
	 * Возвращает текущую позицию источника в мире.
	 * Может вернуть {@link Optional#empty()}, если источник недоступен.
	 */
	Optional<Vec3d> getPos(World world);

	PositionSourceType<? extends PositionSource> getType();
}
