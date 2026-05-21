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
 * {@code PositionSource}.
 */
public interface PositionSource {

	Codec<PositionSource>
			CODEC =
			Registries.POSITION_SOURCE_TYPE.getCodec().dispatch(PositionSource::getType, PositionSourceType::getCodec);

	PacketCodec<RegistryByteBuf, PositionSource>
			PACKET_CODEC =
			PacketCodecs.registryValue(RegistryKeys.POSITION_SOURCE_TYPE)
			            .dispatch(PositionSource::getType, PositionSourceType::getPacketCodec);

	Optional<Vec3d> getPos(World world);

	PositionSourceType<? extends PositionSource> getType();
}
