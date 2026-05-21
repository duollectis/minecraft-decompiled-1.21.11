package net.minecraft.client.gui.tab;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.LoadingWidget;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code LoadingTab}.
 */
public class LoadingTab implements Tab {

	private final Text title;
	private final Text narratedHint;
	protected final DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical();

	public LoadingTab(TextRenderer textRenderer, Text title, Text narratedHint) {
		this.title = title;
		this.narratedHint = narratedHint;
		LoadingWidget loadingWidget = new LoadingWidget(textRenderer, narratedHint);
		this.layout.getMainPositioner().alignVerticalCenter().alignHorizontalCenter();
		this.layout.add(loadingWidget, positioner -> positioner.marginBottom(30));
	}

	@Override
	public Text getTitle() {
		return this.title;
	}

	@Override
	public Text getNarratedHint() {
		return this.narratedHint;
	}

	@Override
	public void forEachChild(Consumer<ClickableWidget> consumer) {
		this.layout.forEachChild(consumer);
	}

	@Override
	public void refreshGrid(ScreenRect tabArea) {
		this.layout.refreshPositions();
		SimplePositioningWidget.setPos(this.layout, tabArea, 0.5F, 0.5F);
	}
}
