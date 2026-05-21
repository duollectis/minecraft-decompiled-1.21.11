package net.minecraft.server.filter;

import net.minecraft.network.message.FilterMask;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * {@code FilteredMessage}.
 */
public record FilteredMessage(String raw, FilterMask mask) {

	public static final FilteredMessage EMPTY = permitted("");

	/**
	 * Permitted.
	 *
	 * @param raw raw
	 *
	 * @return FilteredMessage — результат операции
	 */
	public static FilteredMessage permitted(String raw) {
		return new FilteredMessage(raw, FilterMask.PASS_THROUGH);
	}

	/**
	 * Censored.
	 *
	 * @param raw raw
	 *
	 * @return FilteredMessage — результат операции
	 */
	public static FilteredMessage censored(String raw) {
		return new FilteredMessage(raw, FilterMask.FULLY_FILTERED);
	}

	/**
	 * Filter.
	 *
	 * @return @Nullable String — результат операции
	 */
	public @Nullable String filter() {
		return this.mask.filter(this.raw);
	}

	public String getString() {
		return Objects.requireNonNullElse(this.filter(), "");
	}

	public boolean isFiltered() {
		return !this.mask.isPassThrough();
	}
}
