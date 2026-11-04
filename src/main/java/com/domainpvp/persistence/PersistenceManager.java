package com.domainpvp.persistence;

import com.domainpvp.player.PlayerSnapshot;
import com.domainpvp.player.PlayerStateManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

/**
 * Server persistence manager for safeguarding matches, player snapshots,
 * and recovering from crashes or restarts.
 */
public class PersistenceManager {
    private static final PersistenceManager INSTANCE = new PersistenceManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private File storageDirectory;

    private PersistenceManager() {}

    public static PersistenceManager getInstance() {
        return INSTANCE;
    }

    public void initialize(File worldSaveDir) {
        this.storageDirectory = new File(worldSaveDir, "domainpvp");
        if (!storageDirectory.exists()) {
            storageDirectory.mkdirs();
        }
        File snapshotDir = new File(storageDirectory, "snapshots");
        if (!snapshotDir.exists()) {
            snapshotDir.mkdirs();
        }
        recoverPendingSnapshots();
    }

    public synchronized void persistSnapshot(PlayerSnapshot snapshot) {
        if (storageDirectory == null || snapshot == null) return;
        File snapshotDir = new File(storageDirectory, "snapshots");
        File file = new File(snapshotDir, snapshot.getPlayerUuid() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(snapshot, writer);
        } catch (IOException e) {
            System.err.println("[PvP Domain] Failed to persist snapshot for " + snapshot.getPlayerUuid() + ": " + e.getMessage());
        }
    }

    public synchronized void removePersistedSnapshot(UUID playerUuid) {
        if (storageDirectory == null) return;
        File file = new File(new File(storageDirectory, "snapshots"), playerUuid + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    private void recoverPendingSnapshots() {
        File snapshotDir = new File(storageDirectory, "snapshots");
        File[] files = snapshotDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return;

        System.out.println("[PvP Domain] Found " + files.length + " pending snapshot(s) from previous session. Recovering...");
        for (File f : files) {
            try (FileReader reader = new FileReader(f)) {
                PlayerSnapshot snapshot = GSON.fromJson(reader, PlayerSnapshot.class);
                if (snapshot != null) {
                    PlayerStateManager.getInstance().recordSnapshot(snapshot);
                    System.out.println("[PvP Domain] Loaded emergency recovery snapshot for player: " + snapshot.getUsername() + " (" + snapshot.getPlayerUuid() + ")");
                }
            } catch (Exception e) {
                System.err.println("[PvP Domain] Could not read snapshot " + f.getName() + ": " + e.getMessage());
            }
        }
    }
}

