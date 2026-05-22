package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Состояние рендеринга карты: хранит идентификатор текстуры карты и список декораций
 * (маркеров) для отрисовки поверх неё. Заполняется перед каждым кадром рендеринга карты.
 */
@Environment(EnvType.CLIENT)
public class MapRenderState implements FabricRenderState {

	public @Nullable Identifier texture;
	public final List<MapRenderState.Decoration> decorations = new ArrayList<>();

	/** Декорация карты: спрайт маркера с позицией, поворотом и опциональным именем. */
	@Environment(EnvType.CLIENT)
	public static class Decoration implements FabricRenderState {

		public @Nullable Sprite sprite;
		public byte x;
		public byte z;
		public byte rotation;
		public boolean alwaysRendered;
		public @Nullable Text name;
	}
}
