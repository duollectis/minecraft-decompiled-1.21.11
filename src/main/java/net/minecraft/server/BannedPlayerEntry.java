package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Date;

/**
 * Запись о забаненном игроке, идентифицируемом по {@link PlayerConfigEntry}.
 */
public class BannedPlayerEntry extends BanEntry<PlayerConfigEntry> {

	private static final Text UNKNOWN_PLAYER_TEXT = Text.translatable("commands.banlist.entry.unknown");

	public BannedPlayerEntry(@Nullable PlayerConfigEntry player) {
		this(player, null, null, null, null);
	}

	public BannedPlayerEntry(
		@Nullable PlayerConfigEntry player,
		@Nullable Date created,
		@Nullable String source,
		@Nullable Date expiry,
		@Nullable String reason
	) {
		super(player, created, source, expiry, reason);
	}

	public BannedPlayerEntry(JsonObject json) {
		super(PlayerConfigEntry.read(json), json);
	}

	@Override
	protected void write(JsonObject json) {
		if (getKey() == null) {
			return;
		}

		getKey().write(json);
		super.write(json);
	}

	@Override
	public Text toText() {
		PlayerConfigEntry player = getKey();
		return player != null ? Text.literal(player.name()) : UNKNOWN_PLAYER_TEXT;
	}
}
