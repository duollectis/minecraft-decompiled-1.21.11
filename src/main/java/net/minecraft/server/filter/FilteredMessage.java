package net.minecraft.server.filter;

import net.minecraft.network.message.FilterMask;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Результат фильтрации текстового сообщения: содержит исходный текст и маску отфильтрованных символов.
 */
public record FilteredMessage(String raw, FilterMask mask) {

	public static final FilteredMessage EMPTY = permitted("");

	public static FilteredMessage permitted(String raw) {
		return new FilteredMessage(raw, FilterMask.PASS_THROUGH);
	}

	public static FilteredMessage censored(String raw) {
		return new FilteredMessage(raw, FilterMask.FULLY_FILTERED);
	}

	public @Nullable String filter() {
		return mask.filter(raw);
	}

	public String getString() {
		return Objects.requireNonNullElse(filter(), "");
	}

	public boolean isFiltered() {
		return !mask.isPassThrough();
	}
}
