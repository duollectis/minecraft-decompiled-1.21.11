package net.minecraft.dialog.type;

import com.mojang.serialization.MapCodec;
import net.minecraft.dialog.DialogActionButtonData;

import java.util.List;

/**
 * Диалог с фиксированным набором кнопок действий.
 *
 * <p>Реализации этого интерфейса предоставляют конкретный список кнопок
 * через {@link #getButtons()}, которые отображаются в нижней части диалога.</p>
 */
public interface SimpleDialog extends Dialog {

	@Override
	MapCodec<? extends SimpleDialog> getCodec();

	List<DialogActionButtonData> getButtons();
}
