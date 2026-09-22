package com.example.pickuptweaks.state;

import net.minecraft.world.entity.player.Player;

/**
 * プレイヤーごとの {@link PickupMode} へのアクセスをまとめる。
 *
 * <p>実体は {@link ModAttachments#PICKUP_MODE} という Data Attachment で、
 * プレイヤーのセーブデータに永続化される。そのため、ワールドを出て入り直しても、
 * サーバーを再起動しても、設定したモードは保持される。</p>
 *
 * <p>以前はサーバープロセス内のメモリ（{@code Map<UUID, PickupMode>}）だけで管理しており、
 * 切断のたびに状態を破棄していたが、それでは「ワールドに入り直すと元に戻る」という
 * 問題があったため、この永続化方式に置き換えた。</p>
 */
public final class PickupState {

	private PickupState() {
	}

	/** 未設定のプレイヤーは既定値 {@link PickupMode#PICKUP}（バニラ挙動）。 */
	public static PickupMode getMode(Player player) {
		return player.getAttachedOrCreate(ModAttachments.PICKUP_MODE);
	}

	public static void setMode(Player player, PickupMode mode) {
		player.setAttached(ModAttachments.PICKUP_MODE, mode);
	}
}
