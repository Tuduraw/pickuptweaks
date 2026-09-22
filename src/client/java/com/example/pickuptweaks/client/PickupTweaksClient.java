package com.example.pickuptweaks.client;

import com.example.pickuptweaks.PickupTweaks;
import com.example.pickuptweaks.net.SetPickupModePayload;
import com.example.pickuptweaks.net.SyncPickupModePayload;
import com.example.pickuptweaks.state.PickupMode;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * キー入力の受け取りとサーバーへの通知だけを担当する。
 *
 * <p>拾得処理そのものはサーバー側にしか存在しないため、このmodはクライアント単独では機能しない
 * （{@code fabric.mod.json} の environment は {@code "*"}）。</p>
 *
 * <p>切り替えキーは2種類の遷移を持つ。</p>
 * <ul>
 *   <li>通常押下 — {@link PickupMode#PICKUP} と {@link PickupMode#DISABLED} を行き来する。</li>
 *   <li>スニーク中の押下 — {@link PickupMode#DISCARD} に入る／そこから抜ける。
 *       誤操作でアイテムを失う事故を防ぐため、破棄モードへはこの経路でしか入れない。</li>
 * </ul>
 *
 * <p>モードの永続化はサーバー側（プレイヤーのセーブデータ）で行われる。クライアントは
 * 参加時に {@link SyncPickupModePayload} でその値を受け取り、次の押下時に計算する起点として
 * 一時的に保持するだけ — ゲームを終了したり別のワールドに繋いだりすれば、
 * 改めてそのワールドの値で上書きされる。</p>
 */
public class PickupTweaksClient implements ClientModInitializer {

	// 1.21.9 以降、キーバインドのカテゴリは文字列ではなく KeyMapping.Category になった。
	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(PickupTweaks.MOD_ID, "main"));

	private static KeyMapping toggleKey;

	/**
	 * 現在接続しているワールド／サーバーでの、サーバーから通知された最新のモード。
	 * 参加のたびに {@link SyncPickupModePayload} で上書きされるため、
	 * 別のワールドの値を持ち越すことはない。
	 */
	private static PickupMode currentMode = PickupMode.PICKUP;

	@Override
	public void onInitializeClient() {
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.pickuptweaks.toggle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				CATEGORY
		));

		// サーバーから届く、永続化された現在のモード。参加直後に必ず1回送られてくる。
		ClientPlayNetworking.registerGlobalReceiver(SyncPickupModePayload.TYPE, (payload, context) ->
				context.client().execute(() -> currentMode = payload.mode()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}

			while (toggleKey.consumeClick()) {
				if (!ClientPlayNetworking.canSend(SetPickupModePayload.TYPE)) {
					// サーバー側にこのmodが入っていない
					client.player.displayClientMessage(
							Component.translatable("message.pickuptweaks.server_missing"), true);
					continue;
				}

				boolean sneaking = client.player.isShiftKeyDown();
				currentMode = nextMode(currentMode, sneaking);
				ClientPlayNetworking.send(new SetPickupModePayload(currentMode));
				// 実際に適用されたモードの表示はサーバーからの返信メッセージに任せる
			}
		});
	}

	/**
	 * 現在のモードと、押下時にスニークしていたかどうかから次のモードを決める。
	 *
	 * <p>通常押下は PICKUP ⇔ DISABLED のみを切り替える（DISCARD 中に通常押下した場合は、
	 * 安全側の DISABLED へ倒す）。スニーク押下は DISCARD への出入りのみを行う。</p>
	 */
	private static PickupMode nextMode(PickupMode current, boolean sneaking) {
		if (sneaking) {
			return current == PickupMode.DISCARD ? PickupMode.PICKUP : PickupMode.DISCARD;
		}

		return current == PickupMode.DISABLED ? PickupMode.PICKUP : PickupMode.DISABLED;
	}
}
