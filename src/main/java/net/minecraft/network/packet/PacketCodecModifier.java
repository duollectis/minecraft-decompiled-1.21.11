package net.minecraft.network.packet;

import net.minecraft.network.codec.PacketCodec;

/**
 * Функциональный интерфейс для модификации {@link PacketCodec} в зависимости
 * от контекста {@code C}. Позволяет динамически оборачивать или подменять кодек
 * при регистрации пакетов в конкретном состоянии сети.
 */
@FunctionalInterface
public interface PacketCodecModifier<B, V, C> {

	PacketCodec<? super B, V> apply(PacketCodec<? super B, V> packetCodec, C context);
}
