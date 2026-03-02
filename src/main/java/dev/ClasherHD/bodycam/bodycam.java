package dev.ClasherHD.bodycam;

import dev.ClasherHD.bodycam.enchantment.BodycamReachEnchantment;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.network.PacketHandler;
import dev.ClasherHD.bodycam.util.PersistentData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class bodycam implements ModInitializer {
        public static final String MODID = "bodycam";

        public static final Item BODYCAM_MONITOR = new BodycamMonitorItem(new Item.Properties().stacksTo(1));
        public static final Enchantment REACH_ENCHANTMENT = new BodycamReachEnchantment();
        public static final EntityType<BodycamDummyEntity> BODYCAM_DUMMY = Registry.register(
                        BuiltInRegistries.ENTITY_TYPE,
                        new ResourceLocation(MODID, "bodycam_dummy"),
                        FabricEntityTypeBuilder.<BodycamDummyEntity>create(MobCategory.MISC, BodycamDummyEntity::new)
                                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                                        .trackRangeBlocks(128)
                                        .trackedUpdateRate(1)
                                        .build());

        @Override
        public void onInitialize() {
                Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MODID, "bodycam_monitor"),
                                BODYCAM_MONITOR);
                Registry.register(BuiltInRegistries.ENCHANTMENT, new ResourceLocation(MODID, "reach"),
                                REACH_ENCHANTMENT);

                FabricDefaultAttributeRegistry.register(BODYCAM_DUMMY, BodycamDummyEntity.createAttributes());

                ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
                        ItemStack monitorStack = new ItemStack(BODYCAM_MONITOR);
                        entries.addAfter(Items.RECOVERY_COMPASS, monitorStack);
                });

                ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
                        ((PersistentData) newPlayer).bodycam$getPersistentData().putBoolean("bodycam_active", false);
                        newPlayer.setCamera(newPlayer);
                        ((PersistentData) newPlayer).bodycam$getPersistentData().remove("bodycam_last_x");
                        ((PersistentData) newPlayer).bodycam$getPersistentData().remove("bodycam_last_y");
                        ((PersistentData) newPlayer).bodycam$getPersistentData().remove("bodycam_last_z");
                        ((PersistentData) newPlayer).bodycam$getPersistentData().remove("bodycam_last_fall");
                });

                PacketHandler.init();
        }
}
