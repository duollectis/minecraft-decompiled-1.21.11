package net.minecraft.client.gui.screen.world;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Экран управления экспериментальными функциями (feature flags) при создании или редактировании мира.
 * Отображает список доступных экспериментов с возможностью их включения/отключения.
 */
@Environment(EnvType.CLIENT)
public class ExperimentsScreen extends Screen {

	private static final Text TITLE = Text.translatable("selectWorld.experiments");
	private static final Text INFO_TEXT = Text.translatable("selectWorld.experiments.info").formatted(Formatting.RED);
	private static final int EXPERIMENTS_LIST_WIDTH = 310;
	private static final int EXPERIMENTS_LIST_HEIGHT = 130;
	private static final int OPTION_GRID_WIDTH = 299;
	private static final int TOOLTIP_BOX_PADDING = 2;
	private static final int ROW_SPACING = 4;

	private final ThreePartsLayoutWidget experimentToggleList = new ThreePartsLayoutWidget(this);
	private final Screen parent;
	private final ResourcePackManager resourcePackManager;
	private final Consumer<ResourcePackManager> applier;
	private final Object2BooleanMap<ResourcePackProfile> experiments = new Object2BooleanLinkedOpenHashMap();
	private @Nullable ScrollableLayoutWidget experimentsList;

	public ExperimentsScreen(
			Screen parent,
			ResourcePackManager resourcePackManager,
			Consumer<ResourcePackManager> applier
	) {
		super(TITLE);
		this.parent = parent;
		this.resourcePackManager = resourcePackManager;
		this.applier = applier;

		for (ResourcePackProfile profile : resourcePackManager.getProfiles()) {
			if (profile.getSource() == ResourcePackSource.FEATURE) {
				experiments.put(
						profile,
						resourcePackManager.getEnabledProfiles().contains(profile)
				);
			}
		}
	}

	@Override
	protected void init() {
		experimentToggleList.addHeader(TITLE, textRenderer);

		DirectionalLayoutWidget body = experimentToggleList.addBody(DirectionalLayoutWidget.vertical());
		body.add(
				new MultilineTextWidget(INFO_TEXT, textRenderer).setMaxWidth(EXPERIMENTS_LIST_WIDTH),
				positioner -> positioner.marginBottom(15)
		);

		WorldScreenOptionGrid.Builder builder = WorldScreenOptionGrid
				.builder(OPTION_GRID_WIDTH)
				.withTooltipBox(TOOLTIP_BOX_PADDING, true)
				.setRowSpacing(ROW_SPACING);

		experiments.forEach(
				(pack, enabled) -> builder
						.add(
								getDataPackName(pack),
								() -> experiments.getBoolean(pack),
								newEnabled -> experiments.put(pack, newEnabled)
						)
						.tooltip(pack.getDescription())
		);

		LayoutWidget optionLayout = builder.build().getLayout();
		experimentsList = new ScrollableLayoutWidget(client, optionLayout, EXPERIMENTS_LIST_HEIGHT);
		experimentsList.setWidth(EXPERIMENTS_LIST_WIDTH);
		body.add(experimentsList);

		DirectionalLayoutWidget footer = experimentToggleList.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		footer.add(ButtonWidget.builder(ScreenTexts.DONE, button -> applyAndClose()).build());
		footer.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close()).build());

		experimentToggleList.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	private static Text getDataPackName(ResourcePackProfile packProfile) {
		String translationKey = "dataPack." + packProfile.getId() + ".name";
		return I18n.hasTranslation(translationKey)
				? Text.translatable(translationKey)
				: packProfile.getDisplayName();
	}

	@Override
	protected void refreshWidgetPositions() {
		experimentsList.setHeight(EXPERIMENTS_LIST_HEIGHT);
		experimentToggleList.refreshPositions();

		int extraHeight = height
				- experimentToggleList.getFooterHeight()
				- experimentsList.getNavigationFocus().getBottom();
		experimentsList.setHeight(experimentsList.getHeight() + extraHeight);
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), INFO_TEXT);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private void applyAndClose() {
		List<ResourcePackProfile> enabledProfiles = new ArrayList<>(resourcePackManager.getEnabledProfiles());
		List<ResourcePackProfile> toEnable = new ArrayList<>();

		experiments.forEach((pack, enabled) -> {
			enabledProfiles.remove(pack);
			if (enabled) {
				toEnable.add(pack);
			}
		});

		enabledProfiles.addAll(Lists.reverse(toEnable));
		resourcePackManager.setEnabledProfiles(enabledProfiles.stream().map(ResourcePackProfile::getId).toList());
		applier.accept(resourcePackManager);
	}
}
