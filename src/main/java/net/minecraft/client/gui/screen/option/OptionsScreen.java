package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.GameOptions;
import net.minecraft.network.packet.c2s.play.UpdateDifficultyC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateDifficultyLockC2SPacket;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Главный экран настроек игры — содержит кнопки перехода ко всем подразделам настроек,
 * а также управление сложностью для одиночной игры.
 */
@Environment(EnvType.CLIENT)
public class OptionsScreen extends Screen {

	private static final Text TITLE_TEXT = Text.translatable("options.title");
	private static final Text SKIN_CUSTOMIZATION_TEXT = Text.translatable("options.skinCustomisation");
	private static final Text SOUNDS_TEXT = Text.translatable("options.sounds");
	private static final Text VIDEO_TEXT = Text.translatable("options.video");
	public static final Text CONTROL_TEXT = Text.translatable("options.controls");
	private static final Text LANGUAGE_TEXT = Text.translatable("options.language");
	private static final Text CHAT_TEXT = Text.translatable("options.chat");
	private static final Text RESOURCE_PACK_TEXT = Text.translatable("options.resourcepack");
	private static final Text ACCESSIBILITY_TEXT = Text.translatable("options.accessibility");
	private static final Text TELEMETRY_TEXT = Text.translatable("options.telemetry");
	private static final Tooltip TELEMETRY_DISABLED_TOOLTIP = Tooltip.of(Text.translatable("options.telemetry.disabled"));
	private static final Text CREDITS_AND_ATTRIBUTION_TEXT = Text.translatable("options.credits_and_attribution");

	private static final int DIFFICULTY_BUTTON_WIDTH = 150;
	private static final int DIFFICULTY_BUTTON_HEIGHT = 20;

	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, 61, 33);
	private final Screen parent;
	private final GameOptions settings;
	private @Nullable CyclingButtonWidget<Difficulty> difficultyButton;
	private @Nullable LockButtonWidget lockDifficultyButton;

	public OptionsScreen(Screen parent, GameOptions gameOptions) {
		super(TITLE_TEXT);
		this.parent = parent;
		settings = gameOptions;
	}

	@Override
	protected void init() {
		DirectionalLayoutWidget headerLayout = layout.addHeader(DirectionalLayoutWidget.vertical().spacing(8));
		headerLayout.add(new TextWidget(TITLE_TEXT, textRenderer), Positioner::alignHorizontalCenter);
		DirectionalLayoutWidget fovRow = headerLayout.add(DirectionalLayoutWidget.horizontal()).spacing(8);
		fovRow.add(settings.getFov().createWidget(client.options));
		fovRow.add(createTopRightButton());
		GridWidget gridWidget = new GridWidget();
		gridWidget.getMainPositioner().marginX(4).marginBottom(4).alignHorizontalCenter();
		GridWidget.Adder adder = gridWidget.createAdder(2);
		adder.add(createButton(SKIN_CUSTOMIZATION_TEXT, () -> new SkinOptionsScreen(this, settings)));
		adder.add(createButton(SOUNDS_TEXT, () -> new SoundOptionsScreen(this, settings)));
		adder.add(createButton(VIDEO_TEXT, () -> new VideoOptionsScreen(this, client, settings)));
		adder.add(createButton(CONTROL_TEXT, () -> new ControlsOptionsScreen(this, settings)));
		adder.add(createButton(LANGUAGE_TEXT, () -> new LanguageOptionsScreen(this, settings, client.getLanguageManager())));
		adder.add(createButton(CHAT_TEXT, () -> new ChatOptionsScreen(this, settings)));
		adder.add(createButton(
			RESOURCE_PACK_TEXT,
			() -> new PackScreen(
				client.getResourcePackManager(),
				this::refreshResourcePacks,
				client.getResourcePackDir(),
				Text.translatable("resourcePack.title")
			)
		));
		adder.add(createButton(ACCESSIBILITY_TEXT, () -> new AccessibilityOptionsScreen(this, settings)));
		ButtonWidget telemetryButton = adder.add(createButton(TELEMETRY_TEXT, () -> new TelemetryInfoScreen(this, settings)));
		if (!client.isTelemetryEnabledByApi()) {
			telemetryButton.active = false;
			telemetryButton.setTooltip(TELEMETRY_DISABLED_TOOLTIP);
		}

		adder.add(createButton(CREDITS_AND_ATTRIBUTION_TEXT, () -> new CreditsAndAttributionScreen(this)));
		layout.addBody(gridWidget);
		layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(200).build());
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private void refreshResourcePacks(ResourcePackManager resourcePackManager) {
		settings.refreshResourcePacks(resourcePackManager);
		client.setScreen(this);
	}

	private Widget createTopRightButton() {
		if (client.world == null || !client.isIntegratedServerRunning()) {
			return ButtonWidget.builder(
				Text.translatable("options.online"),
				button -> client.setScreen(new OnlineOptionsScreen(this, settings))
			).dimensions(width / 2 + 5, height / 6 - 12 + 24, DIFFICULTY_BUTTON_WIDTH, DIFFICULTY_BUTTON_HEIGHT).build();
		}

		difficultyButton = createDifficultyButtonWidget(0, 0, "options.difficulty", client);
		if (client.world.getLevelProperties().isHardcore()) {
			difficultyButton.active = false;
			return difficultyButton;
		}

		lockDifficultyButton = new LockButtonWidget(
			0, 0,
			button -> client.setScreen(new ConfirmScreen(
				this::lockDifficulty,
				Text.translatable("difficulty.lock.title"),
				Text.translatable(
					"difficulty.lock.question",
					client.world.getLevelProperties().getDifficulty().getTranslatableName()
				)
			))
		);
		difficultyButton.setWidth(difficultyButton.getWidth() - lockDifficultyButton.getWidth());
		lockDifficultyButton.setLocked(client.world.getLevelProperties().isDifficultyLocked());
		lockDifficultyButton.active = !lockDifficultyButton.isLocked();
		difficultyButton.active = !lockDifficultyButton.isLocked();
		AxisGridWidget difficultyRow = new AxisGridWidget(DIFFICULTY_BUTTON_WIDTH, 0, AxisGridWidget.DisplayAxis.HORIZONTAL);
		difficultyRow.add(difficultyButton);
		difficultyRow.add(lockDifficultyButton);
		return difficultyRow;
	}

	/**
	 * Создаёт кнопку выбора сложности с обработчиком отправки пакета на сервер.
	 */
	public static CyclingButtonWidget<Difficulty> createDifficultyButtonWidget(
		int x,
		int y,
		String translationKey,
		MinecraftClient client
	) {
		return CyclingButtonWidget.builder(Difficulty::getTranslatableName, client.world.getDifficulty())
			.values(Difficulty.values())
			.build(
				x, y,
				DIFFICULTY_BUTTON_WIDTH, DIFFICULTY_BUTTON_HEIGHT,
				Text.translatable(translationKey),
				(button, difficulty) -> client.getNetworkHandler().sendPacket(new UpdateDifficultyC2SPacket(difficulty))
			);
	}

	private void lockDifficulty(boolean difficultyLocked) {
		client.setScreen(this);
		if (!difficultyLocked || client.world == null || lockDifficultyButton == null || difficultyButton == null) {
			return;
		}

		client.getNetworkHandler().sendPacket(new UpdateDifficultyLockC2SPacket(true));
		lockDifficultyButton.setLocked(true);
		lockDifficultyButton.active = false;
		difficultyButton.active = false;
	}

	@Override
	public void removed() {
		settings.write();
	}

	private ButtonWidget createButton(Text message, Supplier<Screen> screenSupplier) {
		return ButtonWidget.builder(message, button -> client.setScreen(screenSupplier.get())).build();
	}
}
