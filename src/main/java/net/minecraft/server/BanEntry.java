package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * {@code BanEntry}.
 */
public abstract class BanEntry<T> extends ServerConfigEntry<T> {

	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
	public static final String FOREVER = "forever";
	protected final Date creationDate;
	protected final String source;
	protected final @Nullable Date expiryDate;
	protected final @Nullable String reason;

	public BanEntry(
			@Nullable T key,
			@Nullable Date creationDate,
			@Nullable String source,
			@Nullable Date expiryDate,
			@Nullable String reason
	) {
		super(key);
		this.creationDate = creationDate == null ? new Date() : creationDate;
		this.source = source == null ? "(Unknown)" : source;
		this.expiryDate = expiryDate;
		this.reason = reason;
	}

	protected BanEntry(@Nullable T key, JsonObject json) {
		super(key);

		Date date;
		try {
			date = json.has("created") ? DATE_FORMAT.parse(json.get("created").getAsString()) : new Date();
		}
		catch (ParseException var7) {
			date = new Date();
		}

		this.creationDate = date;
		this.source = json.has("source") ? json.get("source").getAsString() : "(Unknown)";

		Date date2;
		try {
			date2 = json.has("expires") ? DATE_FORMAT.parse(json.get("expires").getAsString()) : null;
		}
		catch (ParseException var6) {
			date2 = null;
		}

		this.expiryDate = date2;
		this.reason = json.has("reason") ? json.get("reason").getAsString() : null;
	}

	public Date getCreationDate() {
		return this.creationDate;
	}

	public String getSource() {
		return this.source;
	}

	public @Nullable Date getExpiryDate() {
		return this.expiryDate;
	}

	public @Nullable String getReason() {
		return this.reason;
	}

	public Text getReasonText() {
		String string = this.getReason();
		return string == null ? Text.translatable("multiplayer.disconnect.banned.reason.default")
		                      : Text.literal(string);
	}

	/**
	 * To text.
	 *
	 * @return Text — результат операции
	 */
	public abstract Text toText();

	@Override
	boolean isInvalid() {
		return this.expiryDate == null ? false : this.expiryDate.before(new Date());
	}

	@Override
	protected void write(JsonObject json) {
		json.addProperty("created", DATE_FORMAT.format(this.creationDate));
		json.addProperty("source", this.source);
		json.addProperty("expires", this.expiryDate == null ? "forever" : DATE_FORMAT.format(this.expiryDate));
		json.addProperty("reason", this.reason);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else if (o != null && this.getClass() == o.getClass()) {
			BanEntry<?> banEntry = (BanEntry<?>) o;
			return Objects.equals(this.source, banEntry.source)
					&& Objects.equals(this.expiryDate, banEntry.expiryDate)
					&& Objects.equals(this.reason, banEntry.reason)
					&& Objects.equals(this.getKey(), banEntry.getKey());
		}
		else {
			return false;
		}
	}
}
