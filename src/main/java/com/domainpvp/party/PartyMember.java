package com.domainpvp.party;

import java.util.UUID;

/**
 * Represents a member of a PvP Domain party.
 */
public class PartyMember {
    private final UUID uuid;
    private final String username;
    private boolean ready;
    private String selectedLoadoutName;

    public PartyMember(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.ready = false;
        this.selectedLoadoutName = "CLASSIC";
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public String getSelectedLoadoutName() {
        return selectedLoadoutName;
    }

    public void setSelectedLoadoutName(String selectedLoadoutName) {
        this.selectedLoadoutName = selectedLoadoutName;
    }
}

