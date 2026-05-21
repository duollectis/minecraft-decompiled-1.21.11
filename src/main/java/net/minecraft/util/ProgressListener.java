package net.minecraft.util;

import net.minecraft.text.Text;

/**
 * {@code ProgressListener}.
 */
public interface ProgressListener {

	void setTitle(Text title);

	void setTitleAndTask(Text title);

	void setTask(Text task);

	void progressStagePercentage(int percentage);

	void setDone();
}
