package net.minecraft.scoreboard.number;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

/**
 * Формат числового значения, который всегда возвращает фиксированный текст
 * независимо от числового значения очка.
 * Используется для замены числа произвольным текстом в интерфейсе скорборда.
 */
public record FixedNumberFormat(Text text) implements NumberFormat {

	public static final NumberFormatType<FixedNumberFormat> TYPE = new NumberFormatType<>() {
		private static final MapCodec<FixedNumberFormat> CODEC =
				TextCodecs.CODEC.fieldOf("value").xmap(FixedNumberFormat::new, FixedNumberFormat::text);
		private static final PacketCodec<RegistryByteBuf, FixedNumberFormat> PACKET_CODEC = PacketCodec.tuple(
				TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC,
				FixedNumberFormat::text,
				FixedNumberFormat::new
		);

		@Override
		public MapCodec<FixedNumberFormat> getCodec() {
			return CODEC;
		}

		@Override
		public PacketCodec<RegistryByteBuf, FixedNumberFormat> getPacketCodec() {
			return PACKET_CODEC;
		}
	};

	@Override
	public MutableText format(int number) {
		return text.copy();
	}

	@Override
	public NumberFormatType<FixedNumberFormat> getType() {
		return TYPE;
	}
}
