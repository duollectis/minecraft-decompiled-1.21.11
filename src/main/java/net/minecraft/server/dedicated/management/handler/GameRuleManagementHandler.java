package net.minecraft.server.dedicated.management.handler;

import java.util.stream.Stream;
import net.minecraft.server.dedicated.management.dispatch.GameRuleRpcDispatcher;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.world.rule.GameRule;

public interface GameRuleManagementHandler {
   <T> GameRuleRpcDispatcher.RuleEntry<T> updateRule(GameRuleRpcDispatcher.RuleEntry<T> entry, ManagementConnectionId remote);

   <T> T getValue(GameRule<T> rule);

   <T> GameRuleRpcDispatcher.RuleEntry<T> toEntry(GameRule<T> rule, T value);

   Stream<GameRule<?>> getRules();
}
