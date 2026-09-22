package com.example.pickuptweaks;

import com.example.pickuptweaks.config.PickupConfig;
import com.example.pickuptweaks.net.SetPickupModePayload;
import com.example.pickuptweaks.net.SyncPickupModePayload;
import com.example.pickuptweaks.state.ModAttachments;
import com.example.pickuptweaks.state.PickupState;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickupTweaks implements ModInitializer {

	public static final String MOD_ID = "pickuptweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PickupConfig.load();

		// Attachment の登録をクラス初期化時に確定させる
		ModAttachments.init();

		// クライアント → サーバー、サーバー → クライアントのモード通知を登録する。
		// このクラスは共通エントリポイントなので、専用サーバーでも実行される。
		PayloadTypeRegistry.playC2S().register(SetPickupModePayload.TYPE, SetPickupModePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SyncPickupModePayload.TYPE, SyncPickupModePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SetPickupModePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();

			// ネットワークスレッドで届くため、サーバースレッドへ渡し直す
			context.server().execute(() -> {
				// プレイヤーのセーブデータに永続化される（ワールドの入り直し・サーバー再起動をまたいで保持）
				PickupState.setMode(player, payload.mode());

				player.displayClientMessage(Component.translatable(switch (payload.mode()) {
					case PICKUP -> "message.pickuptweaks.mode.pickup";
					case DISABLED -> "message.pickuptweaks.mode.disabled";
					case DISCARD -> "message.pickuptweaks.mode.discard";
				}), true);
			});
		});

		// プレイヤーの参加直後に、永続化されているモードをクライアントへ知らせる。
		// これがないと、クライアントは「前回どこかのワールドで設定したモード」を
		// 覚えたまま別のワールドに繋いでしまい、そちらへ誤って上書き送信してしまう。
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				sender.sendPacket(new SyncPickupModePayload(PickupState.getMode(handler.player))));

		ServerTickEvents.END_SERVER_TICK.register(PickupHandler::onServerTick);

		registerCommands();

		LOGGER.info("Pickup Tweaks を初期化しました。");
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal(MOD_ID)
						// 1.21.11 で権限レベルの整数指定は PermissionCheck に置き換わった。
						// LEVEL_GAMEMASTERS が従来のレベル 2 に相当する。
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("reload")
								.executes(context -> {
									PickupConfig.load();
									context.getSource().sendSuccess(
											() -> Component.translatable("command.pickuptweaks.reloaded"), true);
									return 1;
								}))
						.then(Commands.literal("range")
								.executes(context -> {
									double range = PickupConfig.get().pickupRange;
									context.getSource().sendSuccess(
											() -> Component.translatable("command.pickuptweaks.range.query", range), false);
									return 1;
								})
								.then(Commands.argument("blocks",
												DoubleArgumentType.doubleArg(PickupConfig.RANGE_VANILLA, PickupConfig.MAX_RANGE))
										.executes(context -> {
											double range = DoubleArgumentType.getDouble(context, "blocks");
											PickupConfig.get().pickupRange = range;
											PickupConfig.get().clamp();
											PickupConfig.save();

											double applied = PickupConfig.get().pickupRange;
											context.getSource().sendSuccess(
													() -> Component.translatable("command.pickuptweaks.range.set", applied), true);
											return 1;
										})))));
	}
}
