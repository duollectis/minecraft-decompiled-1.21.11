package net.minecraft.predicate.entity;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * {@code EntitySubPredicateTypes}.
 */
public class EntitySubPredicateTypes {

	public static final MapCodec<LightningBoltPredicate>
			LIGHTNING =
			register("lightning", LightningBoltPredicate.CODEC);
	public static final MapCodec<FishingHookPredicate>
			FISHING_HOOK =
			register("fishing_hook", FishingHookPredicate.CODEC);
	public static final MapCodec<PlayerPredicate> PLAYER = register("player", PlayerPredicate.CODEC);
	public static final MapCodec<SlimePredicate> SLIME = register("slime", SlimePredicate.CODEC);
	public static final MapCodec<RaiderPredicate> RAIDER = register("raider", RaiderPredicate.CODEC);
	public static final MapCodec<SheepPredicate> SHEEP = register("sheep", SheepPredicate.CODEC);

	private static <T extends EntitySubPredicate> MapCodec<T> register(String id, MapCodec<T> codec) {
		return Registry.register(Registries.ENTITY_SUB_PREDICATE_TYPE, id, codec);
	}

	public static MapCodec<? extends EntitySubPredicate> getDefault(Registry<MapCodec<? extends EntitySubPredicate>> registry) {
		return LIGHTNING;
	}
}
