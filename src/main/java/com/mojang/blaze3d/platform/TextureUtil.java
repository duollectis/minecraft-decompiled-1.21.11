package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.annotation.DeobfuscateClass;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class TextureUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int MIN_MIPMAP_LEVEL = 0;
   private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;
   private static final int[][] DIRECTIONS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

   public static ByteBuffer readResource(InputStream inputStream) throws IOException {
      ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
      return readableByteChannel instanceof SeekableByteChannel seekableByteChannel
         ? readResource(readableByteChannel, (int)seekableByteChannel.size() + 1)
         : readResource(readableByteChannel, 8192);
   }

   private static ByteBuffer readResource(ReadableByteChannel channel, int bufSize) throws IOException {
      ByteBuffer byteBuffer = MemoryUtil.memAlloc(bufSize);

      try {
         while (channel.read(byteBuffer) != -1) {
            if (!byteBuffer.hasRemaining()) {
               byteBuffer = MemoryUtil.memRealloc(byteBuffer, byteBuffer.capacity() * 2);
            }
         }

         byteBuffer.flip();
         return byteBuffer;
      } catch (IOException var4) {
         MemoryUtil.memFree(byteBuffer);
         throw var4;
      }
   }

   public static void writeAsPNG(Path directory, String prefix, GpuTexture texture, int scales, IntUnaryOperator colorFunction) {
      RenderSystem.assertOnRenderThread();
      long l = 0L;

      for (int i = 0; i <= scales; i++) {
         l += (long)texture.getFormat().pixelSize() * texture.getWidth(i) * texture.getHeight(i);
      }

      if (l > 2147483647L) {
         throw new IllegalArgumentException("Exporting textures larger than 2GB is not supported");
      } else {
         GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture output buffer", 9, l);
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
         Runnable runnable = () -> {
            try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {
               int jx = 0;

               for (int kx = 0; kx <= scales; kx++) {
                  int lx = texture.getWidth(kx);
                  int m = texture.getHeight(kx);

                  try (NativeImage nativeImage = new NativeImage(lx, m, false)) {
                     for (int n = 0; n < m; n++) {
                        for (int o = 0; o < lx; o++) {
                           int p = mappedView.data().getInt(jx + (o + n * lx) * texture.getFormat().pixelSize());
                           nativeImage.setColor(o, n, colorFunction.applyAsInt(p));
                        }
                     }

                     Path path2 = directory.resolve(prefix + "_" + kx + ".png");
                     nativeImage.writeTo(path2);
                     LOGGER.debug("Exported png to: {}", path2.toAbsolutePath());
                  } catch (IOException var19) {
                     LOGGER.debug("Unable to write: ", var19);
                  }

                  jx += texture.getFormat().pixelSize() * lx * m;
               }
            }

            gpuBuffer.close();
         };
         AtomicInteger atomicInteger = new AtomicInteger();
         int j = 0;

         for (int k = 0; k <= scales; k++) {
            commandEncoder.copyTextureToBuffer(texture, gpuBuffer, j, () -> {
               if (atomicInteger.getAndIncrement() == scales) {
                  runnable.run();
               }
            }, k);
            j += texture.getFormat().pixelSize() * texture.getWidth(k) * texture.getHeight(k);
         }
      }
   }

   public static Path getDebugTexturePath(Path path) {
      return path.resolve("screenshots").resolve("debug");
   }

   public static Path getDebugTexturePath() {
      return getDebugTexturePath(Path.of("."));
   }

   public static void solidify(NativeImage image) {
      int i = image.getWidth();
      int j = image.getHeight();
      int[] is = new int[i * j];
      int[] js = new int[i * j];
      Arrays.fill(js, Integer.MAX_VALUE);
      IntArrayFIFOQueue intArrayFIFOQueue = new IntArrayFIFOQueue();

      for (int k = 0; k < i; k++) {
         for (int l = 0; l < j; l++) {
            int m = image.getColorArgb(k, l);
            if (ColorHelper.getAlpha(m) != 0) {
               int n = pack(k, l, i);
               js[n] = 0;
               is[n] = m;
               intArrayFIFOQueue.enqueue(n);
            }
         }
      }

      while (!intArrayFIFOQueue.isEmpty()) {
         int k = intArrayFIFOQueue.dequeueInt();
         int lx = x(k, i);
         int m = y(k, i);

         for (int[] ks : DIRECTIONS) {
            int o = lx + ks[0];
            int p = m + ks[1];
            int q = pack(o, p, i);
            if (o >= 0 && p >= 0 && o < i && p < j && js[q] > js[k] + 1) {
               js[q] = js[k] + 1;
               is[q] = is[k];
               intArrayFIFOQueue.enqueue(q);
            }
         }
      }

      for (int k = 0; k < i; k++) {
         for (int lx = 0; lx < j; lx++) {
            int m = image.getColorArgb(k, lx);
            if (ColorHelper.getAlpha(m) == 0) {
               image.setColorArgb(k, lx, ColorHelper.withAlpha(0, is[pack(k, lx, i)]));
            } else {
               image.setColorArgb(k, lx, m);
            }
         }
      }
   }

   public static void fillEmptyAreasWithDarkColor(NativeImage image) {
      int i = image.getWidth();
      int j = image.getHeight();
      int k = -1;
      int l = Integer.MAX_VALUE;

      for (int m = 0; m < i; m++) {
         for (int n = 0; n < j; n++) {
            int o = image.getColorArgb(m, n);
            int p = ColorHelper.getAlpha(o);
            if (p != 0) {
               int q = ColorHelper.getRed(o);
               int r = ColorHelper.getGreen(o);
               int s = ColorHelper.getBlue(o);
               int t = q + r + s;
               if (t < l) {
                  l = t;
                  k = o;
               }
            }
         }
      }

      int m = 3 * ColorHelper.getRed(k) / 4;
      int nx = 3 * ColorHelper.getGreen(k) / 4;
      int o = 3 * ColorHelper.getBlue(k) / 4;
      int p = ColorHelper.getArgb(0, m, nx, o);

      for (int q = 0; q < i; q++) {
         for (int r = 0; r < j; r++) {
            int s = image.getColorArgb(q, r);
            if (ColorHelper.getAlpha(s) == 0) {
               image.setColorArgb(q, r, p);
            }
         }
      }
   }

   private static int pack(int x, int y, int width) {
      return x + y * width;
   }

   private static int x(int packed, int width) {
      return packed % width;
   }

   private static int y(int packed, int width) {
      return packed / width;
   }
}
