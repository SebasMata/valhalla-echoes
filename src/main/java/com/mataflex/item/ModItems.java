package com.mataflex.item;

import com.mataflex.ValhallaEchoes;
import com.mataflex.item.custom.HornedHelmetItem;
import com.mataflex.item.custom.MysticalShovelItem;
import com.mataflex.item.custom.TargeShieldItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Function;

public class ModItems {

    public static <GenericItem extends Item> GenericItem register(String name, Function<Item.Properties, GenericItem> itemFactory, Item.Properties settings) {
        // Create the item key.
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ValhallaEchoes.MOD_ID, name));

        // Create the item instance.
        GenericItem item = itemFactory.apply(settings.setId(itemKey));

        // Register the item.
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    // ITEMS
    public static final Item SUSPICIOUS_SUBSTANCE = register("suspicious_substance", Item::new, new Item.Properties());

    public static final Consumable REGENARATION_FOOD_CONSUMABLE_COMPONENT = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 180 * 20, 1), 1.0f))
            .build();

    public static final FoodProperties REGENERATION_FOOD_COMPONENT = new FoodProperties.Builder()
            .alwaysEdible()
            .build();

    public static final Item MYSTICAL_ESSENCE = register("mystical_essence", Item::new,
            new Item.Properties().food(REGENERATION_FOOD_COMPONENT, REGENARATION_FOOD_CONSUMABLE_COMPONENT).rarity(Rarity.EPIC));

    public static final Item MYSTICAL_INGOT = register("mystical_ingot", Item::new, new Item.Properties().rarity(Rarity.EPIC));

    public static final Item OBSIDIAN_ROD = register("obsidian_rod", Item::new, new Item.Properties().rarity(Rarity.EPIC));

    public static final Item RUNIC_GEM = register("runic_gem", Item::new, new Item.Properties().rarity(Rarity.EPIC));

    public static final SpawnEggItem VIKING_SPAWN_EGG = register(
            "viking_spawn_egg",
            SpawnEggItem::new,
            new Item.Properties().spawnEgg(ValhallaEchoes.MYSTICAL_VIKING)
    );

    public static final SpawnEggItem DRAUGR_SPAWN_EGG = register(
            "draugr_spawn_egg",
            SpawnEggItem::new,
            new Item.Properties().spawnEgg(ValhallaEchoes.DRAUGR)
    );

    // MYSTICAL TOOL MATERIAL
    public static final ToolMaterial MYSTICAL_TOOL_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL,
            8192,
            20F,
            1.5F,
            15,
            ToolMaterial.DIAMOND.repairItems()
    );

    // MYSTICAL TOOLS
    public static final Item MYSTICAL_SWORD = register(
            "mystical_sword",
            Item::new,
            new Item.Properties().sword(MYSTICAL_TOOL_MATERIAL, 15f, -2.0f)
                    .rarity(Rarity.EPIC)
    );

    public static final Item MYSTICAL_AXE = register (
            "mystical_axe",
            Item::new,
            new Item.Properties().axe(MYSTICAL_TOOL_MATERIAL, 18.5f, -3.0f)
                    .rarity(Rarity.EPIC)
    );

    public static final Item MYSTICAL_PICKAXE = register (
            "mystical_pickaxe",
            Item::new,
            new Item.Properties().pickaxe(MYSTICAL_TOOL_MATERIAL, 4.5f, -2.8f)
                    .rarity(Rarity.EPIC)
    );

    public static final Item MYSTICAL_SHOVEL = register(
            "mystical_shovel",
            properties -> new MysticalShovelItem(MYSTICAL_TOOL_MATERIAL, 4.5f, -3.0f, properties),
            new Item.Properties()
                    .rarity(Rarity.EPIC)
    );

    // Targe shield
    public static final Item TARGE_SHIELD = register(
            "targe_shield",
            TargeShieldItem::new,
            new Item.Properties()
                    .durability(1024)
                    .rarity(Rarity.EPIC)
    );

    // ARMOR
    public static final Item MYSTICAL_HELMET = register(
            "mystical_helmet",
            Item::new,
            new Item.Properties()
                    .humanoidArmor(ModArmorMaterials.MYSTICAL, ArmorType.HELMET)
                    .durability(2048)
                    .rarity(Rarity.EPIC)
    );

    public static final Item MYSTICAL_CHESTPLATE = register(
            "mystical_chestplate",
            Item::new,
            new Item.Properties()
                    .humanoidArmor(ModArmorMaterials.MYSTICAL, ArmorType.CHESTPLATE)
                    .durability(2048)
                    .rarity(Rarity.EPIC)
    );

    public static final Item MYSTICAL_LEGGINGS = register(
            "mystical_leggings",
            Item::new,
            new Item.Properties()
                    .humanoidArmor(ModArmorMaterials.MYSTICAL, ArmorType.LEGGINGS)
                    .durability(2048)
                    .rarity(Rarity.EPIC)
    );

    public static final Item MYSTICAL_BOOTS = register(
            "mystical_boots",
            Item::new,
            new Item.Properties()
                    .humanoidArmor(ModArmorMaterials.MYSTICAL, ArmorType.BOOTS)
                    .durability(2048)
                    .rarity(Rarity.EPIC)
    );

    public static final Item HORNED_HELMET = registerHornedHelmet(
            new Item.Properties()
                    .durability(3196)
                    .humanoidArmor(ModArmorMaterials.MYSTICAL, ArmorType.HELMET)
                    .rarity(Rarity.EPIC)
    );

    public static HornedHelmetItem registerHornedHelmet(Item.Properties properties) {

        HornedHelmetItem helmetItem;

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ValhallaEchoes.MOD_ID, "horned_helmet"));

        properties.setId(itemKey);

        helmetItem = new HornedHelmetItem(properties);

        Registry.register(BuiltInRegistries.ITEM, itemKey, helmetItem);

        return helmetItem;
    }

    public static void initialize() {

        // ITEMS
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register((itemGroup) -> itemGroup.accept(ModItems.SUSPICIOUS_SUBSTANCE));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS)
                .register((itemGroup) -> itemGroup.accept(ModItems.MYSTICAL_ESSENCE));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register((itemGroup) -> itemGroup.accept(ModItems.MYSTICAL_INGOT));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register((itemGroup) -> itemGroup.accept(ModItems.OBSIDIAN_ROD));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(itemGroup -> {
                    itemGroup.accept(ModItems.VIKING_SPAWN_EGG);
                    itemGroup.accept(ModItems.DRAUGR_SPAWN_EGG);
                });

        // TOOLS
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register((itemGroup) -> itemGroup.accept(ModItems.MYSTICAL_SWORD));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register((itemGroup) -> itemGroup.accept(ModItems.MYSTICAL_AXE));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register((itemGroup) -> itemGroup.accept(ModItems.MYSTICAL_PICKAXE));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register((itemGroup) -> itemGroup.accept(ModItems.MYSTICAL_SHOVEL));
    }

}