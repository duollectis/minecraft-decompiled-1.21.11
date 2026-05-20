package net.minecraft.scoreboard;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Scoreboard {
   public static final String field_47542 = "#";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Object2ObjectMap<String, ScoreboardObjective> objectives = new Object2ObjectOpenHashMap(16, 0.5F);
   private final Reference2ObjectMap<ScoreboardCriterion, List<ScoreboardObjective>> objectivesByCriterion = new Reference2ObjectOpenHashMap();
   private final Map<String, Scores> scores = new Object2ObjectOpenHashMap(16, 0.5F);
   private final Map<ScoreboardDisplaySlot, ScoreboardObjective> objectiveSlots = new EnumMap<>(ScoreboardDisplaySlot.class);
   private final Object2ObjectMap<String, Team> teams = new Object2ObjectOpenHashMap();
   private final Object2ObjectMap<String, Team> teamsByScoreHolder = new Object2ObjectOpenHashMap();

   public @Nullable ScoreboardObjective getNullableObjective(@Nullable String name) {
      return (ScoreboardObjective)this.objectives.get(name);
   }

   public ScoreboardObjective addObjective(
      String name,
      ScoreboardCriterion criterion,
      Text displayName,
      ScoreboardCriterion.RenderType renderType,
      boolean displayAutoUpdate,
      @Nullable NumberFormat numberFormat
   ) {
      if (this.objectives.containsKey(name)) {
         throw new IllegalArgumentException("An objective with the name '" + name + "' already exists!");
      } else {
         ScoreboardObjective scoreboardObjective = new ScoreboardObjective(this, name, criterion, displayName, renderType, displayAutoUpdate, numberFormat);
         ((List)this.objectivesByCriterion.computeIfAbsent(criterion, criterion2 -> Lists.newArrayList())).add(scoreboardObjective);
         this.objectives.put(name, scoreboardObjective);
         this.updateObjective(scoreboardObjective);
         return scoreboardObjective;
      }
   }

   public final void forEachScore(ScoreboardCriterion criterion, ScoreHolder scoreHolder, Consumer<ScoreAccess> action) {
      ((List<ScoreboardObjective>)this.objectivesByCriterion.getOrDefault(criterion, Collections.emptyList()))
         .forEach(objective -> action.accept(this.getOrCreateScore(scoreHolder, objective, true)));
   }

   private Scores getScores(String scoreHolderName) {
      return this.scores.computeIfAbsent(scoreHolderName, name -> new Scores());
   }

   public ScoreAccess getOrCreateScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
      return this.getOrCreateScore(scoreHolder, objective, false);
   }

   public ScoreAccess getOrCreateScore(ScoreHolder scoreHolder, ScoreboardObjective objective, boolean forceWritable) {
      final boolean bl = forceWritable || !objective.getCriterion().isReadOnly();
      Scores scores = this.getScores(scoreHolder.getNameForScoreboard());
      final MutableBoolean mutableBoolean = new MutableBoolean();
      final ScoreboardScore scoreboardScore = scores.getOrCreate(objective, score -> mutableBoolean.setTrue());
      return new ScoreAccess() {
         @Override
         public int getScore() {
            return scoreboardScore.getScore();
         }

         @Override
         public void setScore(int score) {
            if (!bl) {
               throw new IllegalStateException("Cannot modify read-only score");
            } else {
               boolean blx = mutableBoolean.isTrue();
               if (objective.shouldDisplayAutoUpdate()) {
                  Text text = scoreHolder.getDisplayName();
                  if (text != null && !text.equals(scoreboardScore.getDisplayText())) {
                     scoreboardScore.setDisplayText(text);
                     blx = true;
                  }
               }

               if (score != scoreboardScore.getScore()) {
                  scoreboardScore.setScore(score);
                  blx = true;
               }

               if (blx) {
                  this.update();
               }
            }
         }

         @Override
         public @Nullable Text getDisplayText() {
            return scoreboardScore.getDisplayText();
         }

         @Override
         public void setDisplayText(@Nullable Text text) {
            if (mutableBoolean.isTrue() || !Objects.equals(text, scoreboardScore.getDisplayText())) {
               scoreboardScore.setDisplayText(text);
               this.update();
            }
         }

         @Override
         public void setNumberFormat(@Nullable NumberFormat numberFormat) {
            scoreboardScore.setNumberFormat(numberFormat);
            this.update();
         }

         @Override
         public boolean isLocked() {
            return scoreboardScore.isLocked();
         }

         @Override
         public void unlock() {
            this.setLocked(false);
         }

         @Override
         public void lock() {
            this.setLocked(true);
         }

         private void setLocked(boolean locked) {
            scoreboardScore.setLocked(locked);
            if (mutableBoolean.isTrue()) {
               this.update();
            }

            Scoreboard.this.resetScore(scoreHolder, objective);
         }

         private void update() {
            Scoreboard.this.updateScore(scoreHolder, objective, scoreboardScore);
            mutableBoolean.setFalse();
         }
      };
   }

   public @Nullable ReadableScoreboardScore getScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
      Scores scores = this.scores.get(scoreHolder.getNameForScoreboard());
      return scores != null ? scores.get(objective) : null;
   }

   public Collection<ScoreboardEntry> getScoreboardEntries(ScoreboardObjective objective) {
      List<ScoreboardEntry> list = new ArrayList<>();
      this.scores.forEach((scoreHolderName, scores) -> {
         ScoreboardScore scoreboardScore = scores.get(objective);
         if (scoreboardScore != null) {
            list.add(new ScoreboardEntry(scoreHolderName, scoreboardScore.getScore(), scoreboardScore.getDisplayText(), scoreboardScore.getNumberFormat()));
         }
      });
      return list;
   }

   public Collection<ScoreboardObjective> getObjectives() {
      return this.objectives.values();
   }

   public Collection<String> getObjectiveNames() {
      return this.objectives.keySet();
   }

   public Collection<ScoreHolder> getKnownScoreHolders() {
      return this.scores.keySet().stream().map(ScoreHolder::fromName).toList();
   }

   public void removeScores(ScoreHolder scoreHolder) {
      Scores scores = this.scores.remove(scoreHolder.getNameForScoreboard());
      if (scores != null) {
         this.onScoreHolderRemoved(scoreHolder);
      }
   }

   public void removeScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
      Scores scores = this.scores.get(scoreHolder.getNameForScoreboard());
      if (scores != null) {
         boolean bl = scores.remove(objective);
         if (!scores.hasScores()) {
            Scores scores2 = this.scores.remove(scoreHolder.getNameForScoreboard());
            if (scores2 != null) {
               this.onScoreHolderRemoved(scoreHolder);
            }
         } else if (bl) {
            this.onScoreRemoved(scoreHolder, objective);
         }
      }
   }

   public Object2IntMap<ScoreboardObjective> getScoreHolderObjectives(ScoreHolder scoreHolder) {
      Scores scores = this.scores.get(scoreHolder.getNameForScoreboard());
      return scores != null ? scores.getScoresAsIntMap() : Object2IntMaps.emptyMap();
   }

   public void removeObjective(ScoreboardObjective objective) {
      this.objectives.remove(objective.getName());

      for (ScoreboardDisplaySlot scoreboardDisplaySlot : ScoreboardDisplaySlot.values()) {
         if (this.getObjectiveForSlot(scoreboardDisplaySlot) == objective) {
            this.setObjectiveSlot(scoreboardDisplaySlot, null);
         }
      }

      List<ScoreboardObjective> list = (List<ScoreboardObjective>)this.objectivesByCriterion.get(objective.getCriterion());
      if (list != null) {
         list.remove(objective);
      }

      for (Scores scores : this.scores.values()) {
         scores.remove(objective);
      }

      this.updateRemovedObjective(objective);
   }

   public void setObjectiveSlot(ScoreboardDisplaySlot slot, @Nullable ScoreboardObjective objective) {
      this.objectiveSlots.put(slot, objective);
   }

   public @Nullable ScoreboardObjective getObjectiveForSlot(ScoreboardDisplaySlot slot) {
      return this.objectiveSlots.get(slot);
   }

   public @Nullable Team getTeam(String name) {
      return (Team)this.teams.get(name);
   }

   public Team addTeam(String name) {
      Team team = this.getTeam(name);
      if (team != null) {
         LOGGER.warn("Requested creation of existing team '{}'", name);
         return team;
      } else {
         team = new Team(this, name);
         this.teams.put(name, team);
         this.updateScoreboardTeamAndPlayers(team);
         return team;
      }
   }

   public void removeTeam(Team team) {
      this.teams.remove(team.getName());

      for (String string : team.getPlayerList()) {
         this.teamsByScoreHolder.remove(string);
      }

      this.updateRemovedTeam(team);
   }

   public boolean addScoreHolderToTeam(String scoreHolderName, Team team) {
      if (this.getScoreHolderTeam(scoreHolderName) != null) {
         this.clearTeam(scoreHolderName);
      }

      this.teamsByScoreHolder.put(scoreHolderName, team);
      return team.getPlayerList().add(scoreHolderName);
   }

   public boolean clearTeam(String scoreHolderName) {
      Team team = this.getScoreHolderTeam(scoreHolderName);
      if (team != null) {
         this.removeScoreHolderFromTeam(scoreHolderName, team);
         return true;
      } else {
         return false;
      }
   }

   public void removeScoreHolderFromTeam(String scoreHolderName, Team team) {
      if (this.getScoreHolderTeam(scoreHolderName) != team) {
         throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + team.getName() + "'.");
      } else {
         this.teamsByScoreHolder.remove(scoreHolderName);
         team.getPlayerList().remove(scoreHolderName);
      }
   }

   public Collection<String> getTeamNames() {
      return this.teams.keySet();
   }

   public Collection<Team> getTeams() {
      return this.teams.values();
   }

   public @Nullable Team getScoreHolderTeam(String scoreHolderName) {
      return (Team)this.teamsByScoreHolder.get(scoreHolderName);
   }

   public void updateObjective(ScoreboardObjective objective) {
   }

   public void updateExistingObjective(ScoreboardObjective objective) {
   }

   public void updateRemovedObjective(ScoreboardObjective objective) {
   }

   protected void updateScore(ScoreHolder scoreHolder, ScoreboardObjective objective, ScoreboardScore score) {
   }

   protected void resetScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
   }

   public void onScoreHolderRemoved(ScoreHolder scoreHolder) {
   }

   public void onScoreRemoved(ScoreHolder scoreHolder, ScoreboardObjective objective) {
   }

   public void updateScoreboardTeamAndPlayers(Team team) {
   }

   public void updateScoreboardTeam(Team team) {
   }

   public void updateRemovedTeam(Team team) {
   }

   public void clearDeadEntity(Entity entity) {
      if (!(entity instanceof PlayerEntity) && !entity.isAlive()) {
         this.removeScores(entity);
         this.clearTeam(entity.getNameForScoreboard());
      }
   }

   protected List<Scoreboard.PackedEntry> pack() {
      return this.scores
         .entrySet()
         .stream()
         .flatMap(
            entry -> {
               String string = entry.getKey();
               return entry.getValue()
                  .getScores()
                  .entrySet()
                  .stream()
                  .map(entryx -> new Scoreboard.PackedEntry(string, entryx.getKey().getName(), entryx.getValue().toPacked()));
            }
         )
         .toList();
   }

   protected void addEntry(Scoreboard.PackedEntry packedEntry) {
      ScoreboardObjective scoreboardObjective = this.getNullableObjective(packedEntry.objective);
      if (scoreboardObjective == null) {
         LOGGER.error("Unknown objective {} for name {}, ignoring", packedEntry.objective, packedEntry.owner);
      } else {
         this.getScores(packedEntry.owner).put(scoreboardObjective, new ScoreboardScore(packedEntry.score));
      }
   }

   protected List<Team.Packed> getPackedTeams() {
      return this.getTeams().stream().map(Team::pack).toList();
   }

   protected void addTeam(Team.Packed packedTeam) {
      Team team = this.addTeam(packedTeam.name());
      packedTeam.displayName().ifPresent(team::setDisplayName);
      packedTeam.color().ifPresent(team::setColor);
      team.setFriendlyFireAllowed(packedTeam.allowFriendlyFire());
      team.setShowFriendlyInvisibles(packedTeam.seeFriendlyInvisibles());
      team.setPrefix(packedTeam.memberNamePrefix());
      team.setSuffix(packedTeam.memberNameSuffix());
      team.setNameTagVisibilityRule(packedTeam.nameTagVisibility());
      team.setDeathMessageVisibilityRule(packedTeam.deathMessageVisibility());
      team.setCollisionRule(packedTeam.collisionRule());

      for (String string : packedTeam.players()) {
         this.addScoreHolderToTeam(string, team);
      }
   }

   protected List<ScoreboardObjective.Packed> getPackedObjectives() {
      return this.getObjectives().stream().map(ScoreboardObjective::pack).toList();
   }

   protected void addObjective(ScoreboardObjective.Packed packedObjective) {
      this.addObjective(
         packedObjective.name(),
         packedObjective.criteria(),
         packedObjective.displayName(),
         packedObjective.renderType(),
         packedObjective.displayAutoUpdate(),
         packedObjective.numberFormat().orElse(null)
      );
   }

   protected Map<ScoreboardDisplaySlot, String> getObjectivesBySlots() {
      Map<ScoreboardDisplaySlot, String> map = new EnumMap<>(ScoreboardDisplaySlot.class);

      for (ScoreboardDisplaySlot scoreboardDisplaySlot : ScoreboardDisplaySlot.values()) {
         ScoreboardObjective scoreboardObjective = this.getObjectiveForSlot(scoreboardDisplaySlot);
         if (scoreboardObjective != null) {
            map.put(scoreboardDisplaySlot, scoreboardObjective.getName());
         }
      }

      return map;
   }

   public record PackedEntry(String owner, String objective, ScoreboardScore.Packed score) {
      public static final Codec<Scoreboard.PackedEntry> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.STRING.fieldOf("Name").forGetter(Scoreboard.PackedEntry::owner),
               Codec.STRING.fieldOf("Objective").forGetter(Scoreboard.PackedEntry::objective),
               ScoreboardScore.Packed.CODEC.forGetter(Scoreboard.PackedEntry::score)
            )
            .apply(instance, Scoreboard.PackedEntry::new)
      );
   }
}
