package net.minecraft.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.data.DataWriter;
import net.minecraft.data.dev.NbtProvider;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.test.TestInstanceUtil;
import net.minecraft.util.FixedBufferInputStream;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.path.PathUtil;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Управляет загрузкой, кэшированием и сохранением {@link StructureTemplate} (файлов .nbt/.snbt).
 * Поддерживает несколько источников шаблонов: файловая система (generated/), ресурсы датапака
 * и директория игровых тестов (только в режиме разработки).
 */
public class StructureTemplateManager {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final String STRUCTURE_DIRECTORY = "structure";
	private static final String STRUCTURES_DIRECTORY = "structures";
	private static final String NBT_EXTENSION = ".nbt";
	private static final String SNBT_EXTENSION = ".snbt";
	private static final ResourceFinder STRUCTURE_NBT_FINDER = new ResourceFinder("structure", ".nbt");

	private final Map<Identifier, Optional<StructureTemplate>> templates = Maps.newConcurrentMap();
	private final DataFixer dataFixer;
	private ResourceManager resourceManager;
	private final Path generatedPath;
	private final List<Provider> providers;
	private final RegistryEntryLookup<Block> blockLookup;

	public StructureTemplateManager(
		ResourceManager resourceManager,
		LevelStorage.Session session,
		DataFixer dataFixer,
		RegistryEntryLookup<Block> blockLookup
	) {
		this.resourceManager = resourceManager;
		this.dataFixer = dataFixer;
		generatedPath = session.getDirectory(WorldSavePath.GENERATED).normalize();
		this.blockLookup = blockLookup;

		ImmutableList.Builder<Provider> builder = ImmutableList.builder();
		builder.add(new Provider(this::loadTemplateFromFile, this::streamTemplatesFromFile));

		if (SharedConstants.isDevelopment) {
			builder.add(new Provider(this::loadTemplateFromGameTestFile, this::streamTemplatesFromGameTestFile));
		}

		builder.add(new Provider(this::loadTemplateFromResource, this::streamTemplatesFromResource));
		providers = builder.build();
	}

	/**
	 * Возвращает шаблон по идентификатору, либо пустой шаблон, если загрузка не удалась.
	 * Пустой шаблон кэшируется, чтобы повторные обращения не вызывали повторную загрузку.
	 */
	public StructureTemplate getTemplateOrBlank(Identifier id) {
		Optional<StructureTemplate> cached = getTemplate(id);
		if (cached.isPresent()) {
			return cached.get();
		}

		StructureTemplate blank = new StructureTemplate();
		templates.put(id, Optional.of(blank));
		return blank;
	}

	public Optional<StructureTemplate> getTemplate(Identifier id) {
		return templates.computeIfAbsent(id, this::loadTemplate);
	}

	public Stream<Identifier> streamTemplates() {
		return providers.stream().flatMap(provider -> provider.lister().get()).distinct();
	}

	/**
	 * Перебирает провайдеры по приоритету и возвращает первый успешно загруженный шаблон.
	 * Исключения от провайдеров намеренно подавляются — отсутствие шаблона не является ошибкой.
	 */
	private Optional<StructureTemplate> loadTemplate(Identifier id) {
		for (Provider provider : providers) {
			try {
				Optional<StructureTemplate> result = provider.loader().apply(id);
				if (result.isPresent()) {
					return result;
				}
			} catch (Exception ignored) {
				// Провайдер не нашёл шаблон — пробуем следующий
			}
		}

		return Optional.empty();
	}

	public void setResourceManager(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
		templates.clear();
	}

	private Optional<StructureTemplate> loadTemplateFromResource(Identifier id) {
		Identifier resourcePath = STRUCTURE_NBT_FINDER.toResourcePath(id);
		return loadTemplate(
			() -> resourceManager.open(resourcePath),
			error -> LOGGER.error("Couldn't load structure {}", id, error)
		);
	}

	private Stream<Identifier> streamTemplatesFromResource() {
		return STRUCTURE_NBT_FINDER
			.findResources(resourceManager)
			.keySet()
			.stream()
			.map(STRUCTURE_NBT_FINDER::toResourceId);
	}

	private Optional<StructureTemplate> loadTemplateFromGameTestFile(Identifier id) {
		return loadTemplateFromSnbt(id, TestInstanceUtil.testStructuresDirectoryName);
	}

	private Stream<Identifier> streamTemplatesFromGameTestFile() {
		if (!Files.isDirectory(TestInstanceUtil.testStructuresDirectoryName)) {
			return Stream.empty();
		}

		List<Identifier> ids = new ArrayList<>();
		streamTemplates(TestInstanceUtil.testStructuresDirectoryName, "minecraft", SNBT_EXTENSION, ids::add);
		return ids.stream();
	}

	private Optional<StructureTemplate> loadTemplateFromFile(Identifier id) {
		if (!Files.isDirectory(generatedPath)) {
			return Optional.empty();
		}

		Path path = getTemplatePath(id, NBT_EXTENSION);
		return loadTemplate(
			() -> new FileInputStream(path.toFile()),
			error -> LOGGER.error("Couldn't load structure from {}", path, error)
		);
	}

	private Stream<Identifier> streamTemplatesFromFile() {
		if (!Files.isDirectory(generatedPath)) {
			return Stream.empty();
		}

		try {
			List<Identifier> ids = new ArrayList<>();
			try (DirectoryStream<Path> namespaceStream = Files.newDirectoryStream(generatedPath, Files::isDirectory)) {
				for (Path namespacePath : namespaceStream) {
					String namespace = namespacePath.getFileName().toString();
					Path structuresPath = namespacePath.resolve(STRUCTURES_DIRECTORY);
					streamTemplates(structuresPath, namespace, NBT_EXTENSION, ids::add);
				}
			}

			return ids.stream();
		} catch (IOException e) {
			return Stream.empty();
		}
	}

	/**
	 * Рекурсивно обходит директорию и передаёт идентификаторы найденных шаблонов в {@code idConsumer}.
	 *
	 * @param directory корневая директория поиска
	 * @param namespace пространство имён для формирования идентификаторов
	 * @param fileExtension расширение файлов для фильтрации
	 * @param idConsumer получатель найденных идентификаторов
	 */
	private void streamTemplates(
		Path directory,
		String namespace,
		String fileExtension,
		Consumer<Identifier> idConsumer
	) {
		int extensionLength = fileExtension.length();
		Function<String, String> stripExtension = name -> name.substring(0, name.length() - extensionLength);

		try (Stream<Path> fileStream = Files.find(
			directory,
			Integer.MAX_VALUE,
			(path, attrs) -> attrs.isRegularFile() && path.toString().endsWith(fileExtension)
		)) {
			fileStream.forEach(path -> {
				try {
					idConsumer.accept(Identifier.of(namespace, stripExtension.apply(toRelativePath(directory, path))));
				} catch (InvalidIdentifierException e) {
					LOGGER.error("Invalid location while listing folder {} contents", directory, e);
				}
			});
		} catch (IOException e) {
			LOGGER.error("Failed to list folder {} contents", directory, e);
		}
	}

	private String toRelativePath(Path root, Path path) {
		return root.relativize(path).toString().replace(File.separator, "/");
	}

	private Optional<StructureTemplate> loadTemplateFromSnbt(Identifier id, Path directory) {
		if (!Files.isDirectory(directory)) {
			return Optional.empty();
		}

		Path snbtPath = PathUtil.getResourcePath(directory, id.getPath(), SNBT_EXTENSION);

		try (BufferedReader reader = Files.newBufferedReader(snbtPath)) {
			String content = IOUtils.toString(reader);
			return Optional.of(createTemplate(NbtHelper.fromNbtProviderString(content)));
		} catch (NoSuchFileException e) {
			return Optional.empty();
		} catch (CommandSyntaxException | IOException e) {
			LOGGER.error("Couldn't load structure from {}", snbtPath, e);
			return Optional.empty();
		}
	}

	/**
	 * Загружает шаблон из потока, открытого через {@code opener}.
	 * При {@link FileNotFoundException} возвращает пустой Optional без логирования —
	 * отсутствие файла является штатной ситуацией при переборе провайдеров.
	 */
	private Optional<StructureTemplate> loadTemplate(TemplateFileOpener opener, Consumer<Throwable> errorHandler) {
		try (
			InputStream raw = opener.open();
			InputStream buffered = new FixedBufferInputStream(raw)
		) {
			return Optional.of(readTemplate(buffered));
		} catch (FileNotFoundException e) {
			return Optional.empty();
		} catch (Throwable e) {
			errorHandler.accept(e);
			return Optional.empty();
		}
	}

	private StructureTemplate readTemplate(InputStream stream) throws IOException {
		NbtCompound nbt = NbtIo.readCompressed(stream, NbtSizeTracker.ofUnlimitedBytes());
		return createTemplate(nbt);
	}

	public StructureTemplate createTemplate(NbtCompound nbt) {
		StructureTemplate template = new StructureTemplate();
		int dataVersion = NbtHelper.getDataVersion(nbt, 500);
		template.readNbt(blockLookup, DataFixTypes.STRUCTURE.update(dataFixer, nbt, dataVersion));
		return template;
	}

	/**
	 * Сохраняет шаблон на диск в формате .nbt или .snbt в зависимости от флага
	 * {@link SharedConstants#SAVE_STRUCTURES_AS_SNBT}.
	 *
	 * @param id идентификатор шаблона
	 * @return {@code true}, если сохранение прошло успешно
	 */
	public boolean saveTemplate(Identifier id) {
		Optional<StructureTemplate> cached = templates.get(id);
		if (cached == null || cached.isEmpty()) {
			return false;
		}

		StructureTemplate template = cached.get();
		String extension = SharedConstants.SAVE_STRUCTURES_AS_SNBT ? SNBT_EXTENSION : NBT_EXTENSION;
		Path savePath = getTemplatePath(id, extension);
		Path parentDir = savePath.getParent();

		if (parentDir == null) {
			return false;
		}

		try {
			Files.createDirectories(Files.exists(parentDir) ? parentDir.toRealPath() : parentDir);
		} catch (IOException e) {
			LOGGER.error("Failed to create parent directory: {}", parentDir);
			return false;
		}

		NbtCompound nbt = template.writeNbt(new NbtCompound());

		if (SharedConstants.SAVE_STRUCTURES_AS_SNBT) {
			try {
				NbtProvider.writeTo(DataWriter.UNCACHED, savePath, NbtHelper.toNbtProviderString(nbt));
			} catch (Throwable e) {
				return false;
			}
		} else {
			try (OutputStream output = new FileOutputStream(savePath.toFile())) {
				NbtIo.writeCompressed(nbt, output);
			} catch (Throwable e) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Вычисляет путь к файлу шаблона в директории {@code generated/}.
	 * Проверяет, что путь не выходит за пределы {@code generatedPath} (path traversal protection).
	 *
	 * @param id идентификатор шаблона
	 * @param extension расширение файла (например, {@code ".nbt"})
	 * @return абсолютный путь к файлу шаблона
	 * @throws InvalidIdentifierException если путь содержит недопустимые символы или выходит за пределы директории
	 */
	public Path getTemplatePath(Identifier id, String extension) {
		if (id.getPath().contains("//")) {
			throw new InvalidIdentifierException("Invalid resource path: " + id);
		}

		try {
			Path namespacePath = generatedPath.resolve(id.getNamespace());
			Path structuresPath = namespacePath.resolve(STRUCTURES_DIRECTORY);
			Path templatePath = PathUtil.getResourcePath(structuresPath, id.getPath(), extension);

			if (templatePath.startsWith(generatedPath)
				&& PathUtil.isNormal(templatePath)
				&& PathUtil.isAllowedName(templatePath)
			) {
				return templatePath;
			}

			throw new InvalidIdentifierException("Invalid resource path: " + templatePath);
		} catch (InvalidPathException e) {
			throw new InvalidIdentifierException("Invalid resource path: " + id, e);
		}
	}

	public void unloadTemplate(Identifier id) {
		templates.remove(id);
	}

	/**
	 * Провайдер шаблонов: пара из загрузчика по идентификатору и перечислителя доступных идентификаторов.
	 */
	record Provider(Function<Identifier, Optional<StructureTemplate>> loader, Supplier<Stream<Identifier>> lister) {
	}

	@FunctionalInterface
	interface TemplateFileOpener {

		InputStream open() throws IOException;
	}
}
