package net.minecraft.registry;

import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Идентификатор ресурсного пака с версией — используется для отслеживания
 * «известных паков» (known packs) при синхронизации реестров между клиентом
 * и сервером. Позволяет клиенту пропустить передачу данных, если у него
 * уже есть актуальная версия пака.
 *
 * @param namespace пространство имён пака (например, {@code "minecraft"})
 * @param id        идентификатор пака внутри пространства имён
 * @param version   версия пака (обычно версия игры для ванильных паков)
 */
public record VersionedIdentifier(String namespace, String id, String version) {

	public static final PacketCodec<ByteBuf, VersionedIdentifier> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, VersionedIdentifier::namespace,
			PacketCodecs.STRING, VersionedIdentifier::id,
			PacketCodecs.STRING, VersionedIdentifier::version,
			VersionedIdentifier::new
	);
	public static final String DEFAULT_NAMESPACE = "minecraft";

	/**
	 * Создаёт {@link VersionedIdentifier} для ванильного пака с текущей версией игры.
	 *
	 * @param path идентификатор пака
	 * @return версионированный идентификатор ванильного пака
	 */
	public static VersionedIdentifier createVanilla(String path) {
		return new VersionedIdentifier(DEFAULT_NAMESPACE, path, SharedConstants.getGameVersion().id());
	}

	public boolean isVanilla() {
		return namespace.equals(DEFAULT_NAMESPACE);
	}

	@Override
	public String toString() {
		return namespace + ":" + id + ":" + version;
	}
}
