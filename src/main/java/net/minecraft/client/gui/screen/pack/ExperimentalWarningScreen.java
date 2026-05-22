package net.minecraft.client.gui.screen.pack;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Экран предупреждения об экспериментальных функциях — отображается при создании мира
 * с включёнными экспериментальными пакетами данных.
 */
@Environment(EnvType.CLIENT)
public class ExperimentalWarningScreen extends Screen {

	private static final Text TITLE = Text.translatable("selectWorld.experimental.title");
	private static final Text MESSAGE = Text.translatable("selectWorld.experimental.message");
	private static final Text DETAILS = Text.translatable("selectWorld.experimental.details");
	private static final int CONTENT_PADDING = 10;
	private static final int CONTENT_WIDTH = 100;

	private final BooleanConsumer callback;
	final Collection<ResourcePackProfile> enabledProfiles;
	private final GridWidget grid = new GridWidget().setColumnSpacing(CONTENT_PADDING).setRowSpacing(20);

	public ExperimentalWarningScreen(Collection<ResourcePackProfile> enabledProfiles, BooleanConsumer callback) {
		super(TITLE);
		this.enabledProfiles = enabledProfiles;
		this.callback = callback;
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), MESSAGE);
	}

	@Override
	protected void init() {
		super.init();
		GridWidget.Adder adder = grid.createAdder(2);
		Positioner centeredPositioner = adder.copyPositioner().alignHorizontalCenter();
		adder.add(new TextWidget(title, textRenderer), 2, centeredPositioner);
		MultilineTextWidget messageWidget = adder.add(
				new MultilineTextWidget(MESSAGE, textRenderer).setCentered(true), 2, centeredPositioner
		);
		messageWidget.setMaxWidth(310);
		adder.add(
				ButtonWidget
						.builder(DETAILS, button -> client.setScreen(new ExperimentalWarningScreen.DetailsScreen()))
						.width(CONTENT_WIDTH)
						.build(),
				2,
				centeredPositioner
		);
		adder.add(ButtonWidget.builder(ScreenTexts.PROCEED, button -> callback.accept(true)).build());
		adder.add(ButtonWidget.builder(ScreenTexts.BACK, button -> callback.accept(false)).build());
		grid.forEachChild(this::addDrawableChild);
		grid.refreshPositions();
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		SimplePositioningWidget.setPos(grid, 0, 0, width, height, 0.5F, 0.5F);
	}

	@Override
	public void close() {
		callback.accept(false);
	}

	/**
	 * Экран с детальным списком экспериментальных пакетов и их флагов.
	 */
	@Environment(EnvType.CLIENT)
	class DetailsScreen extends Screen {

		private static final Text TITLE = Text.translatable("selectWorld.experimental.details.title");

		final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
		private ExperimentalWarningScreen.DetailsScreen.@Nullable PackListWidget packListWidget;

		DetailsScreen() {
			super(TITLE);
		}

		@Override
		protected void init() {
			layout.addHeader(TITLE, textRenderer);
			packListWidget = layout.addBody(new ExperimentalWarningScreen.DetailsScreen.PackListWidget(
					client,
					ExperimentalWarningScreen.this.enabledProfiles
			));
			layout.addFooter(ButtonWidget.builder(ScreenTexts.BACK, button -> close()).build());
			layout.forEachChild(this::addDrawableChild);
			refreshWidgetPositions();
		}

		@Override
		protected void refreshWidgetPositions() {
			if (packListWidget != null) {
				packListWidget.position(width, layout);
			}

			layout.refreshPositions();
		}

		@Override
		public void close() {
			client.setScreen(ExperimentalWarningScreen.this);
		}

		/**
		 * Список пакетов с экспериментальными флагами.
		 */
		@Environment(EnvType.CLIENT)
		class PackListWidget extends AlwaysSelectedEntryListWidget<ExperimentalWarningScreen.DetailsScreen.PackListWidgetEntry> {

			public PackListWidget(final MinecraftClient client, final Collection<ResourcePackProfile> enabledProfiles) {
				super(
						client,
						DetailsScreen.this.width,
						DetailsScreen.this.layout.getContentHeight(),
						DetailsScreen.this.layout.getHeaderHeight(),
						(9 + 2) * 3
				);

				for (ResourcePackProfile profile : enabledProfiles) {
					String missingFlags = FeatureFlags.printMissingFlags(
							FeatureFlags.VANILLA_FEATURES,
							profile.getRequestedFeatures()
					);
					if (missingFlags.isEmpty()) {
						continue;
					}

					Text displayName = Texts.withStyle(profile.getDisplayName(), Style.EMPTY.withBold(true));
					Text flagsText = Text.translatable("selectWorld.experimental.details.entry", missingFlags);
					addEntry(
							DetailsScreen.this.new PackListWidgetEntry(
									displayName,
									flagsText,
									MultilineText.create(DetailsScreen.this.textRenderer, flagsText, getRowWidth())
							)
					);
				}
			}

			@Override
			public int getRowWidth() {
				return width * 3 / 4;
			}
		}

		/**
		 * Запись одного пакета в списке деталей.
		 */
		@Environment(EnvType.CLIENT)
		class PackListWidgetEntry extends AlwaysSelectedEntryListWidget.Entry<ExperimentalWarningScreen.DetailsScreen.PackListWidgetEntry> {

			private final Text displayName;
			private final Text details;
			private final MultilineText multilineDetails;

			PackListWidgetEntry(final Text displayName, final Text details, final MultilineText multilineDetails) {
				this.displayName = displayName;
				this.details = details;
				this.multilineDetails = multilineDetails;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				DrawnTextConsumer drawnTextConsumer = context.getTextConsumer();
				context.drawTextWithShadow(
						DetailsScreen.this.client.textRenderer,
						displayName,
						getContentX(),
						getContentY(),
						-1
				);
				multilineDetails.draw(Alignment.LEFT, getContentX(), getContentY() + 12, 9, drawnTextConsumer);
			}

			@Override
			public Text getNarration() {
				return Text.translatable("narrator.select", ScreenTexts.joinSentences(displayName, details));
			}
		}
	}
}
