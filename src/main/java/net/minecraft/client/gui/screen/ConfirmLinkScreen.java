package net.minecraft.client.gui.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;

@Environment(EnvType.CLIENT)
/**
 * {@code ConfirmLinkScreen}.
 */
public class ConfirmLinkScreen extends ConfirmScreen {

	private static final Text WARNING = Text.translatable("chat.link.warning").withColor(-13108);
	private static final int BUTTON_WIDTH = 100;
	private final String link;
	private final boolean drawWarning;

	public ConfirmLinkScreen(BooleanConsumer callback, String link, boolean linkTrusted) {
		this(
				callback,
				getConfirmText(linkTrusted),
				Text.literal(link),
				link,
				linkTrusted ? ScreenTexts.CANCEL : ScreenTexts.NO,
				linkTrusted
		);
	}

	public ConfirmLinkScreen(BooleanConsumer callback, Text title, String link, boolean linkTrusted) {
		this(
				callback,
				title,
				getConfirmText(linkTrusted, link),
				link,
				linkTrusted ? ScreenTexts.CANCEL : ScreenTexts.NO,
				linkTrusted
		);
	}

	public ConfirmLinkScreen(BooleanConsumer callback, Text title, URI link, boolean linkTrusted) {
		this(callback, title, link.toString(), linkTrusted);
	}

	public ConfirmLinkScreen(
			BooleanConsumer callback,
			Text title,
			Text message,
			URI link,
			Text noText,
			boolean linkTrusted
	) {
		this(callback, title, message, link.toString(), noText, true);
	}

	public ConfirmLinkScreen(
			BooleanConsumer callback,
			Text title,
			Text message,
			String link,
			Text noText,
			boolean linkTrusted
	) {
		super(callback, title, message);
		this.yesText = linkTrusted ? ScreenTexts.OPEN_LINK : ScreenTexts.YES;
		this.noText = noText;
		this.drawWarning = !linkTrusted;
		this.link = link;
	}

	protected static MutableText getConfirmText(boolean linkTrusted, String link) {
		return getConfirmText(linkTrusted).append(ScreenTexts.SPACE).append(Text.literal(link));
	}

	protected static MutableText getConfirmText(boolean linkTrusted) {
		return Text.translatable(linkTrusted ? "chat.link.confirmTrusted" : "chat.link.confirm");
	}

	@Override
	protected void initExtras() {
		if (this.drawWarning) {
			this.layout.add(new TextWidget(WARNING, this.textRenderer));
		}
	}

	@Override
	protected void addButtons(DirectionalLayoutWidget layout) {
		this.yesButton =
				layout.add(ButtonWidget.builder(this.yesText, button -> this.callback.accept(true)).width(100).build());
		layout.add(ButtonWidget.builder(
				ScreenTexts.COPY, button -> {
					this.copyToClipboard();
					this.callback.accept(false);
				}
		).width(100).build());
		this.noButton =
				layout.add(ButtonWidget.builder(this.noText, button -> this.callback.accept(false)).width(100).build());
	}

	/**
	 * Создаёт копию to clipboard.
	 */
	public void copyToClipboard() {
		this.client.keyboard.setClipboard(this.link);
	}

	/**
	 * Open.
	 *
	 * @param parent parent
	 * @param url url
	 * @param linkTrusted link trusted
	 */
	public static void open(Screen parent, String url, boolean linkTrusted) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		minecraftClient.setScreen(new ConfirmLinkScreen(
				confirmed -> {
					if (confirmed) {
						Util.getOperatingSystem().open(url);
					}

					minecraftClient.setScreen(parent);
				}, url, linkTrusted
		));
	}

	/**
	 * Open.
	 *
	 * @param parent parent
	 * @param uri uri
	 * @param linkTrusted link trusted
	 */
	public static void open(Screen parent, URI uri, boolean linkTrusted) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		minecraftClient.setScreen(new ConfirmLinkScreen(
				confirmed -> {
					if (confirmed) {
						Util.getOperatingSystem().open(uri);
					}

					minecraftClient.setScreen(parent);
				}, uri.toString(), linkTrusted
		));
	}

	/**
	 * Open.
	 *
	 * @param parent parent
	 * @param uri uri
	 */
	public static void open(Screen parent, URI uri) {
		open(parent, uri, true);
	}

	/**
	 * Open.
	 *
	 * @param parent parent
	 * @param url url
	 */
	public static void open(Screen parent, String url) {
		open(parent, url, true);
	}

	public static ButtonWidget.PressAction opening(Screen parent, String url, boolean linkTrusted) {
		return button -> open(parent, url, linkTrusted);
	}

	public static ButtonWidget.PressAction opening(Screen parent, URI uri, boolean linkTrusted) {
		return button -> open(parent, uri, linkTrusted);
	}

	public static ButtonWidget.PressAction opening(Screen parent, String url) {
		return opening(parent, url, true);
	}

	public static ButtonWidget.PressAction opening(Screen parent, URI uri) {
		return opening(parent, uri, true);
	}
}
