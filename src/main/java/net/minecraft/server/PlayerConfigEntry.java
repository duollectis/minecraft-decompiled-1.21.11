package net.minecraft.server;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Uuids;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Минимальная идентификационная запись игрока: UUID и имя.
 * Используется в списках банов, операторов и белого списка.
 */
public record PlayerConfigEntry(UUID id, String name) {

	public static final Codec<PlayerConfigEntry> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Uuids.STRING_CODEC.fieldOf("id").forGetter(PlayerConfigEntry::id),
			Codec.STRING.fieldOf("name").forGetter(PlayerConfigEntry::name)
		).apply(instance, PlayerConfigEntry::new)
	);

	public PlayerConfigEntry(GameProfile profile) {
		this(profile.id(), profile.name());
	}

	public PlayerConfigEntry(NameAndId nameAndId) {
		this(nameAndId.id(), nameAndId.name());
	}

	/**
	 * Десериализует запись из JSON-объекта с полями {@code uuid} и {@code name}.
	 *
	 * @return запись или {@code null}, если поля отсутствуют или UUID некорректен
	 */
	public static @Nullable PlayerConfigEntry read(JsonObject object) {
		if (!object.has("uuid") || !object.has("name")) {
			return null;
		}

		String uuidString = object.get("uuid").getAsString();

		UUID uuid;
		try {
			uuid = UUID.fromString(uuidString);
		} catch (Throwable e) {
			return null;
		}

		return new PlayerConfigEntry(uuid, object.get("name").getAsString());
	}

	/** Сериализует запись в JSON-объект с полями {@code uuid} и {@code name}. */
	public void write(JsonObject object) {
		object.addProperty("uuid", id().toString());
		object.addProperty("name", name());
	}

	/** Создаёт запись для офлайн-игрока по нику, генерируя детерминированный UUID. */
	public static PlayerConfigEntry fromNickname(String nickname) {
		UUID uuid = Uuids.getOfflinePlayerUuid(nickname);
		return new PlayerConfigEntry(uuid, nickname);
	}
}
