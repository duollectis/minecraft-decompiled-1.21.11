package net.minecraft.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Optional;

/**
 * Элемент управления выбором одного варианта из списка.
 * <p>
 * Список вариантов не может быть пустым. Не более одного варианта может быть
 * помечен как начальный ({@link Entry#initial}).
 *
 * @param width        ширина элемента управления в пикселях
 * @param entries      список доступных вариантов
 * @param label        текст метки
 * @param labelVisible отображать ли метку
 */
public record SingleOptionInputControl(
	int width,
	List<Entry> entries,
	Text label,
	boolean labelVisible
) implements InputControl {

	public static final MapCodec<SingleOptionInputControl> CODEC = RecordCodecBuilder.<SingleOptionInputControl>mapCodec(
		instance -> instance.group(
			Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(SingleOptionInputControl::width),
			Codecs.nonEmptyList(Entry.CODEC.listOf()).fieldOf("options").forGetter(SingleOptionInputControl::entries),
			TextCodecs.CODEC.fieldOf("label").forGetter(SingleOptionInputControl::label),
			Codec.BOOL.optionalFieldOf("label_visible", true).forGetter(SingleOptionInputControl::labelVisible)
		).apply(instance, SingleOptionInputControl::new)
	).validate(inputControl -> {
		long initialCount = inputControl.entries.stream().filter(Entry::initial).count();
		return initialCount > 1L
			? DataResult.error(() -> "Multiple initial values")
			: DataResult.success(inputControl);
	});

	@Override
	public MapCodec<SingleOptionInputControl> getCodec() {
		return CODEC;
	}

	/**
	 * Возвращает начальный вариант, если он задан.
	 *
	 * @return опциональный начальный вариант
	 */
	public Optional<Entry> getInitialEntry() {
		return entries.stream().filter(Entry::initial).findFirst();
	}

	/**
	 * Вариант выбора в {@link SingleOptionInputControl}.
	 *
	 * @param id      строковый идентификатор варианта, передаваемый как значение
	 * @param display опциональный отображаемый текст (по умолчанию — {@code id})
	 * @param initial является ли вариант начально выбранным
	 */
	public record Entry(String id, Optional<Text> display, boolean initial) {

		public static final Codec<Entry> BASE_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.STRING.fieldOf("id").forGetter(Entry::id),
				TextCodecs.CODEC.optionalFieldOf("display").forGetter(Entry::display),
				Codec.BOOL.optionalFieldOf("initial", false).forGetter(Entry::initial)
			).apply(instance, Entry::new)
		);

		/**
		 * Альтернативный кодек: принимает либо полный объект, либо просто строку-идентификатор.
		 */
		public static final Codec<Entry> CODEC = Codec.withAlternative(
			BASE_CODEC,
			Codec.STRING,
			id -> new Entry(id, Optional.empty(), false)
		);

		/**
		 * Возвращает отображаемый текст варианта, или {@code id} как литерал если текст не задан.
		 *
		 * @return отображаемый текст
		 */
		public Text getDisplay() {
			return display.orElseGet(() -> Text.literal(id));
		}
	}
}
