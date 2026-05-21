package net.minecraft.client.realms.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.RealmsSlot;
import net.minecraft.client.realms.dto.WorldDownload;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.gui.RealmsPopups;
import net.minecraft.client.realms.gui.RealmsWorldSlotButton;
import net.minecraft.client.realms.task.OpenServerTask;
import net.minecraft.client.realms.task.SwitchSlotTask;
import net.minecraft.client.realms.util.RealmsTextureManager;
import net.minecraft.client.realms.util.RealmsUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsBrokenWorldScreen}.
 */
public class RealmsBrokenWorldScreen extends RealmsScreen {

	private static final Identifier SLOT_FRAME_TEXTURE = Identifier.ofVanilla("widget/slot_frame");
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int BUTTON_WIDTH = 80;
	private final Screen parent;
	private @Nullable RealmsServer serverData;
	private final long serverId;
	private final Text[]
			message =
			new Text[]{
					Text.translatable("mco.brokenworld.message.line1"),
					Text.translatable("mco.brokenworld.message.line2")
			};
	private int left_x;
	private final List<Integer> slotsThatHasBeenDownloaded = Lists.newArrayList();
	private int animTick;

	public RealmsBrokenWorldScreen(Screen parent, long serverId, boolean minigame) {
		super(minigame ? Text.translatable("mco.brokenworld.minigame.title")
		               : Text.translatable("mco.brokenworld.title"));
		this.parent = parent;
		this.serverId = serverId;
	}

	@Override
	public void init() {
		this.left_x = this.width / 2 - 150;
		this.addDrawableChild(ButtonWidget
				.builder(ScreenTexts.BACK, button -> this.close())
				.dimensions((this.width - 150) / 2, row(13) - 5, 150, 20)
				.build());
		if (this.serverData == null) {
			this.fetchServerData(this.serverId);
		}
		else {
			this.addButtons();
		}
	}

	@Override
	public Text getNarratedTitle() {
		return Texts.join(
				Stream.concat(Stream.of(this.title), Stream.of(this.message)).collect(Collectors.toList()),
				ScreenTexts.SPACE
		);
	}

	private void addButtons() {
		for (Entry<Integer, RealmsSlot> entry : this.serverData.slots.entrySet()) {
			int i = entry.getKey();
			boolean bl = i != this.serverData.activeSlot || this.serverData.isMinigame();
			ButtonWidget buttonWidget;
			if (bl) {
				buttonWidget = ButtonWidget.builder(
						                           Text.translatable("mco.brokenworld.play"),
						                           button -> this.client.setScreen(new RealmsLongRunningMcoTaskScreen(
								                           this.parent,
								                           new SwitchSlotTask(this.serverData.id, i, this::play)
						                           ))
				                           )
				                           .dimensions(this.getFramePositionX(i), row(8), 80, 20)
				                           .build();
				buttonWidget.active = !this.serverData.slots.get(i).options.empty;
			}
			else {
				buttonWidget = ButtonWidget.builder(
						                           Text.translatable("mco.brokenworld.download"),
						                           button -> this.client
								                           .setScreen(
										                           RealmsPopups.createInfoPopup(
												                           this,
												                           Text.translatable("mco.configure.world.restore.download.question.line1"),
												                           popupScreen -> this.downloadWorld(i)
										                           )
								                           )
				                           )
				                           .dimensions(this.getFramePositionX(i), row(8), 80, 20)
				                           .build();
			}

			if (this.slotsThatHasBeenDownloaded.contains(i)) {
				buttonWidget.active = false;
				buttonWidget.setMessage(Text.translatable("mco.brokenworld.downloaded"));
			}

			this.addDrawableChild(buttonWidget);
		}
	}

	@Override
	public void tick() {
		this.animTick++;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 17, -1);

		for (int i = 0; i < this.message.length; i++) {
			context.drawCenteredTextWithShadow(
					this.textRenderer,
					this.message[i],
					this.width / 2,
					row(-1) + 3 + i * 12,
					-6250336
			);
		}

		if (this.serverData != null) {
			for (Entry<Integer, RealmsSlot> entry : this.serverData.slots.entrySet()) {
				if (entry.getValue().options.templateImage != null && entry.getValue().options.templateId != -1L) {
					this.drawSlotFrame(
							context,
							this.getFramePositionX(entry.getKey()),
							row(1) + 5,
							mouseX,
							mouseY,
							this.serverData.activeSlot == entry.getKey() && !this.isMinigame(),
							entry.getValue().options.getSlotName(entry.getKey()),
							entry.getKey(),
							entry.getValue().options.templateId,
							entry.getValue().options.templateImage,
							entry.getValue().options.empty
					);
				}
				else {
					this.drawSlotFrame(
							context,
							this.getFramePositionX(entry.getKey()),
							row(1) + 5,
							mouseX,
							mouseY,
							this.serverData.activeSlot == entry.getKey() && !this.isMinigame(),
							entry.getValue().options.getSlotName(entry.getKey()),
							entry.getKey(),
							-1L,
							null,
							entry.getValue().options.empty
					);
				}
			}
		}
	}

	private int getFramePositionX(int i) {
		return this.left_x + (i - 1) * 110;
	}

	/**
	 * Создаёт error screen.
	 *
	 * @param error error
	 *
	 * @return Screen — результат операции
	 */
	public Screen createErrorScreen(RealmsServiceException error) {
		return new RealmsGenericErrorScreen(error, this.parent);
	}

	private void fetchServerData(long worldId) {
		RealmsUtil.<RealmsServer>runAsync(
				          (RealmsUtil.RealmsSupplier<RealmsServer>) client -> client.getOwnWorld(worldId),
				          RealmsUtil.openingScreenAndLogging(this::createErrorScreen, "Couldn't get own world")
		          )
		          .thenAcceptAsync(
				          serverData -> {
					          this.serverData = serverData;
					          this.addButtons();
				          }, this.client
		          );
	}

	/**
	 * Play.
	 */
	public void play() {
		new Thread(
				() -> {
					RealmsClient realmsClient = RealmsClient.create();
					if (this.serverData.state == RealmsServer.State.CLOSED) {
						this.client
								.execute(
										() -> this.client.setScreen(new RealmsLongRunningMcoTaskScreen(
												this,
												new OpenServerTask(this.serverData, this, true, this.client)
										))
								);
					}
					else {
						try {
							RealmsServer realmsServer = realmsClient.getOwnWorld(this.serverId);
							this.client.execute(() -> RealmsMainScreen.play(realmsServer, this));
						}
						catch (RealmsServiceException var3) {
							LOGGER.error("Couldn't get own world", var3);
							this.client.execute(() -> this.client.setScreen(this.createErrorScreen(var3)));
						}
					}
				}
		)
				.start();
	}

	private void downloadWorld(int slotId) {
		RealmsClient realmsClient = RealmsClient.create();

		try {
			WorldDownload worldDownload = realmsClient.download(this.serverData.id, slotId);
			RealmsDownloadLatestWorldScreen realmsDownloadLatestWorldScreen = new RealmsDownloadLatestWorldScreen(
					this, worldDownload, this.serverData.getWorldName(slotId), successful -> {
				if (successful) {
					this.slotsThatHasBeenDownloaded.add(slotId);
					this.clearChildren();
					this.addButtons();
				}
				else {
					this.client.setScreen(this);
				}
			}
			);
			this.client.setScreen(realmsDownloadLatestWorldScreen);
		}
		catch (RealmsServiceException var5) {
			LOGGER.error("Couldn't download world data", var5);
			this.client.setScreen(new RealmsGenericErrorScreen(var5, this));
		}
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	private boolean isMinigame() {
		return this.serverData != null && this.serverData.isMinigame();
	}

	private void drawSlotFrame(
			DrawContext context,
			int x,
			int y,
			int mouseX,
			int mouseY,
			boolean activeSlot,
			String slotName,
			int slotId,
			long templateId,
			@Nullable String templateImage,
			boolean empty
	) {
		Identifier identifier;
		if (empty) {
			identifier = RealmsWorldSlotButton.EMPTY_FRAME;
		}
		else if (templateImage != null && templateId != -1L) {
			identifier = RealmsTextureManager.getTextureId(String.valueOf(templateId), templateImage);
		}
		else if (slotId == 1) {
			identifier = RealmsWorldSlotButton.PANORAMA_0;
		}
		else if (slotId == 2) {
			identifier = RealmsWorldSlotButton.PANORAMA_2;
		}
		else if (slotId == 3) {
			identifier = RealmsWorldSlotButton.PANORAMA_3;
		}
		else {
			identifier =
					RealmsTextureManager.getTextureId(
							String.valueOf(this.serverData.minigameId),
							this.serverData.minigameImage
					);
		}

		if (activeSlot) {
			float f = 0.9F + 0.1F * MathHelper.cos(this.animTick * 0.2F);
			context.drawTexture(
					RenderPipelines.GUI_TEXTURED,
					identifier,
					x + 3,
					y + 3,
					0.0F,
					0.0F,
					74,
					74,
					74,
					74,
					74,
					74,
					ColorHelper.fromFloats(1.0F, f, f, f)
			);
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_TEXTURE, x, y, 80, 80);
		}
		else {
			int i = ColorHelper.fromFloats(1.0F, 0.56F, 0.56F, 0.56F);
			context.drawTexture(
					RenderPipelines.GUI_TEXTURED,
					identifier,
					x + 3,
					y + 3,
					0.0F,
					0.0F,
					74,
					74,
					74,
					74,
					74,
					74,
					i
			);
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_TEXTURE, x, y, 80, 80, i);
		}

		context.drawCenteredTextWithShadow(this.textRenderer, slotName, x + 40, y + 66, -1);
	}
}
