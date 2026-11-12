package com.mataflex.mixin;

import com.mataflex.item.ModItems;
import com.mataflex.item.custom.TargeShieldItem;
import com.mataflex.sound.CustomSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class TargeShieldMixin {

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void onHurtServer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {

		if ((Object) this instanceof Player player) {

			ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);

			if (helmet.is(ModItems.HORNED_HELMET) && source.getDirectEntity() instanceof LivingEntity attacker) {

				if (player.getRandom().nextFloat() < 0.85f) {
					attacker.hurtServer(level, level.damageSources().thorns(player), 3.0f);
					level.playSound(null, player.getX(), player.getY(), player.getZ(),
							CustomSounds.HORNED_HELMET_STAB, SoundSource.PLAYERS, 1.0F, 1.0F);
				}
			}

			ItemStack mainHand = player.getMainHandItem();
			ItemStack offHand = player.getOffhandItem();
			ItemStack shieldStack = (mainHand.getItem() instanceof TargeShieldItem) ? mainHand :
					(offHand.getItem() instanceof TargeShieldItem) ? offHand : null;

			if (shieldStack != null && player.isUsingItem()) {

				int useTicks = player.getTicksUsingItem();

				if (useTicks <= 5) {
					// PARRY PERFECTO
					level.playSound(null, player.getX(), player.getY(), player.getZ(),
							SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.5F, 1.0F);

					if (source.getDirectEntity() instanceof LivingEntity attacker) {
						attacker.knockback(1.0D, player.getX() - attacker.getX(), player.getZ() - attacker.getZ(), source, amount);
					}
					cir.setReturnValue(false);

				} else {
					// BLOQUEO NORMAL
					level.playSound(null, player.getX(), player.getY(), player.getZ(),
							SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);

					EquipmentSlot slot = (shieldStack == mainHand) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
					shieldStack.hurtAndBreak((int) amount, player, slot);

					cir.setReturnValue(false);
				}
			}
		}
	}
}