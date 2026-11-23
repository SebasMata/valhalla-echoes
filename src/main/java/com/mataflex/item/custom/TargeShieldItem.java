package com.mataflex.item.custom;

import com.mataflex.clan.ClanComponents;
import com.mataflex.clan.VikingClan;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public class TargeShieldItem extends ShieldItem {
    public TargeShieldItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull ItemUseAnimation getUseAnimation(@NonNull ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public int getUseDuration(@NonNull ItemStack stack, @NonNull LivingEntity entity) {
        return 72000;
    }

    @Override
    public @NonNull InteractionResult use(@NonNull Level level, Player player, @NonNull InteractionHand interactionHand) {
        player.startUsingItem(interactionHand);
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            @NonNull ItemStack stack,
            @NonNull TooltipContext context,
            @NonNull TooltipDisplay displayComponent,
            @NonNull Consumer<Component> textConsumer,
            @NonNull TooltipFlag type
    ) {
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
        VikingClan clan = stack.get(ClanComponents.CLAN);
        if (clan != null) {
            textConsumer.accept(Component.translatable(
                    "itemTooltip.valhalla-echoes.targe_shield.clan",
                    clan.getDisplayName()
            ).withStyle(ChatFormatting.GRAY));
        }
    }
}
