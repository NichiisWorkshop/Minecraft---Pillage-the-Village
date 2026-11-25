// SPDX-License-Identifier: LGPL-3.0-only
package com.example.pillagethevillage;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.level.ModifyCustomSpawnersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(PillagetheVillage.MODID)
public class PillagetheVillage {
    public static final String MODID = "pillagethevillage";

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Mercenary>> MERCENARY =
            ENTITY_TYPES.register("mercenary", () -> EntityType.Builder.of(Mercenary::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .passengerAttachments(2.0F)
                    .ridingOffset(-0.6F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MODID, "mercenary"))));

    public static final DeferredRegister<GameRule<?>> GAME_RULES =
            DeferredRegister.create(Registries.GAME_RULE, MODID);
    public static final DeferredHolder<GameRule<?>, GameRule<Boolean>> VOLUNTEER_SPAWN =
            GAME_RULES.register("volunteer_spawn", id -> new GameRule<>(
                    GameRuleCategory.SPAWNING, GameRuleType.BOOL, BoolArgumentType.bool(),
                    GameRuleTypeVisitor::visitBoolean, Codec.BOOL, enabled -> enabled ? 1 : 0,
                    true, FeatureFlagSet.of()));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<Item> MERCENARY_SPAWN_EGG = ITEMS.register("mercenary_spawn_egg", id ->
            new SpawnEggItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id))
                    .spawnEgg(MERCENARY.get())));

    public static EntityType<Mercenary> mercenaryType() {
        return MERCENARY.get();
    }

    public static GameRule<Boolean> volunteerSpawnRule() {
        return VOLUNTEER_SPAWN.get();
    }

    public PillagetheVillage(IEventBus modEventBus, ModContainer modContainer) {
        ENTITY_TYPES.register(modEventBus);
        GAME_RULES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(PillagetheVillage::registerAttributes);
        modEventBus.addListener(PillagetheVillage::addSpawnEggToSpawnEggsTab);
        NeoForge.EVENT_BUS.register(this);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MERCENARY.get(), Mercenary.createAttributes().build());
    }

    private static void addSpawnEggToSpawnEggsTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.insertAfter(Items.VINDICATOR_SPAWN_EGG.getDefaultInstance(),
                    MERCENARY_SPAWN_EGG.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    @SubscribeEvent
    public void addVolunteerSpawner(ModifyCustomSpawnersEvent event) {
        if (event.getLevel().dimension().equals(Level.OVERWORLD)) {
            event.addCustomSpawner(new VolunteerSpawner());
        }
    }
}


