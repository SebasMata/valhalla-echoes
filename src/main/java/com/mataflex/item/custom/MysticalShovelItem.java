package com.mataflex.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class MysticalShovelItem extends Item {
    public MysticalShovelItem(ToolMaterial toolMaterial, float attackDamage, float attackSpeed, Item.Properties properties) {
        super(properties.shovel(toolMaterial, attackDamage, attackSpeed));
    }
}
