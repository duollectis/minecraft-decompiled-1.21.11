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

@Environment(EnvType.CLIENT)
/**
 * {@code Screen}.
 */
public abstract class Screen extends AbstractParentElement implements Drawable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text SCREEN_USAGE_TEXT = Text.translatable("narrator.screen.usage");
	public static final Identifier MENU_BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/menu_background.png");
	public static final Identifier HEADER_SEPARATOR_TEXTURE = Identifier.ofVanilla("textures/gui/header_separator.png");
	public static final Identifier FOOTER_SEPARATOR_TEXTURE = Identifier.ofVanilla("textures/gui/footer_separator.png");
	private static final Identifier
			INWORLD_MENU_BACKGROUND_TEXTURE =
			Identifier.ofVanilla("textures/gui/inworld_menu_background.png");
	public static final Identifier
			INWORLD_HEADER_SEPARATOR_TEXTURE =
			Identifier.ofVanilla("textures/gui/inworld_header_separator.png");
	public static final Identifier
			INWORLD_FOOTER_SEPARATOR_TEXTURE =
			Identifier.ofVanilla("textures/gui/inworld_footer_separator.png");
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

	public final void renderWithTooltip(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.createNewRootLayer();
		this.renderBackground(context, mouseX, mouseY, deltaTicks);
		context.createNewRootLayer();
		this.render(context, mouseX, mouseY, deltaTicks);
		context.drawDeferredElements();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		for (Drawable drawable : this.drawables) {
			drawable.render(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEscape() && this.shouldCloseOnEsc()) {
			this.close();
			return true;
		}
		else if (super.keyPressed(input)) {
			return true;
		}
		else {
			GuiNavigation guiNavigation = (GuiNavigation) (switch (input.key()) {
				case 258 -> this.getTabNavigation(!input.hasShift());
				default -> null;
				case 262 -> this.getArrowNavigation(NavigationDirection.RIGHT);
				case 263 -> this.getArrowNavigation(NavigationDirection.LEFT);
				case 264 -> this.getArrowNavigation(NavigationDirection.DOWN);
				case 265 -> this.getArrowNavigation(NavigationDirection.UP);
			}
			);
			if (guiNavigation != null) {
				GuiNavigationPath guiNavigationPath = super.getNavigationPath(guiNavigation);
				if (guiNavigationPath == null && guiNavigation instanceof GuiNavigation.Tab) {
					this.blur();
					guiNavigationPath = super.getNavigationPath(guiNavigation);
				}

				if (guiNavigationPath != null) {
					this.switchFocus(guiNavigationPath);
				}
			}

			return false;
		}
	}

	private GuiNavigation.Tab getTabNavigation(boolean bl) {
		return new GuiNavigation.Tab(bl);
	}

	private GuiNavigation.Arrow getArrowNavigation(NavigationDirection direction) {
		return new GuiNavigation.Arrow(direction);
	}

	protected void setInitialFocus() {
		if (this.client.getNavigationType().isKeyboard()) {
			GuiNavigation.Tab tab = new GuiNavigation.Tab(true);
			GuiNavigationPath guiNavigationPath = super.getNavigationPath(tab);
			if (guiNavigationPath != null) {
				this.switchFocus(guiNavigationPath);
			}
		}
	}

	protected void setInitialFocus(Element element) {
		GuiNavigationPath
				guiNavigationPath =
				GuiNavigationPath.of(this, element.getNavigationPath(new GuiNavigation.Down()));
		if (guiNavigationPath != null) {
			this.switchFocus(guiNavigationPath);
		}
	}

	public void blur() {
		GuiNavigationPath guiNavigationPath = this.getFocusedPath();
		if (guiNavigationPath != null) {
			guiNavigationPath.setFocused(false);
		}
	}

	@VisibleForTesting
	protected void switchFocus(GuiNavigationPath path) {
		this.blur();
		path.setFocused(true);
	}

	public boolean shouldCloseOnEsc() {
		return true;
	}

	public void close() {
		this.client.setScreen(null);
	}

	protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
		this.drawables.add(drawableElement);
		return this.addSelectableChild(drawableElement);
	}

	protected <T extends Drawable> T addDrawable(T drawable) {
		this.drawables.add(drawable);
		return drawable;
	}

	protected <T extends Element & Selectable> T addSelectableChild(T child) {
		this.children.add(child);
		this.selectables.add(child);
		return child;
	}

	protected void remove(Element child) {
		if (child instanceof Drawable) {
			this.drawables.remove((Drawable) child);
		}

		if (child instanceof Selectable) {
			this.selectables.remove((Selectable) child);
		}

		if (this.getFocused() == child) {
			this.blur();
		}

		this.children.remove(child);
	}

	protected void clearChildren() {
		this.drawables.clear();
		this.children.clear();
		this.selectables.clear();
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
		ClientPlayerEntity clientPlayerEntity = Objects.requireNonNull(client.player, "Player not available");
		switch (clickEvent) {
			case ClickEvent.RunCommand(String var11):
				handleRunCommand(clientPlayerEntity, var11, screenAfterRun);
				break;
			case ClickEvent.ShowDialog showDialog:
				clientPlayerEntity.networkHandler.showDialog(showDialog.dialog(), screenAfterRun);
				break;
			case ClickEvent.Custom custom:
				clientPlayerEntity.networkHandler.sendPacket(new CustomClickActionC2SPacket(
						custom.id(),
						custom.payload()
				));
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
		boolean bl = switch (clickEvent) {
			case ClickEvent.OpenUrl(URI var17) -> {
				handleOpenUri(client, screenAfterRun, var17);
				yield false;
			}
			case ClickEvent.OpenFile openFile -> {
				Util.getOperatingSystem().open(openFile.file());
				yield true;
			}
			case ClickEvent.SuggestCommand(String var22) -> {
				String var18 = var22;
				if (screenAfterRun != null) {
					screenAfterRun.insertText(var18, true);
				}

				yield true;
			}
			case ClickEvent.CopyToClipboard(String var13) -> {
				client.keyboard.setClipboard(var13);
				yield true;
			}
			default -> {
				LOGGER.error("Don't know how to handle {}", clickEvent);
				yield true;
			}
		};
		if (bl && client.currentScreen != screenAfterRun) {
			client.setScreen(screenAfterRun);
		}
	}

	protected static boolean handleOpenUri(MinecraftClient client, @Nullable Screen screen, URI uri) {
		if (!client.options.getChatLinks().getValue()) {
			return false;
		}
		else {
			if (client.options.getChatLinksPrompt().getValue()) {
				client.setScreen(new ConfirmLinkScreen(
						confirmed -> {
							if (confirmed) {
								Util.getOperatingSystem().open(uri);
							}

							client.setScreen(screen);
						}, uri.toString(), false
				));
			}
			else {
				Util.getOperatingSystem().open(uri);
			}

			return true;
		}
	}

	protected static void handleRunCommand(ClientPlayerEntity player, String command, @Nullable Screen screenAfterRun) {
		player.networkHandler.runClickEventCommand(CommandManager.stripLeadingSlash(command), screenAfterRun);
	}

	public final void init(int width, int height) {
		this.width = width;
		this.height = height;
		if (!this.screenInitialized) {
			this.init();
			this.setInitialFocus();
		}
		else {
			this.refreshWidgetPositions();
		}

		this.screenInitialized = true;
		this.narrateScreenIfNarrationEnabled(false);
		if (this.client.getNavigationType().isKeyboard()) {
			this.setElementNarrationStartTime(Long.MAX_VALUE);
		}
		else {
			this.setElementNarrationDelay(SCREEN_INIT_NARRATION_DELAY);
		}
	}

	protected void clearAndInit() {
		this.clearChildren();
		this.blur();
		this.init();
		this.setInitialFocus();
	}

	protected void setWidgetAlpha(float alpha) {
		for (Element element : this.children()) {
			if (element instanceof ClickableWidget clickableWidget) {
				clickableWidget.setAlpha(alpha);
			}
		}
	}

	@Override
	public List<? extends Element> children() {
		return this.children;
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
		if (this.deferSubtitles()) {
			this.renderInGameBackground(context);
		}
		else {
			if (this.client.world == null) {
				this.renderPanoramaBackground(context, deltaTicks);
			}

			this.applyBlur(context);
			this.renderDarkening(context);
		}

		this.client.inGameHud.renderDeferredSubtitles();
	}

	protected void applyBlur(DrawContext context) {
		float f = this.client.options.getMenuBackgroundBlurrinessValue();
		if (f >= 1.0F) {
			context.applyBlur();
		}
	}

	protected void renderPanoramaBackground(DrawContext context, float deltaTicks) {
		this.client.gameRenderer
				.getRotatingPanoramaRenderer()
				.render(context, this.width, this.height, this.allowRotatingPanorama());
	}

	protected void renderDarkening(DrawContext context) {
		this.renderDarkening(context, 0, 0, this.width, this.height);
	}

	protected void renderDarkening(DrawContext context, int x, int y, int width, int height) {
		renderBackgroundTexture(
				context,
				this.client.world == null ? MENU_BACKGROUND_TEXTURE : INWORLD_MENU_BACKGROUND_TEXTURE,
				x,
				y,
				0.0F,
				0.0F,
				width,
				height
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
		int i = 32;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 32, 32);
	}

	public void renderInGameBackground(DrawContext context) {
		context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
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
		return this.shouldPause();
	}

	protected void refreshWidgetPositions() {
		this.clearAndInit();
	}

	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		this.refreshWidgetPositions();
	}

	public void addCrashReportSection(CrashReport report) {
		CrashReportSection crashReportSection = report.addElement("Affected screen", 1);
		crashReportSection.add("Screen name", () -> this.getClass().getCanonicalName());
	}

	protected boolean isValidCharacterForName(String name, int codepoint, int cursorPos) {
		int i = name.indexOf(58);
		int j = name.indexOf(47);
		if (codepoint == 58) {
			return (j == -1 || cursorPos <= j) && i == -1;
		}
		else {
			return codepoint == 47
			       ? cursorPos > i
			       : codepoint == 95 || codepoint == 45 || codepoint >= 97 && codepoint <= 122
			         || codepoint >= 48 && codepoint <= 57 || codepoint == 46;
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return true;
	}

	public void onFilesDropped(List<Path> paths) {
	}

	private void setScreenNarrationDelay(long delayMs, boolean restartElementNarration) {
		this.screenNarrationStartTime = Util.getMeasuringTimeMs() + delayMs;
		if (restartElementNarration) {
			this.elementNarrationStartTime = Long.MIN_VALUE;
		}
	}

	private void setElementNarrationDelay(long delayMs) {
		this.setElementNarrationStartTime(Util.getMeasuringTimeMs() + delayMs);
	}

	private void setElementNarrationStartTime(long startTimeMs) {
		this.elementNarrationStartTime = startTimeMs;
	}

	public void applyMouseMoveNarratorDelay() {
		this.setScreenNarrationDelay(750L, false);
	}

	public void applyMousePressScrollNarratorDelay() {
		this.setScreenNarrationDelay(200L, true);
	}

	public void applyKeyPressNarratorDelay() {
		this.setScreenNarrationDelay(200L, true);
	}

	private boolean isNarratorActive() {
		return SharedConstants.UI_NARRATION || this.client.getNarratorManager().isActive();
	}

	public void updateNarrator() {
		if (this.isNarratorActive()) {
			long l = Util.getMeasuringTimeMs();
			if (l > this.screenNarrationStartTime && l > this.elementNarrationStartTime) {
				this.narrateScreen(true);
				this.screenNarrationStartTime = Long.MAX_VALUE;
			}
		}
	}

	public void narrateScreenIfNarrationEnabled(boolean onlyChangedNarrations) {
		if (this.isNarratorActive()) {
			this.narrateScreen(onlyChangedNarrations);
		}
	}

	private void narrateScreen(boolean onlyChangedNarrations) {
		this.narrator.buildNarrations(this::addScreenNarrations);
		String string = this.narrator.buildNarratorText(!onlyChangedNarrations);
		if (!string.isEmpty()) {
			this.client.getNarratorManager().narrateSystemImmediately(string);
		}
	}

	protected boolean hasUsageText() {
		return true;
	}

	protected void addScreenNarrations(NarrationMessageBuilder messageBuilder) {
		messageBuilder.put(NarrationPart.TITLE, this.getNarratedTitle());
		if (this.hasUsageText()) {
			messageBuilder.put(NarrationPart.USAGE, SCREEN_USAGE_TEXT);
		}

		this.addElementNarrations(messageBuilder);
	}

	protected void addElementNarrations(NarrationMessageBuilder builder) {
		List<? extends Selectable> list = this.selectables
				.stream()
				.flatMap(selectable -> selectable.getNarratedParts().stream())
				.filter(Selectable::isInteractable)
				.sorted(Comparator.comparingInt(Navigable::getNavigationOrder))
				.toList();
		Screen.SelectedElementNarrationData selectedElementNarrationData = findSelectedElementData(list, this.selected);
		if (selectedElementNarrationData != null) {
			if (selectedElementNarrationData.selectType.isFocused()) {
				this.selected = selectedElementNarrationData.selectable;
			}

			if (list.size() > 1) {
				builder.put(
						NarrationPart.POSITION,
						Text.translatable(
								"narrator.position.screen",
								selectedElementNarrationData.index + 1,
								list.size()
						)
				);
				if (selectedElementNarrationData.selectType == Selectable.SelectionType.FOCUSED) {
					builder.put(NarrationPart.USAGE, this.getUsageNarrationText());
				}
			}

			selectedElementNarrationData.selectable.appendNarrations(builder.nextMessage());
		}
	}

	protected Text getUsageNarrationText() {
		return Text.translatable("narration.component_list.usage");
	}

	public static Screen.@Nullable SelectedElementNarrationData findSelectedElementData(
			List<? extends Selectable> selectables,
			@Nullable Selectable selectable
	) {
		Screen.SelectedElementNarrationData selectedElementNarrationData = null;
		Screen.SelectedElementNarrationData selectedElementNarrationData2 = null;
		int i = 0;

		for (int j = selectables.size(); i < j; i++) {
			Selectable selectable2 = selectables.get(i);
			Selectable.SelectionType selectionType = selectable2.getType();
			if (selectionType.isFocused()) {
				if (selectable2 != selectable) {
					return new Screen.SelectedElementNarrationData(selectable2, i, selectionType);
				}

				selectedElementNarrationData2 = new Screen.SelectedElementNarrationData(selectable2, i, selectionType);
			}
			else if (selectionType.compareTo(
					selectedElementNarrationData != null ? selectedElementNarrationData.selectType
					                                     : Selectable.SelectionType.NONE) > 0
			) {
				selectedElementNarrationData = new Screen.SelectedElementNarrationData(selectable2, i, selectionType);
			}
		}

		return selectedElementNarrationData != null ? selectedElementNarrationData : selectedElementNarrationData2;
	}

	public void refreshNarrator(boolean previouslyDisabled) {
		if (previouslyDisabled) {
			this.setScreenNarrationDelay(NARRATOR_MODE_CHANGE_DELAY, false);
		}

		if (this.narratorToggleButton != null) {
			this.narratorToggleButton.setValue(this.client.options.getNarrator().getValue());
		}
	}

	public TextRenderer getTextRenderer() {
		return this.textRenderer;
	}

	public boolean showsStatusEffects() {
		return false;
	}

	public boolean canInterruptOtherScreen() {
		return this.shouldCloseOnEsc();
	}

	@Override
	public ScreenRect getNavigationFocus() {
		return new ScreenRect(0, 0, this.width, this.height);
	}

	public @Nullable MusicSound getMusic() {
		return null;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SelectedElementNarrationData}.
	 */
	public record SelectedElementNarrationData(Selectable selectable, int index, Selectable.SelectionType selectType) {
	}
}
