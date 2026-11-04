package com.domainpvp.arena;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * An actively allocated arena domain situated at designated world coordinates.
 */
public class ArenaInstance {
    public enum Status {
        ALLOCATED,
        BUILDING,
        READY,
        CLEANING_UP,
        RELEASED
    }

    private final UUID instanceId;
    private final ArenaTemplate template;
    private final String dimensionId;
    private final int originX;
    private final int originY;
    private final int originZ;

    private final Deque<ArenaBlockEntry> constructionQueue = new ArrayDeque<>();
    private final Deque<ArenaBlockEntry> placedBlocks = new ArrayDeque<>();
    private final int totalBlocks;
    private Status status = Status.ALLOCATED;

    public ArenaInstance(UUID instanceId, ArenaTemplate template, String dimensionId, int originX, int originY, int originZ) {
        this.instanceId = instanceId;
        this.template = template;
        this.dimensionId = dimensionId;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;

        this.constructionQueue.addAll(template.getBlocks());
        this.totalBlocks = template.getBlocks().size();
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public ArenaTemplate getTemplate() {
        return template;
    }

    public String getDimensionId() {
        return dimensionId;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getOriginZ() {
        return originZ;
    }

    public Deque<ArenaBlockEntry> getConstructionQueue() {
        return constructionQueue;
    }

    public Deque<ArenaBlockEntry> getPlacedBlocks() {
        return placedBlocks;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public int getPlacedCount() {
        return placedBlocks.size();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isInsideBoundary(double x, double y, double z) {
        double dx = x - (originX + 0.5);
        double dz = z - (originZ + 0.5);
        double dy = y - originY;

        double horizontalDistanceSq = dx * dx + dz * dz;
        double radius = template.getBoundaryRadius();

        return horizontalDistanceSq <= (radius * radius) && dy >= -2.0 && dy <= template.getBoundaryHeight();
    }

    public ArenaSpawnPoint getSpawn1World() {
        ArenaSpawnPoint rel = template.getSpawnPoints().get(0);
        return new ArenaSpawnPoint(originX + rel.x(), originY + rel.y(), originZ + rel.z(), rel.yaw(), rel.pitch());
    }

    public ArenaSpawnPoint getSpawn2World() {
        ArenaSpawnPoint rel = template.getSpawnPoints().size() > 1 ? template.getSpawnPoints().get(1) : template.getSpawnPoints().get(0);
        return new ArenaSpawnPoint(originX + rel.x(), originY + rel.y(), originZ + rel.z(), rel.yaw(), rel.pitch());
    }
}

