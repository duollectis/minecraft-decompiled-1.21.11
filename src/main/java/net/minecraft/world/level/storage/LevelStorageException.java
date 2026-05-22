package net.minecraft.world.level.storage;

import net.minecraft.text.Text;

/**
 * Исключение, выбрасываемое при ошибках доступа к директории сохранений.
 * Хранит локализованный текст ошибки для отображения в интерфейсе.
 */
public class LevelStorageException extends RuntimeException {

	private final Text messageText;

	public LevelStorageException(Text messageText) {
		super(messageText.getString());
		this.messageText = messageText;
	}

	public Text getMessageText() {
		return messageText;
	}
}
