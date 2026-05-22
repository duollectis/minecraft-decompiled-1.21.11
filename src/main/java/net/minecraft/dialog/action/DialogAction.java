package net.minecraft.dialog.action;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.text.ClickEvent;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Действие, выполняемое при нажатии кнопки диалога.
 * <p>
 * Реализации создают {@link ClickEvent} на основе текущих значений полей ввода диалога.
 * Диспетчеризация типов происходит через реестр {@code DIALOG_ACTION_TYPE}.
 */
public interface DialogAction {

	Codec<DialogAction> CODEC = Registries.DIALOG_ACTION_TYPE
		.getCodec()
		.dispatch(DialogAction::getCodec, codec -> codec);

	MapCodec<? extends DialogAction> getCodec();

	/**
	 * Создаёт событие клика на основе текущих значений полей ввода.
	 *
	 * @param valueGetters карта имён полей ввода к их текущим значениям
	 * @return событие клика, или {@link Optional#empty()} если действие неприменимо
	 */
	Optional<ClickEvent> createClickEvent(Map<String, ValueGetter> valueGetters);

	/**
	 * Поставщик значения поля ввода диалога в строковом и NBT-форматах.
	 */
	interface ValueGetter {

		String get();

		NbtElement getAsNbt();

		/**
		 * Преобразует карту поставщиков значений в карту строковых значений.
		 *
		 * @param valueGetters карта поставщиков значений
		 * @return карта строковых значений
		 */
		static Map<String, String> resolveAll(Map<String, ValueGetter> valueGetters) {
			return Maps.transformValues(valueGetters, ValueGetter::get);
		}

		/**
		 * Создаёт поставщик из фиксированной строки.
		 *
		 * @param value фиксированное строковое значение
		 * @return поставщик, всегда возвращающий данную строку
		 */
		static ValueGetter of(String value) {
			return new ValueGetter() {
				@Override
				public String get() {
					return value;
				}

				@Override
				public NbtElement getAsNbt() {
					return NbtString.of(value);
				}
			};
		}

		/**
		 * Создаёт поставщик из ленивого поставщика строки.
		 *
		 * @param valueSupplier поставщик строкового значения
		 * @return поставщик, делегирующий вычисление значения
		 */
		static ValueGetter of(Supplier<String> valueSupplier) {
			return new ValueGetter() {
				@Override
				public String get() {
					return valueSupplier.get();
				}

				@Override
				public NbtElement getAsNbt() {
					return NbtString.of(valueSupplier.get());
				}
			};
		}
	}
}
