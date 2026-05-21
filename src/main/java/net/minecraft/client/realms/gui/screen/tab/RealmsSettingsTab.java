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

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsSettingsTab}.
 */
public class RealmsSettingsTab extends GridScreenTab implements RealmsUpdatableTab {

	private static final int TEXT_FIELD_WIDTH = 212;
	private static final int SPACING = 2;
	private static final int ROW_SPACING = 6;
	public static final Text TITLE_TEXT = Text.translatable("mco.configure.world.settings.title");
	private static final Text WORLD_NAME_TEXT = Text.translatable("mco.configure.world.name");
	private static final Text DESCRIPTION_TEXT = Text.translatable("mco.configure.world.description");
	private static final Text REGION_PREFERENCE_TEXT = Text.translatable("mco.configure.world.region_preference");
	private static final Tooltip
			WHITESPACE_TOOLTIP =
			Tooltip.of(Text.translatable("mco.configure.world.name.validation.whitespace"));
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
			MinecraftClient minecraftClient,
			RealmsServer realmsServer,
			Map<RealmsRegion, ServiceQuality> availableRegions
	) {
		super(TITLE_TEXT);
		this.screen = screen;
		this.client = minecraftClient;
		this.server = realmsServer;
		this.availableRegions = availableRegions;
		GridWidget.Adder adder = this.grid.setRowSpacing(6).createAdder(1);
		adder.add(new TextWidget(WORLD_NAME_TEXT, screen.getTextRenderer()));
		this.worldNameTextField =
				new TextFieldWidget(
						minecraftClient.textRenderer,
						0,
						0,
						212,
						20,
						Text.translatable("mco.configure.world.name")
				);
		this.worldNameTextField.setMaxLength(32);
		this.worldNameTextField.setChangedListener(string -> {
			if (!this.isWorldNameValid()) {
				this.worldNameTextField.setEditableColor(-2142128);
				this.worldNameTextField.setTooltip(WHITESPACE_TOOLTIP);
			}
			else {
				this.worldNameTextField.setTooltip(null);
				this.worldNameTextField.setEditableColor(-2039584);
			}
		});
		adder.add(this.worldNameTextField);
		adder.add(EmptyWidget.ofHeight(2));
		adder.add(new TextWidget(DESCRIPTION_TEXT, screen.getTextRenderer()));
		this.descriptionTextField =
				new TextFieldWidget(
						minecraftClient.textRenderer,
						0,
						0,
						212,
						20,
						Text.translatable("mco.configure.world.description")
				);
		this.descriptionTextField.setMaxLength(32);
		adder.add(this.descriptionTextField);
		adder.add(EmptyWidget.ofHeight(2));
		adder.add(new TextWidget(REGION_PREFERENCE_TEXT, screen.getTextRenderer()));
		AxisGridWidget axisGridWidget = new AxisGridWidget(0, 0, 212, 9, AxisGridWidget.DisplayAxis.HORIZONTAL);
		this.regionText = axisGridWidget.add(new TextWidget(192, 9, Text.empty(), screen.getTextRenderer()));
		this.serviceQualityIcon = axisGridWidget.add(IconWidget.create(10, 8, ServiceQuality.UNKNOWN.getIcon()));
		adder.add(axisGridWidget);
		adder.add(
				ButtonWidget
						.builder(
								Text.translatable("mco.configure.world.buttons.region_preference"),
								button -> this.showRegionPreferenceScreen()
						)
						.dimensions(0, 0, 212, 20)
						.build()
		);
		adder.add(EmptyWidget.ofHeight(2));
		this.switchStateButton = adder.add(
				ButtonWidget.builder(
						            Text.empty(),
						            buttonWidget -> {
							            if (realmsServer.state == RealmsServer.State.OPEN) {
								            minecraftClient.setScreen(
										            RealmsPopups.createCustomPopup(
												            screen,
												            Text.translatable("mco.configure.world.close.question.title"),
												            Text.translatable("mco.configure.world.close.question.line1"),
												            popupScreen -> {
													            this.saveSettings();
													            screen.closeTheWorld();
												            }
										            )
								            );
							            }
							            else {
								            this.saveSettings();
								            screen.openTheWorld(false);
							            }
						            }
				            )
				            .dimensions(0, 0, 212, 20)
				            .build()
		);
		this.switchStateButton.active = false;
		this.update(realmsServer);
	}

	private static MutableText getRegionText(RealmsSettingsTab.Region region) {
		return (region.preference().equals(RegionSelectionMethod.MANUAL) && region.region() != null
		        ? Text.translatable(region.region().translationKey)
		        : Text.translatable(region.preference().translationKey)
		)
				.formatted(Formatting.GRAY);
	}

	private static Identifier getQualityIcon(
			RealmsSettingsTab.Region region,
			Map<RealmsRegion, ServiceQuality> qualityByRegion
	) {
		if (region.region() != null && qualityByRegion.containsKey(region.region())) {
			ServiceQuality serviceQuality = qualityByRegion.getOrDefault(region.region(), ServiceQuality.UNKNOWN);
			return serviceQuality.getIcon();
		}
		else {
			return ServiceQuality.UNKNOWN.getIcon();
		}
	}

	private boolean isWorldNameValid() {
		String string = this.worldNameTextField.getText();
		String string2 = string.trim();
		return !string2.isEmpty() && string.length() == string2.length();
	}

	private void showRegionPreferenceScreen() {
		this.client.setScreen(new RealmsRegionPreferenceScreen(
				this.screen,
				this::onRegionChanged,
				this.availableRegions,
				this.region
		));
	}

	private void onRegionChanged(RegionSelectionMethod selectionMethod, RealmsRegion region) {
		this.region = new RealmsSettingsTab.Region(selectionMethod, region);
		this.refreshRegionText();
	}

	private void refreshRegionText() {
		this.regionText.setMessage(getRegionText(this.region));
		this.serviceQualityIcon.setTexture(getQualityIcon(this.region, this.availableRegions));
		this.serviceQualityIcon.visible = this.region.preference == RegionSelectionMethod.MANUAL;
	}

	@Override
	public void onLoaded(RealmsServer server) {
		this.update(server);
	}

	@Override
	public void update(RealmsServer server) {
		this.server = server;
		if (server.regionSelectionPreference == null) {
			server.regionSelectionPreference = RealmsRegionSelectionPreference.DEFAULT;
		}

		if (server.regionSelectionPreference.selectionMethod == RegionSelectionMethod.MANUAL
				&& server.regionSelectionPreference.preferredRegion == null) {
			Optional<RealmsRegion> optional = this.availableRegions.keySet().stream().findFirst();
			optional.ifPresent(region -> server.regionSelectionPreference.preferredRegion = region);
		}

		String
				string =
				server.state == RealmsServer.State.OPEN ? "mco.configure.world.buttons.close"
				                                        : "mco.configure.world.buttons.open";
		this.switchStateButton.setMessage(Text.translatable(string));
		this.switchStateButton.active = true;
		this.region =
				new RealmsSettingsTab.Region(
						server.regionSelectionPreference.selectionMethod,
						server.regionSelectionPreference.preferredRegion
				);
		this.worldNameTextField.setText(Objects.requireNonNullElse(server.getName(), ""));
		this.descriptionTextField.setText(server.getDescription());
		this.refreshRegionText();
	}

	@Override
	public void onUnloaded(RealmsServer server) {
		this.saveSettings();
	}

	/**
	 * Сохраняет settings.
	 */
	public void saveSettings() {
		String string = this.worldNameTextField.getText().trim();
		if (this.server.regionSelectionPreference == null
				|| !Objects.equals(string, this.server.name)
				|| !Objects.equals(this.descriptionTextField.getText(), this.server.description)
				|| this.region.preference() != this.server.regionSelectionPreference.selectionMethod
				|| this.region.region() != this.server.regionSelectionPreference.preferredRegion) {
			this.screen.saveSettings(
					string,
					this.descriptionTextField.getText(),
					this.region.preference(),
					this.region.region()
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Region}.
	 */
	public record Region(RegionSelectionMethod preference, @Nullable RealmsRegion region) {
	}
}
