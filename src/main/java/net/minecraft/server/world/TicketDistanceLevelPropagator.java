package net.minecraft.server.world;

class TicketDistanceLevelPropagator extends ChunkPosDistanceLevelPropagator {
   private static final int UNLOADED = ChunkLevels.INACCESSIBLE + 1;
   private final ChunkLevelManager levelManager;
   private final ChunkTicketManager ticketManager;

   public TicketDistanceLevelPropagator(ChunkLevelManager levelManager, ChunkTicketManager ticketManager) {
      super(UNLOADED + 1, 16, 256);
      this.levelManager = levelManager;
      this.ticketManager = ticketManager;
      ticketManager.setLoadingLevelUpdater(this::updateLevel);
   }

   @Override
   protected int getInitialLevel(long id) {
      return this.ticketManager.getLevel(id, false);
   }

   @Override
   protected int getLevel(long id) {
      if (!this.levelManager.isUnloaded(id)) {
         ChunkHolder chunkHolder = this.levelManager.getChunkHolder(id);
         if (chunkHolder != null) {
            return chunkHolder.getLevel();
         }
      }

      return UNLOADED;
   }

   @Override
   protected void setLevel(long id, int level) {
      ChunkHolder chunkHolder = this.levelManager.getChunkHolder(id);
      int i = chunkHolder == null ? UNLOADED : chunkHolder.getLevel();
      if (i != level) {
         chunkHolder = this.levelManager.setLevel(id, level, chunkHolder, i);
         if (chunkHolder != null) {
            this.levelManager.chunkHoldersWithPendingUpdates.add(chunkHolder);
         }
      }
   }

   public int update(int distance) {
      return this.applyPendingUpdates(distance);
   }
}
