package com.example.pickuptweaks.mixin;

import com.example.pickuptweaks.PickupHandler;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * アイテム拾得の唯一の入口を押さえる。
 *
 * <p>{@code playerTouch} はプレイヤーとの衝突時にサーバー側で呼ばれ、
 * 拾得遅延・所有者チェック・インベントリ追加をすべて内包している。
 * ここで HEAD をフックすることで、3 つの要件を 1 箇所で処理できる。</p>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void pickuptweaks$filterPickup(Player player, CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;

		// バニラ同様、拾得判定はサーバー側でのみ行う
		if (self.level().isClientSide()) {
			return;
		}

		if (!PickupHandler.canPickUp(self, player)) {
			ci.cancel();
			return;
		}

		if (PickupHandler.shouldDiscardInsteadOfPickUp(self, player)) {
			ci.cancel();
		}
	}
}
