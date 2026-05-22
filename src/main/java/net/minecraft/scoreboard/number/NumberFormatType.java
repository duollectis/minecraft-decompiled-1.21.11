package net.minecraft.scoreboard.number;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * Тип формата числового значения скорборда.
 * Предоставляет кодеки для сериализации в NBT/JSON и сетевой передачи.
 *
 * @param <T> конкретный тип формата
 */
public interface NumberFormatType<T extends NumberFormat> {

	MapCodec<T> getCodec();

	PacketCodec<RegistryByteBuf, T> getPacketCodec();
}
