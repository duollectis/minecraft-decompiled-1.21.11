package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Date;

/**
 * {@code BannedPlayerEntry}.
 */
public class BannedPlayerEntry extends BanEntry<PlayerConfigEntry> {

	private static final Text UNKNOWN_PLAYER_TEXT = Text.translatable("commands.banlist.entry.unknown");

	public BannedPlayerEntry(@Nullable PlayerConfigEntry playerConfigEntry) {
		this(playerConfigEntry, null, null, null, null);
	}

	public BannedPlayerEntry(
			@Nullable PlayerConfigEntry playerConfigEntry,
			@Nullable Date created,
			@Nullable String source,
			@Nullable Date expiry,
			@Nullable String reason
	) {
		super(playerConfigEntry, created, source, expiry, reason);
	}

	public BannedPlayerEntry(JsonObject json) {
		super(PlayerConfigEntry.read(json), json);
	}

	@Override
	protected void write(JsonObject json) {
		if (this.getKey() != null) {
			this.getKey().write(json);
			super.write(json);
		}
	}

	@Override
	public Text toText() {
		PlayerConfigEntry playerConfigEntry = this.getKey();
		return (Text) (playerConfigEntry != null ? Text.literal(playerConfigEntry.name()) : UNKNOWN_PLAYER_TEXT);
	}
}
