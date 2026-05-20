package net.minecraft.server.dedicated.management;

public class RpcMethodNotFoundException extends RuntimeException {
   public RpcMethodNotFoundException(String message) {
      super(message);
   }
}
