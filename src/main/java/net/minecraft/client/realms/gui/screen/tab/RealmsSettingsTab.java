package net.minecraft.client.realms.gui.screen.tab;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.realms.ServiceQuality;
import net.minecraft.client.realms.dto.RealmsRegion;
import net.minecraft.client.realms.dto.RealmsRegionSelectionPreference;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.RegionSelectionMethod;
import net.minecraft.client.realms.gui.RealmsPopups;
import net.minecraft.client.realms.gui.screen.RealmsConfigureWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsRegionPreferenceScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Вкладка настроек Realms-сервера: имя мира, описание и предпочтение региона.
 * Содержит кнопку переключения состояния сервера (открыт/закрыт).
 * При выгрузке вкладки автоматически сохраняет изменённые настройки.
 */
@Environment(EnvType.CLIENT)
public class RealmsSettingsTab extends GridScreenTab implements RealmsUpdatableTab {

	private static final int TEXT_FIELD_WIDTH = 212;
	private static final int SPACING = 2;
	private static final int ROW_SPACING = 6;
	private static final int COLOR_INVALID_NAME = -2142128;
	private static final int COLOR_VALID_NAME = -2039584;

	public static final Text TITLE_TEXT = Text.translatable("mco.configure.world.settings.title");
	private static final Text WORLD_NAME_TEXT = Text.translatable("mco.configure.world.name");
	private static final Text DESCRIPTION_TEXT = Text.translatable("mco.configure.world.description");
	private static final Text REGION_PREFERENCE_TEXT = Text.translatable("mco.configure.world.region_preference");
	private static final Tooltip WHITESPACE_TOOLTIP = Tooltip.of(
		Text.translatable("mco.configure.world.name.validation.whitespace")
	);

	private final RealmsConfigureWorldScreen screen;
	private final MinecraftClient client;
	private RealmsServer server;
	private final Map<RealmsRegion, ServiceQuality> availableRegions;
	final ButtonWidget switchStateButton;
	private final TextFieldWidget descriptionTextField;
	private final TextFieldWidget worldNameTextField;
	private final TextWidget regionText;
	private final IconWidget serviceQualityIcon;
	private RealmsSettingsTab.Region region;

	public RealmsSettingsTab(
		RealmsConfigureWorldScreen screen,
		MinecraftClient client,
		RealmsServer realmsServer,
		Map<RealmsRegion, ServiceQuality> availableRegions
	) {
		super(TITLE_TEXT);
		this.screen = screen;
		this.client = client;
		server = realmsServer;
		this.availableRegions = availableRegions;

		GridWidget.Adder adder = grid.setRowSpacing(ROW_SPACING).createAdder(1);

		adder.add(new TextWidget(WORLD_NAME_TEXT, screen.getTextRenderer()));
		worldNameTextField = new TextFieldWidget(
			client.textRenderer,
			0,
			0,
			TEXT_FIELD_WIDTH,
			20,
			Text.translatable("mco.configure.world.name")
		);
		worldNameTextField.setMaxLength(32);
		worldNameTextField.setChangedListener(value -> {
			if (!isWorldNameValid()) {
				worldNameTextField.setEditableColor(COLOR_INVALID_NAME);
				worldNameTextField.setTooltip(WHITESPACE_TOOLTIP);
			} else {
				worldNameTextField.setTooltip(null);
				worldNameTextField.setEditableColor(COLOR_VALID_NAME);
			}
		});
		adder.add(worldNameTextField);
		adder.add(EmptyWidget.ofHeight(SPACING));

		adder.add(new TextWidget(DESCRIPTION_TEXT, screen.getTextRenderer()));
		descriptionTextField = new TextFieldWidget(
			client.textRenderer,
			0,
			0,
			TEXT_FIELD_WIDTH,
			20,
			Text.translatable("mco.configure.world.description")
		);
		descriptionTextField.setMaxLength(32);
		adder.add(descriptionTextField);
		adder.add(EmptyWidget.ofHeight(SPACING));

		adder.add(new TextWidget(REGION_PREFERENCE_TEXT, screen.getTextRenderer()));
		AxisGridWidget regionRow = new AxisGridWidget(0, 0, TEXT_FIELD_WIDTH, 9, AxisGridWidget.DisplayAxis.HORIZONTAL);
		regionText = regionRow.add(new TextWidget(192, 9, Text.empty(), screen.getTextRenderer()));
		serviceQualityIcon = regionRow.add(IconWidget.create(10, 8, ServiceQuality.UNKNOWN.getIcon()));
		adder.add(regionRow);
		adder.add(
			ButtonWidget.builder(
				Text.translatable("mco.configure.world.buttons.region_preference"),
				button -> showRegionPreferenceScreen()
			)
			.dimensions(0, 0, TEXT_FIELD_WIDTH, 20)
			.build()
		);
		adder.add(EmptyWidget.ofHeight(SPACING));

		switchStateButton = adder.add(
			ButtonWidget.builder(
				Text.empty(),
				button -> {
					if (realmsServer.state == RealmsServer.State.OPEN) {
						client.setScreen(
							RealmsPopups.createCustomPopup(
								screen,
								Text.translatable("mco.configure.world.close.question.title"),
								Text.translatable("mco.configure.world.close.question.line1"),
								popupScreen -> {
									saveSettings();
									screen.closeTheWorld();
								}
							)
						);
					} else {
						saveSettings();
						screen.openTheWorld(false);
					}
				}
			)
			.dimensions(0, 0, TEXT_FIELD_WIDTH, 20)
			.build()
		);
		switchStateButton.active = false;

		update(realmsServer);
	}

	private static MutableText getRegionText(RealmsSettingsTab.Region region) {
		boolean isManualWithRegion = region.preference().equals(RegionSelectionMethod.MANUAL)
			&& region.region() != null;

		return (isManualWithRegion
			? Text.translatable(region.region().translationKey)
			: Text.translatable(region.preference().translationKey)
		).formatted(Formatting.GRAY);
	}

	private static Identifier getQualityIcon(
		RealmsSettingsTab.Region region,
		Map<RealmsRegion, ServiceQuality> qualityByRegion
	) {
		if (region.region() == null || !qualityByRegion.containsKey(region.region())) {
			return ServiceQuality.UNKNOWN.getIcon();
		}

		return qualityByRegion.getOrDefault(region.region(), ServiceQuality.UNKNOWN).getIcon();
	}

	private boolean isWorldNameValid() {
		String name = worldNameTextField.getText();
		String trimmed = name.trim();
		return !trimmed.isEmpty() && name.length() == trimmed.length();
	}

	private void showRegionPreferenceScreen() {
		client.setScreen(new RealmsRegionPreferenceScreen(
			screen,
			this::onRegionChanged,
			availableRegions,
			region
		));
	}

	private void onRegionChanged(RegionSelectionMethod selectionMethod, RealmsRegion selectedRegion) {
		region = new RealmsSettingsTab.Region(selectionMethod, selectedRegion);
		refreshRegionText();
	}

	private void refreshRegionText() {
		regionText.setMessage(getRegionText(region));
		serviceQualityIcon.setTexture(getQualityIcon(region, availableRegions));
		serviceQualityIcon.visible = region.preference == RegionSelectionMethod.MANUAL;
	}

	@Override
	public void onLoaded(RealmsServer updatedServer) {
		update(updatedServer);
	}

	@Override
	public void update(RealmsServer updatedServer) {
		server = updatedServer;

		if (server.regionSelectionPreference == null) {
			server.regionSelectionPreference = RealmsRegionSelectionPreference.DEFAULT;
		}

		if (server.regionSelectionPreference.selectionMethod == RegionSelectionMethod.MANUAL
			&& server.regionSelectionPreference.preferredRegion == null
		) {
			Optional<RealmsRegion> firstRegion = availableRegions.keySet().stream().findFirst();
			firstRegion.ifPresent(r -> server.regionSelectionPreference.preferredRegion = r);
		}

		String switchKey = server.state == RealmsServer.State.OPEN
			? "mco.configure.world.buttons.close"
			: "mco.configure.world.buttons.open";

		switchStateButton.setMessage(Text.translatable(switchKey));
		switchStateButton.active = true;

		region = new RealmsSettingsTab.Region(
			server.regionSelectionPreference.selectionMethod,
			server.regionSelectionPreference.preferredRegion
		);

		worldNameTextField.setText(Objects.requireNonNullElse(server.getName(), ""));
		descriptionTextField.setText(server.getDescription());
		refreshRegionText();
	}

	@Override
	public void onUnloaded(RealmsServer updatedServer) {
		saveSettings();
	}

	/**
	 * Сохраняет настройки, если хотя бы одно из полей изменилось по сравнению с текущим состоянием сервера.
	 * Имя мира обрезается от пробелов перед сохранением.
	 */
	public void saveSettings() {
		String trimmedName = worldNameTextField.getText().trim();
		boolean hasChanges = server.regionSelectionPreference == null
			|| !Objects.equals(trimmedName, server.name)
			|| !Objects.equals(descriptionTextField.getText(), server.description)
			|| region.preference() != server.regionSelectionPreference.selectionMethod
			|| region.region() != server.regionSelectionPreference.preferredRegion;

		if (!hasChanges) {
			return;
		}

		screen.saveSettings(
			trimmedName,
			descriptionTextField.getText(),
			region.preference(),
			region.region()
		);
	}

	@Environment(EnvType.CLIENT)
	public record Region(RegionSelectionMethod preference, @Nullable RealmsRegion region) {
	}
}
