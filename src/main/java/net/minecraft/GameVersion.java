package net.minecraft;

import net.minecraft.resource.PackVersion;
import net.minecraft.resource.ResourceType;

import java.util.Date;

/**
 * Описывает версию игры Minecraft.
 * <p>
 * Содержит идентификатор, имя, версию данных мира, версию протокола,
 * версии пакетов ресурсов и данных, время сборки и признак стабильности.
 * Стандартная реализация — {@link Impl}.
 */
public interface GameVersion {

	SaveVersion dataVersion();

	String id();

	String name();

	int protocolVersion();

	/**
	 * Возвращает версию пакета для указанного типа ресурсов.
	 *
	 * @param type тип ресурсов (клиентские ресурсы или серверные данные)
	 * @return версия пакета для данного типа
	 */
	PackVersion packVersion(ResourceType type);

	Date buildTime();

	boolean stable();

	/**
	 * Стандартная иммутабельная реализация {@link GameVersion} на основе Java Record.
	 */
	record Impl(
		String id,
		String name,
		SaveVersion dataVersion,
		int protocolVersion,
		PackVersion resourcePackVersion,
		PackVersion datapackVersion,
		Date buildTime,
		boolean stable
	) implements GameVersion {

		@Override
		public PackVersion packVersion(ResourceType type) {
			return switch (type) {
				case CLIENT_RESOURCES -> resourcePackVersion;
				case SERVER_DATA -> datapackVersion;
			};
		}
	}
}
