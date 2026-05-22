package net.minecraft.resource;

import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * Фабрика ресурсов: позволяет получить {@link Resource} по {@link Identifier}.
 * Является функциональным интерфейсом — может быть реализована лямбдой.
 */
@FunctionalInterface
public interface ResourceFactory {

	/** Фабрика, всегда возвращающая пустой результат. */
	ResourceFactory MISSING = id -> Optional.empty();

	/**
	 * Возвращает ресурс по идентификатору, если он существует.
	 *
	 * @param id идентификатор ресурса
	 * @return {@link Optional} с ресурсом или пустой
	 */
	Optional<Resource> getResource(Identifier id);

	/**
	 * Возвращает ресурс по идентификатору или бросает исключение, если он не найден.
	 *
	 * @param id идентификатор ресурса
	 * @return найденный ресурс
	 * @throws FileNotFoundException если ресурс не найден
	 */
	default Resource getResourceOrThrow(Identifier id) throws FileNotFoundException {
		return getResource(id).orElseThrow(() -> new FileNotFoundException(id.toString()));
	}

	default InputStream open(Identifier id) throws IOException {
		return getResourceOrThrow(id).getInputStream();
	}

	default BufferedReader openAsReader(Identifier id) throws IOException {
		return getResourceOrThrow(id).getReader();
	}

	/**
	 * Создаёт фабрику из готовой карты ресурсов.
	 *
	 * @param map карта идентификаторов к ресурсам
	 * @return фабрика, делегирующая поиск в карту
	 */
	static ResourceFactory fromMap(Map<Identifier, Resource> map) {
		return id -> Optional.ofNullable(map.get(id));
	}
}
