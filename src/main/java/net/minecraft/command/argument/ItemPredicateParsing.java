package net.minecraft.command.argument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtParsingRule;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.packrat.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Фабрика Packrat-парсера для предикатов предметов.
 *
 * <p>Грамматика поддерживает:
 * <ul>
 *   <li>Конкретный предмет: {@code minecraft:stone}</li>
 *   <li>Тег предметов: {@code #minecraft:logs}</li>
 *   <li>Любой предмет: {@code *}</li>
 *   <li>Проверку компонента: {@code [custom_name="foo"]}</li>
 *   <li>Отрицание: {@code !minecraft:stone}</li>
 *   <li>Дизъюнкцию: {@code minecraft:stone|minecraft:dirt}</li>
 * </ul>
 */
public class ItemPredicateParsing {

	/**
	 * Создаёт {@link PackratParser} для разбора списка предикатов предметов.
	 * Каждый предикат может быть условием на тип, тег, компонент или их комбинацией.
	 *
	 * @param callbacks реализация, преобразующая разобранные идентификаторы в объекты предикатов
	 * @param <T>       тип результирующего предиката
	 * @param <C>       тип проверки компонента
	 * @param <P>       тип проверки суб-предиката
	 * @return готовый парсер, принимающий {@link StringReader}
	 */
	public static <T, C, P> PackratParser<List<T>> createParser(ItemPredicateParsing.Callbacks<T, C, P> callbacks) {
		Symbol<List<T>> top = Symbol.of("top");
		Symbol<Optional<T>> type = Symbol.of("type");
		Symbol<Unit> anyType = Symbol.of("any_type");
		Symbol<T> elementType = Symbol.of("element_type");
		Symbol<T> tagType = Symbol.of("tag_type");
		Symbol<List<T>> conditions = Symbol.of("conditions");
		Symbol<List<T>> alternatives = Symbol.of("alternatives");
		Symbol<T> term = Symbol.of("term");
		Symbol<T> negation = Symbol.of("negation");
		Symbol<T> test = Symbol.of("test");
		Symbol<C> componentType = Symbol.of("component_type");
		Symbol<P> predicateType = Symbol.of("predicate_type");
		Symbol<Identifier> id = Symbol.of("id");
		Symbol<Dynamic<?>> tag = Symbol.of("tag");

		ParsingRules<StringReader> rules = new ParsingRules<>();
		ParsingRuleEntry<StringReader, Identifier> idRule = rules.set(id, AnyIdParsingRule.INSTANCE);

		ParsingRuleEntry<StringReader, List<T>> topRule = rules.set(
				top,
				Term.anyOf(
						Term.sequence(
								rules.term(type),
								Literals.character('['),
								Term.cutting(),
								Term.optional(rules.term(conditions)),
								Literals.character(']')
						),
						rules.term(type)
				),
				results -> {
					Builder<T> builder = ImmutableList.builder();
					results.getOrThrow(type).ifPresent(builder::add);
					List<T> conditionList = results.get(conditions);

					if (conditionList != null) {
						builder.addAll(conditionList);
					}

					return builder.build();
				}
		);

		rules.set(
				type,
				Term.anyOf(
						rules.term(elementType),
						Term.sequence(Literals.character('#'), Term.cutting(), rules.term(tagType)),
						rules.term(anyType)
				),
				results -> Optional.ofNullable(results.getAny(elementType, tagType))
		);

		rules.set(anyType, Literals.character('*'), results -> Unit.INSTANCE);
		rules.set(elementType, new ItemPredicateParsing.ItemParsingRule<>(idRule, callbacks));
		rules.set(tagType, new ItemPredicateParsing.TagParsingRule<>(idRule, callbacks));

		rules.set(
				conditions,
				Term.sequence(
						rules.term(alternatives),
						Term.optional(Term.sequence(Literals.character(','), rules.term(conditions)))
				),
				results -> {
					T head = callbacks.anyOf(results.getOrThrow(alternatives));
					return Optional
							.ofNullable(results.get(conditions))
							.map(tail -> Util.withPrepended(head, (List<T>) tail))
							.orElse(List.of(head));
				}
		);

		rules.set(
				alternatives,
				Term.sequence(
						rules.term(term),
						Term.optional(Term.sequence(Literals.character('|'), rules.term(alternatives)))
				),
				results -> {
					T head = results.getOrThrow(term);
					return Optional
							.ofNullable(results.get(alternatives))
							.map(tail -> Util.withPrepended(head, (List<T>) tail))
							.orElse(List.of(head));
				}
		);

		rules.set(
				term,
				Term.anyOf(
						rules.term(test),
						Term.sequence(Literals.character('!'), rules.term(negation))
				),
				results -> results.getAnyOrThrow(test, negation)
		);

		rules.set(
				negation,
				rules.term(test),
				results -> callbacks.negate(results.getOrThrow(test))
		);

		rules.set(
				test,
				Term.anyOf(
						Term.sequence(
								rules.term(componentType),
								Literals.character('='),
								Term.cutting(),
								rules.term(tag)
						),
						Term.sequence(
								rules.term(predicateType),
								Literals.character('~'),
								Term.cutting(),
								rules.term(tag)
						),
						rules.term(componentType)
				),
				(ParsingRule.RuleAction<StringReader, T>) state -> {
					ParseResults parseResults = state.getResults();
					P predCheck = parseResults.get(predicateType);

					try {
						if (predCheck != null) {
							Dynamic<?> dynamic = parseResults.getOrThrow(tag);
							return callbacks.subPredicatePredicate(
									(ImmutableStringReader) state.getReader(),
									predCheck,
									dynamic
							);
						}

						C compCheck = parseResults.getOrThrow(componentType);
						Dynamic<?> compDynamic = parseResults.get(tag);

						return compDynamic != null
								? callbacks.componentMatchPredicate(
										(ImmutableStringReader) state.getReader(),
										compCheck,
										compDynamic
								)
								: callbacks.componentPresencePredicate(
										(ImmutableStringReader) state.getReader(),
										compCheck
								);
					}
					catch (CommandSyntaxException ex) {
						state.getErrors().add(state.getCursor(), ex);
						return null;
					}
				}
		);

		rules.set(componentType, new ItemPredicateParsing.ComponentParsingRule<>(idRule, callbacks));
		rules.set(predicateType, new ItemPredicateParsing.SubPredicateParsingRule<>(idRule, callbacks));
		rules.set(tag, new NbtParsingRule(NbtOps.INSTANCE));

		return new PackratParser<>(rules, topRule);
	}

	/**
	 * Набор колбэков для преобразования разобранных идентификаторов в объекты предикатов.
	 *
	 * @param <T> тип результирующего предиката
	 * @param <C> тип проверки компонента
	 * @param <P> тип проверки суб-предиката
	 */
	public interface Callbacks<T, C, P> {

		T itemMatchPredicate(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

		Stream<Identifier> streamItemIds();

		T tagMatchPredicate(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

		Stream<Identifier> streamTags();

		C componentCheck(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

		Stream<Identifier> streamComponentIds();

		T componentMatchPredicate(ImmutableStringReader reader, C check, Dynamic<?> dynamic)
		throws CommandSyntaxException;

		T componentPresencePredicate(ImmutableStringReader reader, C check);

		P subPredicateCheck(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

		Stream<Identifier> streamSubPredicateIds();

		T subPredicatePredicate(ImmutableStringReader reader, P check, Dynamic<?> dynamic)
		throws CommandSyntaxException;

		T negate(T predicate);

		T anyOf(List<T> predicates);

	}

	static class ComponentParsingRule<T, C, P>
			extends IdentifiableParsingRule<ItemPredicateParsing.Callbacks<T, C, P>, C> {

		ComponentParsingRule(
				ParsingRuleEntry<StringReader, Identifier> idRule,
				ItemPredicateParsing.Callbacks<T, C, P> callbacks
		) {
			super(idRule, callbacks);
		}

		@Override
		protected C parse(ImmutableStringReader reader, Identifier id) throws Exception {
			return callbacks.componentCheck(reader, id);
		}

		@Override
		public Stream<Identifier> possibleIds() {
			return callbacks.streamComponentIds();
		}

	}

	static class ItemParsingRule<T, C, P>
			extends IdentifiableParsingRule<ItemPredicateParsing.Callbacks<T, C, P>, T> {

		ItemParsingRule(
				ParsingRuleEntry<StringReader, Identifier> idRule,
				ItemPredicateParsing.Callbacks<T, C, P> callbacks
		) {
			super(idRule, callbacks);
		}

		@Override
		protected T parse(ImmutableStringReader reader, Identifier id) throws Exception {
			return callbacks.itemMatchPredicate(reader, id);
		}

		@Override
		public Stream<Identifier> possibleIds() {
			return callbacks.streamItemIds();
		}

	}

	static class SubPredicateParsingRule<T, C, P>
			extends IdentifiableParsingRule<ItemPredicateParsing.Callbacks<T, C, P>, P> {

		SubPredicateParsingRule(
				ParsingRuleEntry<StringReader, Identifier> idRule,
				ItemPredicateParsing.Callbacks<T, C, P> callbacks
		) {
			super(idRule, callbacks);
		}

		@Override
		protected P parse(ImmutableStringReader reader, Identifier id) throws Exception {
			return callbacks.subPredicateCheck(reader, id);
		}

		@Override
		public Stream<Identifier> possibleIds() {
			return callbacks.streamSubPredicateIds();
		}

	}

	static class TagParsingRule<T, C, P>
			extends IdentifiableParsingRule<ItemPredicateParsing.Callbacks<T, C, P>, T> {

		TagParsingRule(
				ParsingRuleEntry<StringReader, Identifier> idRule,
				ItemPredicateParsing.Callbacks<T, C, P> callbacks
		) {
			super(idRule, callbacks);
		}

		@Override
		protected T parse(ImmutableStringReader reader, Identifier id) throws Exception {
			return callbacks.tagMatchPredicate(reader, id);
		}

		@Override
		public Stream<Identifier> possibleIds() {
			return callbacks.streamTags();
		}

	}

}
