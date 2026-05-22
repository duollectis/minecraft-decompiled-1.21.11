package net.minecraft.resource;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Реализация {@link ResourceManager} с поддержкой перезагрузки ресурсов.
 * Хранит список зарегистрированных {@link ResourceReloader} и при вызове
 * {@link #reload} создаёт новый {@link LifecycledResourceManagerImpl} из переданных паков.
 */
public class ReloadableResourceManagerImpl implements ResourceManager, AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private LifecycledResourceManager activeManager;
	private final List<ResourceReloader> reloaders = Lists.newArrayList();
	private final ResourceType type;

	public ReloadableResourceManagerImpl(ResourceType type) {
		this.type = type;
		activeManager = new LifecycledResourceManagerImpl(type, List.of());
	}

	@Override
	public void close() {
		activeManager.close();
	}

	/**
	 * Регистрирует перезагрузчик, который будет вызываться при каждой перезагрузке ресурсов.
	 *
	 * @param reloader перезагрузчик для регистрации
	 */
	public void registerReloader(ResourceReloader reloader) {
		reloaders.add(reloader);
	}

	/**
	 * Запускает асинхронную перезагрузку ресурсов с новым набором паков.
	 * Закрывает предыдущий менеджер и создаёт новый из переданных паков.
	 *
	 * @param prepareExecutor исполнитель фазы подготовки
	 * @param applyExecutor   исполнитель фазы применения
	 * @param initialStage    начальная стадия (барьер синхронизации)
	 * @param packs           новый список паков
	 * @return объект отслеживания прогресса перезагрузки
	 */
	public ResourceReload reload(
		Executor prepareExecutor,
		Executor applyExecutor,
		CompletableFuture<Unit> initialStage,
		List<ResourcePack> packs
	) {
		LOGGER.info(
			"Reloading ResourceManager: {}",
			LogUtils.defer(() -> packs.stream().map(ResourcePack::getId).collect(Collectors.joining(", ")))
		);
		activeManager.close();
		activeManager = new LifecycledResourceManagerImpl(type, packs);
		return SimpleResourceReload.start(
			activeManager,
			reloaders,
			prepareExecutor,
			applyExecutor,
			initialStage,
			LOGGER.isDebugEnabled()
		);
	}

	@Override
	public Optional<Resource> getResource(Identifier identifier) {
		return activeManager.getResource(identifier);
	}

	@Override
	public Set<String> getAllNamespaces() {
		return activeManager.getAllNamespaces();
	}

	@Override
	public List<Resource> getAllResources(Identifier id) {
		return activeManager.getAllResources(id);
	}

	@Override
	public Map<Identifier, Resource> findResources(String startingPath, Predicate<Identifier> allowedPathPredicate) {
		return activeManager.findResources(startingPath, allowedPathPredicate);
	}

	@Override
	public Map<Identifier, List<Resource>> findAllResources(
		String startingPath,
		Predicate<Identifier> allowedPathPredicate
	) {
		return activeManager.findAllResources(startingPath, allowedPathPredicate);
	}

	@Override
	public Stream<ResourcePack> streamResourcePacks() {
		return activeManager.streamResourcePacks();
	}
}
