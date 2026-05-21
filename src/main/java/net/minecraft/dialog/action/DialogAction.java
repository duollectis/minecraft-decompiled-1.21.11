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
 * {@code DialogAction}.
 */
public interface DialogAction {

	Codec<DialogAction>
			CODEC =
			Registries.DIALOG_ACTION_TYPE.getCodec().dispatch(DialogAction::getCodec, codec -> codec);

	MapCodec<? extends DialogAction> getCodec();

	Optional<ClickEvent> createClickEvent(Map<String, DialogAction.ValueGetter> valueGetters);

	/**
	 * {@code ValueGetter}.
	 */
	public interface ValueGetter {

		String get();

		NbtElement getAsNbt();

		static Map<String, String> resolveAll(Map<String, DialogAction.ValueGetter> valueGetters) {
			return Maps.transformValues(valueGetters, DialogAction.ValueGetter::get);
		}

		static DialogAction.ValueGetter of(String value) {
			return new DialogAction.ValueGetter() {
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

		static DialogAction.ValueGetter of(Supplier<String> valueSupplier) {
			return new DialogAction.ValueGetter() {
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
