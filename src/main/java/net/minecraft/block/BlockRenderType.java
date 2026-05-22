package net.minecraft.block;

/**
 * Тип рендера блока. Определяет, как блок отображается на клиенте:
 * {@link #MODEL} — стандартная модель из ресурспака, {@link #INVISIBLE} — блок не рендерится.
 */
public enum BlockRenderType {
	INVISIBLE,
	MODEL;
}
