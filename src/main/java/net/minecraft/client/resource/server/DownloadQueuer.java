package net.minecraft.client.resource.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Downloader;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code DownloadQueuer}.
 */
public interface DownloadQueuer {

	void enqueue(Map<UUID, Downloader.DownloadEntry> entries, Consumer<Downloader.DownloadResult> callback);
}
