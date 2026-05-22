package net.minecraft.client.gui.screen.world;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.SquareWidgetEntry;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.nbt.NbtCrashException;
import net.minecraft.nbt.NbtException;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.path.SymlinkEntry;
import net.minecraft.util.path.SymlinkValidationException;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * Виджет списка сохранённых миров с асинхронной загрузкой, поиском и иконками.
 */
@Environment(EnvType.CLIENT)
public class WorldListWidget extends AlwaysSelectedEntryListWidget<WorldListWidget.Entry> {

	public static final DateTimeFormatter DATE_FORMAT = Util.getDefaultLocaleFormatter(FormatStyle.SHORT);
	static final Identifier ERROR_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("world_list/error_highlighted");
	static final Identifier ERROR_TEXTURE = Identifier.ofVanilla("world_list/error");
	static final Identifier
			MARKED_JOIN_HIGHLIGHTED_TEXTURE =
			Identifier.ofVanilla("world_list/marked_join_highlighted");
	static final Identifier MARKED_JOIN_TEXTURE = Identifier.ofVanilla("world_list/marked_join");
	static final Identifier WARNING_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("world_list/warning_highlighted");
	static final Identifier WARNING_TEXTURE = Identifier.ofVanilla("world_list/warning");
	static final Identifier JOIN_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("world_list/join_highlighted");
	static final Identifier JOIN_TEXTURE = Identifier.ofVanilla("world_list/join");
	static final Logger LOGGER = LogUtils.getLogger();
	static final Text
			FROM_NEWER_VERSION_FIRST_LINE =
			Text.translatable("selectWorld.tooltip.fromNewerVersion1").formatted(Formatting.RED);
	static final Text
			FROM_NEWER_VERSION_SECOND_LINE =
			Text.translatable("selectWorld.tooltip.fromNewerVersion2").formatted(Formatting.RED);
	static final Text
			SNAPSHOT_FIRST_LINE =
			Text.translatable("selectWorld.tooltip.snapshot1").formatted(Formatting.GOLD);
	static final Text
			SNAPSHOT_SECOND_LINE =
			Text.translatable("selectWorld.tooltip.snapshot2").formatted(Formatting.GOLD);
	static final Text LOCKED_TEXT = Text.translatable("selectWorld.locked").formatted(Formatting.RED);
	static final Text
			CONVERSION_TOOLTIP =
			Text.translatable("selectWorld.conversion.tooltip").formatted(Formatting.RED);
	static final Text
			INCOMPATIBLE_TOOLTIP =
			Text.translatable("selectWorld.incompatible.tooltip").formatted(Formatting.RED);
	static final Text EXPERIMENTAL_TEXT = Text.translatable("selectWorld.experimental");
	private final Screen parent;
	private CompletableFuture<List<LevelSummary>> levelsFuture;
	private @Nullable List<LevelSummary> levels;
	private final WorldListWidget.LoadingEntry loadingEntry;
	final WorldListWidget.WorldListType worldListType;
	private String search;
	private boolean failedToGetLevels;
	private final @Nullable Consumer<LevelSummary> selectionCallback;
	final @Nullable Consumer<WorldListWidget.WorldEntry> confirmationCallback;

	WorldListWidget(
			Screen parent,
			MinecraftClient client,
			int width,
			int height,
			String search,
			@Nullable WorldListWidget predecessor,
			@Nullable Consumer<LevelSummary> selectionCallback,
			@Nullable Consumer<WorldListWidget.WorldEntry> confirmationCallback,
			WorldListWidget.WorldListType worldListType
	) {
		super(client, width, height, 0, 36);
		this.parent = parent;
		this.loadingEntry = new WorldListWidget.LoadingEntry(client);
		this.search = search;
		this.selectionCallback = selectionCallback;
		this.confirmationCallback = confirmationCallback;
		this.worldListType = worldListType;
		if (predecessor != null) {
			this.levelsFuture = predecessor.levelsFuture;
		}
		else {
			this.levelsFuture = this.loadLevels();
		}

		this.addEntry(this.loadingEntry);
		this.show(this.tryGet());
	}

	@Override
	protected void clearEntries() {
		this.children().forEach(WorldListWidget.Entry::close);
		super.clearEntries();
	}

	private @Nullable List<LevelSummary> tryGet() {
		try {
			List<LevelSummary> list = this.levelsFuture.getNow(null);
			if (this.worldListType == WorldListWidget.WorldListType.UPLOAD_WORLD) {
				if (list == null || this.failedToGetLevels) {
					return null;
				}

				this.failedToGetLevels = true;
				list = list.stream().filter(LevelSummary::isImmediatelyLoadable).toList();
			}

			return list;
		}
		catch (CancellationException | CompletionException var2) {
			return null;
		}
	}

	public void load() {
		this.levelsFuture = this.loadLevels();
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		List<LevelSummary> list = this.tryGet();
		if (list != this.levels) {
			this.show(list);
		}

		super.renderWidget(context, mouseX, mouseY, deltaTicks);
	}

	private void show(@Nullable List<LevelSummary> summaries) {
		if (summaries != null) {
			if (summaries.isEmpty()) {
				switch (this.worldListType) {
					case SINGLEPLAYER:
						CreateWorldScreen.show(this.client, () -> this.client.setScreen(null));
						break;
					case UPLOAD_WORLD:
						this.clearEntries();
						this.addEntry(new WorldListWidget.EmptyListEntry(
								Text.translatable("mco.upload.select.world.none"), this.parent.getTextRenderer()));
				}
			}
			else {
				this.showSummaries(this.search, summaries);
				this.levels = summaries;
			}
		}
	}

	public void setSearch(String search) {
		if (this.levels != null && !search.equals(this.search)) {
			this.showSummaries(search, this.levels);
		}

		this.search = search;
	}

	private CompletableFuture<List<LevelSummary>> loadLevels() {
		LevelStorage.LevelList levelList;
		try {
			levelList = this.client.getLevelStorage().getLevelList();
		}
		catch (LevelStorageException var3) {
			LOGGER.error("Couldn't load level list", var3);
			this.showUnableToLoadScreen(var3.getMessageText());
			return CompletableFuture.completedFuture(List.of());
		}

		return this.client.getLevelStorage().loadSummaries(levelList).exceptionally(throwable -> {
			this.client.setCrashReportSupplierAndAddDetails(CrashReport.create(throwable, "Couldn't load level list"));
			return List.of();
		});
	}

	private void showSummaries(String search, List<LevelSummary> summaries) {
		List<WorldListWidget.Entry> list = new ArrayList<>();
		Optional<WorldListWidget.WorldEntry> optional = this.getSelectedAsOptional();
		WorldListWidget.WorldEntry worldEntry = null;

		for (LevelSummary levelSummary : summaries
				.stream()
				.filter(summary -> this.shouldShow(search.toLowerCase(Locale.ROOT), summary))
				.toList()) {
			WorldListWidget.WorldEntry worldEntry2 = new WorldListWidget.WorldEntry(this, levelSummary);
			if (optional.isPresent() && optional.get().getLevel().getName().equals(worldEntry2.getLevel().getName())) {
				worldEntry = worldEntry2;
			}

			list.add(worldEntry2);
		}

		this.removeEntries(this.children().stream().filter(child -> !list.contains(child)).toList());
		list.forEach(entry -> {
			if (!this.children().contains(entry)) {
				this.addEntry(entry);
			}
		});
		this.setSelected((WorldListWidget.Entry) worldEntry);
		this.narrateScreenIfNarrationEnabled();
	}

	private boolean shouldShow(String search, LevelSummary summary) {
		return summary.getDisplayName().toLowerCase(Locale.ROOT).contains(search) || summary
				.getName()
				.toLowerCase(Locale.ROOT)
				.contains(search);
	}

	private void narrateScreenIfNarrationEnabled() {
		this.refreshScroll();
		this.parent.narrateScreenIfNarrationEnabled(true);
	}

	private void showUnableToLoadScreen(Text message) {
		this.client.setScreen(new FatalErrorScreen(Text.translatable("selectWorld.unable_to_load"), message));
	}

	@Override
	public int getRowWidth() {
		return 270;
	}

	public void setSelected(WorldListWidget.@Nullable Entry entry) {
		super.setSelected(entry);
		if (this.selectionCallback != null) {
			this.selectionCallback.accept(
					entry instanceof WorldListWidget.WorldEntry worldEntry ? worldEntry.level : null);
		}
	}

	public Optional<WorldListWidget.WorldEntry> getSelectedAsOptional() {
		WorldListWidget.Entry entry = this.getSelectedOrNull();
		return entry instanceof WorldListWidget.WorldEntry worldEntry ? Optional.of(worldEntry) : Optional.empty();
	}

	public void refresh() {
		this.load();
		this.client.setScreen(this.parent);
	}

	public Screen getParent() {
		return this.parent;
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		if (this.children().contains(this.loadingEntry)) {
			this.loadingEntry.appendNarrations(builder);
		}
		else {
			super.appendClickableNarrations(builder);
		}
	}

	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final MinecraftClient client;
		private final Screen parent;
		private int width;
		private int height;
		private String search = "";
		private WorldListWidget.WorldListType worldListType = WorldListWidget.WorldListType.SINGLEPLAYER;
		private @Nullable WorldListWidget predecessor = null;
		private @Nullable Consumer<LevelSummary> selectionCallback = null;
		private @Nullable Consumer<WorldListWidget.WorldEntry> confirmationCallback = null;

		public Builder(MinecraftClient client, Screen parent) {
			this.client = client;
			this.parent = parent;
		}

		public WorldListWidget.Builder width(int width) {
			this.width = width;
			return this;
		}

		public WorldListWidget.Builder height(int height) {
			this.height = height;
			return this;
		}

		public WorldListWidget.Builder search(String search) {
			this.search = search;
			return this;
		}

		public WorldListWidget.Builder predecessor(@Nullable WorldListWidget predecessor) {
			this.predecessor = predecessor;
			return this;
		}

		public WorldListWidget.Builder selectionCallback(Consumer<LevelSummary> selectionCallback) {
			this.selectionCallback = selectionCallback;
			return this;
		}

		public WorldListWidget.Builder confirmationCallback(Consumer<WorldListWidget.WorldEntry> confirmationCallback) {
			this.confirmationCallback = confirmationCallback;
			return this;
		}

		public WorldListWidget.Builder uploadWorld() {
			this.worldListType = WorldListWidget.WorldListType.UPLOAD_WORLD;
			return this;
		}

		public WorldListWidget toWidget() {
			return new WorldListWidget(
					this.parent,
					this.client,
					this.width,
					this.height,
					this.search,
					this.predecessor,
					this.selectionCallback,
					this.confirmationCallback,
					this.worldListType
			);
		}
	}

	@Environment(EnvType.CLIENT)
	public static final class EmptyListEntry extends WorldListWidget.Entry {

		private final TextWidget widget;

		public EmptyListEntry(Text text, TextRenderer textRenderer) {
			this.widget = new TextWidget(text, textRenderer);
		}

		@Override
		public Text getNarration() {
			return this.widget.getMessage();
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			this.widget.setPosition(
					this.getContentMiddleX() - this.widget.getWidth() / 2,
					this.getContentMiddleY() - this.widget.getHeight() / 2
			);
			this.widget.render(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Environment(EnvType.CLIENT)
	public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<WorldListWidget.Entry> implements AutoCloseable {

		@Override
		public void close() {
		}

		public @Nullable LevelSummary getLevel() {
			return null;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class LoadingEntry extends WorldListWidget.Entry {

		private static final Text LOADING_LIST_TEXT = Text.translatable("selectWorld.loading_list");
		private final MinecraftClient client;

		public LoadingEntry(MinecraftClient client) {
			this.client = client;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int titleX = (client.currentScreen.width - client.textRenderer.getWidth(LOADING_LIST_TEXT)) / 2;
			int titleY = getContentY() + (getContentHeight() - 9) / 2;
			context.drawTextWithShadow(client.textRenderer, LOADING_LIST_TEXT, titleX, titleY, -1);
			String spinner = LoadingDisplay.get(Util.getMeasuringTimeMs());
			int spinnerX = (client.currentScreen.width - client.textRenderer.getWidth(spinner)) / 2;
			int spinnerY = titleY + 9;
			context.drawTextWithShadow(client.textRenderer, spinner, spinnerX, spinnerY, -8355712);
		}

		@Override
		public Text getNarration() {
			return LOADING_LIST_TEXT;
		}
	}

	@Environment(EnvType.CLIENT)
	public final class WorldEntry extends WorldListWidget.Entry implements SquareWidgetEntry {

		private static final int ICON_SIZE = 32;
		private final WorldListWidget parent;
		private final MinecraftClient client;
		private final Screen screen;
		final LevelSummary level;
		private final WorldIcon icon;
		private final TextWidget displayNameWidget;
		private final TextWidget nameWidget;
		private final TextWidget detailsWidget;
		private @Nullable Path iconPath;

		public WorldEntry(final WorldListWidget parent, final LevelSummary level) {
			this.parent = parent;
			this.client = parent.client;
			this.screen = parent.getParent();
			this.level = level;
			this.icon = WorldIcon.forWorld(client.getTextureManager(), level.getName());
			this.iconPath = level.getIconPath();
			int maxTextWidth = parent.getRowWidth() - getTextX() - 2;

			Text displayName = Text.literal(level.getDisplayName());
			this.displayNameWidget = new TextWidget(displayName, client.textRenderer);
			displayNameWidget.setMaxWidth(maxTextWidth);
			if (client.textRenderer.getWidth(displayName) > maxTextWidth) {
				displayNameWidget.setTooltip(Tooltip.of(displayName));
			}

			String nameWithDate = level.getName();
			long lastPlayed = level.getLastPlayed();
			if (lastPlayed != -1L) {
				ZonedDateTime playedAt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastPlayed), ZoneId.systemDefault());
				nameWithDate = nameWithDate + " (" + WorldListWidget.DATE_FORMAT.format(playedAt) + ")";
			}

			Text nameText = Text.literal(nameWithDate).withColor(-8355712);
			this.nameWidget = new TextWidget(nameText, client.textRenderer);
			nameWidget.setMaxWidth(maxTextWidth);
			if (client.textRenderer.getWidth(nameWithDate) > maxTextWidth) {
				nameWidget.setTooltip(Tooltip.of(nameText));
			}

			Text detailsText = Texts.withStyle(level.getDetails(), Style.EMPTY.withColor(-8355712));
			this.detailsWidget = new TextWidget(detailsText, client.textRenderer);
			detailsWidget.setMaxWidth(maxTextWidth);
			if (client.textRenderer.getWidth(detailsText) > maxTextWidth) {
				detailsWidget.setTooltip(Tooltip.of(detailsText));
			}

			validateIconPath();
			loadIcon();
		}

		private void validateIconPath() {
			if (this.iconPath != null) {
				try {
					BasicFileAttributes
							basicFileAttributes =
							Files.readAttributes(this.iconPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
					if (basicFileAttributes.isSymbolicLink()) {
						List<SymlinkEntry> list = this.client.getSymlinkFinder().validate(this.iconPath);
						if (!list.isEmpty()) {
							WorldListWidget.LOGGER.warn(
									"{}",
									SymlinkValidationException.getMessage(this.iconPath, list)
							);
							this.iconPath = null;
						}
						else {
							basicFileAttributes = Files.readAttributes(this.iconPath, BasicFileAttributes.class);
						}
					}

					if (!basicFileAttributes.isRegularFile()) {
						this.iconPath = null;
					}
				}
				catch (NoSuchFileException var3) {
					this.iconPath = null;
				}
				catch (IOException var4) {
					WorldListWidget.LOGGER.error("could not validate symlink", var4);
					this.iconPath = null;
				}
			}
		}

		@Override
		public Text getNarration() {
			Text text = Text.translatable(
					"narrator.select.world_info",
					this.level.getDisplayName(),
					Text.of(new Date(this.level.getLastPlayed())),
					this.level.getDetails()
			);
			if (this.level.isLocked()) {
				text = ScreenTexts.joinSentences(text, WorldListWidget.LOCKED_TEXT);
			}

			if (this.level.isExperimental()) {
				text = ScreenTexts.joinSentences(text, WorldListWidget.EXPERIMENTAL_TEXT);
			}

			return Text.translatable("narrator.select", text);
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int textX = getTextX();
			displayNameWidget.setPosition(textX, getContentY() + 1);
			displayNameWidget.render(context, mouseX, mouseY, deltaTicks);
			nameWidget.setPosition(textX, getContentY() + 9 + 3);
			nameWidget.render(context, mouseX, mouseY, deltaTicks);
			detailsWidget.setPosition(textX, getContentY() + 9 + 9 + 3);
			detailsWidget.render(context, mouseX, mouseY, deltaTicks);
			context.drawTexture(
					RenderPipelines.GUI_TEXTURED,
					this.icon.getTextureId(),
					this.getContentX(),
					this.getContentY(),
					0.0F,
					0.0F,
					ICON_SIZE,
					ICON_SIZE,
					ICON_SIZE,
					ICON_SIZE
			);
			if (parent.worldListType == WorldListWidget.WorldListType.SINGLEPLAYER && (
					client.options.getTouchscreen().getValue() || hovered
			)) {
				context.fill(
						getContentX(),
						getContentY(),
						getContentX() + ICON_SIZE,
						getContentY() + ICON_SIZE,
						-1601138544
				);
				int relX = mouseX - getContentX();
				int relY = mouseY - getContentY();
				boolean overIcon = isInside(relX, relY, ICON_SIZE);
				Identifier joinTexture = overIcon ? WorldListWidget.JOIN_HIGHLIGHTED_TEXTURE : WorldListWidget.JOIN_TEXTURE;
				Identifier warningTexture = overIcon
						? WorldListWidget.WARNING_HIGHLIGHTED_TEXTURE
						: WorldListWidget.WARNING_TEXTURE;
				Identifier errorTexture = overIcon
						? WorldListWidget.ERROR_HIGHLIGHTED_TEXTURE
						: WorldListWidget.ERROR_TEXTURE;
				Identifier markedJoinTexture = overIcon
						? WorldListWidget.MARKED_JOIN_HIGHLIGHTED_TEXTURE
						: WorldListWidget.MARKED_JOIN_TEXTURE;
				if (level instanceof LevelSummary.SymlinkLevelSummary
						|| level instanceof LevelSummary.RecoveryWarning) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, errorTexture, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, markedJoinTexture, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
					return;
				}

				if (level.isLocked()) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, errorTexture, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
					if (overIcon) {
						context.drawTooltip(client.textRenderer.wrapLines(WorldListWidget.LOCKED_TEXT, 175), mouseX, mouseY);
					}
				}
				else if (level.requiresConversion()) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, errorTexture, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
					if (overIcon) {
						context.drawTooltip(client.textRenderer.wrapLines(WorldListWidget.CONVERSION_TOOLTIP, 175), mouseX, mouseY);
					}
				}
				else if (!level.isVersionAvailable()) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, errorTexture, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
					if (overIcon) {
						context.drawTooltip(
								client.textRenderer.wrapLines(WorldListWidget.INCOMPATIBLE_TOOLTIP, 175),
								mouseX,
								mouseY
						);
					}
				}
				else if (level.shouldPromptBackup()) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, markedJoinTexture, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
					if (level.wouldBeDowngraded()) {
						context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, errorTexture, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
						if (overIcon) {
							context.drawTooltip(
									ImmutableList.of(
											WorldListWidget.FROM_NEWER_VERSION_FIRST_LINE.asOrderedText(),
											WorldListWidget.FROM_NEWER_VERSION_SECOND_LINE.asOrderedText()
									),
									mouseX,
									mouseY
							);
						}
					}
					else if (!SharedConstants.getGameVersion().stable()) {
						context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, warningTexture, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
						if (overIcon) {
							context.drawTooltip(
									ImmutableList.of(
											WorldListWidget.SNAPSHOT_FIRST_LINE.asOrderedText(),
											WorldListWidget.SNAPSHOT_SECOND_LINE.asOrderedText()
									),
									mouseX,
									mouseY
							);
						}
					}

					if (overIcon) {
						WorldListWidget.this.setCursor(context);
					}
				}
				else {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, joinTexture, getContentX(), getContentY(), ICON_SIZE, ICON_SIZE);
					if (overIcon) {
						WorldListWidget.this.setCursor(context);
					}
				}
			}
		}

		private int getTextX() {
			return this.getContentX() + ICON_SIZE + 3;
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			if (!allowConfirmationByKeyboard()) {
				return super.mouseClicked(click, doubled);
			}

			int relX = (int) click.x() - getContentX();
			int relY = (int) click.y() - getContentY();
			if (doubled || isInside(relX, relY, ICON_SIZE)
					&& parent.worldListType == WorldListWidget.WorldListType.SINGLEPLAYER) {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				Consumer<WorldListWidget.WorldEntry> callback = parent.confirmationCallback;
				if (callback != null) {
					callback.accept(this);
					return true;
				}
			}

			return super.mouseClicked(click, doubled);
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (input.isEnterOrSpace() && allowConfirmationByKeyboard()) {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				Consumer<WorldListWidget.WorldEntry> callback = parent.confirmationCallback;
				if (callback != null) {
					callback.accept(this);
					return true;
				}
			}

			return super.keyPressed(input);
		}

		public boolean allowConfirmationByKeyboard() {
			return level.isSelectable() || parent.worldListType == WorldListWidget.WorldListType.UPLOAD_WORLD;
		}

		public void play() {
			if (!level.isSelectable()) {
				return;
			}

			if (level instanceof LevelSummary.SymlinkLevelSummary) {
				client.setScreen(SymlinkWarningScreen.world(() -> client.setScreen(screen)));
			}
			else {
				client.createIntegratedServerLoader().start(level.getName(), parent::refresh);
			}
		}

		public void deleteIfConfirmed() {
			this.client
					.setScreen(
							new ConfirmScreen(
									confirmed -> {
										if (confirmed) {
											this.client.setScreen(new ProgressScreen(true));
											this.delete();
										}

										this.parent.refresh();
									},
									Text.translatable("selectWorld.deleteQuestion"),
									Text.translatable("selectWorld.deleteWarning", this.level.getDisplayName()),
									Text.translatable("selectWorld.deleteButton"),
									ScreenTexts.CANCEL
							)
					);
		}

		public void delete() {
			LevelStorage levelStorage = this.client.getLevelStorage();
			String string = this.level.getName();

			try (LevelStorage.Session session = levelStorage.createSessionWithoutSymlinkCheck(string)) {
				session.deleteSessionLock();
			}
			catch (IOException var8) {
				SystemToast.addWorldDeleteFailureToast(this.client, string);
				WorldListWidget.LOGGER.error("Failed to delete world {}", string, var8);
			}
		}

		public void edit() {
			this.openReadingWorldScreen();
			String string = this.level.getName();

			LevelStorage.Session session;
			try {
				session = this.client.getLevelStorage().createSession(string);
			}
			catch (IOException var6) {
				SystemToast.addWorldAccessFailureToast(this.client, string);
				WorldListWidget.LOGGER.error("Failed to access level {}", string, var6);
				this.parent.load();
				return;
			}
			catch (SymlinkValidationException var7) {
				WorldListWidget.LOGGER.warn("{}", var7.getMessage());
				this.client.setScreen(SymlinkWarningScreen.world(() -> this.client.setScreen(this.screen)));
				return;
			}

			EditWorldScreen editWorldScreen;
			try {
				editWorldScreen = EditWorldScreen.create(
						this.client, session, edited -> {
							session.tryClose();
							this.parent.refresh();
						}
				);
			}
			catch (NbtException | NbtCrashException | IOException var5) {
				session.tryClose();
				SystemToast.addWorldAccessFailureToast(this.client, string);
				WorldListWidget.LOGGER.error("Failed to load world data {}", string, var5);
				this.parent.load();
				return;
			}

			this.client.setScreen(editWorldScreen);
		}

		public void recreate() {
			this.openReadingWorldScreen();

			try (LevelStorage.Session session = this.client.getLevelStorage().createSession(this.level.getName())) {
				Pair<LevelInfo, GeneratorOptionsHolder>
						pair =
						this.client.createIntegratedServerLoader().loadForRecreation(session);
				LevelInfo levelInfo = (LevelInfo) pair.getFirst();
				GeneratorOptionsHolder generatorOptionsHolder = (GeneratorOptionsHolder) pair.getSecond();
				Path path = CreateWorldScreen.copyDataPack(session.getDirectory(WorldSavePath.DATAPACKS), this.client);
				generatorOptionsHolder.initializeIndexedFeaturesLists();
				if (generatorOptionsHolder.generatorOptions().isLegacyCustomizedType()) {
					this.client
							.setScreen(
									new ConfirmScreen(
											confirmed -> this.client
													.setScreen(
															(Screen) (confirmed
															          ? CreateWorldScreen.create(
																	this.client,
																	this.parent::refresh,
																	levelInfo,
																	generatorOptionsHolder,
																	path
															)
															          : this.screen
															)
													),
											Text.translatable("selectWorld.recreate.customized.title"),
											Text.translatable("selectWorld.recreate.customized.text"),
											ScreenTexts.PROCEED,
											ScreenTexts.CANCEL
									)
							);
				}
				else {
					this.client.setScreen(CreateWorldScreen.create(
							this.client,
							this.parent::refresh,
							levelInfo,
							generatorOptionsHolder,
							path
					));
				}
			}
			catch (SymlinkValidationException var8) {
				WorldListWidget.LOGGER.warn("{}", var8.getMessage());
				this.client.setScreen(SymlinkWarningScreen.world(() -> this.client.setScreen(this.screen)));
			}
			catch (Exception var9) {
				WorldListWidget.LOGGER.error("Unable to recreate world", var9);
				this.client
						.setScreen(
								new NoticeScreen(
										() -> this.client.setScreen(this.screen),
										Text.translatable("selectWorld.recreate.error.title"),
										Text.translatable("selectWorld.recreate.error.text")
								)
						);
			}
		}

		private void openReadingWorldScreen() {
			this.client.setScreenAndRender(new MessageScreen(Text.translatable("selectWorld.data_read")));
		}

		private void loadIcon() {
			if (iconPath != null && Files.isRegularFile(iconPath)) {
				try (InputStream inputStream = Files.newInputStream(iconPath)) {
					icon.load(NativeImage.read(inputStream));
				}
				catch (Throwable error) {
					WorldListWidget.LOGGER.error("Invalid icon for world {}", level.getName(), error);
					iconPath = null;
				}
			}
			else {
				icon.destroy();
			}
		}

		@Override
		public void close() {
			if (!this.icon.isClosed()) {
				this.icon.close();
			}
		}

		public String getLevelDisplayName() {
			return this.level.getDisplayName();
		}

		@Override
		public LevelSummary getLevel() {
			return this.level;
		}
	}

	@Environment(EnvType.CLIENT)
	public enum WorldListType {
		SINGLEPLAYER,
		UPLOAD_WORLD
	}
}
