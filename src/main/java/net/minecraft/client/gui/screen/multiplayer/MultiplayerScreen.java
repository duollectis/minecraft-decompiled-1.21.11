package net.minecraft.client.gui.screen.multiplayer;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.*;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code MultiplayerScreen}.
 */
public class MultiplayerScreen extends Screen {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int WIDE_BUTTON_WIDTH = 100;
	private static final int NARROW_BUTTON_WIDTH = 74;
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, 33, 60);
	private final MultiplayerServerListPinger serverListPinger = new MultiplayerServerListPinger();
	private final Screen parent;
	protected MultiplayerServerListWidget serverListWidget;
	private ServerList serverList;
	private ButtonWidget buttonEdit;
	private ButtonWidget buttonJoin;
	private ButtonWidget buttonDelete;
	private ServerInfo selectedEntry;
	private LanServerQueryManager.LanServerEntryList lanServers;
	private LanServerQueryManager.@Nullable LanServerDetector lanServerDetector;

	public MultiplayerScreen(Screen parent) {
		super(Text.translatable("multiplayer.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.layout.addHeader(this.title, this.textRenderer);
		this.serverList = new ServerList(this.client);
		this.serverList.loadFile();
		this.lanServers = new LanServerQueryManager.LanServerEntryList();

		try {
			this.lanServerDetector = new LanServerQueryManager.LanServerDetector(this.lanServers);
			this.lanServerDetector.start();
		}
		catch (Exception var4) {
			LOGGER.warn("Unable to start LAN server detection: {}", var4.getMessage());
		}

		this.serverListWidget = this.layout
				.addBody(new MultiplayerServerListWidget(
						this,
						this.client,
						this.width,
						this.layout.getContentHeight(),
						this.layout.getHeaderHeight(),
						36
				));
		this.serverListWidget.setServers(this.serverList);
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addFooter(DirectionalLayoutWidget.vertical().spacing(4));
		directionalLayoutWidget.getMainPositioner().alignHorizontalCenter();
		DirectionalLayoutWidget
				directionalLayoutWidget2 =
				directionalLayoutWidget.add(DirectionalLayoutWidget.horizontal().spacing(4));
		DirectionalLayoutWidget
				directionalLayoutWidget3 =
				directionalLayoutWidget.add(DirectionalLayoutWidget.horizontal().spacing(4));
		this.buttonJoin = directionalLayoutWidget2.add(ButtonWidget.builder(
				Text.translatable("selectServer.select"), button -> {
					MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
					if (entry != null) {
						entry.connect();
					}
				}
		).width(100).build());
		directionalLayoutWidget2.add(ButtonWidget.builder(
				Text.translatable("selectServer.direct"), button -> {
					this.selectedEntry =
							new ServerInfo(I18n.translate("selectServer.defaultName"), "", ServerInfo.ServerType.OTHER);
					this.client.setScreen(new DirectConnectScreen(this, this::directConnect, this.selectedEntry));
				}
		).width(100).build());
		directionalLayoutWidget2.add(ButtonWidget.builder(
				Text.translatable("selectServer.add"), button -> {
					this.selectedEntry = new ServerInfo("", "", ServerInfo.ServerType.OTHER);
					this.client.setScreen(new AddServerScreen(
							this,
							Text.translatable("manageServer.add.title"),
							this::addEntry,
							this.selectedEntry
					));
				}
		).width(100).build());
		this.buttonEdit = directionalLayoutWidget3.add(ButtonWidget.builder(
				Text.translatable("selectServer.edit"), button -> {
					MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
					if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
						ServerInfo serverInfo = ((MultiplayerServerListWidget.ServerEntry) entry).getServer();
						this.selectedEntry =
								new ServerInfo(serverInfo.name, serverInfo.address, ServerInfo.ServerType.OTHER);
						this.selectedEntry.copyWithSettingsFrom(serverInfo);
						this.client.setScreen(new AddServerScreen(
								this,
								Text.translatable("manageServer.edit.title"),
								this::editEntry,
								this.selectedEntry
						));
					}
				}
		).width(74).build());
		this.buttonDelete = directionalLayoutWidget3.add(ButtonWidget.builder(
				Text.translatable("selectServer.delete"), button -> {
					MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
					if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
						String string = ((MultiplayerServerListWidget.ServerEntry) entry).getServer().name;
						if (string != null) {
							Text text = Text.translatable("selectServer.deleteQuestion");
							Text text2 = Text.translatable("selectServer.deleteWarning", string);
							Text text3 = Text.translatable("selectServer.deleteButton");
							Text text4 = ScreenTexts.CANCEL;
							this.client.setScreen(new ConfirmScreen(this::removeEntry, text, text2, text3, text4));
						}
					}
				}
		).width(74).build());
		directionalLayoutWidget3.add(ButtonWidget
				.builder(Text.translatable("selectServer.refresh"), button -> this.refresh())
				.width(74)
				.build());
		directionalLayoutWidget3.add(ButtonWidget.builder(ScreenTexts.BACK, button -> this.close()).width(74).build());
		this.layout.forEachChild(element -> {
			ClickableWidget var10000 = this.addDrawableChild(element);
		});
		this.refreshWidgetPositions();
		this.updateButtonActivationStates();
	}

	@Override
	protected void refreshWidgetPositions() {
		this.layout.refreshPositions();
		if (this.serverListWidget != null) {
			this.serverListWidget.position(this.width, this.layout);
		}
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	@Override
	public void tick() {
		super.tick();
		List<LanServerInfo> list = this.lanServers.getEntriesIfUpdated();
		if (list != null) {
			this.serverListWidget.setLanServers(list);
		}

		this.serverListPinger.tick();
	}

	@Override
	public void removed() {
		if (this.lanServerDetector != null) {
			this.lanServerDetector.interrupt();
			this.lanServerDetector = null;
		}

		this.serverListPinger.cancel();
		this.serverListWidget.onRemoved();
	}

	private void refresh() {
		this.client.setScreen(new MultiplayerScreen(this.parent));
	}

	private void removeEntry(boolean confirmedAction) {
		MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
		if (confirmedAction && entry instanceof MultiplayerServerListWidget.ServerEntry) {
			this.serverList.remove(((MultiplayerServerListWidget.ServerEntry) entry).getServer());
			this.serverList.saveFile();
			this.serverListWidget.setSelected(null);
			this.serverListWidget.setServers(this.serverList);
		}

		this.client.setScreen(this);
	}

	private void editEntry(boolean confirmedAction) {
		MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
		if (confirmedAction && entry instanceof MultiplayerServerListWidget.ServerEntry) {
			ServerInfo serverInfo = ((MultiplayerServerListWidget.ServerEntry) entry).getServer();
			serverInfo.name = this.selectedEntry.name;
			serverInfo.address = this.selectedEntry.address;
			serverInfo.copyWithSettingsFrom(this.selectedEntry);
			this.serverList.saveFile();
			this.serverListWidget.setServers(this.serverList);
		}

		this.client.setScreen(this);
	}

	private void addEntry(boolean confirmedAction) {
		if (confirmedAction) {
			ServerInfo serverInfo = this.serverList.tryUnhide(this.selectedEntry.address);
			if (serverInfo != null) {
				serverInfo.copyFrom(this.selectedEntry);
				this.serverList.saveFile();
			}
			else {
				this.serverList.add(this.selectedEntry, false);
				this.serverList.saveFile();
			}

			this.serverListWidget.setSelected(null);
			this.serverListWidget.setServers(this.serverList);
		}

		this.client.setScreen(this);
	}

	private void directConnect(boolean confirmedAction) {
		if (confirmedAction) {
			ServerInfo serverInfo = this.serverList.get(this.selectedEntry.address);
			if (serverInfo == null) {
				this.serverList.add(this.selectedEntry, true);
				this.serverList.saveFile();
				this.connect(this.selectedEntry);
			}
			else {
				this.connect(serverInfo);
			}
		}
		else {
			this.client.setScreen(this);
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (super.keyPressed(input)) {
			return true;
		}
		else if (input.key() == 294) {
			this.refresh();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Connect.
	 *
	 * @param entry entry
	 */
	public void connect(ServerInfo entry) {
		ConnectScreen.connect(this, this.client, ServerAddress.parse(entry.address), entry, false, null);
	}

	/**
	 * Обновляет button activation states.
	 */
	protected void updateButtonActivationStates() {
		this.buttonJoin.active = false;
		this.buttonEdit.active = false;
		this.buttonDelete.active = false;
		MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
		if (entry != null && !(entry instanceof MultiplayerServerListWidget.ScanningEntry)) {
			this.buttonJoin.active = true;
			if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
				this.buttonEdit.active = true;
				this.buttonDelete.active = true;
			}
		}
	}

	public MultiplayerServerListPinger getServerListPinger() {
		return this.serverListPinger;
	}

	public ServerList getServerList() {
		return this.serverList;
	}
}
