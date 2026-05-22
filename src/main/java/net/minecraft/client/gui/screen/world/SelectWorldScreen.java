package net.minecraft.client.gui.screen.world;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.path.PathUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Экран выбора мира для одиночной игры.
 * Отображает список сохранённых миров с поиском и кнопками управления.
 */
@Environment(EnvType.CLIENT)
public class SelectWorldScreen extends Screen {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final GeneratorOptions
			DEBUG_GENERATOR_OPTIONS =
			new GeneratorOptions("test1".hashCode(), true, false);
	protected final Screen parent;
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, 8 + 9 + 8 + 20 + 4, 60);
	private @Nullable ButtonWidget deleteButton;
	private @Nullable ButtonWidget selectButton;
	private @Nullable ButtonWidget editButton;
	private @Nullable ButtonWidget recreateButton;
	protected @Nullable TextFieldWidget searchBox;
	private @Nullable WorldListWidget levelList;

	public SelectWorldScreen(Screen parent) {
		super(Text.translatable("selectWorld.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		DirectionalLayoutWidget headerLayout = layout.addHeader(DirectionalLayoutWidget.vertical().spacing(4));
		headerLayout.getMainPositioner().alignHorizontalCenter();
		headerLayout.add(new TextWidget(title, textRenderer));
		DirectionalLayoutWidget searchRow = headerLayout.add(DirectionalLayoutWidget.horizontal().spacing(4));

		if (SharedConstants.WORLD_RECREATE) {
			searchRow.add(createDebugRecreateButton());
		}

		searchBox = searchRow.add(
				new TextFieldWidget(
						textRenderer,
						width / 2 - 100,
						22,
						200,
						20,
						searchBox,
						Text.translatable("selectWorld.search")
				)
		);
		searchBox.setChangedListener(search -> {
			if (levelList != null) {
				levelList.setSearch(search);
			}
		});
		searchBox.setPlaceholder(Text
				.translatable("gui.selectWorld.search")
				.setStyle(TextFieldWidget.SEARCH_STYLE));

		Consumer<WorldListWidget.WorldEntry> playAction = WorldListWidget.WorldEntry::play;
		levelList = layout.addBody(
				new WorldListWidget.Builder(client, this)
						.width(width)
						.height(layout.getContentHeight())
						.search(searchBox.getText())
						.predecessor(levelList)
						.selectionCallback(this::worldSelected)
						.confirmationCallback(playAction)
						.toWidget()
		);
		addButtons(playAction, levelList);
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
		worldSelected(null);
	}

	private void addButtons(Consumer<WorldListWidget.WorldEntry> playAction, WorldListWidget levelList) {
		GridWidget gridWidget = layout.addFooter(new GridWidget().setColumnSpacing(8).setRowSpacing(4));
		gridWidget.getMainPositioner().alignHorizontalCenter();
		GridWidget.Adder adder = gridWidget.createAdder(4);
		selectButton = adder.add(
				ButtonWidget
						.builder(
								LevelSummary.SELECT_WORLD_TEXT,
								button -> levelList.getSelectedAsOptional().ifPresent(playAction)
						)
						.build(), 2
		);
		adder.add(
				ButtonWidget
						.builder(
								Text.translatable("selectWorld.create"),
								button -> CreateWorldScreen.show(client, levelList::refresh)
						)
						.build(), 2
		);
		editButton = adder.add(
				ButtonWidget
						.builder(
								Text.translatable("selectWorld.edit"),
								button -> levelList.getSelectedAsOptional().ifPresent(WorldListWidget.WorldEntry::edit)
						)
						.width(71)
						.build()
		);
		deleteButton = adder.add(
				ButtonWidget.builder(
						Text.translatable("selectWorld.delete"),
						button -> levelList
								.getSelectedAsOptional()
								.ifPresent(WorldListWidget.WorldEntry::deleteIfConfirmed)
				)
				.width(71)
				.build()
		);
		recreateButton = adder.add(
				ButtonWidget.builder(
						Text.translatable("selectWorld.recreate"),
						button -> levelList.getSelectedAsOptional().ifPresent(WorldListWidget.WorldEntry::recreate)
				)
				.width(71)
				.build()
		);
		adder.add(ButtonWidget
				.builder(ScreenTexts.BACK, button -> client.setScreen(parent))
				.width(71)
				.build());
	}

	private ButtonWidget createDebugRecreateButton() {
		return ButtonWidget.builder(
				Text.literal("DEBUG recreate"),
				button -> {
					try {
						if (levelList != null && !levelList.children().isEmpty()) {
							WorldListWidget.Entry entry = levelList.children().getFirst();
							if (entry instanceof WorldListWidget.WorldEntry worldEntry
									&& worldEntry.getLevelDisplayName().equals("DEBUG world")) {
								worldEntry.delete();
							}
						}

						LevelInfo levelInfo = new LevelInfo(
								"DEBUG world",
								GameMode.SPECTATOR,
								false,
								Difficulty.NORMAL,
								true,
								new GameRules(DataConfiguration.SAFE_MODE.enabledFeatures()),
								DataConfiguration.SAFE_MODE
						);
						String dirName = PathUtil.getNextUniqueName(
								client.getLevelStorage().getSavesDirectory(),
								"DEBUG world",
								""
						);
						client.createIntegratedServerLoader()
								.createAndStart(
										dirName,
										levelInfo,
										DEBUG_GENERATOR_OPTIONS,
										WorldPresets::createDemoOptions,
										this
								);
					}
					catch (IOException exception) {
						LOGGER.error("Failed to recreate the debug world", exception);
					}
				}
		)
		.width(72)
		.build();
	}

	@Override
	protected void refreshWidgetPositions() {
		if (levelList != null) {
			levelList.position(width, layout);
		}

		layout.refreshPositions();
	}

	@Override
	protected void setInitialFocus() {
		if (searchBox != null) {
			setInitialFocus(searchBox);
		}
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	public void worldSelected(@Nullable LevelSummary levelSummary) {
		if (selectButton == null || editButton == null || recreateButton == null || deleteButton == null) {
			return;
		}

		if (levelSummary == null) {
			selectButton.setMessage(LevelSummary.SELECT_WORLD_TEXT);
			selectButton.active = false;
			editButton.active = false;
			recreateButton.active = false;
			deleteButton.active = false;
		}
		else {
			selectButton.setMessage(levelSummary.getSelectWorldText());
			selectButton.active = levelSummary.isSelectable();
			editButton.active = levelSummary.isEditable();
			recreateButton.active = levelSummary.isRecreatable();
			deleteButton.active = levelSummary.isDeletable();
		}
	}

	@Override
	public void removed() {
		if (levelList != null) {
			levelList.children().forEach(WorldListWidget.Entry::close);
		}
	}
}
