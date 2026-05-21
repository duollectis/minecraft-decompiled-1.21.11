package net.minecraft.world.gen.feature.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * {@code FeatureDebugLogger}.
 */
public class FeatureDebugLogger {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final LoadingCache<ServerWorld, FeatureDebugLogger.Features> FEATURES = CacheBuilder.newBuilder()
	                                                                                                   .weakKeys()
	                                                                                                   .expireAfterAccess(
			                                                                                                   5L,
			                                                                                                   TimeUnit.MINUTES
	                                                                                                   )
	                                                                                                   .build(new CacheLoader<ServerWorld, FeatureDebugLogger.Features>() {
		                                                                                                   public FeatureDebugLogger.Features load(
				                                                                                                   ServerWorld serverWorld
		                                                                                                   ) {
			                                                                                                   return new FeatureDebugLogger.Features(
					                                                                                                   Object2IntMaps.synchronize(
							                                                                                                   new Object2IntOpenHashMap()),
					                                                                                                   new MutableInt(
							                                                                                                   0)
			                                                                                                   );
		                                                                                                   }
	                                                                                                   });

	public static void incrementTotalChunksCount(ServerWorld world) {
		try {
			((FeatureDebugLogger.Features) FEATURES.get(world)).chunksWithFeatures().increment();
		}
		catch (Exception var2) {
			LOGGER.error("Failed to increment chunk count", var2);
		}
	}

	public static void incrementFeatureCount(
			ServerWorld world,
			ConfiguredFeature<?, ?> configuredFeature,
			Optional<PlacedFeature> placedFeature
	) {
		try {
			((FeatureDebugLogger.Features) FEATURES.get(world))
					.featureData()
					.computeInt(
							new FeatureDebugLogger.FeatureData(configuredFeature, placedFeature),
							(featureData, count) -> count == null ? 1 : count + 1
					);
		}
		catch (Exception var4) {
			LOGGER.error("Failed to increment feature count", var4);
		}
	}

	public static void clear() {
		FEATURES.invalidateAll();
		LOGGER.debug("Cleared feature counts");
	}

	public static void dump() {
		LOGGER.debug("Logging feature counts:");
		FEATURES.asMap()
		        .forEach(
				        (world, features) -> {
					        String string = world.getRegistryKey().getValue().toString();
					        boolean bl = world.getServer().isRunning();
					        Registry<PlacedFeature>
							        registry =
							        world.getRegistryManager().getOrThrow(RegistryKeys.PLACED_FEATURE);
					        String string2 = (bl ? "running" : "dead") + " " + string;
					        int i = features.chunksWithFeatures().intValue();
					        LOGGER.debug("{} total_chunks: {}", string2, i);
					        features.featureData()
					                .forEach(
							                (featureData, count) -> LOGGER.debug(
									                "{} {} {} {} {} {}",
									                new Object[]{
											                string2,
											                String.format(Locale.ROOT, "%10d", count),
											                String.format(Locale.ROOT, "%10f", (double) count / i),
											                featureData.topFeature().flatMap(registry::getKey).map(
													                RegistryKey::getValue),
											                featureData.feature().feature(),
											                featureData.feature()
									                }
							                )
					                );
				        }
		        );
	}

	/**
	 * {@code FeatureData}.
	 */
	record FeatureData(ConfiguredFeature<?, ?> feature, Optional<PlacedFeature> topFeature) {
	}

	/**
	 * {@code Features}.
	 */
	record Features(Object2IntMap<FeatureDebugLogger.FeatureData> featureData, MutableInt chunksWithFeatures) {
	}
}
