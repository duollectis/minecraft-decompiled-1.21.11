package net.minecraft.client.realms.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.realms.ServiceQuality;
import net.minecraft.client.realms.dto.RealmsRegion;
import net.minecraft.client.realms.dto.RegionSelectionMethod;
import net.minecraft.client.realms.gui.screen.tab.RealmsSettingsTab;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsRegionPreferenceScreen}.
 */
public class RealmsRegionPreferenceScreen extends Screen {

	private static final Text TITLE_TEXT = Text.translatable("mco.configure.world.region_preference.title");
	private static final int ENTRY_PADDING = 8;
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	private final Screen parent;
	private final BiConsumer<RegionSelectionMethod, RealmsRegion> onRegionChanged;
	final Map<RealmsRegion, ServiceQuality> availableRegions;
	private RealmsRegionPreferenceScreen.@Nullable RegionListWidget regionList;
	RealmsSettingsTab.Region currentRegion;
	private @Nullable ButtonWidget doneButton;

	public RealmsRegionPreferenceScreen(
			Screen parent,
			BiConsumer<RegionSelectionMethod, RealmsRegion> onRegionChanged,
			Map<RealmsRegion, ServiceQuality> availableRegions,
			RealmsSettingsTab.Region textSupplier
	) {
		super(TITLE_TEXT);
		this.parent = parent;
		this.onRegionChanged = onRegionChanged;
		this.availableRegions = availableRegions;
		this.currentRegion = textSupplier;
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	@Override
	protected void init() {
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addHeader(DirectionalLayoutWidget.vertical().spacing(8));
		directionalLayoutWidget.getMainPositioner().alignHorizontalCenter();
		directionalLayoutWidget.add(new TextWidget(this.getTitle(), this.textRenderer));
		this.regionList = this.layout.addBody(new RealmsRegionPreferenceScreen.RegionListWidget());
		DirectionalLayoutWidget
				directionalLayoutWidget2 =
				this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		this.doneButton =
				directionalLayoutWidget2.add(ButtonWidget.builder(ScreenTexts.DONE, button -> this.onDone()).build());
		directionalLayoutWidget2.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> this.close()).build());
		this.regionList
				.setSelected(this.regionList
						.children()
						.stream()
						.filter(region -> Objects.equals(region.region, this.currentRegion))
						.findFirst()
						.orElse(null));
		this.layout.forEachChild(child -> {
			ClickableWidget var10000 = this.addDrawableChild(child);
		});
		this.refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		this.layout.refreshPositions();
		if (this.regionList != null) {
			this.regionList.position(this.width, this.layout);
		}
	}

	void onDone() {
		if (this.currentRegion.region() != null) {
			this.onRegionChanged.accept(this.currentRegion.preference(), this.currentRegion.region());
		}

		this.close();
	}

	void refreshDoneButton() {
		if (this.doneButton != null && this.regionList != null) {
			this.doneButton.active = this.regionList.getSelectedOrNull() != null;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code RegionListWidget}.
	 */
	class RegionListWidget extends AlwaysSelectedEntryListWidget<RealmsRegionPreferenceScreen.RegionListWidget.RegionEntry> {

		RegionListWidget() {
			super(
					RealmsRegionPreferenceScreen.this.client,
					RealmsRegionPreferenceScreen.this.width,
					RealmsRegionPreferenceScreen.this.height - 77,
					40,
					16
			);
			this.addEntry(new RealmsRegionPreferenceScreen.RegionListWidget.RegionEntry(
					RegionSelectionMethod.AUTOMATIC_PLAYER,
					null
			));
			this.addEntry(new RealmsRegionPreferenceScreen.RegionListWidget.RegionEntry(
					RegionSelectionMethod.AUTOMATIC_OWNER,
					null
			));
			RealmsRegionPreferenceScreen.this.availableRegions
					.keySet()
					.stream()
					.map(region -> new RealmsRegionPreferenceScreen.RegionListWidget.RegionEntry(
							RegionSelectionMethod.MANUAL,
							region
					))
					.forEach(entry -> this.addEntry(entry));
		}

		public void setSelected(RealmsRegionPreferenceScreen.RegionListWidget.@Nullable RegionEntry regionEntry) {
			super.setSelected(regionEntry);
			if (regionEntry != null) {
				RealmsRegionPreferenceScreen.this.currentRegion = regionEntry.region;
			}

			RealmsRegionPreferenceScreen.this.refreshDoneButton();
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code RegionEntry}.
		 */
		class RegionEntry extends AlwaysSelectedEntryListWidget.Entry<RealmsRegionPreferenceScreen.RegionListWidget.RegionEntry> {

			final RealmsSettingsTab.Region region;
			private final Text name;

			public RegionEntry(final @Nullable RegionSelectionMethod selectionMethod, final RealmsRegion region) {
				this(new RealmsSettingsTab.Region(selectionMethod, region));
			}

			public RegionEntry(final RealmsSettingsTab.Region region) {
				this.region = region;
				if (region.preference() == RegionSelectionMethod.MANUAL) {
					if (region.region() != null) {
						this.name = Text.translatable(region.region().translationKey);
					}
					else {
						this.name = Text.empty();
					}
				}
				else {
					this.name = Text.translatable(region.preference().translationKey);
				}
			}

			@Override
			public Text getNarration() {
				return Text.translatable("narrator.select", this.name);
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				context.drawTextWithShadow(
						RealmsRegionPreferenceScreen.this.textRenderer,
						this.name,
						this.getContentX() + 5,
						this.getContentY() + 2,
						-1
				);
				if (this.region.region() != null
						&& RealmsRegionPreferenceScreen.this.availableRegions.containsKey(this.region.region())) {
					ServiceQuality
							serviceQuality =
							RealmsRegionPreferenceScreen.this.availableRegions.getOrDefault(
									this.region.region(),
									ServiceQuality.UNKNOWN
							);
					context.drawGuiTexture(
							RenderPipelines.GUI_TEXTURED,
							serviceQuality.getIcon(),
							this.getContentRightEnd() - 18,
							this.getContentY() + 2,
							10,
							8
					);
				}
			}

			@Override
			public boolean mouseClicked(Click click, boolean doubled) {
				RegionListWidget.this.setSelected(this);
				if (doubled) {
					RegionListWidget.this.playDownSound(RegionListWidget.this.client.getSoundManager());
					RealmsRegionPreferenceScreen.this.onDone();
					return true;
				}
				else {
					return super.mouseClicked(click, doubled);
				}
			}

			@Override
			public boolean keyPressed(KeyInput input) {
				if (input.isEnterOrSpace()) {
					RegionListWidget.this.playDownSound(RegionListWidget.this.client.getSoundManager());
					RealmsRegionPreferenceScreen.this.onDone();
					return true;
				}
				else {
					return super.keyPressed(input);
				}
			}
		}
	}
}
