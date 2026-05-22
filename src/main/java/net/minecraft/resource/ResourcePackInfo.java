package net.minecraft.resource;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

import java.util.Optional;

/**
 * Метаданные ресурс-пака: идентификатор, отображаемое имя, источник и версионный идентификатор.
 */
public record ResourcePackInfo(
	String id,
	Text title,
	ResourcePackSource source,
	Optional<VersionedIdentifier> knownPackInfo
) {

	/**
	 * Формирует текст с информацией о паке для отображения в интерфейсе.
	 * Включает цветовое кодирование (зелёный — включён, красный — выключен),
	 * вставку идентификатора и всплывающую подсказку с названием и описанием.
	 *
	 * @param enabled     включён ли пак
	 * @param description описание пака
	 * @return форматированный текст
	 */
	public Text getInformationText(boolean enabled, Text description) {
		return Texts.bracketed(source.decorate(Text.literal(id)))
			.styled(style -> style
				.withColor(enabled ? Formatting.GREEN : Formatting.RED)
				.withInsertion(StringArgumentType.escapeIfRequired(id))
				.withHoverEvent(new HoverEvent.ShowText(
					Text.empty()
						.append(title)
						.append("\n")
						.append(description)
				))
			);
	}
}
