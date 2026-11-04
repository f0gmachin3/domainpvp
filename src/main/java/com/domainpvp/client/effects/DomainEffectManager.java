package com.domainpvp.client.effects;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * Client-side visual and auditory effect manager.
 */
public class DomainEffectManager {
    private static final DomainEffectManager INSTANCE = new DomainEffectManager();

    private boolean arenaActive = false;
    private Vec3d arenaCenter = Vec3d.ZERO;
    private double boundaryRadius = 20.0;

    private DomainEffectManager() {}

    public static DomainEffectManager getInstance() {
        return INSTANCE;
    }

    public void onArenaStarted(double centerX, double centerY, double centerZ, double radius) {
        this.arenaActive = true;
        this.arenaCenter = new Vec3d(centerX, centerY, centerZ);
        this.boundaryRadius = radius;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            mc.world.playSound(centerX, centerY, centerZ, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.AMBIENT, 1.5f, 1.0f, false);
        }
    }

    public void onBlockConstructed(double x, double y, double z) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            mc.world.addParticle(ParticleTypes.POOF, x + 0.5, y + 1.0, z + 0.5, 0.0, 0.05, 0.0);
        }
    }

    public void onDomainCollapsed() {
        this.arenaActive = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null && arenaCenter != null) {
            mc.world.playSound(arenaCenter.x, arenaCenter.y, arenaCenter.z, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.AMBIENT, 1.5f, 0.8f, false);
            for (int i = 0; i < 30; i++) {
                double angle = (i / 30.0) * Math.PI * 2;
                double px = arenaCenter.x + Math.cos(angle) * boundaryRadius;
                double pz = arenaCenter.z + Math.sin(angle) * boundaryRadius;
                mc.world.addParticle(ParticleTypes.PORTAL, px, arenaCenter.y + 2.0, pz, 0, 0.1, 0);
            }
        }
    }

    public void tickBoundaryParticles() {
        if (!arenaActive) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        // Render circular perimeter particles
        int points = 32;
        for (int i = 0; i < points; i++) {
            double angle = (i / (double) points) * Math.PI * 2.0;
            double px = arenaCenter.x + Math.cos(angle) * boundaryRadius;
            double pz = arenaCenter.z + Math.sin(angle) * boundaryRadius;
            mc.world.addParticle(ParticleTypes.ENCHANT, px, arenaCenter.y + 1.5, pz, 0.0, 0.0, 0.0);
        }
    }
}

