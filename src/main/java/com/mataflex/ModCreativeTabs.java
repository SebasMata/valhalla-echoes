package com.mataflex; // Tu paquete aquí

import com.mataflex.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {

    // Crear la clave para tu pestaña
    public static final ResourceKey<CreativeModeTab> MYSTICAL_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    Identifier.fromNamespaceAndPath("tumodid", "mystical_tab"));

    public static void register() {
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                MYSTICAL_TAB,
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 7)
                        .title(Component.translatable("itemGroup.tumodid.mystical_tab"))
                        .icon(() -> new ItemStack(ModItems.MYSTICAL_SWORD))
                        .displayItems((itemDisplayParameters, output) -> {
                            output.accept(ModItems.SUSPICIOUS_SUBSTANCE);
                            output.accept(ModItems.MYSTICAL_ESSENCE);
                            output.accept(ModItems.MYSTICAL_INGOT);
                            output.accept(ModItems.OBSIDIAN_ROD);
                            output.accept(ModItems.MYSTICAL_SWORD);
                            output.accept(ModItems.MYSTICAL_AXE);
                            output.accept(ModItems.MYSTICAL_PICKAXE);
                            output.accept(ModItems.MYSTICAL_SHOVEL);
                            output.accept(ModItems.RUNIC_GEM);
                            output.accept(ModItems.TARGE_SHIELD);
                            output.accept(ModItems.MYSTICAL_HELMET);
                            output.accept(ModItems.MYSTICAL_CHESTPLATE);
                            output.accept(ModItems.MYSTICAL_LEGGINGS);
                            output.accept(ModItems.MYSTICAL_BOOTS);
                            output.accept(ModItems.HORNED_HELMET);
                            output.accept(ModItems.WOLF_BANNER_PATTERN);
                            output.accept(ModItems.RAVEN_BANNER_PATTERN);
                            output.accept(ModItems.BEAR_BANNER_PATTERN);
                            output.accept(ModItems.SERPENT_BANNER_PATTERN);
                            output.accept(ModItems.VIKING_SPAWN_EGG);
                            output.accept(ModItems.DRAUGR_SPAWN_EGG);
                        })
                        .build()
        );
    }
}