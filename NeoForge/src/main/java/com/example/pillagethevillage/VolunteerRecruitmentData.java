// SPDX-License-Identifier: LGPL-3.0-only
package com.example.pillagethevillage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class VolunteerRecruitmentData extends SavedData {
    public static final Codec<VolunteerRecruitmentData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("spawn_delay", 24000).forGetter(data -> data.spawnDelay),
            Codec.INT.optionalFieldOf("spawn_chance", 25).forGetter(data -> data.spawnChance)
    ).apply(instance, VolunteerRecruitmentData::new));

    public static final SavedDataType<VolunteerRecruitmentData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PillagetheVillage.MODID, "volunteer_recruitment"),
            VolunteerRecruitmentData::new, CODEC, null);

    private int spawnDelay;
    private int spawnChance;

    public VolunteerRecruitmentData() {
        this(24000, 25);
    }

    private VolunteerRecruitmentData(int spawnDelay, int spawnChance) {
        this.spawnDelay = spawnDelay;
        this.spawnChance = spawnChance;
    }

    public int spawnDelay() {
        return this.spawnDelay;
    }

    public int spawnChance() {
        return this.spawnChance;
    }

    public void setSpawnDelay(int spawnDelay) {
        if (this.spawnDelay != spawnDelay) {
            this.spawnDelay = spawnDelay;
            this.setDirty();
        }
    }

    public void setSpawnChance(int spawnChance) {
        if (this.spawnChance != spawnChance) {
            this.spawnChance = spawnChance;
            this.setDirty();
        }
    }
}


