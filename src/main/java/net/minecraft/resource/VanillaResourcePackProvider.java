package net.minecraft.resource;

import com.mojang.logging.LogUtils;
import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.path.SymlinkFinder;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Базовый провайдер ванильных ресурс-паков.
 * Регистрирует встроенный пак по умолчанию и сканирует дополнительные паки
 * из директории, соответствующей {@link #id} в classpath.
 */
public abstract class VanillaResourcePackProvider implements ResourcePackProvider {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final String VANILLA_KEY = "vanilla";
	public static final String TESTS_KEY = "tests";
	public static final VersionedIdentifier VANILLA_ID = VersionedIdentifier.createVanilla("core");

	private final ResourceType type;
	private final DefaultResourcePack resourcePack;
	private final Identifier id;
	private final SymlinkFinder symlinkFinder;

	public VanillaResourcePackProvider(
		ResourceType type,
		DefaultResourcePack resourcePack,
		Identifier id,
		SymlinkFinder symlinkFinder
	) {
		this.type = type;
		this.resourcePack = resourcePack;
		this.id = id;
		this.symlinkFinder = symlinkFinder;
	}

	@Override
	public void register(Consumer<ResourcePackProfile> profileAdder) {
		ResourcePackProfile defaultProfile = createDefault(resourcePack);
		if (defaultProfile != null) {
			profileAdder.accept(defaultProfile);
		}

		forEachProfile(profileAdder);
	}

	protected abstract @Nullable ResourcePackProfile createDefault(ResourcePack pack);

	protected abstract Text getDisplayName(String id);

	public DefaultResourcePack getResourcePack() {
		return resourcePack;
	}

	private void forEachProfile(Consumer<ResourcePackProfile> consumer) {
		Map<String, Function<String, ResourcePackProfile>> profileFactories = new HashMap<>();
		forEachProfile(profileFactories::put);
		profileFactories.forEach((packId, factory) -> {
			ResourcePackProfile profile = factory.apply(packId);
			if (profile != null) {
				consumer.accept(profile);
			}
		});
	}

	protected void forEachProfile(BiConsumer<String, Function<String, ResourcePackProfile>> consumer) {
		resourcePack.forEachNamespacedPath(
			type,
			id,
			namespacedPath -> forEachProfile(namespacedPath, consumer)
		);
	}

	protected void forEachProfile(
		@Nullable Path namespacedPath,
		BiConsumer<String, Function<String, @Nullable ResourcePackProfile>> consumer
	) {
		if (namespacedPath == null || !Files.isDirectory(namespacedPath)) {
			return;
		}

		try {
			FileResourcePackProvider.forEachProfile(
				namespacedPath,
				symlinkFinder,
				(profilePath, factory) -> consumer.accept(
					getFileName(profilePath),
					packId -> create(packId, factory, getDisplayName(packId))
				)
			);
		} catch (IOException exception) {
			LOGGER.warn("Failed to discover packs in {}", namespacedPath, exception);
		}
	}

	private static String getFileName(Path path) {
		return StringUtils.removeEnd(path.getFileName().toString(), ".zip");
	}

	protected abstract @Nullable ResourcePackProfile create(
		String fileName,
		ResourcePackProfile.PackFactory packFactory,
		Text displayName
	);

	/**
	 * Создаёт фабрику пака, всегда возвращающую один и тот же экземпляр {@code pack}.
	 * Используется для встроенных паков, которые не нужно пересоздавать.
	 *
	 * @param pack экземпляр пака
	 * @return фабрика, делегирующая к переданному паку
	 */
	protected static ResourcePackProfile.PackFactory createPackFactory(ResourcePack pack) {
		return new ResourcePackProfile.PackFactory() {
			@Override
			public ResourcePack open(ResourcePackInfo info) {
				return pack;
			}

			@Override
			public ResourcePack openWithOverlays(ResourcePackInfo info, ResourcePackProfile.Metadata metadata) {
				return pack;
			}
		};
	}
}
