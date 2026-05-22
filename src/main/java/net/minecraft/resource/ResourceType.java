package net.minecraft.resource;

/**
 * Тип ресурс-пака: определяет директорию, в которой хранятся ресурсы данного типа.
 */
public enum ResourceType {
	/** Клиентские ресурсы (текстуры, звуки, модели и т.д.), хранятся в директории {@code assets}. */
	CLIENT_RESOURCES("assets"),
	/** Серверные данные (рецепты, теги, структуры и т.д.), хранятся в директории {@code data}. */
	SERVER_DATA("data");

	private final String directory;

	ResourceType(String directory) {
		this.directory = directory;
	}

	public String getDirectory() {
		return directory;
	}
}
