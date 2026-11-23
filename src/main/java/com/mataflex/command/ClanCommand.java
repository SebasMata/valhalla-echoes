package com.mataflex.command;

import com.mataflex.clan.ClanVillageManager;
import com.mataflex.clan.VikingClan;
import com.mataflex.clan.reputation.ClanReputationData;
import com.mataflex.clan.reputation.ClanReputationData.ReputationChangeResult;
import com.mataflex.clan.reputation.ClanReputationLevel;
import com.mataflex.clan.reputation.ClanReputationManager;
import com.mataflex.entity.MysticalVikingEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;

/**
 * Comando {@code /clan} para consultar la reputación del jugador con los 4 clanes vikingos,
 * identificar el clan territorial de la región actual y gestionar reputaciones o afiliaciones como administrador.
 */
public final class ClanCommand {

    private static final DynamicCommandExceptionType INVALID_CLAN_EXCEPTION = new DynamicCommandExceptionType(
            id -> Component.translatable("command.valhalla-echoes.clan.invalid_clan", id)
    );

    private static final SuggestionProvider<CommandSourceStack> CLAN_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(VikingClan.VALUES).map(VikingClan::getId),
                    builder
            );

    private ClanCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommand(dispatcher)
        );
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("clan")
                .executes(context -> showSelfStatus(context.getSource()))
                .then(Commands.literal("status")
                        .executes(context -> showSelfStatus(context.getSource()))
                )
                .then(Commands.literal("region")
                        .executes(context -> showCurrentRegion(context.getSource()))
                )
                .then(Commands.literal("assign")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("clan", StringArgumentType.word())
                                        .suggests(CLAN_SUGGESTIONS)
                                        .executes(context -> assignEntitiesClan(
                                                context.getSource(),
                                                EntityArgument.getEntities(context, "targets"),
                                                parseClan(StringArgumentType.getString(context, "clan"))
                                        ))
                                )
                        )
                )
                .then(Commands.literal("reputation")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> getAllPlayerReputation(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player")
                                        ))
                                        .then(Commands.argument("clan", StringArgumentType.word())
                                                .suggests(CLAN_SUGGESTIONS)
                                                .executes(context -> getSinglePlayerReputation(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        parseClan(StringArgumentType.getString(context, "clan"))
                                                ))
                                        )
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("clan", StringArgumentType.word())
                                                .suggests(CLAN_SUGGESTIONS)
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(
                                                                ClanReputationData.MIN_REPUTATION,
                                                                ClanReputationData.MAX_REPUTATION
                                                        ))
                                                        .executes(context -> setPlayerReputation(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                parseClan(StringArgumentType.getString(context, "clan")),
                                                                IntegerArgumentType.getInteger(context, "amount")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("clan", StringArgumentType.word())
                                                .suggests(CLAN_SUGGESTIONS)
                                                .then(Commands.argument("delta", IntegerArgumentType.integer(-250, 250))
                                                        .executes(context -> addPlayerReputation(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                parseClan(StringArgumentType.getString(context, "clan")),
                                                                IntegerArgumentType.getInteger(context, "delta")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> resetAllPlayerReputation(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player")
                                        ))
                                        .then(Commands.argument("clan", StringArgumentType.word())
                                                .suggests(CLAN_SUGGESTIONS)
                                                .executes(context -> resetSinglePlayerReputation(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        parseClan(StringArgumentType.getString(context, "clan"))
                                                ))
                                        )
                                )
                        )
                )
        );
    }

    private static int assignEntitiesClan(
            CommandSourceStack source,
            Collection<? extends Entity> targets,
            VikingClan clan
    ) {
        int updated = 0;
        for (Entity entity : targets) {
            if (entity instanceof MysticalVikingEntity viking) {
                viking.setClan(clan);
                viking.equipClanShield();
                updated++;
            }
        }

        if (updated == 0) {
            source.sendFailure(Component.translatable("command.valhalla-echoes.clan.assign.none"));
            return 0;
        }

        int finalCount = updated;
        source.sendSuccess(
                () -> Component.translatable(
                        "command.valhalla-echoes.clan.assign.success",
                        finalCount,
                        clan.getDisplayName()
                ),
                true
        );
        return updated;
    }

    private static VikingClan parseClan(String rawClanId) throws CommandSyntaxException {
        return VikingClan.fromId(rawClanId)
                .orElseThrow(() -> INVALID_CLAN_EXCEPTION.create(rawClanId));
    }

    private static int showSelfStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        VikingClan regionClan = ClanVillageManager.getClanForPos(player.blockPosition());
        sendReputationTable(source, player, regionClan, true);
        return 1;
    }

    private static int showCurrentRegion(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        VikingClan regionClan = ClanVillageManager.getClanForPos(player.blockPosition());
        int score = ClanReputationManager.getReputation(player, regionClan);
        ClanReputationLevel level = ClanReputationLevel.fromReputation(score);

        source.sendSuccess(
                () -> Component.translatable(
                        "command.valhalla-echoes.clan.region",
                        regionClan.getDisplayName(),
                        regionClan.getRival().getDisplayName(),
                        formatScore(score),
                        level.getDisplayName()
                ),
                false
        );
        return 1;
    }

    private static int getAllPlayerReputation(CommandSourceStack source, ServerPlayer target) {
        VikingClan targetRegionClan = ClanVillageManager.getClanForPos(target.blockPosition());
        sendReputationTable(source, target, targetRegionClan, false);
        return 1;
    }

    private static int getSinglePlayerReputation(CommandSourceStack source, ServerPlayer target, VikingClan clan) {
        int score = ClanReputationManager.getReputation(target, clan);
        ClanReputationLevel level = ClanReputationLevel.fromReputation(score);

        source.sendSuccess(
                () -> Component.translatable(
                        "command.valhalla-echoes.clan.reputation.get_single",
                        target.getDisplayName(),
                        clan.getDisplayName(),
                        formatScore(score),
                        level.getDisplayName()
                ),
                false
        );
        return score;
    }

    private static int setPlayerReputation(
            CommandSourceStack source,
            ServerPlayer target,
            VikingClan clan,
            int amount
    ) {
        ReputationChangeResult result = ClanReputationManager.setReputation(target, clan, amount, false);
        source.sendSuccess(
                () -> Component.translatable(
                        "command.valhalla-echoes.clan.reputation.set",
                        target.getDisplayName(),
                        clan.getDisplayName(),
                        formatScore(result.newValue()),
                        result.newLevel().getDisplayName()
                ),
                true
        );
        return result.newValue();
    }

    private static int addPlayerReputation(
            CommandSourceStack source,
            ServerPlayer target,
            VikingClan clan,
            int delta
    ) {
        // Los comandos administrativos modifican únicamente el clan indicado (sin rivalidad cruzada)
        ReputationChangeResult result = ClanReputationManager.addReputation(
                target,
                clan,
                delta,
                false,
                false
        ).primary();

        String signedDelta = delta >= 0 ? "+" + delta : Integer.toString(delta);
        source.sendSuccess(
                () -> Component.translatable(
                        "command.valhalla-echoes.clan.reputation.add",
                        signedDelta,
                        target.getDisplayName(),
                        clan.getDisplayName(),
                        formatScore(result.newValue()),
                        result.newLevel().getDisplayName()
                ),
                true
        );
        return result.newValue();
    }

    private static int resetAllPlayerReputation(CommandSourceStack source, ServerPlayer target) {
        ClanReputationManager.resetReputation(target, null);
        source.sendSuccess(
                () -> Component.translatable(
                        "command.valhalla-echoes.clan.reputation.reset_all",
                        target.getDisplayName()
                ),
                true
        );
        return 1;
    }

    private static int resetSinglePlayerReputation(CommandSourceStack source, ServerPlayer target, VikingClan clan) {
        ClanReputationManager.resetReputation(target, clan);
        source.sendSuccess(
                () -> Component.translatable(
                        "command.valhalla-echoes.clan.reputation.reset_single",
                        target.getDisplayName(),
                        clan.getDisplayName()
                ),
                true
        );
        return 1;
    }

    private static void sendReputationTable(
            CommandSourceStack source,
            ServerPlayer target,
            VikingClan regionClan,
            boolean isSelf
    ) {
        EnumMap<VikingClan, Integer> scores = ClanReputationManager.getAllReputations(target.level(), target.getUUID());

        source.sendSuccess(
                () -> isSelf
                        ? Component.translatable("command.valhalla-echoes.clan.status.header")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        : Component.translatable("command.valhalla-echoes.clan.status.header_other", target.getDisplayName())
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                false
        );

        for (VikingClan clan : VikingClan.VALUES) {
            int score = scores.getOrDefault(clan, ClanReputationData.DEFAULT_REPUTATION);
            ClanReputationLevel level = ClanReputationLevel.fromReputation(score);

            MutableComponent line = Component.translatable(
                    "command.valhalla-echoes.clan.status.entry",
                    clan.getDisplayName(),
                    formatScore(score),
                    level.getDisplayName()
            );

            if (clan == regionClan) {
                line.append(Component.literal(" "))
                        .append(Component.translatable("command.valhalla-echoes.clan.status.current_region_tag")
                                .withStyle(ChatFormatting.GOLD));
            }

            source.sendSuccess(() -> line, false);
        }
    }

    private static MutableComponent formatScore(int score) {
        ChatFormatting color = score > 0 ? ChatFormatting.GREEN
                : (score < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
        return Component.literal(Integer.toString(score)).withStyle(color);
    }
}
