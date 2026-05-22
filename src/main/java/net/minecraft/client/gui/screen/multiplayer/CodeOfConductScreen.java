package net.minecraft.client.gui.screen.multiplayer;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ReconfiguringScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WarningScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.LayoutWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

/**
 * Экран кодекса поведения сервера — показывается при первом подключении к серверу
 * с установленным кодексом. Позволяет принять или отклонить условия.
 */
@Environment(EnvType.CLIENT)
public class CodeOfConductScreen extends WarningScreen {

	private static final Text TITLE_TEXT = Text.translatable("multiplayer.codeOfConduct.title").formatted(Formatting.BOLD);
	private static final Text CHECK_TEXT = Text.translatable("multiplayer.codeOfConduct.check");

	private final @Nullable ServerInfo serverInfo;
	private final String rawCodeOfConduct;
	private final BooleanConsumer callback;
	private final Screen backgroundScreen;

	private CodeOfConductScreen(
		@Nullable ServerInfo serverInfo,
		Screen screen,
		Text text,
		String rawCodeOfConduct,
		BooleanConsumer callback
	) {
		super(TITLE_TEXT, text, CHECK_TEXT, TITLE_TEXT.copy().append("\n").append(text));
		this.serverInfo = serverInfo;
		backgroundScreen = screen;
		this.rawCodeOfConduct = rawCodeOfConduct;
		this.callback = callback;
	}

	public CodeOfConductScreen(
		@Nullable ServerInfo serverInfo,
		Screen screen,
		String rawCodeOfConduct,
		BooleanConsumer callback
	) {
		this(serverInfo, screen, Text.literal(rawCodeOfConduct), rawCodeOfConduct, callback);
	}

	@Override
	protected LayoutWidget getLayout() {
		DirectionalLayoutWidget layout = DirectionalLayoutWidget.horizontal().spacing(8);
		layout.add(ButtonWidget.builder(ScreenTexts.ACKNOWLEDGE, button -> onAnswer(true)).build());
		layout.add(ButtonWidget.builder(ScreenTexts.DISCONNECT, button -> onAnswer(false)).build());
		return layout;
	}

	private void onAnswer(boolean acknowledged) {
		callback.accept(acknowledged);
		if (serverInfo == null) {
			return;
		}

		if (acknowledged && checkbox.isChecked()) {
			serverInfo.setAcceptedCodeOfConduct(rawCodeOfConduct);
		}
		else {
			serverInfo.resetAcceptedCodeOfConduct();
		}

		ServerList.updateServerListEntry(serverInfo);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (backgroundScreen instanceof ConnectScreen || backgroundScreen instanceof ReconfiguringScreen) {
			backgroundScreen.tick();
		}
	}
}
