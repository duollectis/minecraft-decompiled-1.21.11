package net.minecraft.util;

import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс для объектов, имеющих имя — стандартное и опционально пользовательское.
 * Реализуется блок-сущностями, контейнерами и другими именуемыми объектами мира.
 */
public interface Nameable {

	Text getName();

	default String getStringifiedName() {
		return getName().getString();
	}

	default boolean hasCustomName() {
		return getCustomName() != null;
	}

	default Text getDisplayName() {
		return getName();
	}

	default @Nullable Text getCustomName() {
		return null;
	}
}
