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
 * {@code PlayerConfigEntry}.
 */
public record PlayerConfigEntry(UUID id, String name) {

	public static final Codec<PlayerConfigEntry> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Uuids.STRING_CODEC.fieldOf("id").forGetter(PlayerConfigEntry::id),
					                    Codec.STRING.fieldOf("name").forGetter(PlayerConfigEntry::name)
			                    )
			                    .apply(instance, PlayerConfigEntry::new)
	);

	public PlayerConfigEntry(GameProfile profile) {
		this(profile.id(), profile.name());
	}

	public PlayerConfigEntry(NameAndId nameAndId) {
		this(nameAndId.id(), nameAndId.name());
	}

	public static @Nullable PlayerConfigEntry read(JsonObject object) {
		if (object.has("uuid") && object.has("name")) {
			String string = object.get("uuid").getAsString();

			UUID uUID;
			try {
				uUID = UUID.fromString(string);
			}
			catch (Throwable var4) {
				return null;
			}

			return new PlayerConfigEntry(uUID, object.get("name").getAsString());
		}
		else {
			return null;
		}
	}

	public void write(JsonObject object) {
		object.addProperty("uuid", this.id().toString());
		object.addProperty("name", this.name());
	}

	public static PlayerConfigEntry fromNickname(String nickname) {
		UUID uUID = Uuids.getOfflinePlayerUuid(nickname);
		return new PlayerConfigEntry(uUID, nickname);
	}
}
