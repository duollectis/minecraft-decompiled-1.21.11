package net.minecraft.loot.provider.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootEntityValueSource;
import net.minecraft.nbt.NbtElement;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.util.context.ContextParameter;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Провайдер NBT-данных, извлекающий данные из сущности или блок-сущности контекста лута.
 */
public class ContextLootNbtProvider implements LootNbtProvider {

	private static final Codec<LootEntityValueSource<NbtElement>> TARGET_CODEC = LootEntityValueSource.createCodec(
		builder -> builder
			.forBlockEntities(ContextLootNbtProvider.BlockEntityTarget::new)
			.forEntities(ContextLootNbtProvider.EntityTarget::new)
	);

	public static final MapCodec<ContextLootNbtProvider> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(TARGET_CODEC.fieldOf("target").forGetter(provider -> provider.target))
			.apply(instance, ContextLootNbtProvider::new)
	);

	public static final Codec<ContextLootNbtProvider> INLINE_CODEC =
		TARGET_CODEC.xmap(ContextLootNbtProvider::new, provider -> provider.target);

	private final LootEntityValueSource<NbtElement> target;

	private ContextLootNbtProvider(LootEntityValueSource<NbtElement> target) {
		this.target = target;
	}

	@Override
	public LootNbtProviderType getType() {
		return LootNbtProviderTypes.CONTEXT;
	}

	@Override
	public @Nullable NbtElement getNbt(LootContext context) {
		return target.get(context);
	}

	@Override
	public Set<ContextParameter<?>> getRequiredParameters() {
		return Set.of(target.contextParam());
	}

	public static LootNbtProvider fromTarget(LootContext.EntityReference target) {
		return new ContextLootNbtProvider(new ContextLootNbtProvider.EntityTarget(target.contextParam()));
	}

	record BlockEntityTarget(ContextParameter<? extends BlockEntity> contextParam)
		implements LootEntityValueSource.ContextComponentBased<BlockEntity, NbtElement> {

		@Override
		public NbtElement get(BlockEntity blockEntity) {
			return blockEntity.createNbtWithIdentifyingData(blockEntity.getWorld().getRegistryManager());
		}
	}

	record EntityTarget(ContextParameter<? extends Entity> contextParam)
		implements LootEntityValueSource.ContextComponentBased<Entity, NbtElement> {

		@Override
		public NbtElement get(Entity entity) {
			return NbtPredicate.entityToNbt(entity);
		}
	}
}
