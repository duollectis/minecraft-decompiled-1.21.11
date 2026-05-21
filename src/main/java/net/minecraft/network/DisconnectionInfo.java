package net.minecraft.network;

import net.minecraft.text.Text;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Информация об отключении игрока от сервера.
 *
 * @param reason        текстовое сообщение с причиной отключения
 * @param report        путь к файлу отчёта об ошибке (если есть)
 * @param bugReportLink ссылка на баг-трекер (если есть)
 */
public record DisconnectionInfo(Text reason, Optional<Path> report, Optional<URI> bugReportLink) {

	/**
	 * Создаёт информацию об отключении только с причиной, без отчёта и ссылки.
	 *
	 * @param reason текстовое сообщение с причиной отключения
	 */
	public DisconnectionInfo(Text reason) {
		this(reason, Optional.empty(), Optional.empty());
	}
}
