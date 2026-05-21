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

@Environment(EnvType.CLIENT)
/**
 * {@code CodeOfConductScreen}.
 */
public class CodeOfConductScreen extends WarningScreen {

	private static final Text
			TITLE_TEXT =
			Text.translatable("multiplayer.codeOfConduct.title").formatted(Formatting.BOLD);
	private static final Text CHECK_TEXT = Text.translatable("multiplayer.codeOfConduct.check");
	private final @Nullable ServerInfo serverInfo;
	private final String rawCodeOfConduct;
	private final BooleanConsumer callback;
	private final Screen backgroundScreen;

	private CodeOfConductScreen(
			@Nullable ServerInfo serverInfo,
			Screen screen,
			Text text,
			String string,
			BooleanConsumer booleanConsumer
	) {
		super(TITLE_TEXT, text, CHECK_TEXT, TITLE_TEXT.copy().append("\n").append(text));
		this.serverInfo = serverInfo;
		this.backgroundScreen = screen;
		this.rawCodeOfConduct = string;
		this.callback = booleanConsumer;
	}

	public CodeOfConductScreen(
			@Nullable ServerInfo serverInfo,
			Screen screen,
			String string,
			BooleanConsumer booleanConsumer
	) {
		this(serverInfo, screen, Text.literal(string), string, booleanConsumer);
	}

	@Override
	protected LayoutWidget getLayout() {
		DirectionalLayoutWidget directionalLayoutWidget = DirectionalLayoutWidget.horizontal().spacing(8);
		directionalLayoutWidget.add(ButtonWidget
				.builder(ScreenTexts.ACKNOWLEDGE, button -> this.onAnswer(true))
				.build());
		directionalLayoutWidget.add(ButtonWidget
				.builder(ScreenTexts.DISCONNECT, button -> this.onAnswer(false))
				.build());
		return directionalLayoutWidget;
	}

	private void onAnswer(boolean acknowledged) {
		this.callback.accept(acknowledged);
		if (this.serverInfo != null) {
			if (acknowledged && this.checkbox.isChecked()) {
				this.serverInfo.setAcceptedCodeOfConduct(this.rawCodeOfConduct);
			}
			else {
				this.serverInfo.resetAcceptedCodeOfConduct();
			}

			ServerList.updateServerListEntry(this.serverInfo);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.backgroundScreen instanceof ConnectScreen || this.backgroundScreen instanceof ReconfiguringScreen) {
			this.backgroundScreen.tick();
		}
	}
}
