package net.minecraft.world.debug.data;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record GoalSelectorDebugData(List<GoalSelectorDebugData.Goal> goals) {
   public static final PacketCodec<ByteBuf, GoalSelectorDebugData> PACKET_CODEC = PacketCodec.tuple(
      GoalSelectorDebugData.Goal.PACKET_CODEC.collect(PacketCodecs.toList()), GoalSelectorDebugData::goals, GoalSelectorDebugData::new
   );

   public record Goal(int priority, boolean isRunning, String name) {
      public static final PacketCodec<ByteBuf, GoalSelectorDebugData.Goal> PACKET_CODEC = PacketCodec.tuple(
         PacketCodecs.VAR_INT,
         GoalSelectorDebugData.Goal::priority,
         PacketCodecs.BOOLEAN,
         GoalSelectorDebugData.Goal::isRunning,
         PacketCodecs.string(255),
         GoalSelectorDebugData.Goal::name,
         GoalSelectorDebugData.Goal::new
      );
   }
}
