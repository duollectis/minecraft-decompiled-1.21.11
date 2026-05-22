package net.minecraft.advancement;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Пара «идентификатор + данные достижения», используемая для передачи
 * достижений по сети и хранения в менеджере.
 * <p>
 * Равенство и хэш определяются исключительно по {@link #id}, чтобы
 * достижения корректно работали в {@link java.util.Set} и {@link java.util.Map}.
 */
public record AdvancementEntry(Identifier id, Advancement value) {

	public static final PacketCodec<RegistryByteBuf, AdvancementEntry> PACKET_CODEC = PacketCodec.tuple(
		Identifier.PACKET_CODEC,
		AdvancementEntry::id,
		Advancement.PACKET_CODEC,
		AdvancementEntry::value,
		AdvancementEntry::new
	);

	public static final PacketCodec<RegistryByteBuf, List<AdvancementEntry>> LIST_PACKET_CODEC =
		PACKET_CODEC.collect(PacketCodecs.toList());

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		return o instanceof AdvancementEntry other && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return id.toString();
	}
}
