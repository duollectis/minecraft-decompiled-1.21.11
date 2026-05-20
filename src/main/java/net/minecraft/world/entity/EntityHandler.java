package net.minecraft.world.entity;

public interface EntityHandler<T> {
   void create(T entity);

   void destroy(T entity);

   void startTicking(T entity);

   void stopTicking(T entity);

   void startTracking(T entity);

   void stopTracking(T entity);

   void updateLoadStatus(T entity);
}
