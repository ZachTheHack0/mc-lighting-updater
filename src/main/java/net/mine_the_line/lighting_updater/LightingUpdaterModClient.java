package net.mine_the_line.lighting_updater;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.LinkedHashSet;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.mine_the_line.lighting_updater.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class LightingUpdaterModClient implements ClientModInitializer {
    private int tick = 0; // client tick number
    private final LinkedHashSet<BlockPos> alreadyTickedBlocks = new LinkedHashSet<>();

    @Override
	public void onInitializeClient() {
        ConfigManager.load(); // load config on startup
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("lightingupdater")
                    .then(ClientCommandManager.literal("reload")
                        .executes(ctx -> {
                            ConfigManager.load();
                            ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.reload"));
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("save")
                        .executes(ctx -> {
                            ConfigManager.save();
                            ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.save"));
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("set")
                        .then(ClientCommandManager.literal("radius")
                            .then(ClientCommandManager.argument("value", IntegerArgumentType.integer())
                                .executes(ctx -> {
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    ConfigManager.config.radius = value;
                                    ConfigManager.save();
                                    ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.update.radius", value));
                                    return 1;
                                })
                            )
                        )
                        .then(ClientCommandManager.literal("update_interval")
                            .then(ClientCommandManager.argument("value", IntegerArgumentType.integer())
                                .executes(ctx -> {
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    ConfigManager.config.update_interval = value;
                                    ConfigManager.save();
                                    ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.update.update_interval", value));
                                    return 1;
                                })
                            )
                        )
                        .then(ClientCommandManager.literal("reupdate_interval")
                            .then(ClientCommandManager.argument("value", IntegerArgumentType.integer())
                                .executes(ctx -> {
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    ConfigManager.config.reupdate_interval = value;
                                    ConfigManager.save();
                                    ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.update.reupdate_interval", value));
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(ClientCommandManager.literal("get")
                        .then(ClientCommandManager.literal("radius")
                            .executes(ctx -> {
                                ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.get.radius", ConfigManager.config.radius));
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("update_interval")
                            .executes(ctx -> {
                                ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.get.update_interval", ConfigManager.config.update_interval));
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("reupdate_interval")
                            .executes(ctx -> {
                                ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.get.reupdate_interval", ConfigManager.config.reupdate_interval));
                                return 1;
                            })
                        )
                )
            );
        });
	}
    protected void onClientTick(@NotNull Minecraft client) {
        tick %= 100000; // to prevent overflow into negatives
        tick++;
        if (tick % ConfigManager.config.update_interval != 0) return;
        if (tick % ConfigManager.config.reupdate_interval == 0) this.alreadyTickedBlocks.clear(); // clear alreadyTickedBlocks if needed

        if (client.player == null || client.level == null) return; // null checks
        int radius = ConfigManager.config.radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = client.player.blockPosition().offset(x, y, z);
                    BlockState state = client.level.getBlockState(pos);
                    if (state.isAir()) continue; // skip air

                    // detect light-emitting blocks
                    if (state.getLightEmission() > 0 || !state.isSolidRender()) {
                        // skip already ticked blocks
                        if (this.alreadyTickedBlocks.contains(pos)) continue;
                        this.alreadyTickedBlocks.add(pos);
                        client.level.getLightEngine().checkBlock(pos); // tick block
                        for (Direction dir : Direction.values()) { // also repeat for each adjacent block
                            BlockPos relPos = pos.relative(dir);
                            if (this.alreadyTickedBlocks.contains(relPos)) continue;
                            client.level.getLightEngine().checkBlock(relPos);
                            this.alreadyTickedBlocks.add(relPos);
                        }
                    }
                }
            }
        }
    }
}