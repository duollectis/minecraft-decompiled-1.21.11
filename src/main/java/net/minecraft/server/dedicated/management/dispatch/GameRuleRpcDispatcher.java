package net.minecraft.server.dedicated.management.dispatch;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.server.dedicated.management.RpcException;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Класс Game Rule Rpc Dispatcher.
 */
public class GameRuleRpcDispatcher {

	public static List<GameRuleRpcDispatcher.RuleEntry<?>> get(ManagementHandlerDispatcher dispatcher) {
		List<GameRuleRpcDispatcher.RuleEntry<?>> list = new ArrayList<>();
		dispatcher.getGameRuleHandler().getRules().forEach(rule -> add(dispatcher, (GameRule<?>) rule, list));
		return list;
	}

	private static <T> void add(
			ManagementHandlerDispatcher dispatcher,
			GameRule<T> rule,
			List<GameRuleRpcDispatcher.RuleEntry<?>> results
	) {
		T object = dispatcher.getGameRuleHandler().getValue(rule);
		results.add(toEntry(dispatcher, rule, Objects.requireNonNull(object)));
	}

	public static <T> GameRuleRpcDispatcher.RuleEntry<T> toEntry(
			ManagementHandlerDispatcher dispatcher,
			GameRule<T> rule,
			T value
	) {
		return dispatcher.getGameRuleHandler().toEntry(rule, value);
	}

	public static <T> GameRuleRpcDispatcher.RuleEntry<T> updateRule(
			ManagementHandlerDispatcher dispatcher,
			GameRuleRpcDispatcher.RuleEntry<T> rule,
			ManagementConnectionId remote
	) {
		return dispatcher.getGameRuleHandler().updateRule(rule, remote);
	}

	public record RuleEntry<T>(GameRule<T> gameRule, T value) {

		public static final Codec<GameRuleRpcDispatcher.RuleEntry<?>> TYPED_CODEC = Registries.GAME_RULE
				.getCodec()
				.dispatch(
						"key",
						GameRuleRpcDispatcher.RuleEntry::gameRule,
						GameRuleRpcDispatcher.RuleEntry::typedCodec
				);
		public static final Codec<GameRuleRpcDispatcher.RuleEntry<?>> UNTYPED_CODEC = Registries.GAME_RULE
				.getCodec()
				.dispatch(
						"key",
						GameRuleRpcDispatcher.RuleEntry::gameRule,
						GameRuleRpcDispatcher.RuleEntry::untypedCodec
				);

		private static <T> MapCodec<? extends GameRuleRpcDispatcher.RuleEntry<T>> untypedCodec(GameRule<T> rule) {
			return rule
					.getCodec()
					.fieldOf("value")
					.xmap(
							entry -> new GameRuleRpcDispatcher.RuleEntry<>(rule, (T) entry),
							GameRuleRpcDispatcher.RuleEntry::value
					);
		}

		private static <T> MapCodec<? extends GameRuleRpcDispatcher.RuleEntry<T>> typedCodec(GameRule<T> rule) {
			return RecordCodecBuilder.mapCodec(
					instance -> instance.group(
							                    StringIdentifiable
									                    .createCodec(GameRuleType::values)
									                    .fieldOf("type")
									                    .forGetter(entry -> entry.gameRule.getType()),
							                    rule.getCodec().fieldOf("value").forGetter(GameRuleRpcDispatcher.RuleEntry::value)
					                    )
					                    .apply(instance, (type, value) -> validateType(rule, type, (T) value))
			);
		}

		private static <T> GameRuleRpcDispatcher.RuleEntry<T> validateType(
				GameRule<T> rule,
				GameRuleType type,
				T value
		) {
			if (rule.getType() != type) {
				throw new RpcException(
						"Stated type \"" + type + "\" mismatches with actual type \"" + rule.getType()
								+ "\" of gamerule \"" + rule.toShortString() + "\""
				);
			}
			else {
				return new GameRuleRpcDispatcher.RuleEntry<>(rule, value);
			}
		}
	}
}
