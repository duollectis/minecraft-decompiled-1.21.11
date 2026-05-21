package net.minecraft.resource;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Range;

/**
 * {@code ResourcePackCompatibility}.
 */
public enum ResourcePackCompatibility {
	TOO_OLD("old"),
	TOO_NEW("new"),
	UNKNOWN("unknown"),
	COMPATIBLE("compatible");

	public static final int ALWAYS_COMPATIBLE_VERSION = Integer.MAX_VALUE;
	private final Text notification;
	private final Text confirmMessage;

	private ResourcePackCompatibility(final String translationSuffix) {
		this.notification = Text.translatable("pack.incompatible." + translationSuffix).formatted(Formatting.GRAY);
		this.confirmMessage = Text.translatable("pack.incompatible.confirm." + translationSuffix);
	}

	public boolean isCompatible() {
		return this == COMPATIBLE;
	}

	public static ResourcePackCompatibility from(Range<PackVersion> range, PackVersion packVersion) {
		if (range.minInclusive().major() == Integer.MAX_VALUE) {
			return UNKNOWN;
		}
		else if (range.maxInclusive().compareTo(packVersion) < 0) {
			return TOO_OLD;
		}
		else {
			return packVersion.compareTo(range.minInclusive()) < 0 ? TOO_NEW : COMPATIBLE;
		}
	}

	public Text getNotification() {
		return this.notification;
	}

	public Text getConfirmMessage() {
		return this.confirmMessage;
	}
}
