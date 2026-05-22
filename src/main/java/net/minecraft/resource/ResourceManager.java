package net.minecraft.resource;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Менеджер ресурсов: предоставляет доступ к ресурсам из всех активных паков.
 * Расширяет {@link ResourceFactory} для получения одиночных ресурсов.
 */
public interface ResourceManager extends ResourceFactory {

	/**
	 * Возвращает все зарегистрированные пространства имён.
	 *
	 * @return множество пространств имён
	 */
	Set<String> getAllNamespaces();

	/**
	 * Возвращает все ресурсы с данным идентификатором из всех паков
	 * (от низшего приоритета к высшему).
	 *
	 * @param id идентификатор ресурса
	 * @return список ресурсов
	 */
	List<Resource> getAllResources(Identifier id);

	/**
	 * Находит все ресурсы, путь которых начинается с {@code startingPath}
	 * и удовлетворяет предикату. Возвращает ресурс с наивысшим приоритетом для каждого ID.
	 *
	 * @param startingPath         начало пути для поиска
	 * @param allowedPathPredicate предикат фильтрации по идентификатору
	 * @return карта идентификаторов к ресурсам
	 */
	Map<Identifier, Resource> findResources(String startingPath, Predicate<Identifier> allowedPathPredicate);

	/**
	 * Находит все ресурсы из всех паков, путь которых начинается с {@code startingPath}
	 * и удовлетворяет предикату.
	 *
	 * @param startingPath         начало пути для поиска
	 * @param allowedPathPredicate предикат фильтрации по идентификатору
	 * @return карта идентификаторов к спискам ресурсов из разных паков
	 */
	Map<Identifier, List<Resource>> findAllResources(String startingPath, Predicate<Identifier> allowedPathPredicate);

	/**
	 * Возвращает поток всех активных ресурс-паков.
	 *
	 * @return поток паков
	 */
	Stream<ResourcePack> streamResourcePacks();

	/**
	 * Пустая реализация менеджера ресурсов, не содержащая ни одного ресурса.
	 */
	enum Empty implements ResourceManager {
		INSTANCE;

		@Override
		public Set<String> getAllNamespaces() {
			return Set.of();
		}

		@Override
		public Optional<Resource> getResource(Identifier identifier) {
			return Optional.empty();
		}

		@Override
		public List<Resource> getAllResources(Identifier id) {
			return List.of();
		}

		@Override
		public Map<Identifier, Resource> findResources(
			String startingPath,
			Predicate<Identifier> allowedPathPredicate
		) {
			return Map.of();
		}

		@Override
		public Map<Identifier, List<Resource>> findAllResources(
			String startingPath,
			Predicate<Identifier> allowedPathPredicate
		) {
			return Map.of();
		}

		@Override
		public Stream<ResourcePack> streamResourcePacks() {
			return Stream.of();
		}
	}
}
