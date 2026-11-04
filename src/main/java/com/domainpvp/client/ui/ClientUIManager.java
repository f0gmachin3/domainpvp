package com.domainpvp.client.ui;

import com.domainpvp.network.s2c.MatchFinishedPayload;
import com.domainpvp.network.s2c.PartyUpdatedPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.UUID;

/**
 * Manages client HUD overlays, top-right Mojang-style Party UI, and combat presentations.
 */
public class ClientUIManager {
    private static final ClientUIManager INSTANCE = new ClientUIManager();

    private PartyUpdatedPayload currentParty;
    private int countdownSeconds = -1;
    private boolean combatActive = false;
    private MatchFinishedPayload lastMatchResult;

    private ClientUIManager() {}

    public static ClientUIManager getInstance() {
        return INSTANCE;
    }

    public void updateParty(PartyUpdatedPayload payload) {
        if (payload == null || payload.partyId().equals(new UUID(0, 0)) || payload.member1Name().isEmpty()) {
            this.currentParty = null;
        } else {
            this.currentParty = payload;
        }
    }

    public PartyUpdatedPayload getCurrentParty() {
        return currentParty;
    }

    public void setCountdown(int seconds) {
        this.countdownSeconds = seconds;
        if (seconds == 0) {
            this.combatActive = true;
        }
    }

    public void setCombatActive(boolean active) {
        this.combatActive = active;
        if (!active) {
            this.countdownSeconds = -1;
        }
    }

    public boolean isCombatActive() {
        return combatActive;
    }

    public void setMatchResult(MatchFinishedPayload result) {
        this.lastMatchResult = result;
        this.combatActive = false;
        this.countdownSeconds = -1;
    }

    public MatchFinishedPayload getLastMatchResult() {
        return lastMatchResult;
    }

    public void reset() {
        this.countdownSeconds = -1;
        this.combatActive = false;
        this.lastMatchResult = null;
    }

    /**
     * Renders both the top-right Mojang-style Party GUI and active combat HUD.
     */
    public void renderCombatHud(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return;

        // 1. Render Top-Right Mojang-style Party GUI (when in party and not in active combat)
        renderPartyHud(context, mc);

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        // 2. Render animated countdown if active
        if (countdownSeconds >= 0) {
            String text = countdownSeconds == 0 ? "§c§lFIGHT!" : ("§e§l" + countdownSeconds);
            int textWidth = mc.textRenderer.getWidth(text);
            int x = (screenWidth - textWidth) / 2;
            int y = screenHeight / 3;

            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(2.0f, 2.0f, 1.0f);
            context.drawTextWithShadow(mc.textRenderer, text, 0, 0, 0xFFFFFFFF);
            context.getMatrices().pop();
        }

        // 3. Render match banner during active combat
        if (combatActive) {
            String title = "§6§lPvP Domain §8| §cCombat Zone";
            int w = mc.textRenderer.getWidth(title);
            context.drawTextWithShadow(mc.textRenderer, title, (screenWidth - w) / 2, 8, 0xFFFFFFFF);
        }
    }

    /**
     * Renders a small Mojang-style Party GUI in the top-right corner.
     */
    private void renderPartyHud(DrawContext context, MinecraftClient mc) {
        if (currentParty == null || currentParty.member1Name().isEmpty() || combatActive) {
            return;
        }

        boolean hasP2 = currentParty.member2Name() != null && !currentParty.member2Name().isEmpty();
        int memberCount = hasP2 ? 2 : 1;
        String headerTitle = "§6§lPvP Domain §8| §eParty §7[" + memberCount + "/2]";

        // Member 1 display
        boolean isM1Leader = currentParty.member1Uuid().equals(currentParty.leaderUuid().toString());
        String m1Prefix = isM1Leader ? "§6★ §f" : "  §f";
        String m1Name = m1Prefix + currentParty.member1Name();
        String m1Status = currentParty.member1Ready() ? "§a✔ READY" : "§c✖ NOT READY";

        // Member 2 display
        String m2Name;
        String m2Status = "";
        if (hasP2) {
            boolean isM2Leader = currentParty.member2Uuid().equals(currentParty.leaderUuid().toString());
            String m2Prefix = isM2Leader ? "§6★ §f" : "  §f";
            m2Name = m2Prefix + currentParty.member2Name();
            m2Status = currentParty.member2Ready() ? "§a✔ READY" : "§c✖ NOT READY";
        } else {
            m2Name = "  §7... Waiting for player";
        }

        // Action hint footer
        UUID localUuid = mc.player != null ? mc.player.getUuid() : null;
        boolean isLocalLeader = localUuid != null && localUuid.equals(currentParty.leaderUuid());
        boolean allReady = hasP2 && currentParty.member1Ready() && currentParty.member2Ready();

        String footerText;
        if (!hasP2) {
            footerText = "§8/domain invite <player>";
        } else if (allReady) {
            if (isLocalLeader) {
                footerText = "§a§lAll Ready! Type §2§l/start";
            } else {
                footerText = "§aAll Ready! §eWaiting for /start";
            }
        } else {
            boolean isLocalReady = false;
            if (localUuid != null) {
                if (currentParty.member1Uuid().equals(localUuid.toString())) {
                    isLocalReady = currentParty.member1Ready();
                } else if (currentParty.member2Uuid().equals(localUuid.toString())) {
                    isLocalReady = currentParty.member2Ready();
                }
            }

            if (!isLocalReady) {
                footerText = "§7Type §e/ready §7to ready up";
            } else {
                footerText = "§eWaiting for opponent...";
            }
        }

        // Calculate dynamic dimensions
        int headerWidth = mc.textRenderer.getWidth(headerTitle);
        int m1TotalWidth = mc.textRenderer.getWidth(m1Name) + 10 + mc.textRenderer.getWidth(m1Status);
        int m2TotalWidth = mc.textRenderer.getWidth(m2Name) + (hasP2 ? (10 + mc.textRenderer.getWidth(m2Status)) : 0);
        int footerWidth = mc.textRenderer.getWidth(footerText);

        int maxContentWidth = Math.max(headerWidth, Math.max(m1TotalWidth, Math.max(m2TotalWidth, footerWidth)));
        int boxWidth = Math.max(145, maxContentWidth + 12);
        int boxHeight = 58;

        int screenWidth = mc.getWindow().getScaledWidth();
        int boxX = screenWidth - boxWidth - 8;
        int boxY = 8;

        // Render Mojang-style container box
        drawMojangBox(context, boxX, boxY, boxWidth, boxHeight);

        // Header Title
        context.drawTextWithShadow(mc.textRenderer, headerTitle, boxX + 6, boxY + 5, 0xFFFFFFFF);

        // Divider 1
        context.fill(boxX + 4, boxY + 16, boxX + boxWidth - 4, boxY + 17, 0x40808080);

        // Member 1
        context.drawTextWithShadow(mc.textRenderer, m1Name, boxX + 6, boxY + 20, 0xFFFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, m1Status, boxX + boxWidth - mc.textRenderer.getWidth(m1Status) - 6, boxY + 20, 0xFFFFFFFF);

        // Member 2
        context.drawTextWithShadow(mc.textRenderer, m2Name, boxX + 6, boxY + 31, 0xFFFFFFFF);
        if (hasP2) {
            context.drawTextWithShadow(mc.textRenderer, m2Status, boxX + boxWidth - mc.textRenderer.getWidth(m2Status) - 6, boxY + 31, 0xFFFFFFFF);
        }

        // Divider 2
        context.fill(boxX + 4, boxY + 42, boxX + boxWidth - 4, boxY + 43, 0x40808080);

        // Footer prompt
        context.drawTextWithShadow(mc.textRenderer, footerText, boxX + 6, boxY + 46, 0xFFFFFFFF);
    }

    /**
     * Draws an authentic Mojang-styled bordered box with dark translucent backdrop.
     */
    private void drawMojangBox(DrawContext context, int x, int y, int width, int height) {
        // Semi-transparent dark background
        context.fill(x, y, x + width, y + height, 0xD0101010);

        // Outer border (1px)
        context.fill(x, y, x + width, y + 1, 0xFF3C3C3C); // top
        context.fill(x, y + height - 1, x + width, y + height, 0xFF242424); // bottom
        context.fill(x, y, x + 1, y + height, 0xFF3C3C3C); // left
        context.fill(x + width - 1, y, x + width, y + height, 0xFF242424); // right

        // Subtle inner highlight (1px)
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x30FFFFFF);
        context.fill(x + 1, y + 1, x + 2, y + height - 1, 0x20FFFFFF);
    }
}
