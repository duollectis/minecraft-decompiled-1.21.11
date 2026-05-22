package net.minecraft.dialog.input;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registries;

/**
 * Элемент управления вводом в диалоге.
 * <p>
 * Диспетчеризация типов происходит через реестр {@code INPUT_CONTROL_TYPE}.
 * Реализации предоставляют конкретные виджеты ввода: текст, булево значение,
 * числовой диапазон, выбор из списка.
 */
public interface InputControl {

	MapCodec<InputControl> CODEC = Registries.INPUT_CONTROL_TYPE
		.getCodec()
		.dispatchMap(InputControl::getCodec, mapCodec -> mapCodec);

	MapCodec<? extends InputControl> getCodec();
}
