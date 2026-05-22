package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
	 * Компонент цвета карты. Хранит RGB-цвет, отображаемый на иконке карты в инвентаре.
	 */
public record MapColorComponent(int rgb) {

	/** Стандартный серо-коричневый цвет карты по умолчанию (RGB #46402E). */
	private static final int DEFAULT_MAP_COLOR = 0x46402E;

	public static final Codec<MapColorComponent> CODEC = Codec.INT.xmap(MapColorComponent::new, MapColorComponent::rgb);
	public static final PacketCodec<ByteBuf, MapColorComponent>
			PACKET_CODEC =
			PacketCodecs.INTEGER.xmap(MapColorComponent::new, MapColorComponent::rgb);
	public static final MapColorComponent DEFAULT = new MapColorComponent(DEFAULT_MAP_COLOR);
}
