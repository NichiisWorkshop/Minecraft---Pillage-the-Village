// SPDX-License-Identifier: LGPL-3.0-only
package com.example.pillagethevillage;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class MercenaryAdvancements {
    private MercenaryAdvancements() {}

    public static void award(ServerPlayer player, String advancementPath, String criterion) {
        AdvancementHolder advancement = ((ServerLevel) player.level()).getServer().getAdvancements().get(
                Identifier.fromNamespaceAndPath(PillagetheVillage.MODID, advancementPath));
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
