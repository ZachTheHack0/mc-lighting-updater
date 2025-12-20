package net.mine_the_line.lighting_updater;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.LinkedHashSet;
import java.util.Set;

import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.mine_the_line.lighting_updater.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import static net.mine_the_line.lighting_updater.config.ConfigManager.config;

@Environment(EnvType.CLIENT)
public class LightingUpdaterModClient implements ClientModInitializer {
	private int tick = 0;
	private final Set<BlockPos> alreadyTickedBlocks = new LinkedHashSet<>();

	@Override
	public void onInitializeClient() {
		ConfigManager.load();   // load config on startup
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
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
								config.radius = value;
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
								config.update_interval = value;
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
								config.reupdate_interval = value;
								ConfigManager.save();
								ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.update.reupdate_interval", value));
								return 1;
							})
						)
					)
					.then(ClientCommandManager.literal("tick_adjacent_blocks")
						.then(ClientCommandManager.argument("value", BoolArgumentType.bool())
							.executes(ctx -> {
								boolean value = BoolArgumentType.getBool(ctx, "value");
								config.tick_adjacent_blocks = value;
								ConfigManager.save();
								ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.update.tick_adjacent_blocks", value));
								return 1;
							})
						)
					)
				)
				.then(ClientCommandManager.literal("get")
					.then(ClientCommandManager.literal("radius")
						.executes(ctx -> {
							ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.get.radius", config.radius));
							return 1;
                        })
					)
					.then(ClientCommandManager.literal("update_interval")
						.executes(ctx -> {
							ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.get.update_interval", config.update_interval));
							return 1;
                        })
					)
					.then(ClientCommandManager.literal("reupdate_interval")
						.executes(ctx -> {
							ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.get.reupdate_interval", config.reupdate_interval));
							return 1;
                        })
					)
					.then(ClientCommandManager.literal("tick_adjacent_blocks")
						.executes(ctx -> {
							ctx.getSource().sendFeedback(Component.translatable("lightingupdater.config.key.get.tick_adjacent_blocks", config.tick_adjacent_blocks));
							return 1;
                        })
					)
				)
			)
		);
	}
	private void tickBlock(@NotNull Minecraft client, BlockPos pos) {
		// skip already ticked blocks for performance
		if (alreadyTickedBlocks.contains(pos)) return;
		alreadyTickedBlocks.add(pos);

		// force a recheck for this block
		client.level.getLightEngine().checkBlock(pos);

		// also check adjacent blocks if configured that way to update them
		if (config.tick_adjacent_blocks)
			for (Direction dir : Direction.values()) {
				BlockPos relPos = pos.relative(dir);
				if (alreadyTickedBlocks.contains(relPos))
					continue;
				client.level.getLightEngine().checkBlock(relPos);
				alreadyTickedBlocks.add(relPos);
			}
	}
	protected void onClientTick(@NotNull Minecraft client) {
		tick++;
		if (tick % config.update_interval == 0) return;
		if (tick % config.reupdate_interval == 0) alreadyTickedBlocks.clear();

		if (client.player == null || client.level == null) return;
		int radius = config.radius;

		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					BlockPos pos = client.player.blockPosition().offset(x, y, z);
					BlockState state = client.level.getBlockState(pos);

					// skip air for performance
					if (state.isAir()) continue;

					// detect light-emitting blocks
					if (state.getLightEmission() > 0 || !state.isSolidRender())
                        tickBlock(client, pos);
				}
			}
		}
	}
}