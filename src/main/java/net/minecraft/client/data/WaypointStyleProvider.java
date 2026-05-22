package net.minecraft.client.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.waypoint.WaypointStyleAsset;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.waypoint.WaypointStyle;
import net.minecraft.world.waypoint.WaypointStyles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Провайдер данных, генерирующий JSON-определения стилей путевых точек (waypoint styles)
 * в ресурс-пак. Регистрирует все стандартные стили через {@link #bootstrap}.
 */
@Environment(EnvType.CLIENT)
public class WaypointStyleProvider implements DataProvider {

	/** Ширина спрайта стиля DEFAULT (в пикселях). */
	private static final int DEFAULT_SPRITE_WIDTH = 128;

	/** Ширина спрайта стиля BOWTIE (в пикселях). */
	private static final int BOWTIE_SPRITE_WIDTH = 64;

	/** Высота спрайт-листа, общая для всех стилей (в пикселях). */
	private static final int SPRITE_SHEET_HEIGHT = 332;

	private final DataOutput.PathResolver pathResolver;

	public WaypointStyleProvider(DataOutput output) {
		pathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "waypoint_style");
	}

	private static void bootstrap(BiConsumer<RegistryKey<WaypointStyle>, WaypointStyleAsset> consumer) {
		consumer.accept(
				WaypointStyles.DEFAULT,
				new WaypointStyleAsset(
						DEFAULT_SPRITE_WIDTH,
						SPRITE_SHEET_HEIGHT,
						List.of(
								Identifier.ofVanilla("default_0"),
								Identifier.ofVanilla("default_1"),
								Identifier.ofVanilla("default_2"),
								Identifier.ofVanilla("default_3")
						)
				)
		);

		consumer.accept(
				WaypointStyles.BOWTIE,
				new WaypointStyleAsset(
						BOWTIE_SPRITE_WIDTH,
						SPRITE_SHEET_HEIGHT,
						List.of(
								Identifier.ofVanilla("bowtie"),
								Identifier.ofVanilla("default_0"),
								Identifier.ofVanilla("default_1"),
								Identifier.ofVanilla("default_2"),
								Identifier.ofVanilla("default_3")
						)
				)
		);
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		Map<RegistryKey<WaypointStyle>, WaypointStyleAsset> styles = new HashMap<>();

		bootstrap((key, asset) -> {
			if (styles.putIfAbsent(key, asset) != null) {
				throw new IllegalStateException("Tried to register waypoint style twice for id: " + key);
			}
		});

		return DataProvider.writeAllToPath(writer, WaypointStyleAsset.CODEC, pathResolver::resolveJson, styles);
	}

	@Override
	public String getName() {
		return "Waypoint Style Definitions";
	}
}
