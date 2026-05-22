package net.minecraft.server.dedicated;

import java.nio.file.Path;
import java.util.function.UnaryOperator;

/**
 * Загрузчик server.properties: чтение файла свойств с диска и создание обработчика.
 */
public class ServerPropertiesLoader {

	private final Path path;
	private ServerPropertiesHandler propertiesHandler;

	public ServerPropertiesLoader(Path path) {
		this.path = path;
		this.propertiesHandler = ServerPropertiesHandler.load(path);
	}

	public ServerPropertiesHandler getPropertiesHandler() {
		return this.propertiesHandler;
	}

	/**
	 * Store.
	 */
	public void store() {
		this.propertiesHandler.saveProperties(this.path);
	}

	/**
	 * Apply.
	 *
	 * @param applier applier
	 *
	 * @return ServerPropertiesLoader — результат операции
	 */
	public ServerPropertiesLoader apply(UnaryOperator<ServerPropertiesHandler> applier) {
		(this.propertiesHandler = applier.apply(this.propertiesHandler)).saveProperties(this.path);
		return this;
	}
}
