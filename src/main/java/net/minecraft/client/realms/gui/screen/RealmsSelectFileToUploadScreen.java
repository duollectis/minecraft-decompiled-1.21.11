package net.minecraft.client.realms.gui.screen;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.realms.task.WorldCreationTask;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelSummary;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsSelectFileToUploadScreen}.
 */
public class RealmsSelectFileToUploadScreen extends RealmsScreen {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Text TITLE = Text.translatable("mco.upload.select.world.title");
	private static final Text LOADING_ERROR_TEXT = Text.translatable("selectWorld.unable_to_load");
	private final @Nullable WorldCreationTask creationTask;
	private final RealmsCreateWorldScreen parent;
	private final long worldId;
	private final int slotId;
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, 8 + 9 + 8 + 20 + 4, 33);
	protected @Nullable TextFieldWidget searchField;
	private @Nullable WorldListWidget worldSelectionList;
	private @Nullable ButtonWidget uploadButton;

	public RealmsSelectFileToUploadScreen(
			@Nullable WorldCreationTask creationTask,
			long worldId,
			int slotId,
			RealmsCreateWorldScreen parent
	) {
		super(TITLE);
		this.creationTask = creationTask;
		this.parent = parent;
		this.worldId = worldId;
		this.slotId = slotId;
	}

	@Override
	public void init() {
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addHeader(DirectionalLayoutWidget.vertical().spacing(4));
		directionalLayoutWidget.getMainPositioner().alignHorizontalCenter();
		directionalLayoutWidget.add(new TextWidget(this.title, this.textRenderer));
		this.searchField = directionalLayoutWidget.add(
				new TextFieldWidget(
						this.textRenderer,
						this.width / 2 - 100,
						22,
						200,
						20,
						this.searchField,
						Text.translatable("selectWorld.search")
				)
		);
		this.searchField.setChangedListener(string -> {
			if (this.worldSelectionList != null) {
				this.worldSelectionList.setSearch(string);
			}
		});

		try {
			this.worldSelectionList = this.layout
					.addBody(
							new WorldListWidget.Builder(this.client, this)
									.width(this.width)
									.height(this.layout.getContentHeight())
									.search(this.searchField.getText())
									.predecessor(this.worldSelectionList)
									.uploadWorld()
									.selectionCallback(this::worldSelected)
									.confirmationCallback(this::upload)
									.toWidget()
					);
		}
		catch (Exception var3) {
			LOGGER.error("Couldn't load level list", var3);
			this.client.setScreen(new RealmsGenericErrorScreen(
					LOADING_ERROR_TEXT,
					Text.of(var3.getMessage()),
					this.parent
			));
			return;
		}

		DirectionalLayoutWidget
				directionalLayoutWidget2 =
				this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		directionalLayoutWidget2.getMainPositioner().alignHorizontalCenter();
		this.uploadButton = directionalLayoutWidget2.add(
				ButtonWidget.builder(
						            Text.translatable("mco.upload.button.name"),
						            buttonWidget -> this.worldSelectionList.getSelectedAsOptional().ifPresent(this::upload)
				            )
				            .build()
		);
		directionalLayoutWidget2.add(ButtonWidget.builder(ScreenTexts.BACK, buttonWidget -> this.close()).build());
		this.worldSelected(null);
		this.layout.forEachChild(element -> {
			ClickableWidget var10000 = this.addDrawableChild(element);
		});
		this.refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		if (this.worldSelectionList != null) {
			this.worldSelectionList.position(this.width, this.layout);
		}

		this.layout.refreshPositions();
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.searchField);
	}

	private void worldSelected(@Nullable LevelSummary level) {
		if (this.worldSelectionList != null && this.uploadButton != null) {
			this.uploadButton.active = this.worldSelectionList.getSelectedOrNull() != null;
		}
	}

	private void upload(WorldListWidget.WorldEntry worldEntry) {
		this.client.setScreen(new RealmsUploadScreen(
				this.creationTask,
				this.worldId,
				this.slotId,
				this.parent,
				worldEntry.getLevel()
		));
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(this.getTitle(), this.narrateLabels());
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}
}
