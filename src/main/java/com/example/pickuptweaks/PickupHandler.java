package com.example.pickuptweaks;

import com.example.pickuptweaks.config.PickupConfig;
import com.example.pickuptweaks.mixin.ItemEntityAccessor;
import com.example.pickuptweaks.state.PickupMode;
import com.example.pickuptweaks.state.PickupState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 拾得可否の判定と、拡張範囲の走査。
 *
 * <p>判定は必ずこのクラスを通る。{@link com.example.pickuptweaks.mixin.ItemEntityMixin} が
 * バニラの衝突経路を、{@link #onServerTick(MinecraftServer)} が拡張範囲の経路を担当するが、
 * どちらも最終的に {@code ItemEntity#playerTouch} を呼ぶため、モード判定は一箇所で完結する。</p>
 */
public final class PickupHandler {

	/**
	 * バニラの実効ピックアップ範囲のおおよその値。
	 * バニラは {@code getBoundingBox().inflate(1.0, 0.5, 1.0)} で判定するため、
	 * プレイヤー中心からは水平方向におよそ 1.3〜1.4 ブロック。
	 */
	private static final double VANILLA_RANGE = 1.5D;

	private PickupHandler() {
	}

	/**
	 * このアイテムをこのプレイヤーが拾得処理の対象としてよいか。
	 *
	 * <p>{@link PickupMode#DISCARD} でも範囲判定は同様に行う —
	 * 「範囲内で触れたら破棄する」という動作にするため。</p>
	 *
	 * @return false ならピックアップ処理全体をキャンセルする
	 */
	public static boolean canPickUp(ItemEntity item, Player player) {
		PickupMode mode = PickupState.getMode(player);

		if (mode == PickupMode.DISABLED) {
			return false;
		}

		PickupConfig config = PickupConfig.get();
		double range = config.pickupRange;

		if (range != PickupConfig.RANGE_VANILLA && item.distanceToSqr(player) > range * range) {
			return false;
		}

		return true;
	}

	/**
	 * {@link PickupMode#DISCARD} での破棄処理。クリエイティブ・サバイバルを問わず動作する。
	 *
	 * @return true ならバニラのピックアップ処理をキャンセルする
	 */
	public static boolean shouldDiscardInsteadOfPickUp(ItemEntity item, Player player) {
		if (PickupState.getMode(player) != PickupMode.DISCARD) {
			return false;
		}

		// 拾得遅延中（プレイヤーが今しがた投げ捨てた直後など）は何もしない。
		// これを省くと、Q キーで捨てたアイテムが着地する前に消滅してしまう。
		if (((ItemEntityAccessor) item).getPickupDelay() > 0) {
			return true;
		}

		item.discard();
		return true;
	}

	/**
	 * 拡張範囲の走査。設定範囲がバニラより広い場合のみ動作する。
	 *
	 * <p>見つけたアイテムに対して {@code playerTouch} を呼ぶだけなので、
	 * 拾得遅延・所有者制限・インベントリ合流・取得アニメーションといった
	 * バニラの挙動はすべてそのまま維持される。</p>
	 */
	public static void onServerTick(MinecraftServer server) {
		PickupConfig config = PickupConfig.get();
		double range = config.pickupRange;

		if (range == PickupConfig.RANGE_VANILLA || range <= VANILLA_RANGE) {
			return;
		}

		if (config.scanIntervalTicks > 1 && server.getTickCount() % config.scanIntervalTicks != 0) {
			return;
		}

		double rangeSq = range * range;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive() || player.isSpectator()) {
				continue;
			}

			if (PickupState.getMode(player) == PickupMode.DISABLED) {
				continue;
			}

			AABB box = player.getBoundingBox().inflate(range);

			List<ItemEntity> items = player.level().getEntitiesOfClass(
					ItemEntity.class,
					box,
					entity -> entity.isAlive() && entity.distanceToSqr(player) <= rangeSq
			);

			for (ItemEntity item : items) {
				// ItemEntityMixin を再度通るため、モード判定と破棄が改めて適用される。
				item.playerTouch(player);
			}
		}
	}
}
