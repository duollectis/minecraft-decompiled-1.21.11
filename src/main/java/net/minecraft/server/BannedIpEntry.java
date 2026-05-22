package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Date;

/**
 * Запись о забаненном IP-адресе.
 */
public class BannedIpEntry extends BanEntry<String> {

	public BannedIpEntry(String ip) {
		this(ip, null, null, null, null);
	}

	public BannedIpEntry(
		String ip,
		@Nullable Date created,
		@Nullable String source,
		@Nullable Date expiry,
		@Nullable String reason
	) {
		super(ip, created, source, expiry, reason);
	}

	public BannedIpEntry(JsonObject json) {
		super(readIp(json), json);
	}

	private static String readIp(JsonObject json) {
		return json.has("ip") ? json.get("ip").getAsString() : null;
	}

	@Override
	public Text toText() {
		return Text.literal(String.valueOf(getKey()));
	}

	@Override
	protected void write(JsonObject json) {
		if (getKey() == null) {
			return;
		}

		json.addProperty("ip", getKey());
		super.write(json);
	}
}
