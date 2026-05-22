package net.minecraft.util;

import net.minecraft.text.Text;

/**
 * Слушатель прогресса длительных операций (загрузка мира, перезагрузка ресурсов и т.д.).
 * Используется для обновления экрана загрузки на клиенте.
 */
public interface ProgressListener {

	void setTitle(Text title);

	void setTitleAndTask(Text title);

	void setTask(Text task);

	void progressStagePercentage(int percentage);

	void setDone();
}
