package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.sound.sampled.AudioFormat;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
/**
 * {@code AudioStream}.
 */
public interface AudioStream extends Closeable {

	AudioFormat getFormat();

	ByteBuffer read(int size) throws IOException;
}
