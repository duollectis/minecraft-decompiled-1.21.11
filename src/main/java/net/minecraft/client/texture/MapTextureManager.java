package net.minecraft.client.texture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.MapColor;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

/**
 * Управляет GL-текстурами карт предметов. Для каждого уникального ID карты
 * создаётся {@link NativeImageBackedTexture} размером 128×128 пикселей,
 * которая обновляется при изменении {@link MapState}.
 */
@Environment(EnvType.CLIENT)
public class MapTextureManager implements AutoCloseable {

	private static final int MAP_SIZE = 128;

	private final Int2ObjectMap<MapTextureManager.MapTexture> texturesByMapId = new Int2ObjectOpenHashMap<>();
	final TextureManager textureManager;

	public MapTextureManager(TextureManager textureManager) {
		this.textureManager = textureManager;
	}

	public void setNeedsUpdate(MapIdComponent mapIdComponent, MapState mapState) {
		getMapTexture(mapIdComponent, mapState).setNeedsUpdate();
	}

	public Identifier getTextureId(MapIdComponent mapIdComponent, MapState mapState) {
		MapTextureManager.MapTexture mapTexture = getMapTexture(mapIdComponent, mapState);
		mapTexture.updateTexture();
		return mapTexture.textureId;
	}

	public void clear() {
		for (MapTextureManager.MapTexture mapTexture : texturesByMapId.values()) {
			mapTexture.close();
		}

		texturesByMapId.clear();
	}

	private MapTextureManager.MapTexture getMapTexture(MapIdComponent mapId, MapState mapState) {
		return texturesByMapId.compute(mapId.id(), (id, existing) -> {
			if (existing == null) {
				return new MapTextureManager.MapTexture(id, mapState);
			}

			existing.setState(mapState);
			return existing;
		});
	}

	@Override
	public void close() {
		clear();
	}

	@Environment(EnvType.CLIENT)
	class MapTexture implements AutoCloseable {

		private MapState state;
		private final NativeImageBackedTexture texture;
		private boolean needsUpdate = true;
		final Identifier textureId;

		MapTexture(final int id, final MapState state) {
			this.state = state;
			texture = new NativeImageBackedTexture(() -> "Map " + id, MAP_SIZE, MAP_SIZE, true);
			textureId = Identifier.ofVanilla("map/" + id);
			MapTextureManager.this.textureManager.registerTexture(textureId, texture);
		}

		void setState(MapState state) {
			boolean changed = this.state != state;
			this.state = state;
			needsUpdate |= changed;
		}

		public void setNeedsUpdate() {
			needsUpdate = true;
		}

		void updateTexture() {
			if (!needsUpdate) {
				return;
			}

			NativeImage image = texture.getImage();
			if (image != null) {
				for (int row = 0; row < MAP_SIZE; row++) {
					for (int col = 0; col < MAP_SIZE; col++) {
						int colorIndex = col + row * MAP_SIZE;
						image.setColorArgb(col, row, MapColor.getRenderColor(state.colors[colorIndex]));
					}
				}
			}

			texture.upload();
			needsUpdate = false;
		}

		@Override
		public void close() {
			texture.close();
		}
	}
}
