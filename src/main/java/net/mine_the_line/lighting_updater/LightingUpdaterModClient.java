package net.mine_the_line.lighting_updater;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import java.util.*;

import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.mine_the_line.lighting_updater.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class LightingUpdaterModClient implements ClientModInitializer {
	private static final Logger LOGGER = LogUtils.getLogger();
	private int tick = 0;
	private final Set<BlockPos> alreadyTickedBlocks = new LinkedHashSet<>();

	private int sendMsg(CommandContext<FabricClientCommandSource> ctx, Component msg) {
		ctx.getSource().sendFeedback(msg);
		return 1;
	}
	private int getValue(CommandContext<FabricClientCommandSource> ctx) {
		String elementName = ctx.getNodes().getLast().getNode().getName();
		return sendMsg(ctx, Component.translatable("lightingupdater.config.key.get.".concat(elementName), switch (elementName) {
				case "radius" -> ConfigManager.config.radius;
				case "update_interval" -> ConfigManager.config.update_interval;
				case "reupdate_interval" -> ConfigManager.config.reupdate_interval;
				case "tick_adjacent_blocks" -> ConfigManager.config.tick_adjacent_blocks;
				default -> "impossible"; // Impossible to reach since I technically cover all possible values
			})
		);
	}
	@Override
	public void onInitializeClient() {
		ConfigManager.load();   // load ConfigManager.config on startup
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommandManager.literal("lightingupdater")
				.then(ClientCommandManager.literal("reload")
					.executes(ctx -> {
						ConfigManager.load();
						return sendMsg(ctx, Component.translatable("lightingupdater.config.reload"));
					})
				)
				.then(ClientCommandManager.literal("save")
					.executes(ctx -> {
						ConfigManager.save();
						return sendMsg(ctx, Component.translatable("lightingupdater.config.save"));

					})
				)
				.then(ClientCommandManager.literal("set")
					.then(ClientCommandManager.literal("radius")
						.then(ClientCommandManager.argument("value", IntegerArgumentType.integer())
							.executes(ctx -> {
								int value = IntegerArgumentType.getInteger(ctx, "value");
								ConfigManager.config.radius = value;
								ConfigManager.save();
								return sendMsg(ctx, Component.translatable("lightingupdater.config.key.update.radius", value));
							})
						)
					)
					.then(ClientCommandManager.literal("update_interval")
						.then(ClientCommandManager.argument("value", IntegerArgumentType.integer())
							.executes(ctx -> {
								int value = IntegerArgumentType.getInteger(ctx, "value");
								ConfigManager.config.update_interval = value;
								ConfigManager.save();
								return sendMsg(ctx, Component.translatable("lightingupdater.config.key.update.update_interval", value));
							})
						)
					)
					.then(ClientCommandManager.literal("reupdate_interval")
						.then(ClientCommandManager.argument("value", IntegerArgumentType.integer())
							.executes(ctx -> {
								int value = IntegerArgumentType.getInteger(ctx, "value");
								ConfigManager.config.reupdate_interval = value;
								ConfigManager.save();
								return sendMsg(ctx, Component.translatable("lightingupdater.config.key.update.reupdate_interval", value));
							})
						)
					)
					.then(ClientCommandManager.literal("tick_adjacent_blocks")
						.then(ClientCommandManager.argument("value", BoolArgumentType.bool())
							.executes(ctx -> {
								boolean value = BoolArgumentType.getBool(ctx, "value");
								ConfigManager.config.tick_adjacent_blocks = value;
								ConfigManager.save();
								return sendMsg(ctx, Component.translatable("lightingupdater.config.key.update.tick_adjacent_blocks", value));
							})
						)
					)
				)
				.then(ClientCommandManager.literal("get")
					.then(ClientCommandManager.literal("radius").executes(this::getValue))
					.then(ClientCommandManager.literal("update_interval").executes(this::getValue))
					.then(ClientCommandManager.literal("reupdate_interval").executes(this::getValue))
					.then(ClientCommandManager.literal("tick_adjacent_blocks").executes(this::getValue))
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

		// also check adjacent blocks if ConfigManager.configured that way to update them
		if (ConfigManager.config.tick_adjacent_blocks)
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
		if (tick % ConfigManager.config.update_interval == 0) return;
		if (tick % ConfigManager.config.reupdate_interval == 0) alreadyTickedBlocks.clear();

		if (client.player == null || client.level == null) return;
		int r = ConfigManager.config.radius;

		for (int x = -r; x <= r; x++) {
			for (int y = -r; y <= r; y++) {
				for (int z = -r; z <= r; z++) {
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