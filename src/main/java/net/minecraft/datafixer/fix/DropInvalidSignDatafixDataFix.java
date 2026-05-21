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
 * {@code DropInvalidSignDatafixDataFix}.
 */
public class DropInvalidSignDatafixDataFix extends DataFix {

	private final String blockEntityName;

	public DropInvalidSignDatafixDataFix(Schema outputSchema, String name) {
		super(outputSchema, false);
		this.blockEntityName = name;
	}

	private <T> Dynamic<T> dropInvalidDatafixData(Dynamic<T> dynamic) {
		dynamic = dynamic.update("front_text", DropInvalidSignDatafixDataFix::dropInvalidDatafixDataOnSide);
		dynamic = dynamic.update("back_text", DropInvalidSignDatafixDataFix::dropInvalidDatafixDataOnSide);

		for (String string : UpdateSignTextFormatFix.SIGN_SIDES) {
			dynamic = dynamic.remove(string);
		}

		return dynamic;
	}

	private static <T> Dynamic<T> dropInvalidDatafixDataOnSide(Dynamic<T> textData) {
		Optional<Stream<Dynamic<T>>> optional = textData.get("filtered_messages").asStreamOpt().result();
		if (optional.isEmpty()) {
			return textData;
		}
		else {
			Dynamic<T> dynamic = TextFixes.empty(textData.getOps());
			List<Dynamic<T>> list = textData.get("messages").asStreamOpt().result().orElse(Stream.of()).toList();
			List<Dynamic<T>> list2 = Streams.mapWithIndex(
					optional.get(), (message, index) -> {
						Dynamic<T> dynamic2 = index < list.size() ? list.get((int) index) : dynamic;
						return message.equals(dynamic) ? dynamic2 : message;
					}
			).toList();
			return list2.equals(list) ? textData.remove("filtered_messages")
			                          : textData.set("filtered_messages", textData.createList(list2.stream()));
		}
	}

	public TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.BLOCK_ENTITY);
		Type<?> type2 = this.getInputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, this.blockEntityName);
		OpticFinder<?> opticFinder = DSL.namedChoice(this.blockEntityName, type2);
		return this.fixTypeEverywhereTyped(
				"DropInvalidSignDataFix for " + this.blockEntityName,
				type,
				typed -> typed.updateTyped(
						opticFinder,
						type2,
						typedx -> {
							boolean
									bl =
									((Dynamic) typedx.get(DSL.remainderFinder()))
											.get("_filtered_correct")
											.asBoolean(false);
							return bl
							       ? typedx.update(
									DSL.remainderFinder(),
									dynamic -> dynamic.remove("_filtered_correct")
							)
							       : Util.apply(typedx, type2, this::dropInvalidDatafixData);
						}
				)
		);
	}
}
