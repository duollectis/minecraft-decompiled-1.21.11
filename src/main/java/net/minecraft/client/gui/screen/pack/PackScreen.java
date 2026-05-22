package net.minecraft.client.gui.screen.pack;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SymlinkWarningScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.resource.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.path.SymlinkEntry;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Экран управления пакетами ресурсов: позволяет включать, отключать и перетаскивать паки.
 * Отслеживает изменения в директории паков через {@link DirectoryWatcher}.
 */
@Environment(EnvType.CLIENT)
public class PackScreen extends Screen {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final Text AVAILABLE_TITLE = Text.translatable("pack.available.title");
	private static final Text SELECTED_TITLE = Text.translatable("pack.selected.title");
	private static final Text OPEN_FOLDER = Text.translatable("pack.openFolder");
	private static final Text SEARCH_BOX_PLACEHOLDER =
			Text.translatable("gui.packSelection.search").fillStyle(TextFieldWidget.SEARCH_STYLE);
	private static final int PACK_LIST_WIDTH = 200;
	private static final int HEADER_SPACING = 4;
	private static final int SEARCH_BOX_HEIGHT = 15;
	private static final Text DROP_INFO = Text.translatable("pack.dropInfo").formatted(Formatting.GRAY);
	private static final Text FOLDER_INFO = Text.translatable("pack.folderInfo");
	private static final int BUTTON_HEIGHT = 20;
	private static final Identifier UNKNOWN_PACK = Identifier.ofVanilla("textures/misc/unknown_pack.png");
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	private final ResourcePackOrganizer organizer;
	private PackScreen.@Nullable DirectoryWatcher directoryWatcher;
	private long refreshTimeout;
	private @Nullable PackListWidget availablePackList;
	private @Nullable PackListWidget selectedPackList;
	private @Nullable TextFieldWidget searchBox;
	private final Path file;
	private @Nullable ButtonWidget doneButton;
	private final Map<String, Identifier> iconTextures = Maps.newHashMap();

	public PackScreen(
			ResourcePackManager resourcePackManager,
			Consumer<ResourcePackManager> applier,
			Path file,
			Text title
	) {
		super(title);
		this.organizer =
				new ResourcePackOrganizer(
						this::updatePackLists,
						this::getPackIconTexture,
						resourcePackManager,
						applier
				);
		this.file = file;
		this.directoryWatcher = PackScreen.DirectoryWatcher.create(file);
	}

	@Override
	public void close() {
		this.organizer.apply();
		this.closeDirectoryWatcher();
	}

	private void closeDirectoryWatcher() {
		if (directoryWatcher == null) {
			return;
		}

		try {
			directoryWatcher.close();
			directoryWatcher = null;
		} catch (Exception ignored) {
			// AutoCloseable.close() не должен бросать, но API это допускает
		}
	}

	@Override
	protected void init() {
		layout.setHeaderHeight(4 + 9 + 4 + 9 + 4 + SEARCH_BOX_HEIGHT + 4);
		DirectionalLayoutWidget headerLayout = layout.addHeader(DirectionalLayoutWidget.vertical().spacing(4));
		headerLayout.getMainPositioner().alignHorizontalCenter();
		headerLayout.add(new TextWidget(getTitle(), textRenderer));
		headerLayout.add(new TextWidget(DROP_INFO, textRenderer));
		searchBox = headerLayout.add(new TextFieldWidget(textRenderer, 0, 0, PACK_LIST_WIDTH, SEARCH_BOX_HEIGHT, Text.empty()));
		searchBox.setPlaceholder(SEARCH_BOX_PLACEHOLDER);
		searchBox.setChangedListener(this::setSearch);
		availablePackList = layout.addBody(new PackListWidget(client, this, PACK_LIST_WIDTH, height - 66, AVAILABLE_TITLE));
		selectedPackList = layout.addBody(new PackListWidget(client, this, PACK_LIST_WIDTH, height - 66, SELECTED_TITLE));
		DirectionalLayoutWidget footerLayout = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		footerLayout.add(
				ButtonWidget
						.builder(OPEN_FOLDER, button -> Util.getOperatingSystem().open(file))
						.tooltip(Tooltip.of(FOLDER_INFO))
						.build()
		);
		doneButton = footerLayout.add(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).build());
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
		refresh();
	}

	@Override
	protected void setInitialFocus() {
		if (searchBox != null) {
			setInitialFocus(searchBox);
		} else {
			super.setInitialFocus();
		}
	}

	private void setSearch(String search) {
		this.filter(search, this.organizer.getEnabledPacks(), this.selectedPackList);
		this.filter(search, this.organizer.getDisabledPacks(), this.availablePackList);
	}

	private void filter(String search, Stream<ResourcePackOrganizer.Pack> packs, @Nullable PackListWidget listWidget) {
		if (listWidget != null) {
			String string = search.toLowerCase(Locale.ROOT);
			Stream<ResourcePackOrganizer.Pack> stream = packs.filter(
					pack -> search.isBlank()
							|| pack.getName().toLowerCase(Locale.ROOT).contains(string)
							|| pack.getDisplayName().getString().toLowerCase(Locale.ROOT).contains(string)
							|| pack.getDescription().getString().toLowerCase(Locale.ROOT).contains(string)
			);
			listWidget.set(stream, null);
		}
	}

	@Override
	protected void refreshWidgetPositions() {
		this.layout.refreshPositions();
		if (this.availablePackList != null) {
			this.availablePackList.position(
					PACK_LIST_WIDTH,
					this.layout.getContentHeight(),
					this.width / 2 - SEARCH_BOX_HEIGHT - PACK_LIST_WIDTH,
					this.layout.getHeaderHeight()
			);
		}

		if (this.selectedPackList != null) {
			this.selectedPackList.position(
					PACK_LIST_WIDTH,
					this.layout.getContentHeight(),
					this.width / 2 + SEARCH_BOX_HEIGHT,
					this.layout.getHeaderHeight()
			);
		}
	}

	@Override
	public void tick() {
		if (this.directoryWatcher != null) {
			try {
				if (this.directoryWatcher.pollForChange()) {
					this.refreshTimeout = 20L;
				}
			}
			catch (IOException var2) {
				LOGGER.warn("Failed to poll for directory {} changes, stopping", this.file);
				this.closeDirectoryWatcher();
			}
		}

		if (this.refreshTimeout > 0L && --this.refreshTimeout == 0L) {
			this.refresh();
		}
	}

	private void updatePackLists(ResourcePackOrganizer.@Nullable AbstractPack focused) {
		if (this.selectedPackList != null) {
			this.selectedPackList.set(this.organizer.getEnabledPacks(), focused);
		}

		if (this.availablePackList != null) {
			this.availablePackList.set(this.organizer.getDisabledPacks(), focused);
		}

		if (this.searchBox != null) {
			this.setSearch(this.searchBox.getText());
		}

		if (this.doneButton != null) {
			this.doneButton.active = !this.selectedPackList.children().isEmpty();
		}
	}

	private void refresh() {
		this.organizer.refresh();
		this.updatePackLists(null);
		this.refreshTimeout = 0L;
		this.iconTextures.clear();
	}

	protected static void copyPacks(MinecraftClient client, List<Path> srcPaths, Path destPath) {
		MutableBoolean hasError = new MutableBoolean();
		srcPaths.forEach(src -> {
			try (Stream<Path> stream = Files.walk(src)) {
				stream.forEach(toCopy -> {
					try {
						Util.relativeCopy(src.getParent(), destPath, toCopy);
					} catch (IOException copyError) {
						LOGGER.warn("Failed to copy datapack file from {} to {}", toCopy, destPath, copyError);
						hasError.setTrue();
					}
				});
			} catch (IOException error) {
				LOGGER.warn("Failed to copy datapack file from {} to {}", src, destPath);
				hasError.setTrue();
			}
		});

		if (hasError.isTrue()) {
			SystemToast.addPackCopyFailure(client, destPath.toString());
		}
	}

	@Override
	public void onFilesDropped(List<Path> paths) {
		String fileNames = streamFileNames(paths).collect(Collectors.joining(", "));
		client.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						List<Path> validPaths = new ArrayList<>(paths.size());
						Set<Path> remainingPaths = new HashSet<>(paths);
						ResourcePackOpener<Path> opener = new ResourcePackOpener<Path>(client.getSymlinkFinder()) {
							@Override
							protected Path openZip(Path path) {
								return path;
							}

							@Override
							protected Path openDirectory(Path path) {
								return path;
							}
						};
						List<SymlinkEntry> symlinkEntries = new ArrayList<>();

						for (Path path : paths) {
							try {
								Path resolved = opener.open(path, symlinkEntries);
								if (resolved == null) {
									LOGGER.warn("Path {} does not seem like pack", path);
								} else {
									validPaths.add(resolved);
									remainingPaths.remove(resolved);
								}
							} catch (IOException error) {
								LOGGER.warn("Failed to check {} for packs", path, error);
							}
						}

						if (!symlinkEntries.isEmpty()) {
							client.setScreen(SymlinkWarningScreen.pack(() -> client.setScreen(this)));
							return;
						}

						if (!validPaths.isEmpty()) {
							copyPacks(client, validPaths, file);
							refresh();
						}

						if (!remainingPaths.isEmpty()) {
							String rejected = streamFileNames(remainingPaths).collect(Collectors.joining(", "));
							client.setScreen(new NoticeScreen(
									() -> client.setScreen(this),
									Text.translatable("pack.dropRejected.title"),
									Text.translatable("pack.dropRejected.message", rejected)
							));
							return;
						}
					}

					client.setScreen(this);
				},
				Text.translatable("pack.dropConfirm"),
				Text.literal(fileNames)
		));
	}

	private static Stream<String> streamFileNames(Collection<Path> paths) {
		return paths.stream().map(Path::getFileName).map(Path::toString);
	}

	private Identifier loadPackIcon(TextureManager textureManager, ResourcePackProfile resourcePackProfile) {
		try (ResourcePack resourcePack = resourcePackProfile.createResourcePack()) {
			InputSupplier<InputStream> inputSupplier = resourcePack.openRoot("pack.png");
			if (inputSupplier == null) {
				return UNKNOWN_PACK;
			}

			String packId = resourcePackProfile.getId();
			Identifier iconId = Identifier.ofVanilla(
					"pack/" + Util.replaceInvalidChars(packId, Identifier::isPathCharacterValid)
							+ "/" + Hashing.sha1().hashUnencodedChars(packId) + "/icon"
			);

			try (InputStream inputStream = inputSupplier.get()) {
				NativeImage nativeImage = NativeImage.read(inputStream);
				textureManager.registerTexture(iconId, new NativeImageBackedTexture(iconId::toString, nativeImage));
				return iconId;
			}
		} catch (Exception error) {
			LOGGER.warn("Failed to load icon from pack {}", resourcePackProfile.getId(), error);
			return UNKNOWN_PACK;
		}
	}

	private Identifier getPackIconTexture(ResourcePackProfile resourcePackProfile) {
		return this.iconTextures
				.computeIfAbsent(
						resourcePackProfile.getId(),
						profileName -> this.loadPackIcon(this.client.getTextureManager(), resourcePackProfile)
				);
	}

	/**
	 * Отслеживает изменения файловой системы в директории паков через {@link WatchService}.
	 */
	@Environment(EnvType.CLIENT)
	static class DirectoryWatcher implements AutoCloseable {

		private final WatchService watchService;
		private final Path path;

		public DirectoryWatcher(Path path) throws IOException {
			this.path = path;
			this.watchService = path.getFileSystem().newWatchService();

			try {
				this.watchDirectory(path);

				try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
					for (Path path2 : directoryStream) {
						if (Files.isDirectory(path2, LinkOption.NOFOLLOW_LINKS)) {
							this.watchDirectory(path2);
						}
					}
				}
			}
			catch (Exception var7) {
				this.watchService.close();
				throw var7;
			}
		}

		public static PackScreen.@Nullable DirectoryWatcher create(Path path) {
			try {
				return new PackScreen.DirectoryWatcher(path);
			} catch (IOException error) {
				PackScreen.LOGGER.warn("Failed to initialize pack directory {} monitoring", path, error);
				return null;
			}
		}

		private void watchDirectory(Path path) throws IOException {
			path.register(
					this.watchService,
					StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY
			);
		}

		public boolean pollForChange() throws IOException {
			boolean hasChanges = false;

			WatchKey watchKey;
			while ((watchKey = watchService.poll()) != null) {
				for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
					hasChanges = true;
					if (watchKey.watchable() == path && watchEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
						Path created = path.resolve((Path) watchEvent.context());
						if (Files.isDirectory(created, LinkOption.NOFOLLOW_LINKS)) {
							watchDirectory(created);
						}
					}
				}

				watchKey.reset();
			}

			return hasChanges;
		}

		@Override
		public void close() throws IOException {
			this.watchService.close();
		}
	}
}
