package net.minecraft.client.realms.gui.screen;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.realms.RepeatedNarrator;
import net.minecraft.client.realms.exception.RealmsDefaultUncaughtExceptionHandler;
import net.minecraft.client.realms.task.LongRunningTask;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsLongRunningMcoTaskScreen}.
 */
public class RealmsLongRunningMcoTaskScreen extends RealmsScreen {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final RepeatedNarrator NARRATOR = new RepeatedNarrator(Duration.ofSeconds(5L));
	private final List<LongRunningTask> tasks;
	private final Screen parent;
	protected final DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical();
	private volatile Text title;
	private @Nullable LoadingWidget loading;

	public RealmsLongRunningMcoTaskScreen(Screen parent, LongRunningTask... tasks) {
		super(NarratorManager.EMPTY);
		this.parent = parent;
		this.tasks = List.of(tasks);
		if (this.tasks.isEmpty()) {
			throw new IllegalArgumentException("No tasks added");
		}
		else {
			this.title = this.tasks.get(0).getTitle();
			Runnable runnable = () -> {
				for (LongRunningTask longRunningTask : tasks) {
					this.setTitle(longRunningTask.getTitle());
					if (longRunningTask.aborted()) {
						break;
					}

					longRunningTask.run();
					if (longRunningTask.aborted()) {
						return;
					}
				}
			};
			Thread thread = new Thread(runnable, "Realms-long-running-task");
			thread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
			thread.start();
		}
	}

	@Override
	public boolean canInterruptOtherScreen() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.loading != null) {
			NARRATOR.narrate(this.client.getNarratorManager(), this.loading.getMessage());
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.key() == 256) {
			this.onCancel();
			return true;
		}
		else {
			return super.keyPressed(input);
		}
	}

	@Override
	public void init() {
		this.layout.getMainPositioner().alignHorizontalCenter();
		this.layout.add(createRealmsLogoIconWidget());
		this.loading = new LoadingWidget(this.textRenderer, this.title);
		this.layout.add(this.loading, positioner -> positioner.marginTop(10).marginBottom(30));
		this.layout.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> this.onCancel()).build());
		this.layout.forEachChild(child -> {
			ClickableWidget var10000 = this.addDrawableChild(child);
		});
		this.refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		this.layout.refreshPositions();
		SimplePositioningWidget.setPos(this.layout, this.getNavigationFocus());
	}

	/**
	 * Обрабатывает событие cancel.
	 */
	protected void onCancel() {
		for (LongRunningTask longRunningTask : this.tasks) {
			longRunningTask.abortTask();
		}

		this.client.setScreen(this.parent);
	}

	public void setTitle(Text title) {
		if (this.loading != null) {
			this.loading.setMessage(title);
		}

		this.title = title;
	}
}
