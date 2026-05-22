package net.minecraft.loot.provider.nbt;

import net.minecraft.loot.context.LootContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.context.ContextParameter;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/** Провайдер NBT-данных для функций лута, извлекающий данные из контекста или хранилища. */
public interface LootNbtProvider {

	@Nullable NbtElement getNbt(LootContext context);

	Set<ContextParameter<?>> getRequiredParameters();

	LootNbtProviderType getType();
}
