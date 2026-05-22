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
 * Базовая запись о бане игрока или IP-адреса.
 * Хранит дату создания, источник, дату истечения и причину бана.
 *
 * @param <T> тип ключа записи (IP-строка или {@link PlayerConfigEntry})
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

	/**
	 * Десериализует запись о бане из JSON-объекта.
	 * Если поля {@code created} или {@code expires} содержат некорректную дату — используются значения по умолчанию.
	 */
	protected BanEntry(@Nullable T key, JsonObject json) {
		super(key);

		Date parsedCreation;
		try {
			parsedCreation = json.has("created") ? DATE_FORMAT.parse(json.get("created").getAsString()) : new Date();
		} catch (ParseException e) {
			parsedCreation = new Date();
		}

		this.creationDate = parsedCreation;
		this.source = json.has("source") ? json.get("source").getAsString() : "(Unknown)";

		Date parsedExpiry;
		try {
			parsedExpiry = json.has("expires") ? DATE_FORMAT.parse(json.get("expires").getAsString()) : null;
		} catch (ParseException e) {
			parsedExpiry = null;
		}

		this.expiryDate = parsedExpiry;
		this.reason = json.has("reason") ? json.get("reason").getAsString() : null;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public String getSource() {
		return source;
	}

	public @Nullable Date getExpiryDate() {
		return expiryDate;
	}

	public @Nullable String getReason() {
		return reason;
	}

	public Text getReasonText() {
		return reason == null
			? Text.translatable("multiplayer.disconnect.banned.reason.default")
			: Text.literal(reason);
	}

	/** @return текстовое представление субъекта бана для отображения в списке */
	public abstract Text toText();

	@Override
	boolean isInvalid() {
		return expiryDate != null && expiryDate.before(new Date());
	}

	@Override
	protected void write(JsonObject json) {
		json.addProperty("created", DATE_FORMAT.format(creationDate));
		json.addProperty("source", source);
		json.addProperty("expires", expiryDate == null ? FOREVER : DATE_FORMAT.format(expiryDate));
		json.addProperty("reason", reason);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		BanEntry<?> other = (BanEntry<?>) o;
		return Objects.equals(source, other.source)
			&& Objects.equals(expiryDate, other.expiryDate)
			&& Objects.equals(reason, other.reason)
			&& Objects.equals(getKey(), other.getKey());
	}
}
