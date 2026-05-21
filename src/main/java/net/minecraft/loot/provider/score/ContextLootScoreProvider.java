package net.minecraft.loot.provider.score;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.context.LootContext;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.util.context.ContextParameter;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * {@code ContextLootScoreProvider}.
 */
public record ContextLootScoreProvider(LootContext.EntityReference target) implements LootScoreProvider {

	public static final MapCodec<ContextLootScoreProvider> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(LootContext.EntityReference.CODEC
							.fieldOf("target")
							.forGetter(ContextLootScoreProvider::target))
					.apply(instance, ContextLootScoreProvider::new)
	);
	public static final Codec<ContextLootScoreProvider> INLINE_CODEC = LootContext.EntityReference.CODEC
			.xmap(ContextLootScoreProvider::new, ContextLootScoreProvider::target);

	public static LootScoreProvider create(LootContext.EntityReference target) {
		return new ContextLootScoreProvider(target);
	}

	@Override
	public LootScoreProviderType getType() {
		return LootScoreProviderTypes.CONTEXT;
	}

	@Override
	public @Nullable ScoreHolder getScoreHolder(LootContext context) {
		return context.get(this.target.contextParam());
	}

	@Override
	public Set<ContextParameter<?>> getRequiredParameters() {
		return Set.of(this.target.contextParam());
	}
}
