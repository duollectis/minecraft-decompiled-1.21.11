package net.minecraft.client.gui.screen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.navigation.Navigable;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.screen.narration.ScreenNarrator;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.NarratorMode;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.sound.MusicSound;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Базовый абстрактный класс всех экранов GUI клиента Minecraft.
 * Управляет жизненным циклом экрана: инициализацией, рендерингом, навигацией и нарратором.
 */
@Environment(EnvType.CLIENT)
public abstract class Screen extends AbstractParentElement implements Drawable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text SCREEN_USAGE_TEXT = Text.translatable("narrator.screen.usage");
	private static final int CHAR_COLON = 58;
	private static final int CHAR_SLASH = 47;
	private static final int CHAR_UNDERSCORE = 95;
	private static final int CHAR_DASH = 45;
	private static final int CHAR_DOT = 46;
	private static final int CHAR_LOWERCASE_A = 97;
	private static final int CHAR_LOWERCASE_Z = 122;
	private static final int CHAR_DIGIT_0 = 48;
	private static final int CHAR_DIGIT_9 = 57;
	public static final Identifier MENU_BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/menu_background.png");
	public static final Identifier HEADER_SEPARATOR_TEXTURE = Identifier.ofVanilla("textures/gui/header_separator.png");
	public static final Identifier FOOTER_SEPARATOR_TEXTURE = Identifier.ofVanilla("textures/gui/footer_separator.png");
	private static final Identifier INWORLD_MENU_BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/inworld_menu_background.png");
	public static final Identifier INWORLD_HEADER_SEPARATOR_TEXTURE = Identifier.ofVanilla("textures/gui/inworld_header_separator.png");
	public static final Identifier INWORLD_FOOTER_SEPARATOR_TEXTURE = Identifier.ofVanilla("textures/gui/inworld_footer_separator.png");
	private static final int BACKGROUND_TEXTURE_SIZE = 32;
	protected static final float TOOLTIP_FADE_DURATION_MS = 2000.0F;
	protected final Text title;
	private final List<Element> children = Lists.newArrayList();
	private final List<Selectable> selectables = Lists.newArrayList();
	protected final MinecraftClient client;
	private boolean screenInitialized;
	public int width;
	public int height;
	private final List<Drawable> drawables = Lists.newArrayList();
	protected final TextRenderer textRenderer;
	private static final long SCREEN_INIT_NARRATION_DELAY = TimeUnit.SECONDS.toMillis(2L);
	private static final long NARRATOR_MODE_CHANGE_DELAY = SCREEN_INIT_NARRATION_DELAY;
	private static final long MOUSE_MOVE_NARRATION_DELAY = 750L;
	private static final long MOUSE_PRESS_SCROLL_NARRATION_DELAY = 200L;
	private static final long KEY_PRESS_NARRATION_DELAY = 200L;
	private final ScreenNarrator narrator = new ScreenNarrator();
	private long elementNarrationStartTime = Long.MIN_VALUE;
	private long screenNarrationStartTime = Long.MAX_VALUE;
	protected @Nullable CyclingButtonWidget<NarratorMode> narratorToggleButton;
	private @Nullable Selectable selected;
	protected final Executor executor;

	protected Screen(Text title) {
		this(MinecraftClient.getInstance(), MinecraftClient.getInstance().textRenderer, title);
	}

	protected Screen(MinecraftClient minecraftClient, TextRenderer textRenderer, Text text) {
		this.client = minecraftClient;
		this.textRenderer = textRenderer;
		this.title = text;
		this.executor = runnable -> minecraftClient.execute(() -> {
			if (minecraftClient.currentScreen == this) {
				runnable.run();
			}
		});
	}

	public Text getTitle() {
		return this.title;
	}

	public Text getNarratedTitle() {
		return this.getTitle();
	}

	/**
	 * Выполняет полный цикл рендеринга экрана: фон, содержимое и отложенные элементы (тултипы).
	 * Каждый этап изолирован в отдельном корневом слое GUI.
	 */
	public final void renderWithTooltip(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.createNewRootLayer();
		renderBackground(context, mouseX, mouseY, deltaTicks);
		context.createNewRootLayer();
		render(context, mouseX, mouseY, deltaTicks);
		context.drawDeferredElements();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		for (Drawable drawable : drawables) {
			drawable.render(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEscape() && shouldCloseOnEsc()) {
			close();
			return true;
		}

		if (super.keyPressed(input)) {
			return true;
		}

		GuiNavigation navigation = (GuiNavigation) (switch (input.key()) {
			case 258 -> getTabNavigation(!input.hasShift());
			case 262 -> getArrowNavigation(NavigationDirection.RIGHT);
			case 263 -> getArrowNavigation(NavigationDirection.LEFT);
			case 264 -> getArrowNavigation(NavigationDirection.DOWN);
			case 265 -> getArrowNavigation(NavigationDirection.UP);
			default -> null;
		});

		if (navigation != null) {
			GuiNavigationPath path = super.getNavigationPath(navigation);

			if (path == null && navigation instanceof GuiNavigation.Tab) {
				blur();
				path = super.getNavigationPath(navigation);
			}

			if (path != null) {
				switchFocus(path);
			}
		}

		return false;
	}

	private GuiNavigation.Tab getTabNavigation(boolean forward) {
		return new GuiNavigation.Tab(forward);
	}

	private GuiNavigation.Arrow getArrowNavigation(NavigationDirection direction) {
		return new GuiNavigation.Arrow(direction);
	}

	protected void setInitialFocus() {
		if (!client.getNavigationType().isKeyboard()) {
			return;
		}

		GuiNavigationPath path = super.getNavigationPath(new GuiNavigation.Tab(true));

		if (path != null) {
			switchFocus(path);
		}
	}

	protected void setInitialFocus(Element element) {
		GuiNavigationPath path = GuiNavigationPath.of(this, element.getNavigationPath(new GuiNavigation.Down()));

		if (path != null) {
			switchFocus(path);
		}
	}

	public void blur() {
		GuiNavigationPath path = getFocusedPath();

		if (path != null) {
			path.setFocused(false);
		}
	}

	@VisibleForTesting
	protected void switchFocus(GuiNavigationPath path) {
		blur();
		path.setFocused(true);
	}

	public boolean shouldCloseOnEsc() {
		return true;
	}

	public void close() {
		client.setScreen(null);
	}

	protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
		drawables.add(drawableElement);
		return addSelectableChild(drawableElement);
	}

	protected <T extends Drawable> T addDrawable(T drawable) {
		drawables.add(drawable);
		return drawable;
	}

	protected <T extends Element & Selectable> T addSelectableChild(T child) {
		children.add(child);
		selectables.add(child);
		return child;
	}

	protected void remove(Element child) {
		if (child instanceof Drawable drawable) {
			drawables.remove(drawable);
		}

		if (child instanceof Selectable selectable) {
			selectables.remove(selectable);
		}

		if (getFocused() == child) {
			blur();
		}

		children.remove(child);
	}

	protected void clearChildren() {
		drawables.clear();
		children.clear();
		selectables.clear();
	}

	public static List<Text> getTooltipFromItem(MinecraftClient client, ItemStack stack) {
		return stack.getTooltip(
			Item.TooltipContext.create(client.world),
			client.player,
			client.options.advancedItemTooltips ? TooltipType.Default.ADVANCED : TooltipType.Default.BASIC
		);
	}

	protected void insertText(String text, boolean override) {
	}

	protected static void handleClickEvent(
		ClickEvent clickEvent,
		MinecraftClient client,
		@Nullable Screen screenAfterRun
	) {
		ClientPlayerEntity player = Objects.requireNonNull(client.player, "Player not available");

		switch (clickEvent) {
			case ClickEvent.RunCommand(String command):
				handleRunCommand(player, command, screenAfterRun);
				break;
			case ClickEvent.ShowDialog showDialog:
				player.networkHandler.showDialog(showDialog.dialog(), screenAfterRun);
				break;
			case ClickEvent.Custom custom:
				player.networkHandler.sendPacket(new CustomClickActionC2SPacket(custom.id(), custom.payload()));

				if (client.currentScreen != screenAfterRun) {
					client.setScreen(screenAfterRun);
				}

				break;
			default:
				handleBasicClickEvent(clickEvent, client, screenAfterRun);
		}
	}

	protected static void handleBasicClickEvent(
		ClickEvent clickEvent,
		MinecraftClient client,
		@Nullable Screen screenAfterRun
	) {
		boolean shouldNavigate = switch (clickEvent) {
			case ClickEvent.OpenUrl(URI uri) -> {
				handleOpenUri(client, screenAfterRun, uri);
				yield false;
			}
			case ClickEvent.OpenFile openFile -> {
				Util.getOperatingSystem().open(openFile.file());
				yield true;
			}
			case ClickEvent.SuggestCommand(String command) -> {
				if (screenAfterRun != null) {
					screenAfterRun.insertText(command, true);
				}

				yield true;
			}
			case ClickEvent.CopyToClipboard(String text) -> {
				client.keyboard.setClipboard(text);
				yield true;
			}
			default -> {
				LOGGER.error("Don't know how to handle {}", clickEvent);
				yield true;
			}
		};

		if (shouldNavigate && client.currentScreen != screenAfterRun) {
			client.setScreen(screenAfterRun);
		}
	}

	/**
	 * Открывает URI с учётом настроек чата: если включён промпт — показывает экран подтверждения,
	 * иначе открывает напрямую. Возвращает {@code false} если ссылки отключены в настройках.
	 */
	protected static boolean handleOpenUri(MinecraftClient client, @Nullable Screen screen, URI uri) {
		if (!client.options.getChatLinks().getValue()) {
			return false;
		}

		if (client.options.getChatLinksPrompt().getValue()) {
			client.setScreen(new ConfirmLinkScreen(
				confirmed -> {
					if (confirmed) {
						Util.getOperatingSystem().open(uri);
					}

					client.setScreen(screen);
				}, uri.toString(), false
			));
		} else {
			Util.getOperatingSystem().open(uri);
		}

		return true;
	}

	protected static void handleRunCommand(ClientPlayerEntity player, String command, @Nullable Screen screenAfterRun) {
		player.networkHandler.runClickEventCommand(CommandManager.stripLeadingSlash(command), screenAfterRun);
	}

	public final void init(int width, int height) {
		this.width = width;
		this.height = height;

		if (!screenInitialized) {
			init();
			setInitialFocus();
		} else {
			refreshWidgetPositions();
		}

		screenInitialized = true;
		narrateScreenIfNarrationEnabled(false);

		if (client.getNavigationType().isKeyboard()) {
			setElementNarrationStartTime(Long.MAX_VALUE);
		} else {
			setElementNarrationDelay(SCREEN_INIT_NARRATION_DELAY);
		}
	}

	protected void clearAndInit() {
		clearChildren();
		blur();
		init();
		setInitialFocus();
	}

	protected void setWidgetAlpha(float alpha) {
		for (Element element : children()) {
			if (element instanceof ClickableWidget widget) {
				widget.setAlpha(alpha);
			}
		}
	}

	@Override
	public List<? extends Element> children() {
		return children;
	}

	protected void init() {
	}

	public void tick() {
	}

	public void removed() {
	}

	public void onDisplayed() {
	}

	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (deferSubtitles()) {
			renderInGameBackground(context);
		} else {
			if (client.world == null) {
				renderPanoramaBackground(context, deltaTicks);
			}

			applyBlur(context);
			renderDarkening(context);
		}

		client.inGameHud.renderDeferredSubtitles();
	}

	protected void applyBlur(DrawContext context) {
		if (client.options.getMenuBackgroundBlurrinessValue() >= 1.0F) {
			context.applyBlur();
		}
	}

	protected void renderPanoramaBackground(DrawContext context, float deltaTicks) {
		client.gameRenderer
			.getRotatingPanoramaRenderer()
			.render(context, width, height, allowRotatingPanorama());
	}

	protected void renderDarkening(DrawContext context) {
		renderDarkening(context, 0, 0, width, height);
	}

	protected void renderDarkening(DrawContext context, int x, int y, int width, int height) {
		renderBackgroundTexture(
			context,
			client.world == null ? MENU_BACKGROUND_TEXTURE : INWORLD_MENU_BACKGROUND_TEXTURE,
			x, y, 0.0F, 0.0F, width, height
		);
	}

	public static void renderBackgroundTexture(
		DrawContext context,
		Identifier texture,
		int x,
		int y,
		float u,
		float v,
		int width,
		int height
	) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);
	}

	public void renderInGameBackground(DrawContext context) {
		context.fillGradient(0, 0, width, height, -1072689136, -804253680);
	}

	public boolean shouldPause() {
		return true;
	}

	public boolean deferSubtitles() {
		return false;
	}

	protected boolean allowRotatingPanorama() {
		return true;
	}

	public boolean keepOpenThroughPortal() {
		return shouldPause();
	}

	protected void refreshWidgetPositions() {
		clearAndInit();
	}

	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		refreshWidgetPositions();
	}

	public void addCrashReportSection(CrashReport report) {
		CrashReportSection section = report.addElement("Affected screen", 1);
		section.add("Screen name", () -> getClass().getCanonicalName());
	}

	protected boolean isValidCharacterForName(String name, int codepoint, int cursorPos) {
		int colonPos = name.indexOf(CHAR_COLON);
		int slashPos = name.indexOf(CHAR_SLASH);

		if (codepoint == CHAR_COLON) {
			return (slashPos == -1 || cursorPos <= slashPos) && colonPos == -1;
		}

		return codepoint == CHAR_SLASH
			? cursorPos > colonPos
			: codepoint == CHAR_UNDERSCORE
				|| codepoint == CHAR_DASH
				|| codepoint >= CHAR_LOWERCASE_A && codepoint <= CHAR_LOWERCASE_Z
				|| codepoint >= CHAR_DIGIT_0 && codepoint <= CHAR_DIGIT_9
				|| codepoint == CHAR_DOT;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return true;
	}

	public void onFilesDropped(List<Path> paths) {
	}

	private void setScreenNarrationDelay(long delayMs, boolean restartElementNarration) {
		screenNarrationStartTime = Util.getMeasuringTimeMs() + delayMs;
		if (restartElementNarration) {
			elementNarrationStartTime = Long.MIN_VALUE;
		}
	}

	private void setElementNarrationDelay(long delayMs) {
		setElementNarrationStartTime(Util.getMeasuringTimeMs() + delayMs);
	}

	private void setElementNarrationStartTime(long startTimeMs) {
		elementNarrationStartTime = startTimeMs;
	}

	public void applyMouseMoveNarratorDelay() {
		setScreenNarrationDelay(MOUSE_MOVE_NARRATION_DELAY, false);
	}

	public void applyMousePressScrollNarratorDelay() {
		setScreenNarrationDelay(MOUSE_PRESS_SCROLL_NARRATION_DELAY, true);
	}

	public void applyKeyPressNarratorDelay() {
		setScreenNarrationDelay(MOUSE_PRESS_SCROLL_NARRATION_DELAY, true);
	}

	private boolean isNarratorActive() {
		return SharedConstants.UI_NARRATION || client.getNarratorManager().isActive();
	}

	public void updateNarrator() {
		if (isNarratorActive()) {
			long now = Util.getMeasuringTimeMs();
			if (now > screenNarrationStartTime && now > elementNarrationStartTime) {
				narrateScreen(true);
				screenNarrationStartTime = Long.MAX_VALUE;
			}
		}
	}

	public void narrateScreenIfNarrationEnabled(boolean onlyChangedNarrations) {
		if (isNarratorActive()) {
			narrateScreen(onlyChangedNarrations);
		}
	}

	private void narrateScreen(boolean onlyChangedNarrations) {
		narrator.buildNarrations(this::addScreenNarrations);
		String text = narrator.buildNarratorText(!onlyChangedNarrations);
		if (!text.isEmpty()) {
			client.getNarratorManager().narrateSystemImmediately(text);
		}
	}

	protected boolean hasUsageText() {
		return true;
	}

	protected void addScreenNarrations(NarrationMessageBuilder messageBuilder) {
		messageBuilder.put(NarrationPart.TITLE, getNarratedTitle());
		if (hasUsageText()) {
			messageBuilder.put(NarrationPart.USAGE, SCREEN_USAGE_TEXT);
		}

		addElementNarrations(messageBuilder);
	}

	protected void addElementNarrations(NarrationMessageBuilder builder) {
		List<? extends Selectable> interactables = selectables
			.stream()
			.flatMap(selectable -> selectable.getNarratedParts().stream())
			.filter(Selectable::isInteractable)
			.sorted(Comparator.comparingInt(Navigable::getNavigationOrder))
			.toList();

		SelectedElementNarrationData narrationData = findSelectedElementData(interactables, selected);
		if (narrationData == null) {
			return;
		}

		if (narrationData.selectType.isFocused()) {
			selected = narrationData.selectable;
		}

		if (interactables.size() > 1) {
			builder.put(
				NarrationPart.POSITION,
				Text.translatable(
					"narrator.position.screen",
					narrationData.index + 1,
					interactables.size()
				)
			);
			if (narrationData.selectType == Selectable.SelectionType.FOCUSED) {
				builder.put(NarrationPart.USAGE, getUsageNarrationText());
			}
		}

		narrationData.selectable.appendNarrations(builder.nextMessage());
	}

	protected Text getUsageNarrationText() {
		return Text.translatable("narration.component_list.usage");
	}

	/**
	 * Находит данные нарратора для выбранного элемента из списка.
	 * Приоритет: сфокусированный новый элемент → сфокусированный текущий → элемент с наибольшим типом выделения.
	 */
	public static Screen.@Nullable SelectedElementNarrationData findSelectedElementData(
		List<? extends Selectable> selectables,
		@Nullable Selectable currentSelected
	) {
		SelectedElementNarrationData bestNonFocused = null;
		SelectedElementNarrationData currentFocused = null;
		int size = selectables.size();

		for (int index = 0; index < size; index++) {
			Selectable candidate = selectables.get(index);
			Selectable.SelectionType selectionType = candidate.getType();

			if (selectionType.isFocused()) {
				if (candidate != currentSelected) {
					return new SelectedElementNarrationData(candidate, index, selectionType);
				}

				currentFocused = new SelectedElementNarrationData(candidate, index, selectionType);
			} else if (selectionType.compareTo(
				bestNonFocused != null ? bestNonFocused.selectType : Selectable.SelectionType.NONE
			) > 0) {
				bestNonFocused = new SelectedElementNarrationData(candidate, index, selectionType);
			}
		}

		return bestNonFocused != null ? bestNonFocused : currentFocused;
	}

	public void refreshNarrator(boolean previouslyDisabled) {
		if (previouslyDisabled) {
			setScreenNarrationDelay(NARRATOR_MODE_CHANGE_DELAY, false);
		}

		if (narratorToggleButton != null) {
			narratorToggleButton.setValue(client.options.getNarrator().getValue());
		}
	}

	public TextRenderer getTextRenderer() {
		return textRenderer;
	}

	public boolean showsStatusEffects() {
		return false;
	}

	public boolean canInterruptOtherScreen() {
		return shouldCloseOnEsc();
	}

	@Override
	public ScreenRect getNavigationFocus() {
		return new ScreenRect(0, 0, width, height);
	}

	public @Nullable MusicSound getMusic() {
		return null;
	}

	/**
	 * Данные нарратора для выбранного элемента экрана.
	 */
	@Environment(EnvType.CLIENT)
	public record SelectedElementNarrationData(Selectable selectable, int index, Selectable.SelectionType selectType) {
	}
}
