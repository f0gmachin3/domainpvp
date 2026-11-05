package com.domainpvp.client.ui;

import com.domainpvp.network.c2s.SelectLoadoutRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Interactive 6-icon Kit Selection Screen opened when players stand on the spawn pedestals.
 */
public class KitSelectionScreen extends Screen {

    public record KitCard(String id, String displayName, ItemStack icon, String desc1, String desc2) {}

    private final List<KitCard> kits = List.of(
            new KitCard("SWORD_PVP", "Sword PvP", new ItemStack(Items.DIAMOND_SWORD), "Diamond Armor, Rod", "8 G-Apples, 16 Pearls"),
            new KitCard("AXE_PVP", "Axe PvP", new ItemStack(Items.DIAMOND_AXE), "Diamond Armor, Shield", "Bow & 32 Arrows, Axe"),
            new KitCard("UHC_PVP", "UHC PvP", new ItemStack(Items.GOLDEN_APPLE), "Water & Lava Buckets", "Cobwebs, Planks, Rod"),
            new KitCard("SMP_PVP", "SMP PvP", new ItemStack(Items.NETHERITE_SWORD), "Netherite Armor, Totem", "16 Pearls, 4 God Apples"),
            new KitCard("MACE_PVP", "Mace PvP", new ItemStack(Items.MACE), "Mace, 32 Wind Charges", "Totem, Pearls, Rockets"),
            new KitCard("POT_PVP", "Pot PvP", new ItemStack(Items.SPLASH_POTION), "Diamond Armor, Sword", "36 Splash Healing II")
    );

    private String votedKitId = null;

    public KitSelectionScreen() {
        super(Text.literal("Select PvP Kit"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark translucent dimmed overlay
        context.fill(0, 0, this.width, this.height, 0xB0000000);

        int topY = Math.max(15, this.height / 6 - 20);
        context.drawCenteredTextWithShadow(this.textRenderer, "§6§lPvP Domain §8| §eChoose Combat Kit", this.width / 2, topY, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Click an icon to cast your vote. If votes differ, one is selected at random!", this.width / 2, topY + 14, 0xFFAAAAAA);

        int cardW = 90;
        int cardH = 92;
        int gapX = 14;
        int gapY = 12;
        int totalGridW = 3 * cardW + 2 * gapX;
        int startX = (this.width - totalGridW) / 2;
        int startY = topY + 36;

        for (int i = 0; i < kits.size(); i++) {
            KitCard kit = kits.get(i);
            int col = i % 3;
            int row = i / 3;
            int cx = startX + col * (cardW + gapX);
            int cy = startY + row * (cardH + gapY);

            boolean hovered = mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH;
            boolean selected = kit.id().equals(votedKitId);

            // Card background
            int bg = selected ? 0xD0183818 : (hovered ? 0xD02C2C2C : 0xD0141414);
            context.fill(cx, cy, cx + cardW, cy + cardH, bg);

            // 1px Border
            int border = selected ? 0xFF00FF00 : (hovered ? 0xFFFFD700 : 0xFF3C3C3C);
            context.fill(cx, cy, cx + cardW, cy + 1, border);
            context.fill(cx, cy + cardH - 1, cx + cardW, cy + cardH, border);
            context.fill(cx, cy, cx + 1, cy + cardH, border);
            context.fill(cx + cardW - 1, cy, cx + cardW, cy + cardH, border);

            // Icon
            context.drawItem(kit.icon(), cx + (cardW - 16) / 2, cy + 8);

            // Kit name
            context.drawCenteredTextWithShadow(this.textRenderer, "§e§l" + kit.displayName(), cx + cardW / 2, cy + 30, 0xFFFFFFFF);

            // Kit descriptions
            context.drawCenteredTextWithShadow(this.textRenderer, "§7" + kit.desc1(), cx + cardW / 2, cy + 44, 0xFFAAAAAA);
            context.drawCenteredTextWithShadow(this.textRenderer, "§7" + kit.desc2(), cx + cardW / 2, cy + 56, 0xFFAAAAAA);

            // Action / Status tag
            if (selected) {
                context.drawCenteredTextWithShadow(this.textRenderer, "§a§l✔ VOTED", cx + cardW / 2, cy + 74, 0xFF55FF55);
            } else if (hovered) {
                context.drawCenteredTextWithShadow(this.textRenderer, "§e[Click to Vote]", cx + cardW / 2, cy + 74, 0xFFFFFF55);
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer, "§8[Key: " + (i + 1) + "]", cx + cardW / 2, cy + 74, 0xFF888888);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            int topY = Math.max(15, this.height / 6 - 20);
            int cardW = 90;
            int cardH = 92;
            int gapX = 14;
            int gapY = 12;
            int totalGridW = 3 * cardW + 2 * gapX;
            int startX = (this.width - totalGridW) / 2;
            int startY = topY + 36;

            for (int i = 0; i < kits.size(); i++) {
                KitCard kit = kits.get(i);
                int col = i % 3;
                int row = i / 3;
                int cx = startX + col * (cardW + gapX);
                int cy = startY + row * (cardH + gapY);

                if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                    castVote(kit.id());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_6) {
            int index = keyCode - GLFW.GLFW_KEY_1;
            if (index < kits.size()) {
                castVote(kits.get(index).id());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void castVote(String kitId) {
        this.votedKitId = kitId;
        ClientPlayNetworking.send(new SelectLoadoutRequestPayload(kitId, ""));
        if (this.client != null && this.client.player != null) {
            this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.2f);
        }
    }
}

