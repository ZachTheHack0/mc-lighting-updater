package net.mcreator.lightingupdater;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.LinkedHashSet;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.mcreator.lightingupdater.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import static net.mcreator.lightingupdater.config.ConfigManager.config;
import static net.minecraft.network.chat.Component.translatable;

@Environment(EnvType.CLIENT)
public class LightingUpdaterModClient implements ClientModInitializer {
	private int tick = 0;
	private final LinkedHashSet<BlockPos> alreadyTickedBlocks = new LinkedHashSet<>();

	private int printChat(CommandContext<? extends FabricClientCommandSource> ctx, Component msg) {
		ctx.getSource().sendFeedback(msg);
		return 1;
	}

	@Override
	public void onInitializeClient() {
		ConfigManager.load();   // load config on startup
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommandManager.literal("lightingupdater")
				.then(ClientCommandManager.literal("reload")
					.executes(ctx -> {
						ConfigManager.load();
						return printChat(ctx, translatable("lightingupdater.config.reload"));
					})
				)
				.then(ClientCommandManager.literal("save")
					.executes(ctx -> {
						ConfigManager.save();
						return printChat(ctx, translatable("lightingupdater.config.save"));
					})
				)
				.then(ClientCommandManager.literal("set")
					.then(ClientCommandManager.literal("radius")
						.then(ClientCommandManager.argument("value", IntegerArgumentType.integer())
							.executes(ctx -> {
								int value = IntegerArgumentType.getInteger(ctx, "value");
								config.radius = value;
								ConfigManager.save();
								return printChat(ctx, translatable("lightingupdater.config.key.update.radius", value));
							})
						)
					)
					.then(ClientCommandManager.literal("update_interval")
						.then(ClientCommandManager.argument("value", IntegerArgumentType.integer())
							.executes(ctx -> {
								int value = IntegerArgumentType.getInteger(ctx, "value");
								config.update_interval = value;
								ConfigManager.save();
								return printChat(ctx, translatable("lightingupdater.config.key.update.update_interval", value));
							})
						)
					)
					.then(ClientCommandManager.literal("reupdate_interval")
						.then(ClientCommandManager.argument("value", IntegerArgumentType.integer())
							.executes(ctx -> {
								int value = IntegerArgumentType.getInteger(ctx, "value");
								config.reupdate_interval = value;
								ConfigManager.save();
								return printChat(ctx, translatable("lightingupdater.config.key.update.reupdate_interval", value));
							})
						)
					)
				)
				.then(ClientCommandManager.literal("get")
					.then(ClientCommandManager.literal("radius")
						.executes(ctx -> printChat(ctx, translatable("lightingupdater.config.key.get.radius", config.radius)))
					)
					.then(ClientCommandManager.literal("update_interval")
						.executes(ctx -> printChat(ctx, translatable("lightingupdater.config.key.get.update_interval", config.update_interval)))
					)
					.then(ClientCommandManager.literal("reupdate_interval")
						.executes(ctx -> printChat(ctx, translatable("lightingupdater.config.key.get.reupdate_interval", config.reupdate_interval)))
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

		// also check adjacent blocks to refresh propagation
		for (Direction dir : Direction.values()) {
			BlockPos relPos = pos.relative(dir);
			if (alreadyTickedBlocks.contains(relPos)) continue;
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