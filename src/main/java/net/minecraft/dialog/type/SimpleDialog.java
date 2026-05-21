package net.minecraft.dialog.type;

import com.mojang.serialization.MapCodec;
import net.minecraft.dialog.DialogActionButtonData;

import java.util.List;

/**
 * {@code SimpleDialog}.
 */
public interface SimpleDialog extends Dialog {

	@Override
	MapCodec<? extends SimpleDialog> getCodec();

	List<DialogActionButtonData> getButtons();
}
