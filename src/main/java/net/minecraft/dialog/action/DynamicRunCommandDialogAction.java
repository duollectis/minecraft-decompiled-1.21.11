package net.minecraft.dialog.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.ClickEvent;

import java.util.Map;
import java.util.Optional;

/**
 * Динамическое действие диалога, выполняющее команду с подстановкой значений полей ввода.
 * <p>
 * Шаблон команды {@link ParsedTemplate} содержит переменные, которые заменяются
 * текущими значениями полей ввода диалога перед выполнением команды.
 *
 * @param template шаблон команды с переменными для подстановки
 */
public record DynamicRunCommandDialogAction(ParsedTemplate template) implements DialogAction {

	public static final MapCodec<DynamicRunCommandDialogAction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(ParsedTemplate.CODEC.fieldOf("template").forGetter(DynamicRunCommandDialogAction::template))
			.apply(instance, DynamicRunCommandDialogAction::new)
	);

	@Override
	public MapCodec<DynamicRunCommandDialogAction> getCodec() {
		return CODEC;
	}

	@Override
	public Optional<ClickEvent> createClickEvent(Map<String, DialogAction.ValueGetter> valueGetters) {
		String command = template.apply(DialogAction.ValueGetter.resolveAll(valueGetters));
		return Optional.of(new ClickEvent.RunCommand(command));
	}
}
