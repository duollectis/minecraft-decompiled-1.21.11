package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.text.Text;
import net.minecraft.util.ProgressListener;
import org.jspecify.annotations.Nullable;

/**
 * Экран отображения прогресса длительных операций (загрузка мира, сохранение и т.д.).
 * Реализует {@link ProgressListener} для получения обновлений от фоновых задач.
 */
@Environment(EnvType.CLIENT)
public class ProgressScreen extends Screen implements ProgressListener {

	private @Nullable Text title;
	private @Nullable Text task;
	private int progress;
	private boolean done;
	private final boolean closeAfterFinished;

	public ProgressScreen(boolean closeAfterFinished) {
		super(NarratorManager.EMPTY);
		this.closeAfterFinished = closeAfterFinished;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	protected boolean hasUsageText() {
		return false;
	}

	@Override
	public void setTitle(Text title) {
		setTitleAndTask(title);
	}

	@Override
	public void setTitleAndTask(Text title) {
		this.title = title;
		setTask(Text.translatable("menu.working"));
	}

	@Override
	public void setTask(Text task) {
		this.task = task;
		progressStagePercentage(0);
	}

	@Override
	public void progressStagePercentage(int percentage) {
		progress = percentage;
	}

	@Override
	public void setDone() {
		done = true;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (done) {
			if (closeAfterFinished) {
				client.setScreen(null);
			}

			return;
		}

		super.render(context, mouseX, mouseY, deltaTicks);

		if (title != null) {
			context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 70, -1);
		}

		if (task != null && progress != 0) {
			context.drawCenteredTextWithShadow(
				textRenderer,
				Text.empty().append(task).append(" " + progress + "%"),
				width / 2,
				90,
				-1
			);
		}
	}
}
