package net.minecraft.client.sound;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Environment(EnvType.CLIENT)
/**
 * {@code SoundLoader}.
 */
public class SoundLoader {

	private final ResourceFactory resourceFactory;
	private final Map<Identifier, CompletableFuture<StaticSound>> loadedSounds = Maps.newHashMap();

	public SoundLoader(ResourceFactory resourceFactory) {
		this.resourceFactory = resourceFactory;
	}

	public CompletableFuture<StaticSound> loadStatic(Identifier id) {
		return this.loadedSounds.computeIfAbsent(
				id, id2 -> CompletableFuture.supplyAsync(
						() -> {
							try {
								StaticSound var5;
								try (
										InputStream inputStream = this.resourceFactory.open(id2);
										NonRepeatingAudioStream nonRepeatingAudioStream = new OggAudioStream(inputStream);
								) {
									ByteBuffer byteBuffer = nonRepeatingAudioStream.readAll();
									var5 = new StaticSound(byteBuffer, nonRepeatingAudioStream.getFormat());
								}

								return var5;
							}
							catch (IOException var10) {
								throw new CompletionException(var10);
							}
						}, Util.getDownloadWorkerExecutor()
				)
		);
	}

	public CompletableFuture<AudioStream> loadStreamed(Identifier id, boolean repeatInstantly) {
		return CompletableFuture.supplyAsync(
				() -> {
					try {
						InputStream inputStream = this.resourceFactory.open(id);
						return (AudioStream) (repeatInstantly ? new RepeatingAudioStream(
								OggAudioStream::new,
								inputStream
						) : new OggAudioStream(inputStream)
						);
					}
					catch (IOException var4) {
						throw new CompletionException(var4);
					}
				}, Util.getDownloadWorkerExecutor()
		);
	}

	public void close() {
		this.loadedSounds.values().forEach(soundFuture -> soundFuture.thenAccept(StaticSound::close));
		this.loadedSounds.clear();
	}

	public CompletableFuture<?> loadStatic(Collection<Sound> sounds) {
		return CompletableFuture.allOf(sounds
				.stream()
				.map(sound -> this.loadStatic(sound.getLocation()))
				.toArray(CompletableFuture[]::new));
	}
}
