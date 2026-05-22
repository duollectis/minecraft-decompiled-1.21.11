package net.minecraft.scoreboard.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;

import java.util.Optional;

/**
 * Реестр и кодеки для всех типов форматов числовых значений скорборда.
 * <p>
 * Регистрирует три встроенных типа: {@code blank}, {@code styled}, {@code fixed}.
 * Предоставляет кодеки для сериализации в NBT/JSON ({@link #CODEC})
 * и сетевой передачи ({@link #PACKET_CODEC}, {@link #OPTIONAL_PACKET_CODEC}).
 */
public class NumberFormatTypes {

	public static final MapCodec<NumberFormat> REGISTRY_CODEC = Registries.NUMBER_FORMAT_TYPE
			.getCodec()
			.dispatchMap(NumberFormat::getType, NumberFormatType::getCodec);

	public static final Codec<NumberFormat> CODEC = REGISTRY_CODEC.codec();

	public static final PacketCodec<RegistryByteBuf, NumberFormat> PACKET_CODEC =
			PacketCodecs.registryValue(RegistryKeys.NUMBER_FORMAT_TYPE)
					.dispatch(NumberFormat::getType, NumberFormatType::getPacketCodec);

	public static final PacketCodec<RegistryByteBuf, Optional<NumberFormat>> OPTIONAL_PACKET_CODEC =
			PACKET_CODEC.collect(PacketCodecs::optional);

	/**
	 * Регистрирует все встроенные типы форматов в реестре.
	 * Возвращает тип по умолчанию ({@code fixed}), используемый при инициализации реестра.
	 */
	public static NumberFormatType<?> registerAndGetDefault(Registry<NumberFormatType<?>> registry) {
		Registry.register(registry, "blank", BlankNumberFormat.TYPE);
		Registry.register(registry, "styled", StyledNumberFormat.TYPE);
		return Registry.register(registry, "fixed", FixedNumberFormat.TYPE);
	}
}
