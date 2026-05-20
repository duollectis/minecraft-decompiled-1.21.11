package net.minecraft.command.argument;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.command.permission.PermissionSourcePredicate;
import net.minecraft.registry.Registries;
import org.slf4j.Logger;

public class ArgumentHelper {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final byte MIN_FLAG = 1;
   private static final byte MAX_FLAG = 2;

   public static int getMinMaxFlag(boolean hasMin, boolean hasMax) {
      int i = 0;
      if (hasMin) {
         i |= 1;
      }

      if (hasMax) {
         i |= 2;
      }

      return i;
   }

   public static boolean hasMinFlag(byte flags) {
      return (flags & 1) != 0;
   }

   public static boolean hasMaxFlag(byte flags) {
      return (flags & 2) != 0;
   }

   private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void writeArgumentTypeProperties(
      JsonObject json, ArgumentSerializer<A, T> serializer, ArgumentSerializer.ArgumentTypeProperties<A> properties
   ) {
      serializer.writeJson((T)properties, json);
   }

   private static <T extends ArgumentType<?>> void writeArgument(JsonObject json, T argumentType) {
      ArgumentSerializer.ArgumentTypeProperties<T> argumentTypeProperties = ArgumentTypes.getArgumentTypeProperties(argumentType);
      json.addProperty("type", "argument");
      json.addProperty("parser", String.valueOf(Registries.COMMAND_ARGUMENT_TYPE.getId(argumentTypeProperties.getSerializer())));
      JsonObject jsonObject = new JsonObject();
      writeArgumentTypeProperties(jsonObject, argumentTypeProperties.getSerializer(), argumentTypeProperties);
      if (!jsonObject.isEmpty()) {
         json.add("properties", jsonObject);
      }
   }

   public static <S> JsonObject toJson(CommandDispatcher<S> dispatcher, CommandNode<S> node) {
      JsonObject jsonObject = new JsonObject();
      switch (node) {
         case RootCommandNode<S> rootCommandNode:
            jsonObject.addProperty("type", "root");
            break;
         case LiteralCommandNode<S> literalCommandNode:
            jsonObject.addProperty("type", "literal");
            break;
         case ArgumentCommandNode<S, ?> argumentCommandNode:
            writeArgument(jsonObject, argumentCommandNode.getType());
            break;
         default:
            LOGGER.error("Could not serialize node {} ({})!", node, node.getClass());
            jsonObject.addProperty("type", "unknown");
      }

      Collection<CommandNode<S>> collection = node.getChildren();
      if (!collection.isEmpty()) {
         JsonObject jsonObject2 = new JsonObject();

         for (CommandNode<S> commandNode : collection) {
            jsonObject2.add(commandNode.getName(), toJson(dispatcher, commandNode));
         }

         jsonObject.add("children", jsonObject2);
      }

      if (node.getCommand() != null) {
         jsonObject.addProperty("executable", true);
      }

      if (node.getRequirement() instanceof PermissionSourcePredicate<?> permissionSourcePredicate) {
         JsonElement jsonElement = (JsonElement)PermissionCheck.CODEC
            .encodeStart(JsonOps.INSTANCE, permissionSourcePredicate.test())
            .getOrThrow(error -> new IllegalStateException("Failed to serialize requirement: " + error));
         jsonObject.add("permissions", jsonElement);
      }

      if (node.getRedirect() != null) {
         Collection<String> collection2 = dispatcher.getPath(node.getRedirect());
         if (!collection2.isEmpty()) {
            JsonArray jsonArray = new JsonArray();

            for (String string : collection2) {
               jsonArray.add(string);
            }

            jsonObject.add("redirect", jsonArray);
         }
      }

      return jsonObject;
   }

   public static <T> Set<ArgumentType<?>> collectUsedArgumentTypes(CommandNode<T> rootNode) {
      Set<CommandNode<T>> set = new ReferenceOpenHashSet();
      Set<ArgumentType<?>> set2 = new HashSet<>();
      collectUsedArgumentTypes(rootNode, set2, set);
      return set2;
   }

   private static <T> void collectUsedArgumentTypes(CommandNode<T> node, Set<ArgumentType<?>> usedArgumentTypes, Set<CommandNode<T>> visitedNodes) {
      if (visitedNodes.add(node)) {
         if (node instanceof ArgumentCommandNode<T, ?> argumentCommandNode) {
            usedArgumentTypes.add(argumentCommandNode.getType());
         }

         node.getChildren().forEach(child -> collectUsedArgumentTypes(child, usedArgumentTypes, visitedNodes));
         CommandNode<T> commandNode = node.getRedirect();
         if (commandNode != null) {
            collectUsedArgumentTypes(commandNode, usedArgumentTypes, visitedNodes);
         }
      }
   }
}
