package net.minecraft.datafixer.fix;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Удаляет некорректные данные datafixer из текста вывески: если флаг {@code _filtered_correct}
 * установлен — просто убирает его; иначе — очищает {@code filtered_messages} от пустых записей,
 * заменяя их соответствующими значениями из {@code messages}.
 */
public class DropInvalidSignDatafixDataFix extends DataFix {

	private final String blockEntityName;

	public DropInvalidSignDatafixDataFix(Schema outputSchema, String blockEntityName) {
		super(outputSchema, false);
		this.blockEntityName = blockEntityName;
	}

	private <T> Dynamic<T> dropInvalidDatafixData(Dynamic<T> signData) {
		signData = signData.update("front_text", DropInvalidSignDatafixDataFix::dropInvalidDatafixDataOnSide);
		signData = signData.update("back_text", DropInvalidSignDatafixDataFix::dropInvalidDatafixDataOnSide);

		for (String side : UpdateSignTextFormatFix.SIGN_SIDES) {
			signData = signData.remove(side);
		}

		return signData;
	}

	private static <T> Dynamic<T> dropInvalidDatafixDataOnSide(Dynamic<T> textData) {
		Optional<Stream<Dynamic<T>>> filteredMessages = textData.get("filtered_messages").asStreamOpt().result();

		if (filteredMessages.isEmpty()) {
			return textData;
		}

		Dynamic<T> emptyText = TextFixes.empty(textData.getOps());
		List<Dynamic<T>> messages = textData.get("messages").asStreamOpt().result().orElse(Stream.of()).toList();
		List<Dynamic<T>> fixedFiltered = Streams.mapWithIndex(
				filteredMessages.get(),
				(message, index) -> {
					Dynamic<T> fallback = index < messages.size() ? messages.get((int) index) : emptyText;
					return message.equals(emptyText) ? fallback : message;
				}
		).toList();

		return fixedFiltered.equals(messages)
				? textData.remove("filtered_messages")
				: textData.set("filtered_messages", textData.createList(fixedFiltered.stream()));
	}

	public TypeRewriteRule makeRule() {
		Type<?> blockEntityType = getInputSchema().getType(TypeReferences.BLOCK_ENTITY);
		Type<?> signType = getInputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, blockEntityName);
		OpticFinder<?> signFinder = DSL.namedChoice(blockEntityName, signType);

		return fixTypeEverywhereTyped(
				"DropInvalidSignDataFix for " + blockEntityName,
				blockEntityType,
				typed -> typed.updateTyped(
						signFinder,
						signType,
						signTyped -> {
							boolean isFilteredCorrect = ((Dynamic) signTyped.get(DSL.remainderFinder()))
									.get("_filtered_correct")
									.asBoolean(false);

							return isFilteredCorrect
									? signTyped.update(
											DSL.remainderFinder(),
											dynamic -> dynamic.remove("_filtered_correct")
									)
									: Util.apply(signTyped, signType, this::dropInvalidDatafixData);
						}
				)
		);
	}
}
