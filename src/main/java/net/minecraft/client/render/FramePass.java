package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.ClosableFactory;
import net.minecraft.client.util.Handle;

/**
 * Представляет один проход рендеринга в графе кадра ({@link FrameGraphBuilder}).
 * Каждый проход объявляет зависимости от ресурсов и других проходов,
 * а также задаёт {@link Runnable}-рендерер, выполняемый при обходе графа.
 */
@Environment(EnvType.CLIENT)
public interface FramePass {

	<T> Handle<T> addRequiredResource(String name, ClosableFactory<T> factory);

	<T> void dependsOn(Handle<T> handle);

	<T> Handle<T> transfer(Handle<T> handle);

	void addRequired(FramePass pass);

	void markToBeVisited();

	void setRenderer(Runnable renderer);
}
