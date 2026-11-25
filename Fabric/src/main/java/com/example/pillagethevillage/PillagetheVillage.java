// SPDX-License-Identifier: LGPL-3.0-only
package com.example.pillagethevillage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.Level;

public final class PillagetheVillage implements ModInitializer {
    public static final String MODID = "pillagethevillage";
    public static final EntityType<Mercenary> MERCENARY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            id("mercenary"), EntityType.Builder.of(Mercenary::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).passengerAttachments(2.0F).ridingOffset(-0.6F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id("mercenary"))));
    public static final Item MERCENARY_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, id("mercenary_spawn_egg"),
            new SpawnEggItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id("mercenary_spawn_egg")))
                    .spawnEgg(MERCENARY)));
    public static final GameRule<Boolean> VOLUNTEER_SPAWN = GameRuleBuilder.forBoolean(true)
            .category(GameRuleCategory.SPAWNING).buildAndRegister(id("volunteer_spawn"));

    private static final VolunteerSpawner VOLUNTEER_SPAWNER = new VolunteerSpawner();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(MERCENARY, Mercenary.createAttributes());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output ->
                output.insertAfter(Items.VINDICATOR_SPAWN_EGG, MERCENARY_SPAWN_EGG));
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            if (level.dimension() == Level.OVERWORLD) VOLUNTEER_SPAWNER.tick(level, true);
        });
    }
    public static EntityType<Mercenary> mercenaryType() {
        return MERCENARY;
    }

    public static GameRule<Boolean> volunteerSpawnRule() {
        return VOLUNTEER_SPAWN;
    }
}
