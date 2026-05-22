package net.minecraft.resource;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Range;

/**
 * Совместимость ресурс-пака с текущей версией игры.
 * Определяется путём сравнения диапазона поддерживаемых версий пака с версией игры.
 */
public enum ResourcePackCompatibility {
	TOO_OLD("old"),
	TOO_NEW("new"),
	UNKNOWN("unknown"),
	COMPATIBLE("compatible");

	/** Версия, при которой пак считается совместимым с любой версией игры. */
	public static final int ALWAYS_COMPATIBLE_VERSION = Integer.MAX_VALUE;

	private final Text notification;
	private final Text confirmMessage;

	ResourcePackCompatibility(final String translationSuffix) {
		notification = Text.translatable("pack.incompatible." + translationSuffix).formatted(Formatting.GRAY);
		confirmMessage = Text.translatable("pack.incompatible.confirm." + translationSuffix);
	}

	public boolean isCompatible() {
		return this == COMPATIBLE;
	}

	/**
	 * Определяет совместимость пака по диапазону поддерживаемых версий и текущей версии игры.
	 *
	 * @param range       диапазон версий, поддерживаемых паком
	 * @param packVersion текущая версия пака игры
	 * @return статус совместимости
	 */
	public static ResourcePackCompatibility from(Range<PackVersion> range, PackVersion packVersion) {
		if (range.minInclusive().major() == Integer.MAX_VALUE) {
			return UNKNOWN;
		}

		if (range.maxInclusive().compareTo(packVersion) < 0) {
			return TOO_OLD;
		}

		return packVersion.compareTo(range.minInclusive()) < 0 ? TOO_NEW : COMPATIBLE;
	}

	public Text getNotification() {
		return notification;
	}

	public Text getConfirmMessage() {
		return confirmMessage;
	}
}
