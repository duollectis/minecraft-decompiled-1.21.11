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

@Environment(EnvType.CLIENT)
/**
 * {@code SelectWorldScreen}.
 */
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
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addHeader(DirectionalLayoutWidget.vertical().spacing(4));
		directionalLayoutWidget.getMainPositioner().alignHorizontalCenter();
		directionalLayoutWidget.add(new TextWidget(this.title, this.textRenderer));
		DirectionalLayoutWidget
				directionalLayoutWidget2 =
				directionalLayoutWidget.add(DirectionalLayoutWidget.horizontal().spacing(4));
		if (SharedConstants.WORLD_RECREATE) {
			directionalLayoutWidget2.add(this.createDebugRecreateButton());
		}

		this.searchBox = directionalLayoutWidget2.add(
				new TextFieldWidget(
						this.textRenderer,
						this.width / 2 - 100,
						22,
						200,
						20,
						this.searchBox,
						Text.translatable("selectWorld.search")
				)
		);
		this.searchBox.setChangedListener(search -> {
			if (this.levelList != null) {
				this.levelList.setSearch(search);
			}
		});
		this.searchBox.setPlaceholder(Text
				.translatable("gui.selectWorld.search")
				.setStyle(TextFieldWidget.SEARCH_STYLE));
		Consumer<WorldListWidget.WorldEntry> consumer = WorldListWidget.WorldEntry::play;
		this.levelList = this.layout
				.addBody(
						new WorldListWidget.Builder(this.client, this)
								.width(this.width)
								.height(this.layout.getContentHeight())
								.search(this.searchBox.getText())
								.predecessor(this.levelList)
								.selectionCallback(this::worldSelected)
								.confirmationCallback(consumer)
								.toWidget()
				);
		this.addButtons(consumer, this.levelList);
		this.layout.forEachChild(child -> {
			ClickableWidget var10000 = this.addDrawableChild(child);
		});
		this.refreshWidgetPositions();
		this.worldSelected(null);
	}

	private void addButtons(Consumer<WorldListWidget.WorldEntry> playAction, WorldListWidget levelList) {
		GridWidget gridWidget = this.layout.addFooter(new GridWidget().setColumnSpacing(8).setRowSpacing(4));
		gridWidget.getMainPositioner().alignHorizontalCenter();
		GridWidget.Adder adder = gridWidget.createAdder(4);
		this.selectButton = adder.add(
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
								button -> CreateWorldScreen.show(this.client, levelList::refresh)
						)
						.build(), 2
		);
		this.editButton = adder.add(
				ButtonWidget
						.builder(
								Text.translatable("selectWorld.edit"),
								button -> levelList.getSelectedAsOptional().ifPresent(WorldListWidget.WorldEntry::edit)
						)
						.width(71)
						.build()
		);
		this.deleteButton = adder.add(
				ButtonWidget.builder(
						            Text.translatable("selectWorld.delete"),
						            button -> levelList
								            .getSelectedAsOptional()
								            .ifPresent(WorldListWidget.WorldEntry::deleteIfConfirmed)
				            )
				            .width(71)
				            .build()
		);
		this.recreateButton = adder.add(
				ButtonWidget.builder(
						            Text.translatable("selectWorld.recreate"),
						            button -> levelList.getSelectedAsOptional().ifPresent(WorldListWidget.WorldEntry::recreate)
				            )
				            .width(71)
				            .build()
		);
		adder.add(ButtonWidget
				.builder(ScreenTexts.BACK, button -> this.client.setScreen(this.parent))
				.width(71)
				.build());
	}

	private ButtonWidget createDebugRecreateButton() {
		return ButtonWidget.builder(
				                   Text.literal("DEBUG recreate"),
				                   button -> {
					                   try {
						                   String string = "DEBUG world";
						                   if (this.levelList != null && !this.levelList.children().isEmpty()) {
							                   WorldListWidget.Entry entry = this.levelList.children().getFirst();
							                   if (entry instanceof WorldListWidget.WorldEntry worldEntry && worldEntry
									                   .getLevelDisplayName()
									                   .equals("DEBUG world")) {
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
						                   String
								                   string2 =
								                   PathUtil.getNextUniqueName(
										                   this.client.getLevelStorage().getSavesDirectory(),
										                   "DEBUG world",
										                   ""
								                   );
						                   this.client
								                   .createIntegratedServerLoader()
								                   .createAndStart(
										                   string2,
										                   levelInfo,
										                   DEBUG_GENERATOR_OPTIONS,
										                   WorldPresets::createDemoOptions,
										                   this
								                   );
					                   }
					                   catch (IOException var5) {
						                   LOGGER.error("Failed to recreate the debug world", var5);
					                   }
				                   }
		                   )
		                   .width(72)
		                   .build();
	}

	@Override
	protected void refreshWidgetPositions() {
		if (this.levelList != null) {
			this.levelList.position(this.width, this.layout);
		}

		this.layout.refreshPositions();
	}

	@Override
	protected void setInitialFocus() {
		if (this.searchBox != null) {
			this.setInitialFocus(this.searchBox);
		}
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	/**
	 * World selected.
	 *
	 * @param levelSummary level summary
	 */
	public void worldSelected(@Nullable LevelSummary levelSummary) {
		if (this.selectButton != null && this.editButton != null && this.recreateButton != null
				&& this.deleteButton != null) {
			if (levelSummary == null) {
				this.selectButton.setMessage(LevelSummary.SELECT_WORLD_TEXT);
				this.selectButton.active = false;
				this.editButton.active = false;
				this.recreateButton.active = false;
				this.deleteButton.active = false;
			}
			else {
				this.selectButton.setMessage(levelSummary.getSelectWorldText());
				this.selectButton.active = levelSummary.isSelectable();
				this.editButton.active = levelSummary.isEditable();
				this.recreateButton.active = levelSummary.isRecreatable();
				this.deleteButton.active = levelSummary.isDeletable();
			}
		}
	}

	@Override
	public void removed() {
		if (this.levelList != null) {
			this.levelList.children().forEach(WorldListWidget.Entry::close);
		}
	}
}
