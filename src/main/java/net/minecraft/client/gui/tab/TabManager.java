package net.minecraft.client.gui.tab;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code TabManager}.
 */
public class TabManager {

	private final Consumer<ClickableWidget> tabLoadWidgetConsumer;
	private final Consumer<ClickableWidget> tabUnloadWidgetConsumer;
	private final Consumer<Tab> tabLoadTabConsumer;
	private final Consumer<Tab> tabUnloadTabConsumer;
	private @Nullable Tab currentTab;
	private @Nullable ScreenRect tabArea;

	public TabManager(
			Consumer<ClickableWidget> tabLoadWidgetConsumer,
			Consumer<ClickableWidget> tabUnloadWidgetConsumer
	) {
		this(tabLoadWidgetConsumer, tabUnloadWidgetConsumer, loadedTab -> {}, unloadedTab -> {});
	}

	public TabManager(
			Consumer<ClickableWidget> tabLoadWidgetConsumer,
			Consumer<ClickableWidget> tabUnloadWidgetConsumer,
			Consumer<Tab> tabLoadTabConsumer,
			Consumer<Tab> tabUnloadTabConsumer
	) {
		this.tabLoadWidgetConsumer = tabLoadWidgetConsumer;
		this.tabUnloadWidgetConsumer = tabUnloadWidgetConsumer;
		this.tabLoadTabConsumer = tabLoadTabConsumer;
		this.tabUnloadTabConsumer = tabUnloadTabConsumer;
	}

	public void setTabArea(ScreenRect tabArea) {
		this.tabArea = tabArea;
		Tab tab = this.getCurrentTab();
		if (tab != null) {
			tab.refreshGrid(tabArea);
		}
	}

	public void setCurrentTab(Tab tab, boolean clickSound) {
		if (!Objects.equals(this.currentTab, tab)) {
			if (this.currentTab != null) {
				this.currentTab.forEachChild(this.tabUnloadWidgetConsumer);
			}

			Tab tab2 = this.currentTab;
			this.currentTab = tab;
			tab.forEachChild(this.tabLoadWidgetConsumer);
			if (this.tabArea != null) {
				tab.refreshGrid(this.tabArea);
			}

			if (clickSound) {
				MinecraftClient
						.getInstance()
						.getSoundManager()
						.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}

			this.tabUnloadTabConsumer.accept(tab2);
			this.tabLoadTabConsumer.accept(this.currentTab);
		}
	}

	public @Nullable Tab getCurrentTab() {
		return this.currentTab;
	}
}
