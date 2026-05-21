package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@code TextComponentHoverAndClickEventFix}.
 */
public class TextComponentHoverAndClickEventFix extends DataFix {

	public TextComponentHoverAndClickEventFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	@SuppressWarnings("unchecked")
	protected TypeRewriteRule makeRule() {
		Type<? extends Pair<String, ?>>
				type =
				(Type<? extends Pair<String, ?>>) this
						.getInputSchema()
						.getType(TypeReferences.TEXT_COMPONENT)
						.findFieldType("hoverEvent");
		return this.buildRewriteRule(
				this.getInputSchema().getTypeRaw(TypeReferences.TEXT_COMPONENT),
				this.getOutputSchema().getType(TypeReferences.TEXT_COMPONENT),
				type
		);
	}

	private <C1, C2, H extends Pair<String, ?>> TypeRewriteRule buildRewriteRule(
			Type<C1> type,
			Type<C2> type2,
			Type<H> type3
	) {
		Type<Pair<String, Either<Either<String, List<C1>>, Pair<Either<List<C1>, Unit>, Pair<Either<C1, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>>
				type4 =
				DSL.named(
						TypeReferences.TEXT_COMPONENT.typeName(),
						DSL.or(
								DSL.or(DSL.string(), DSL.list(type)),
								DSL.and(
										DSL.optional(DSL.field("extra", DSL.list(type))),
										DSL.optional(DSL.field("separator", type)),
										DSL.optional(DSL.field("hoverEvent", type3)),
										DSL.remainderType()
								)
						)
				);
		if (!type4.equals(this.getInputSchema().getType(TypeReferences.TEXT_COMPONENT))) {
			throw new IllegalStateException(
					"Text component type did not match, expected " + type4 + " but got " + this
							.getInputSchema()
							.getType(TypeReferences.TEXT_COMPONENT)
			);
		}
		else {
			Type<?> type5 = FixUtil.withTypeChanged(type4, type4, type2);
			@SuppressWarnings("unchecked")
			Type<C2> type6 = (Type<C2>) type5;
			return buildTextComponentRule(type4, type2, type6);
		}
	}

	@SuppressWarnings("unchecked")
	private <A, B> TypeRewriteRule buildTextComponentRule(Type<A> inputType, Type<B> outputType, Type<B> mappedType) {
		return this.fixTypeEverywhere(
				"TextComponentHoverAndClickEventFix", inputType, outputType, dynamicOps -> pair -> {
					boolean bl = (Boolean) ((Either) ((Pair<?, ?>) pair).getSecond()).map(
							either -> false, pairx -> {
								Pair<?, Dynamic<?>>
										pair2 =
										(Pair<?, Dynamic<?>>) ((Pair<?, ?>) ((Pair<?, ?>) pairx).getSecond()).getSecond();
								boolean blx = ((Either<?, ?>) pair2.getFirst()).left().isPresent();
								boolean bl2 = ((Dynamic<?>) pair2.getSecond()).get("clickEvent").result().isPresent();
								return blx || bl2;
							}
					);
					return bl
					       ? (B) Util
					             .apply(
							             FixUtil.withType(mappedType, (A) pair, dynamicOps),
							             outputType,
							             TextComponentHoverAndClickEventFix::renameEventFields
					             )
					             .getValue()
					       : (B) pair;
				}
		);
	}

	private static Dynamic<?> renameEventFields(Dynamic<?> dynamic) {
		return dynamic.renameAndFixField("hoverEvent", "hover_event", TextComponentHoverAndClickEventFix::fixHoverEvent)
		              .renameAndFixField("clickEvent", "click_event", TextComponentHoverAndClickEventFix::fixClickEvent);
	}

	private static Dynamic<?> copyFields(Dynamic<?> dynamic, Dynamic<?> dynamic2, String... strings) {
		for (String string : strings) {
			dynamic = Dynamic.copyField(dynamic2, string, dynamic, string);
		}

		return dynamic;
	}

	private static Dynamic<?> fixHoverEvent(Dynamic<?> dynamic) {
		String string = dynamic.get("action").asString("");

		return switch (string) {
			case "show_text" -> dynamic.renameField("contents", "value");
			case "show_item" -> {
				Dynamic<?> dynamic2 = dynamic.get("contents").orElseEmptyMap();
				Optional<String> optional = dynamic2.asString().result();
				yield optional.isPresent()
				      ? dynamic.renameField("contents", "id")
				      : copyFields(dynamic.remove("contents"), dynamic2, "id", "count", "components");
			}
			case "show_entity" -> {
				Dynamic<?> dynamic2 = dynamic.get("contents").orElseEmptyMap();
				yield copyFields(dynamic.remove("contents"), dynamic2, "id", "type", "name")
						.renameField("id", "uuid")
						.renameField("type", "id");
			}
			default -> dynamic;
		};
	}

	private static <T> @Nullable Dynamic<T> fixClickEvent(Dynamic<T> dynamic) {
		String string = dynamic.get("action").asString("");
		String string2 = dynamic.get("value").asString("");

		return switch (string) {
			case "open_url" -> !isValidUrl(string2) ? null : dynamic.renameField("value", "url");
			case "open_file" -> dynamic.renameField("value", "path");
			case "run_command", "suggest_command" ->
					!isValidCommand(string2) ? null : dynamic.renameField("value", "command");
			case "change_page" -> {
				Integer
						integer =
						dynamic
								.get("value")
								.result()
								.map(TextComponentHoverAndClickEventFix::parsePageNumber)
								.orElse(null);
				if (integer == null) {
					yield null;
				}
				else {
					int i = Math.max(integer, 1);
					yield dynamic.remove("value").set("page", dynamic.createInt(i));
				}
			}
			default -> dynamic;
		};
	}

	private static @Nullable Integer parsePageNumber(Dynamic<?> dynamic) {
		Optional<Number> optional = dynamic.asNumber().result();
		if (optional.isPresent()) {
			return optional.get().intValue();
		}
		else {
			try {
				return Integer.parseInt(dynamic.asString(""));
			}
			catch (Exception var3) {
				return null;
			}
		}
	}

	private static boolean isValidUrl(String string) {
		try {
			URI uRI = new URI(string);
			String string2 = uRI.getScheme();
			if (string2 == null) {
				return false;
			}
			else {
				String string3 = string2.toLowerCase(Locale.ROOT);
				return "http".equals(string3) || "https".equals(string3);
			}
		}
		catch (URISyntaxException var4) {
			return false;
		}
	}

	private static boolean isValidCommand(String string) {
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == 167 || c < ' ' || c == 127) {
				return false;
			}
		}

		return true;
	}
}
