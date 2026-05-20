package net.minecraft.client.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class VertexFormats {
   public static final VertexFormat EMPTY = VertexFormat.builder().build();
   public static final VertexFormat POSITION_COLOR_TEXTURE_LIGHT_NORMAL = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV0", VertexFormatElement.UV0)
      .add("UV2", VertexFormatElement.UV2)
      .add("Normal", VertexFormatElement.NORMAL)
      .padding(1)
      .build();
   public static final VertexFormat POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV0", VertexFormatElement.UV0)
      .add("UV1", VertexFormatElement.UV1)
      .add("UV2", VertexFormatElement.UV2)
      .add("Normal", VertexFormatElement.NORMAL)
      .padding(1)
      .build();
   public static final VertexFormat POSITION_TEXTURE_COLOR_LIGHT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV2", VertexFormatElement.UV2)
      .build();
   public static final VertexFormat POSITION = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).build();
   public static final VertexFormat POSITION_COLOR = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   public static final VertexFormat POSITION_COLOR_NORMAL = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("Normal", VertexFormatElement.NORMAL)
      .padding(1)
      .build();
   public static final VertexFormat POSITION_COLOR_LIGHT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV2", VertexFormatElement.UV2)
      .build();
   public static final VertexFormat POSITION_TEXTURE = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .build();
   public static final VertexFormat POSITION_TEXTURE_COLOR = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   public static final VertexFormat POSITION_COLOR_TEXTURE_LIGHT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV0", VertexFormatElement.UV0)
      .add("UV2", VertexFormatElement.UV2)
      .build();
   public static final VertexFormat POSITION_TEXTURE_LIGHT_COLOR = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("UV2", VertexFormatElement.UV2)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   public static final VertexFormat POSITION_TEXTURE_COLOR_NORMAL = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("Normal", VertexFormatElement.NORMAL)
      .padding(1)
      .build();
   public static final VertexFormat POSITION_COLOR_LINE_WIDTH = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final VertexFormat POSITION_COLOR_NORMAL_LINE_WIDTH = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("Normal", VertexFormatElement.NORMAL)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
}
