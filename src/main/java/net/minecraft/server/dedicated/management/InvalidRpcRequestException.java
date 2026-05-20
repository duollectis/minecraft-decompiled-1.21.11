package net.minecraft.server.dedicated.management;

public class InvalidRpcRequestException extends RuntimeException {
   public InvalidRpcRequestException(String message) {
      super(message);
   }
}
