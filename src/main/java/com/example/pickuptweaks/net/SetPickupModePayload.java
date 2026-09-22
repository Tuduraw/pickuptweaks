package com.example.pickuptweaks.net;

import com.example.pickuptweaks.PickupTweaks;
import com.example.pickuptweaks.state.PickupMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー。プレイヤーが設定したい {@link PickupMode} を通知する。
 *
 * <p>どのモードへ遷移するかはクライアント側のキー入力ロジックが決定する
 * （{@code PickupTweaksClient} を参照）。サーバーはその値をそのまま採用する —
 * この設定はプレイヤー自身の利便性のためのものであり、公平性に関わる判定ではないため、
 * クライアントの申告を信頼して問題ない。</p>
 */
public record SetPickupModePayload(PickupMode mode) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SetPickupModePayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PickupTweaks.MOD_ID, "set_mode"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SetPickupModePayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.BYTE.<RegistryFriendlyByteBuf>cast().map(
							b -> PickupMode.byOrdinal(b),
							mode -> (byte) mode.ordinal()
					),
					SetPickupModePayload::mode,
					SetPickupModePayload::new
			);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
