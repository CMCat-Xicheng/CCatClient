/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI;
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement;
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.Element;
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement;
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style;
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle;
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner;
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer;
import net.ccbluex.liquidbounce.utils.AnimationUtils;
import net.ccbluex.liquidbounce.utils.EntityUtils;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.render.UiUtils;
import net.ccbluex.liquidbounce.utils.render.EaseUtils;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClickGui extends GuiScreen {

    public final List<Panel> panels = new ArrayList<>();
    private final ResourceLocation hudIcon = new ResourceLocation("liquidbounce+/custom_hud_icon.png");
    public Style style = new SlowlyStyle();
    private Panel clickedPanel;
    private int mouseX;
    private int mouseY;

    public double slide, progress = 0;

    public long lastMS = System.currentTimeMillis();

    public ClickGui() {
        final int width = 100;
        final int height = 18;

        int yPos = 5;
        for (final ModuleCategory category : ModuleCategory.values()) {
            panels.add(new Panel(category.getDisplayName(), 100, yPos, width, height, false) {

                @Override
                public void setupItems() {
                    for (Module module : LiquidBounce.moduleManager.getModules())
                        if (module.getCategory() == category)
                            getElements().add(new ModuleElement(module));
                }
            });

            yPos += 20;
        }

        yPos += 20;

    }

    @Override
    public void initGui() {
        slide = progress = 0;
        lastMS = System.currentTimeMillis();
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (progress < 1) progress = (float)(System.currentTimeMillis() - lastMS) / (500F / Objects.requireNonNull(LiquidBounce.moduleManager.getModule(ClickGUI.class)).animSpeedValue.get()); // fully fps async
        else progress = 1;

        switch (Objects.requireNonNull(LiquidBounce.moduleManager.getModule(ClickGUI.class)).animationValue.get().toLowerCase()) {
            case "slidebounce":
            case "zoombounce":
            slide = EaseUtils.easeOutBack(progress);
            break;
            case "slide":
            case "zoom":
            case "azura":
            slide = EaseUtils.easeOutQuart(progress);
            break;
            case "none":
            slide = 1;
            break;
        }

        if (Mouse.isButtonDown(0) && mouseX >= 5 && mouseX <= 50 && mouseY <= height - 5 && mouseY >= height - 50)
            mc.displayGuiScreen(new GuiHudDesigner());

        // Enable DisplayList optimization
        AWTFontRenderer.Companion.setAssumeNonVolatile(true);

        final double scale = Objects.requireNonNull(LiquidBounce.moduleManager.getModule(ClickGUI.class)).scaleValue.get();

        mouseX /= scale;
        mouseY /= scale;

        this.mouseX = mouseX;
        this.mouseY = mouseY;

        switch (Objects.requireNonNull(LiquidBounce.moduleManager.getModule(ClickGUI.class)).backgroundValue.get()) {
        case "Default":
            drawDefaultBackground();
            break;
        case "Gradient":
            drawGradientRect(0, 0, width, height, 
                ColorUtils.reAlpha(ClickGUI.generateColor(), Objects.requireNonNull(LiquidBounce.moduleManager.getModule(ClickGUI.class)).gradEndValue.get()).getRGB(), 
                ColorUtils.reAlpha(ClickGUI.generateColor(), Objects.requireNonNull(LiquidBounce.moduleManager.getModule(ClickGUI.class)).gradStartValue.get()).getRGB());
            break;
        default:
            break;
        }

        GlStateManager.disableAlpha();
        RenderUtils.drawImage(hudIcon, 9, height - 41, 32, 32);
        GlStateManager.enableAlpha();

        switch (Objects.requireNonNull(LiquidBounce.moduleManager.getModule(ClickGUI.class)).animationValue.get().toLowerCase()) {
            case "azura":
            GlStateManager.translate(0, (1.0 - slide) * height * 2.0, 0);
            GlStateManager.scale(scale, scale + (1.0 - slide) * 2.0, scale);
            break;
            case "slide":
            case "slidebounce":
            GlStateManager.translate(0, (1.0 - slide) * height * 2.0, 0);
            GlStateManager.scale(scale, scale, scale);
            break;
            case "zoom":
            GlStateManager.translate((1.0 - slide) * (width / 2.0), (1.0 - slide) * (height / 2.0), (1.0 - slide) * (width / 2.0));
            GlStateManager.scale(scale * slide, scale * slide, scale * slide);
            break;
            case "zoombounce":
            GlStateManager.translate((1.0 - slide) * (width / 2.0), (1.0 - slide) * (height / 2.0), 0);
            GlStateManager.scale(scale * slide, scale * slide, scale * slide);
            break;
            case "none":
            GlStateManager.scale(scale, scale, scale);
            break;
        }

        for (final Panel panel : panels) {
            panel.updateFade(RenderUtils.deltaTime);
            panel.drawScreen(mouseX, mouseY, partialTicks);
        }

        for (final Panel panel : panels) {
            for (final Element element : panel.getElements()) {
                if (element instanceof ModuleElement) {
                    final ModuleElement moduleElement = (ModuleElement) element;

                    if (mouseX != 0 && mouseY != 0 && moduleElement.isHovering(mouseX, mouseY) && moduleElement.isVisible() && element.getY() <= panel.getY() + panel.getFade())
                        style.drawDescription(mouseX, mouseY, moduleElement.getModule().getDescription());
                }
            }
        }

        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();

        switch (Objects.requireNonNull(LiquidBounce.moduleManager.getModule(ClickGUI.class)).animationValue.get().toLowerCase()) {
            case "azura":
            GlStateManager.translate(0, (1.0 - slide) * height * -2.0, 0);
            break;
            case "slide":
            case "slidebounce":
            GlStateManager.translate(0, (1.0 - slide) * height * -2.0, 0);
            break;
            case "zoom":
            GlStateManager.translate(-1 * (1.0 - slide) * (width / 2.0), -1 * (1.0 - slide) * (height / 2.0), -1 * (1.0 - slide) * (width / 2.0));
            break;
            case "zoombounce":
            GlStateManager.translate(-1 * (1.0 - slide) * (width / 2.0), -1 * (1.0 - slide) * (height / 2.0), 0);
            break;
        }
        GlStateManager.scale(1, 1, 1);

        AWTFontRenderer.Companion.setAssumeNonVolatile(false);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        for (int i = panels.size() - 1; i >= 0; i--)
            if (panels.get(i).handleScroll(mouseX, mouseY, wheel))
                break;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        final double scale = Objects.requireNonNull(LiquidBounce.moduleManager.getModule(ClickGUI.class)).scaleValue.get();

        mouseX /= scale;
        mouseY /= scale;

        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseClicked(mouseX, mouseY, mouseButton)){
                break;
            }
        }

        for (final Panel panel : panels) {
            panel.drag = false;

            if (mouseButton == 0 && panel.isHovering(mouseX, mouseY)) {
                clickedPanel = panel;
                break;
            }
        }

        if (clickedPanel != null) {
            clickedPanel.x2 = clickedPanel.x - mouseX;
            clickedPanel.y2 = clickedPanel.y - mouseY;
            clickedPanel.drag = true;

            panels.remove(clickedPanel);
            panels.add(clickedPanel);
            clickedPanel = null;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        final double scale = Objects.requireNonNull(LiquidBounce.moduleManager.getModule(ClickGUI.class)).scaleValue.get();

        mouseX /= scale;
        mouseY /= scale;

        for (Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, state);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void updateScreen() {
        for (final Panel panel : panels) {
            for (final Element element : panel.getElements()) {
                if (element instanceof ButtonElement) {
                    final ButtonElement buttonElement = (ButtonElement) element;

                    if (buttonElement.isHovering(mouseX, mouseY)) {
                        if (buttonElement.hoverTime < 7)
                            buttonElement.hoverTime++;
                    } else if (buttonElement.hoverTime > 0)
                        buttonElement.hoverTime--;
                }

                if (element instanceof ModuleElement) {
                    if (((ModuleElement) element).getModule().getState()) {
                        if (((ModuleElement) element).slowlyFade < 255)
                            ((ModuleElement) element).slowlyFade += 50;
                    } else if (((ModuleElement) element).slowlyFade > 0)
                        ((ModuleElement) element).slowlyFade -= 50;

                    if (((ModuleElement) element).slowlyFade > 255)
                        ((ModuleElement) element).slowlyFade = 255;

                    if (((ModuleElement) element).slowlyFade < 0)
                        ((ModuleElement) element).slowlyFade = 0;
                }
            }
        }
        super.updateScreen();
    }

    @Override
    public void onGuiClosed() {
        LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.clickGuiConfig);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
