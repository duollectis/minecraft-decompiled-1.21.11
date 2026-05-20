package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

public enum SideChainPart implements StringIdentifiable {
   UNCONNECTED("unconnected"),
   RIGHT("right"),
   CENTER("center"),
   LEFT("left");

   private final String id;

   private SideChainPart(final String id) {
      this.id = id;
   }

   @Override
   public String toString() {
      return this.asString();
   }

   @Override
   public String asString() {
      return this.id;
   }

   public boolean isConnected() {
      return this != UNCONNECTED;
   }

   public boolean isCenterOr(SideChainPart sideChainPart) {
      return this == CENTER || this == sideChainPart;
   }

   public boolean isNotCenter() {
      return this != CENTER;
   }

   public SideChainPart connectToRight() {
      return switch (this) {
         case UNCONNECTED, LEFT -> LEFT;
         case RIGHT, CENTER -> CENTER;
      };
   }

   public SideChainPart connectToLeft() {
      return switch (this) {
         case UNCONNECTED, RIGHT -> RIGHT;
         case CENTER, LEFT -> CENTER;
      };
   }

   public SideChainPart disconnectFromRight() {
      return switch (this) {
         case UNCONNECTED, LEFT -> UNCONNECTED;
         case RIGHT, CENTER -> RIGHT;
      };
   }

   public SideChainPart disconnectFromLeft() {
      return switch (this) {
         case UNCONNECTED, RIGHT -> UNCONNECTED;
         case CENTER, LEFT -> LEFT;
      };
   }
}
