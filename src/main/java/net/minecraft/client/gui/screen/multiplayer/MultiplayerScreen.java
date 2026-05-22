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

/**
 * Экран выбора многопользовательского сервера.
 * Отображает список серверов, LAN-серверов и предоставляет кнопки управления.
 */
@Environment(EnvType.CLIENT)
public class MultiplayerScreen extends Screen {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int WIDE_BUTTON_WIDTH = 100;
	private static final int NARROW_BUTTON_WIDTH = 74;
	private static final int KEY_F5 = 294;
	private static final int SERVER_ENTRY_HEIGHT = 36;

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
		layout.addHeader(title, textRenderer);
		serverList = new ServerList(client);
		serverList.loadFile();
		lanServers = new LanServerQueryManager.LanServerEntryList();

		try {
			lanServerDetector = new LanServerQueryManager.LanServerDetector(lanServers);
			lanServerDetector.start();
		}
		catch (Exception exception) {
			LOGGER.warn("Unable to start LAN server detection: {}", exception.getMessage());
		}

		serverListWidget = layout.addBody(new MultiplayerServerListWidget(
			this,
			client,
			width,
			layout.getContentHeight(),
			layout.getHeaderHeight(),
			SERVER_ENTRY_HEIGHT
		));
		serverListWidget.setServers(serverList);

		DirectionalLayoutWidget footerLayout = layout.addFooter(DirectionalLayoutWidget.vertical().spacing(4));
		footerLayout.getMainPositioner().alignHorizontalCenter();
		DirectionalLayoutWidget topRow = footerLayout.add(DirectionalLayoutWidget.horizontal().spacing(4));
		DirectionalLayoutWidget bottomRow = footerLayout.add(DirectionalLayoutWidget.horizontal().spacing(4));

		buttonJoin = topRow.add(ButtonWidget.builder(
			Text.translatable("selectServer.select"), button -> {
				MultiplayerServerListWidget.Entry entry = serverListWidget.getSelectedOrNull();
				if (entry != null) {
					entry.connect();
				}
			}
		).width(WIDE_BUTTON_WIDTH).build());

		topRow.add(ButtonWidget.builder(
			Text.translatable("selectServer.direct"), button -> {
				selectedEntry = new ServerInfo(I18n.translate("selectServer.defaultName"), "", ServerInfo.ServerType.OTHER);
				client.setScreen(new DirectConnectScreen(this, this::directConnect, selectedEntry));
			}
		).width(WIDE_BUTTON_WIDTH).build());

		topRow.add(ButtonWidget.builder(
			Text.translatable("selectServer.add"), button -> {
				selectedEntry = new ServerInfo("", "", ServerInfo.ServerType.OTHER);
				client.setScreen(new AddServerScreen(
					this,
					Text.translatable("manageServer.add.title"),
					this::addEntry,
					selectedEntry
				));
			}
		).width(WIDE_BUTTON_WIDTH).build());

		buttonEdit = bottomRow.add(ButtonWidget.builder(
			Text.translatable("selectServer.edit"), button -> {
				MultiplayerServerListWidget.Entry entry = serverListWidget.getSelectedOrNull();
				if (entry instanceof MultiplayerServerListWidget.ServerEntry serverEntry) {
					ServerInfo serverInfo = serverEntry.getServer();
					selectedEntry = new ServerInfo(serverInfo.name, serverInfo.address, ServerInfo.ServerType.OTHER);
					selectedEntry.copyWithSettingsFrom(serverInfo);
					client.setScreen(new AddServerScreen(
						this,
						Text.translatable("manageServer.edit.title"),
						this::editEntry,
						selectedEntry
					));
				}
			}
		).width(NARROW_BUTTON_WIDTH).build());

		buttonDelete = bottomRow.add(ButtonWidget.builder(
			Text.translatable("selectServer.delete"), button -> {
				MultiplayerServerListWidget.Entry entry = serverListWidget.getSelectedOrNull();
				if (entry instanceof MultiplayerServerListWidget.ServerEntry serverEntry) {
					String serverName = serverEntry.getServer().name;
					if (serverName != null) {
						Text question = Text.translatable("selectServer.deleteQuestion");
						Text warning = Text.translatable("selectServer.deleteWarning", serverName);
						Text deleteButton = Text.translatable("selectServer.deleteButton");
						client.setScreen(new ConfirmScreen(this::removeEntry, question, warning, deleteButton, ScreenTexts.CANCEL));
					}
				}
			}
		).width(NARROW_BUTTON_WIDTH).build());

		bottomRow.add(ButtonWidget.builder(Text.translatable("selectServer.refresh"), button -> refresh())
			.width(NARROW_BUTTON_WIDTH)
			.build());

		bottomRow.add(ButtonWidget.builder(ScreenTexts.BACK, button -> close())
			.width(NARROW_BUTTON_WIDTH)
			.build());

		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
		updateButtonActivationStates();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
		if (serverListWidget != null) {
			serverListWidget.position(width, layout);
		}
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	@Override
	public void tick() {
		super.tick();
		List<LanServerInfo> updatedLanServers = lanServers.getEntriesIfUpdated();
		if (updatedLanServers != null) {
			serverListWidget.setLanServers(updatedLanServers);
		}

		serverListPinger.tick();
	}

	@Override
	public void removed() {
		if (lanServerDetector != null) {
			lanServerDetector.interrupt();
			lanServerDetector = null;
		}

		serverListPinger.cancel();
		serverListWidget.onRemoved();
	}

	private void refresh() {
		client.setScreen(new MultiplayerScreen(parent));
	}

	private void removeEntry(boolean confirmed) {
		MultiplayerServerListWidget.Entry entry = serverListWidget.getSelectedOrNull();
		if (confirmed && entry instanceof MultiplayerServerListWidget.ServerEntry serverEntry) {
			serverList.remove(serverEntry.getServer());
			serverList.saveFile();
			serverListWidget.setSelected(null);
			serverListWidget.setServers(serverList);
		}

		client.setScreen(this);
	}

	private void editEntry(boolean confirmed) {
		MultiplayerServerListWidget.Entry entry = serverListWidget.getSelectedOrNull();
		if (confirmed && entry instanceof MultiplayerServerListWidget.ServerEntry serverEntry) {
			ServerInfo serverInfo = serverEntry.getServer();
			serverInfo.name = selectedEntry.name;
			serverInfo.address = selectedEntry.address;
			serverInfo.copyWithSettingsFrom(selectedEntry);
			serverList.saveFile();
			serverListWidget.setServers(serverList);
		}

		client.setScreen(this);
	}

	private void addEntry(boolean confirmed) {
		if (confirmed) {
			ServerInfo existing = serverList.tryUnhide(selectedEntry.address);
			if (existing != null) {
				existing.copyFrom(selectedEntry);
			}
			else {
				serverList.add(selectedEntry, false);
			}

			serverList.saveFile();
			serverListWidget.setSelected(null);
			serverListWidget.setServers(serverList);
		}

		client.setScreen(this);
	}

	private void directConnect(boolean confirmed) {
		if (!confirmed) {
			client.setScreen(this);
			return;
		}

		ServerInfo existing = serverList.get(selectedEntry.address);
		if (existing == null) {
			serverList.add(selectedEntry, true);
			serverList.saveFile();
			connect(selectedEntry);
		}
		else {
			connect(existing);
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (super.keyPressed(input)) {
			return true;
		}

		if (input.key() == KEY_F5) {
			refresh();
			return true;
		}

		return false;
	}

	/**
	 * Подключается к указанному серверу через {@link ConnectScreen}.
	 *
	 * @param entry информация о сервере для подключения
	 */
	public void connect(ServerInfo entry) {
		ConnectScreen.connect(this, client, ServerAddress.parse(entry.address), entry, false, null);
	}

	/**
	 * Обновляет активность кнопок в зависимости от выбранного элемента списка.
	 */
	protected void updateButtonActivationStates() {
		buttonJoin.active = false;
		buttonEdit.active = false;
		buttonDelete.active = false;
		MultiplayerServerListWidget.Entry entry = serverListWidget.getSelectedOrNull();
		if (entry == null || entry instanceof MultiplayerServerListWidget.ScanningEntry) {
			return;
		}

		buttonJoin.active = true;
		if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
			buttonEdit.active = true;
			buttonDelete.active = true;
		}
	}

	public MultiplayerServerListPinger getServerListPinger() {
		return serverListPinger;
	}

	public ServerList getServerList() {
		return serverList;
	}
}
