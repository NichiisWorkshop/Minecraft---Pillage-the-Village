// SPDX-License-Identifier: LGPL-3.0-only
package com.example.pillagethevillage;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public final class VolunteerSpawner implements CustomSpawner {
    private static final int TICK_DELAY = 1200;
    private static final int SPAWN_DELAY = 24000;
    private static final int SPAWN_CHANCE_INCREASE = 25;
    private final RandomSource random = RandomSource.create();
    private int tickDelay = TICK_DELAY;

    @Override
    public void tick(ServerLevel level, boolean spawnEnemies) {
        if (level.dimension() != Level.OVERWORLD
                || !level.getGameRules().get(PillagetheVillage.volunteerSpawnRule())) return;

        if (--this.tickDelay > 0) return;
        this.tickDelay = TICK_DELAY;

        VolunteerRecruitmentData data = level.getDataStorage().computeIfAbsent(VolunteerRecruitmentData.TYPE);
        int delay = data.spawnDelay() - TICK_DELAY;
        data.setSpawnDelay(delay);
        if (delay > 0) return;

        data.setSpawnDelay(SPAWN_DELAY);
        List<Mercenary> tamedMercenaries = this.findLoadedTamedMercenaries(level);
        if (tamedMercenaries.isEmpty()) return;
        if (this.hasUnrecruitedVolunteer(level)) return;

        int chance = data.spawnChance();
        data.setSpawnChance(Mth.clamp(chance + SPAWN_CHANCE_INCREASE, 25, 75));
        if (this.random.nextInt(100) > chance) return;

        Mercenary anchor = tamedMercenaries.get(this.random.nextInt(tamedMercenaries.size()));
        if (this.spawnVolunteer(anchor)) data.setSpawnChance(25);
    }

    private List<Mercenary> findLoadedTamedMercenaries(ServerLevel level) {
        List<Mercenary> mercenaries = new ArrayList<>();
        for (ServerLevel loadedLevel : level.getServer().getAllLevels()) {
            for (Entity entity : loadedLevel.getAllEntities()) {
                if (entity instanceof Mercenary mercenary && mercenary.isAlive() && mercenary.isTame()) {
                    mercenaries.add(mercenary);
                }
            }
        }
        return mercenaries;
    }

    private boolean hasUnrecruitedVolunteer(ServerLevel level) {
        for (ServerLevel loadedLevel : level.getServer().getAllLevels()) {
            for (Entity entity : loadedLevel.getAllEntities()) {
                if (entity instanceof Mercenary mercenary && mercenary.isTemporaryVolunteer()) return true;
            }
        }
        return false;
    }

    private boolean spawnVolunteer(Mercenary anchor) {
        ServerLevel level = (ServerLevel) anchor.level();
        BlockPos center = anchor.blockPosition();

        for (int i = 0; i < 10; i++) {
            int x = center.getX() + this.random.nextInt(96) - 48;
            int z = center.getZ() + this.random.nextInt(96) - 48;
            BlockPos spawnPos = new BlockPos(x,
                    level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            BlockPos support = spawnPos.below();
            if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
                    || !level.getFluidState(spawnPos).isEmpty()
                    || !level.getBlockState(spawnPos).getCollisionShape(level, spawnPos).isEmpty()
                    || !level.getBlockState(spawnPos.above()).getCollisionShape(level, spawnPos.above()).isEmpty()) {
                continue;
            }

            Mercenary volunteer = PillagetheVillage.mercenaryType().spawn(
                    level, spawnPos, EntitySpawnReason.EVENT);
            if (volunteer != null) {
                volunteer.setSkinVariant("volunteer");
                volunteer.setVolunteerDespawnDelay(48000);
                return true;
            }
        }
        return false;
    }
}


