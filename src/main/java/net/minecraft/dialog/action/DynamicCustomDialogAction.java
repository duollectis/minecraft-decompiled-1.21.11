package net.minecraft.dialog.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;

/**
 * Динамическое пользовательское действие диалога, отправляющее кастомное событие клика.
 * <p>
 * Создаёт {@link ClickEvent.Custom} с заданным идентификатором и NBT-данными,
 * дополненными текущими значениями полей ввода диалога.
 *
 * @param id        идентификатор кастомного события
 * @param additions базовые NBT-данные, к которым добавляются значения полей ввода
 */
public record DynamicCustomDialogAction(Identifier id, Optional<NbtCompound> additions) implements DialogAction {

	public static final MapCodec<DynamicCustomDialogAction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(DynamicCustomDialogAction::id),
			NbtCompound.CODEC.optionalFieldOf("additions").forGetter(DynamicCustomDialogAction::additions)
		).apply(instance, DynamicCustomDialogAction::new)
	);

	@Override
	public MapCodec<DynamicCustomDialogAction> getCodec() {
		return CODEC;
	}

	@Override
	public Optional<ClickEvent> createClickEvent(Map<String, DialogAction.ValueGetter> valueGetters) {
		NbtCompound payload = additions.<NbtCompound>map(NbtCompound::copy).orElseGet(NbtCompound::new);
		valueGetters.forEach((key, getter) -> payload.put(key, getter.getAsNbt()));
		return Optional.of(new ClickEvent.Custom(id, Optional.of(payload)));
	}
}
