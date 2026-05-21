package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
/**
 * {@code NonRepeatingAudioStream}.
 */
public interface NonRepeatingAudioStream extends AudioStream {

	ByteBuffer readAll() throws IOException;
}
