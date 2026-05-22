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

/**
 * Менеджер вкладок экрана: управляет переключением между {@link Tab}-объектами,
 * регистрирует и снимает регистрацию дочерних виджетов через переданные колбэки,
 * а также обновляет позиции виджетов при изменении области вкладки.
 *
 * <p>При переключении вкладки сначала выгружаются виджеты текущей вкладки,
 * затем загружаются виджеты новой, после чего опционально воспроизводится
 * звук нажатия кнопки UI.</p>
 */
@Environment(EnvType.CLIENT)
public class TabManager {

	/** Громкость звука нажатия при переключении вкладки. */
	private static final float CLICK_SOUND_PITCH = 1.0F;

	private final Consumer<ClickableWidget> tabLoadWidgetConsumer;
	private final Consumer<ClickableWidget> tabUnloadWidgetConsumer;
	private final Consumer<Tab> tabLoadTabConsumer;
	private final Consumer<Tab> tabUnloadTabConsumer;
	private @Nullable Tab currentTab;
	private @Nullable ScreenRect tabArea;

	/**
	 * Создаёт менеджер без колбэков на загрузку/выгрузку самих вкладок.
	 *
	 * @param tabLoadWidgetConsumer   колбэк добавления виджета в экран
	 * @param tabUnloadWidgetConsumer колбэк удаления виджета из экрана
	 */
	public TabManager(
		Consumer<ClickableWidget> tabLoadWidgetConsumer,
		Consumer<ClickableWidget> tabUnloadWidgetConsumer
	) {
		this(tabLoadWidgetConsumer, tabUnloadWidgetConsumer, loadedTab -> {}, unloadedTab -> {});
	}

	/**
	 * Создаёт менеджер с полным набором колбэков.
	 *
	 * @param tabLoadWidgetConsumer   колбэк добавления виджета в экран
	 * @param tabUnloadWidgetConsumer колбэк удаления виджета из экрана
	 * @param tabLoadTabConsumer      колбэк при активации новой вкладки
	 * @param tabUnloadTabConsumer    колбэк при деактивации предыдущей вкладки
	 */
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

	/**
	 * Устанавливает область отображения вкладок и немедленно обновляет
	 * позиции виджетов текущей активной вкладки, если она есть.
	 *
	 * @param tabArea прямоугольная область экрана для содержимого вкладки
	 */
	public void setTabArea(ScreenRect tabArea) {
		this.tabArea = tabArea;

		Tab tab = getCurrentTab();
		if (tab != null) {
			tab.refreshGrid(tabArea);
		}
	}

	/**
	 * Переключает активную вкладку: выгружает виджеты предыдущей,
	 * загружает виджеты новой и при необходимости воспроизводит звук клика.
	 * Если запрошенная вкладка уже активна — ничего не происходит.
	 *
	 * @param tab        вкладка для активации
	 * @param clickSound {@code true} — воспроизвести звук нажатия кнопки UI
	 */
	public void setCurrentTab(Tab tab, boolean clickSound) {
		if (Objects.equals(currentTab, tab)) {
			return;
		}

		if (currentTab != null) {
			currentTab.forEachChild(tabUnloadWidgetConsumer);
		}

		Tab previousTab = currentTab;
		currentTab = tab;
		tab.forEachChild(tabLoadWidgetConsumer);

		if (tabArea != null) {
			tab.refreshGrid(tabArea);
		}

		if (clickSound) {
			MinecraftClient.getInstance()
				.getSoundManager()
				.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, CLICK_SOUND_PITCH));
		}

		tabUnloadTabConsumer.accept(previousTab);
		tabLoadTabConsumer.accept(currentTab);
	}

	public @Nullable Tab getCurrentTab() {
		return currentTab;
	}
}
