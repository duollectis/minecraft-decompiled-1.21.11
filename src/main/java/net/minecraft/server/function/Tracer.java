package net.minecraft.server.function;

import net.minecraft.util.Identifier;

public interface Tracer extends AutoCloseable {
   void traceCommandStart(int depth, String command);

   void traceCommandEnd(int depth, String command, int result);

   void traceError(String message);

   void traceFunctionCall(int depth, Identifier function, int size);

   @Override
   void close();
}
