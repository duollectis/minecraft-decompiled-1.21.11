package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.text.Text;
import net.minecraft.util.ProgressListener;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ProgressScreen}.
 */
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
		this.setTitleAndTask(title);
	}

	@Override
	public void setTitleAndTask(Text title) {
		this.title = title;
		this.setTask(Text.translatable("menu.working"));
	}

	@Override
	public void setTask(Text task) {
		this.task = task;
		this.progressStagePercentage(0);
	}

	@Override
	public void progressStagePercentage(int percentage) {
		this.progress = percentage;
	}

	@Override
	public void setDone() {
		this.done = true;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (this.done) {
			if (this.closeAfterFinished) {
				this.client.setScreen(null);
			}
		}
		else {
			super.render(context, mouseX, mouseY, deltaTicks);
			if (this.title != null) {
				context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 70, -1);
			}

			if (this.task != null && this.progress != 0) {
				context.drawCenteredTextWithShadow(
						this.textRenderer,
						Text.empty().append(this.task).append(" " + this.progress + "%"),
						this.width / 2,
						90,
						-1
				);
			}
		}
	}
}
