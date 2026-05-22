package net.minecraft.client.realms.gui.screen;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.IconWidget;
import net.minecraft.client.realms.RealmsLabel;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Базовый абстрактный экран для всех экранов Realms.
 * Предоставляет общие константы, утилиты для меток и логотип Realms.
 */
@Environment(EnvType.CLIENT)
public abstract class RealmsScreen extends Screen {

	protected static final int STATUS_ICON_SIZE = 17;
	protected static final int STATUS_ICON_OFFSET = 7;
	protected static final long MAX_FILE_SIZE = 5368709120L;
	protected static final int DARK_GRAY = -11776948;
	protected static final int GRAY = -9671572;
	protected static final int GREEN = -8388737;
	protected static final int BLUE = -13408581;
	protected static final int PURPLE = -9670204;
	protected static final int PLAYER_HEAD_SIZE = 32;
	protected static final int SMALL_ICON_SIZE = 8;
	protected static final Identifier REALMS_LOGO_TEXTURE = Identifier.ofVanilla("textures/gui/title/realms.png");
	protected static final int REALMS_LOGO_WIDGET_WIDTH = 128;
	protected static final int REALMS_LOGO_WIDGET_HEIGHT = 34;
	protected static final int REALMS_LOGO_TEXTURE_WIDTH = 128;
	protected static final int REALMS_LOGO_TEXTURE_HEIGHT = 64;
	private final List<RealmsLabel> labels = Lists.newArrayList();

	public RealmsScreen(Text text) {
		super(text);
	}

	protected static int row(int index) {
		return 40 + index * 13;
	}

	protected RealmsLabel addLabel(RealmsLabel label) {
		this.labels.add(label);
		return this.addDrawable(label);
	}

	public Text narrateLabels() {
		return ScreenTexts.joinLines(this.labels.stream().map(RealmsLabel::getText).collect(Collectors.toList()));
	}

	protected static IconWidget createRealmsLogoIconWidget() {
		return IconWidget.create(REALMS_LOGO_TEXTURE_WIDTH, REALMS_LOGO_WIDGET_HEIGHT, REALMS_LOGO_TEXTURE, REALMS_LOGO_TEXTURE_WIDTH, REALMS_LOGO_TEXTURE_HEIGHT);
	}
}
