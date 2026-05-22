package net.minecraft.dialog.action;

import com.mojang.serialization.MapCodec;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Util;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Простое действие диалога, оборачивающее готовое {@link ClickEvent}.
 * <p>
 * Для каждого пользовательски определяемого типа {@link ClickEvent.Action}
 * создаётся отдельный кодек, хранящийся в {@link #CODECS}.
 *
 * @param value событие клика, которое будет выполнено при нажатии кнопки
 */
public record SimpleDialogAction(ClickEvent value) implements DialogAction {

	/**
	 * Карта кодеков для каждого пользовательски определяемого типа действия клика.
	 * Заполняется при инициализации класса.
	 */
	public static final Map<ClickEvent.Action, MapCodec<SimpleDialogAction>> CODECS = Util.make(() -> {
		Map<ClickEvent.Action, MapCodec<SimpleDialogAction>> codecs = new EnumMap<>(ClickEvent.Action.class);

		for (ClickEvent.Action action : ClickEvent.Action.class.getEnumConstants()) {
			if (action.isUserDefinable()) {
				@SuppressWarnings("unchecked")
				MapCodec<ClickEvent> clickCodec = (MapCodec<ClickEvent>) (MapCodec<?>) action.getCodec();
				codecs.put(action, clickCodec.xmap(SimpleDialogAction::new, SimpleDialogAction::value));
			}
		}

		return Collections.unmodifiableMap(codecs);
	});

	@Override
	public MapCodec<SimpleDialogAction> getCodec() {
		return CODECS.get(value.getAction());
	}

	@Override
	public Optional<ClickEvent> createClickEvent(Map<String, DialogAction.ValueGetter> valueGetters) {
		return Optional.of(value);
	}
}
