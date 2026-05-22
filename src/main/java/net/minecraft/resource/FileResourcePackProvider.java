package net.minecraft.resource;

import com.mojang.logging.LogUtils;
import net.minecraft.resource.fs.ResourceFileSystem;
import net.minecraft.text.Text;
import net.minecraft.util.path.PathUtil;
import net.minecraft.util.path.SymlinkEntry;
import net.minecraft.util.path.SymlinkFinder;
import net.minecraft.util.path.SymlinkValidationException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Провайдер паков, сканирующий директорию файловой системы и регистрирующий
 * найденные zip-архивы и директории как ресурс-паки.
 */
public class FileResourcePackProvider implements ResourcePackProvider {

	static final Logger LOGGER = LogUtils.getLogger();

	private static final ResourcePackPosition POSITION = new ResourcePackPosition(
		false,
		ResourcePackProfile.InsertionPosition.TOP,
		false
	);

	private final Path packsDir;
	private final ResourceType type;
	private final ResourcePackSource source;
	private final SymlinkFinder symlinkFinder;

	public FileResourcePackProvider(
		Path packsDir,
		ResourceType type,
		ResourcePackSource source,
		SymlinkFinder symlinkFinder
	) {
		this.packsDir = packsDir;
		this.type = type;
		this.source = source;
		this.symlinkFinder = symlinkFinder;
	}

	private static String getFileName(Path path) {
		return path.getFileName().toString();
	}

	@Override
	public void register(Consumer<ResourcePackProfile> profileAdder) {
		try {
			PathUtil.createDirectories(packsDir);
			forEachProfile(packsDir, symlinkFinder, (path, packFactory) -> {
				ResourcePackInfo packInfo = createPackInfo(path);
				ResourcePackProfile profile = ResourcePackProfile.create(packInfo, packFactory, type, POSITION);
				if (profile != null) {
					profileAdder.accept(profile);
				}
			});
		} catch (IOException exception) {
			LOGGER.warn("Failed to list packs in {}", packsDir, exception);
		}
	}

	private ResourcePackInfo createPackInfo(Path path) {
		String name = getFileName(path);
		return new ResourcePackInfo("file/" + name, Text.literal(name), source, Optional.empty());
	}

	/**
	 * Перебирает все записи в директории {@code path} и вызывает {@code callback}
	 * для каждой, которая является допустимым паком (zip или директория с {@code pack.mcmeta}).
	 *
	 * @param path          директория для сканирования
	 * @param symlinkFinder валидатор символических ссылок
	 * @param callback      получатель пар (путь, фабрика пака)
	 * @throws IOException при ошибке чтения директории
	 */
	public static void forEachProfile(
		Path path,
		SymlinkFinder symlinkFinder,
		BiConsumer<Path, ResourcePackProfile.PackFactory> callback
	) throws IOException {
		PackOpenerImpl opener = new PackOpenerImpl(symlinkFinder);

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
			for (Path entry : directoryStream) {
				try {
					List<SymlinkEntry> symlinks = new ArrayList<>();
					ResourcePackProfile.PackFactory packFactory = opener.open(entry, symlinks);

					if (!symlinks.isEmpty()) {
						LOGGER.warn(
							"Ignoring potential pack entry: {}",
							SymlinkValidationException.getMessage(entry, symlinks)
						);
					} else if (packFactory != null) {
						callback.accept(entry, packFactory);
					} else {
						LOGGER.info("Found non-pack entry '{}', ignoring", entry);
					}
				} catch (IOException exception) {
					LOGGER.warn("Failed to read properties of '{}', ignoring", entry, exception);
				}
			}
		}
	}

	/**
	 * Внутренняя реализация {@link ResourcePackOpener} для открытия паков из файловой системы.
	 */
	static class PackOpenerImpl extends ResourcePackOpener<ResourcePackProfile.PackFactory> {

		protected PackOpenerImpl(SymlinkFinder symlinkFinder) {
			super(symlinkFinder);
		}

		@Override
		protected ResourcePackProfile.@Nullable PackFactory openZip(Path path) {
			FileSystem fileSystem = path.getFileSystem();
			if (fileSystem != FileSystems.getDefault() && !(fileSystem instanceof ResourceFileSystem)) {
				LOGGER.info("Can't open pack archive at {}", path);
				return null;
			}

			return new ZipResourcePack.ZipBackedFactory(path);
		}

		@Override
		protected ResourcePackProfile.PackFactory openDirectory(Path path) {
			return new DirectoryResourcePack.DirectoryBackedFactory(path);
		}
	}
}
