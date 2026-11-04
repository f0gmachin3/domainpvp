package com.domainpvp.arena;

import com.domainpvp.config.ModConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative manager controlling arena template allocation,
 * progressive chunk-safe construction ticks, and deterministic cleanup.
 */
public class ArenaManager {
    private static final ArenaManager INSTANCE = new ArenaManager();

    private final Map<String, ArenaTemplate> templates = new HashMap<>();
    private final Map<UUID, ArenaInstance> activeArenas = new ConcurrentHashMap<>();
    private int nextAllocationSlot = 0;

    public interface ConstructionListener {
        void onProgress(ArenaInstance instance, int placed, int total);
        void onComplete(ArenaInstance instance);
    }

    public interface CleanupListener {
        void onComplete(ArenaInstance instance);
    }

    private ConstructionListener constructionListener;
    private CleanupListener cleanupListener;

    private ArenaManager() {
        ArenaTemplate colosseum = ArenaTemplate.createColosseumTemplate();
        templates.put(colosseum.getId(), colosseum);
    }

    public static ArenaManager getInstance() {
        return INSTANCE;
    }

    public void setConstructionListener(ConstructionListener listener) {
        this.constructionListener = listener;
    }

    public void setCleanupListener(CleanupListener listener) {
        this.cleanupListener = listener;
    }

    public synchronized ArenaInstance allocateArena(String templateId) {
        ArenaTemplate template = templates.get(templateId);
        if (template == null) {
            template = templates.get("domainpvp:colosseum");
        }

        ModConfig.ArenaConfig cfg = ModConfig.get().arena;
        int slot = nextAllocationSlot++;
        int originX = cfg.arenaOriginX + (slot * cfg.arenaSpacing);
        int originY = cfg.arenaOriginY;
        int originZ = cfg.arenaOriginZ;

        UUID id = UUID.randomUUID();
        ArenaInstance instance = new ArenaInstance(id, template, cfg.arenaDimension, originX, originY, originZ);
        activeArenas.put(id, instance);
        return instance;
    }

    public ArenaInstance getArena(UUID instanceId) {
        return activeArenas.get(instanceId);
    }

    /**
     * Advances progressive construction on the server thread by a configured batch size.
     */
    public synchronized List<ArenaBlockEntry> tickConstruction(ArenaInstance instance, int maxBlocksToPlace) {
        if (instance.getStatus() != ArenaInstance.Status.ALLOCATED && instance.getStatus() != ArenaInstance.Status.BUILDING) {
            return Collections.emptyList();
        }

        instance.setStatus(ArenaInstance.Status.BUILDING);
        List<ArenaBlockEntry> placedThisTick = new ArrayList<>();

        for (int i = 0; i < maxBlocksToPlace && !instance.getConstructionQueue().isEmpty(); i++) {
            ArenaBlockEntry entry = instance.getConstructionQueue().poll();
            if (entry != null) {
                instance.getPlacedBlocks().push(entry);
                placedThisTick.add(entry);
            }
        }

        if (constructionListener != null) {
            constructionListener.onProgress(instance, instance.getPlacedCount(), instance.getTotalBlocks());
        }

        if (instance.getConstructionQueue().isEmpty()) {
            instance.setStatus(ArenaInstance.Status.READY);
            if (constructionListener != null) {
                constructionListener.onComplete(instance);
            }
        }

        return placedThisTick;
    }

    /**
     * Ticks destruction/cleanup, rolling back placed blocks.
     */
    public synchronized List<ArenaBlockEntry> tickCleanup(ArenaInstance instance, int maxBlocksToClear) {
        instance.setStatus(ArenaInstance.Status.CLEANING_UP);
        List<ArenaBlockEntry> clearedThisTick = new ArrayList<>();

        for (int i = 0; i < maxBlocksToClear && !instance.getPlacedBlocks().isEmpty(); i++) {
            ArenaBlockEntry entry = instance.getPlacedBlocks().pop();
            clearedThisTick.add(entry);
        }

        if (instance.getPlacedBlocks().isEmpty()) {
            instance.setStatus(ArenaInstance.Status.RELEASED);
            activeArenas.remove(instance.getInstanceId());
            if (cleanupListener != null) {
                cleanupListener.onComplete(instance);
            }
        }

        return clearedThisTick;
    }

    public Collection<ArenaInstance> getActiveArenas() {
        return Collections.unmodifiableCollection(activeArenas.values());
    }

    public synchronized void clear() {
        activeArenas.clear();
        nextAllocationSlot = 0;
    }
}

