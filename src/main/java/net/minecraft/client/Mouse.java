package net.minecraft.client;

import com.mojang.logging.LogUtils;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigationType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.input.Scroller;
import net.minecraft.client.input.SystemKeycodes;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Smoother;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWDropCallback;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class Mouse {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final long field_61505 = 250L;
   private final MinecraftClient client;
   private boolean leftButtonClicked;
   private boolean middleButtonClicked;
   private boolean rightButtonClicked;
   private double x;
   private double y;
   private Mouse.@Nullable MouseClickTime lastMouseClick;
   @MouseInput.ButtonCode
   protected int lastMouseButton;
   private int controlLeftClicks;
   private @Nullable MouseInput activeButton = null;
   private boolean hasResolutionChanged = true;
   private int touchHoldTime;
   private double glfwTime;
   private final Smoother cursorXSmoother = new Smoother();
   private final Smoother cursorYSmoother = new Smoother();
   private double cursorDeltaX;
   private double cursorDeltaY;
   private final Scroller scroller;
   private double lastTickTime = Double.MIN_VALUE;
   private boolean cursorLocked;

   public Mouse(MinecraftClient client) {
      this.client = client;
      this.scroller = new Scroller();
   }

   private void onMouseButton(long window, MouseInput input, @MouseInput.MouseAction int action) {
      Window window2 = this.client.getWindow();
      if (window == window2.getHandle()) {
         this.client.getInactivityFpsLimiter().onInput();
         if (this.client.currentScreen != null) {
            this.client.setNavigationType(GuiNavigationType.MOUSE);
         }

         boolean bl = action == 1;
         MouseInput mouseInput = this.modifyMouseInput(input, bl);
         if (bl) {
            if (this.client.options.getTouchscreen().getValue() && this.touchHoldTime++ > 0) {
               return;
            }

            this.activeButton = mouseInput;
            this.glfwTime = GlfwUtil.getTime();
         } else if (this.activeButton != null) {
            if (this.client.options.getTouchscreen().getValue() && --this.touchHoldTime > 0) {
               return;
            }

            this.activeButton = null;
         }

         if (this.client.getOverlay() == null) {
            if (this.client.currentScreen == null) {
               if (!this.cursorLocked && bl) {
                  this.lockCursor();
               }
            } else {
               double d = this.getScaledX(window2);
               double e = this.getScaledY(window2);
               Screen screen = this.client.currentScreen;
               Click click = new Click(d, e, mouseInput);
               if (bl) {
                  screen.applyMousePressScrollNarratorDelay();

                  try {
                     long l = Util.getMeasuringTimeMs();
                     boolean bl2 = this.lastMouseClick != null
                        && l - this.lastMouseClick.time() < 250L
                        && this.lastMouseClick.screen() == screen
                        && this.lastMouseButton == click.button();
                     if (screen.mouseClicked(click, bl2)) {
                        this.lastMouseClick = new Mouse.MouseClickTime(l, screen);
                        this.lastMouseButton = mouseInput.button();
                        return;
                     }
                  } catch (Throwable var18) {
                     CrashReport crashReport = CrashReport.create(var18, "mouseClicked event handler");
                     screen.addCrashReportSection(crashReport);
                     CrashReportSection crashReportSection = crashReport.addElement("Mouse");
                     this.addCrashReportSection(crashReportSection, window2);
                     crashReportSection.add("Button", click.button());
                     throw new CrashException(crashReport);
                  }
               } else {
                  try {
                     if (screen.mouseReleased(click)) {
                        return;
                     }
                  } catch (Throwable var17) {
                     CrashReport crashReport = CrashReport.create(var17, "mouseReleased event handler");
                     screen.addCrashReportSection(crashReport);
                     CrashReportSection crashReportSection = crashReport.addElement("Mouse");
                     this.addCrashReportSection(crashReportSection, window2);
                     crashReportSection.add("Button", click.button());
                     throw new CrashException(crashReport);
                  }
               }
            }
         }

         if (this.client.currentScreen == null && this.client.getOverlay() == null) {
            if (mouseInput.button() == 0) {
               this.leftButtonClicked = bl;
            } else if (mouseInput.button() == 2) {
               this.middleButtonClicked = bl;
            } else if (mouseInput.button() == 1) {
               this.rightButtonClicked = bl;
            }

            InputUtil.Key key = InputUtil.Type.MOUSE.createFromCode(mouseInput.button());
            KeyBinding.setKeyPressed(key, bl);
            if (bl) {
               KeyBinding.onKeyPressed(key);
            }
         }
      }
   }

   private MouseInput modifyMouseInput(MouseInput input, boolean pressed) {
      if (SystemKeycodes.USE_LONG_LEFT_PRESS && input.button() == 0) {
         if (pressed) {
            if ((input.modifiers() & 2) == 2) {
               this.controlLeftClicks++;
               return new MouseInput(1, input.modifiers());
            }
         } else if (this.controlLeftClicks > 0) {
            this.controlLeftClicks--;
            return new MouseInput(1, input.modifiers());
         }
      }

      return input;
   }

   public void addCrashReportSection(CrashReportSection section, Window window) {
      section.add(
         "Mouse location",
         () -> String.format(Locale.ROOT, "Scaled: (%f, %f). Absolute: (%f, %f)", scaleX(window, this.x), scaleY(window, this.y), this.x, this.y)
      );
      section.add(
         "Screen size",
         () -> String.format(
            Locale.ROOT,
            "Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %d",
            window.getScaledWidth(),
            window.getScaledHeight(),
            window.getFramebufferWidth(),
            window.getFramebufferHeight(),
            window.getScaleFactor()
         )
      );
   }

   private void onMouseScroll(long window, double horizontal, double vertical) {
      if (window == this.client.getWindow().getHandle()) {
         this.client.getInactivityFpsLimiter().onInput();
         boolean bl = this.client.options.getDiscreteMouseScroll().getValue();
         double d = this.client.options.getMouseWheelSensitivity().getValue();
         double e = (bl ? Math.signum(horizontal) : horizontal) * d;
         double f = (bl ? Math.signum(vertical) : vertical) * d;
         if (this.client.getOverlay() == null) {
            if (this.client.currentScreen != null) {
               double g = this.getScaledX(this.client.getWindow());
               double h = this.getScaledY(this.client.getWindow());
               this.client.currentScreen.mouseScrolled(g, h, e, f);
               this.client.currentScreen.applyMousePressScrollNarratorDelay();
            } else if (this.client.player != null) {
               Vector2i vector2i = this.scroller.update(e, f);
               if (vector2i.x == 0 && vector2i.y == 0) {
                  return;
               }

               int i = vector2i.y == 0 ? -vector2i.x : vector2i.y;
               if (this.client.player.isSpectator()) {
                  if (this.client.inGameHud.getSpectatorHud().isOpen()) {
                     this.client.inGameHud.getSpectatorHud().cycleSlot(-i);
                  } else {
                     float j = MathHelper.clamp(this.client.player.getAbilities().getFlySpeed() + vector2i.y * 0.005F, 0.0F, 0.2F);
                     this.client.player.getAbilities().setFlySpeed(j);
                  }
               } else {
                  PlayerInventory playerInventory = this.client.player.getInventory();
                  playerInventory.setSelectedSlot(Scroller.scrollCycling(i, playerInventory.getSelectedSlot(), PlayerInventory.getHotbarSize()));
               }
            }
         }
      }
   }

   private void onFilesDropped(long window, List<Path> paths, int invalidFilesCount) {
      this.client.getInactivityFpsLimiter().onInput();
      if (this.client.currentScreen != null) {
         this.client.currentScreen.onFilesDropped(paths);
      }

      if (invalidFilesCount > 0) {
         SystemToast.addFileDropFailure(this.client, invalidFilesCount);
      }
   }

   public void setup(Window window) {
      InputUtil.setMouseCallbacks(
         window, (windowx, x, y) -> this.client.execute(() -> this.onCursorPos(windowx, x, y)), (windowx, button, action, modifiers) -> {
            MouseInput mouseInput = new MouseInput(button, modifiers);
            this.client.execute(() -> this.onMouseButton(windowx, mouseInput, action));
         }, (windowx, offsetX, offsetY) -> this.client.execute(() -> this.onMouseScroll(windowx, offsetX, offsetY)), (windowx, count, names) -> {
            List<Path> list = new ArrayList<>(count);
            int i = 0;

            for (int j = 0; j < count; j++) {
               String string = GLFWDropCallback.getName(names, j);

               try {
                  list.add(Paths.get(string));
               } catch (InvalidPathException var11) {
                  i++;
                  LOGGER.error("Failed to parse path '{}'", string, var11);
               }
            }

            if (!list.isEmpty()) {
               int j = i;
               this.client.execute(() -> this.onFilesDropped(windowx, list, j));
            }
         }
      );
   }

   private void onCursorPos(long window, double x, double y) {
      if (window == this.client.getWindow().getHandle()) {
         if (this.hasResolutionChanged) {
            this.x = x;
            this.y = y;
            this.hasResolutionChanged = false;
         } else {
            if (this.client.isWindowFocused()) {
               this.cursorDeltaX = this.cursorDeltaX + (x - this.x);
               this.cursorDeltaY = this.cursorDeltaY + (y - this.y);
            }

            this.x = x;
            this.y = y;
         }
      }
   }

   public void tick() {
      double d = GlfwUtil.getTime();
      double e = d - this.lastTickTime;
      this.lastTickTime = d;
      if (this.client.isWindowFocused()) {
         Screen screen = this.client.currentScreen;
         boolean bl = this.cursorDeltaX != 0.0 || this.cursorDeltaY != 0.0;
         if (bl) {
            this.client.getInactivityFpsLimiter().onInput();
         }

         if (screen != null && this.client.getOverlay() == null && bl) {
            Window window = this.client.getWindow();
            double f = this.getScaledX(window);
            double g = this.getScaledY(window);

            try {
               screen.mouseMoved(f, g);
            } catch (Throwable var20) {
               CrashReport crashReport = CrashReport.create(var20, "mouseMoved event handler");
               screen.addCrashReportSection(crashReport);
               CrashReportSection crashReportSection = crashReport.addElement("Mouse");
               this.addCrashReportSection(crashReportSection, window);
               throw new CrashException(crashReport);
            }

            if (this.activeButton != null && this.glfwTime > 0.0) {
               double h = scaleX(window, this.cursorDeltaX);
               double i = scaleY(window, this.cursorDeltaY);

               try {
                  screen.mouseDragged(new Click(f, g, this.activeButton), h, i);
               } catch (Throwable var19) {
                  CrashReport crashReport2 = CrashReport.create(var19, "mouseDragged event handler");
                  screen.addCrashReportSection(crashReport2);
                  CrashReportSection crashReportSection2 = crashReport2.addElement("Mouse");
                  this.addCrashReportSection(crashReportSection2, window);
                  throw new CrashException(crashReport2);
               }
            }

            screen.applyMouseMoveNarratorDelay();
         }

         if (this.isCursorLocked() && this.client.player != null) {
            this.updateMouse(e);
         }
      }

      this.cursorDeltaX = 0.0;
      this.cursorDeltaY = 0.0;
   }

   public static double scaleX(Window window, double x) {
      return x * window.getScaledWidth() / window.getWidth();
   }

   public double getScaledX(Window window) {
      return scaleX(window, this.x);
   }

   public static double scaleY(Window window, double y) {
      return y * window.getScaledHeight() / window.getHeight();
   }

   public double getScaledY(Window window) {
      return scaleY(window, this.y);
   }

   private void updateMouse(double timeDelta) {
      double d = this.client.options.getMouseSensitivity().getValue() * 0.6F + 0.2F;
      double e = d * d * d;
      double f = e * 8.0;
      double i;
      double j;
      if (this.client.options.smoothCameraEnabled) {
         double g = this.cursorXSmoother.smooth(this.cursorDeltaX * f, timeDelta * f);
         double h = this.cursorYSmoother.smooth(this.cursorDeltaY * f, timeDelta * f);
         i = g;
         j = h;
      } else if (this.client.options.getPerspective().isFirstPerson() && this.client.player.isUsingSpyglass()) {
         this.cursorXSmoother.clear();
         this.cursorYSmoother.clear();
         i = this.cursorDeltaX * e;
         j = this.cursorDeltaY * e;
      } else {
         this.cursorXSmoother.clear();
         this.cursorYSmoother.clear();
         i = this.cursorDeltaX * f;
         j = this.cursorDeltaY * f;
      }

      this.client.getTutorialManager().onUpdateMouse(i, j);
      if (this.client.player != null) {
         this.client
            .player
            .changeLookDirection(this.client.options.getInvertMouseX().getValue() ? -i : i, this.client.options.getInvertMouseY().getValue() ? -j : j);
      }
   }

   public boolean wasLeftButtonClicked() {
      return this.leftButtonClicked;
   }

   public boolean wasMiddleButtonClicked() {
      return this.middleButtonClicked;
   }

   public boolean wasRightButtonClicked() {
      return this.rightButtonClicked;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public void onResolutionChanged() {
      this.hasResolutionChanged = true;
   }

   public boolean isCursorLocked() {
      return this.cursorLocked;
   }

   public void lockCursor() {
      if (this.client.isWindowFocused()) {
         if (!this.cursorLocked) {
            if (SystemKeycodes.UPDATE_PRESSED_STATE_ON_MOUSE_GRAB) {
               KeyBinding.updatePressedStates();
            }

            this.cursorLocked = true;
            this.x = this.client.getWindow().getWidth() / 2;
            this.y = this.client.getWindow().getHeight() / 2;
            InputUtil.setCursorParameters(this.client.getWindow(), 212995, this.x, this.y);
            this.client.setScreen(null);
            this.client.attackCooldown = 10000;
            this.hasResolutionChanged = true;
         }
      }
   }

   public void unlockCursor() {
      if (this.cursorLocked) {
         this.cursorLocked = false;
         this.x = this.client.getWindow().getWidth() / 2;
         this.y = this.client.getWindow().getHeight() / 2;
         InputUtil.setCursorParameters(this.client.getWindow(), 212993, this.x, this.y);
      }
   }

   public void setResolutionChanged() {
      this.hasResolutionChanged = true;
   }

   public void drawScaledPos(TextRenderer textRenderer, DrawContext context) {
      Window window = this.client.getWindow();
      double d = this.getScaledX(window);
      double e = this.getScaledY(window) - 8.0;
      String string = String.format(Locale.ROOT, "%.0f,%.0f", d, e);
      context.drawTextWithShadow(textRenderer, string, (int)d, (int)e, -1);
   }

   @Environment(EnvType.CLIENT)
   record MouseClickTime(long time, Screen screen) {
   }
}
