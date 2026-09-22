package com.example.pickuptweaks.net;

import com.example.pickuptweaks.PickupTweaks;
import com.example.pickuptweaks.state.PickupMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント。プレイヤーの永続化された {@link PickupMode} を通知する。
 *
 * <p>プレイヤーが参加した直後に送られる。モードはプレイヤーのセーブデータに
 * 永続化されているため、これによりクライアントは「今どのワールド／サーバーに
 * 繋がっていて、そこでのモードが何か」を正しく把握できる。
 * これがないと、クライアントが前回接続したワールドのモードを次に繋いだ
 * 別のワールドへ誤って送ってしまう恐れがある。</p>
 */
public record SyncPickupModePayload(PickupMode mode) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncPickupModePayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PickupTweaks.MOD_ID, "sync_mode"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncPickupModePayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.BYTE.<RegistryFriendlyByteBuf>cast().map(
							b -> PickupMode.byOrdinal(b),
							mode -> (byte) mode.ordinal()
					),
					SyncPickupModePayload::mode,
					SyncPickupModePayload::new
			);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
