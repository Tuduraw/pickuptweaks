package com.example.pickuptweaks.state;

import com.example.pickuptweaks.PickupTweaks;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * modが使う Data Attachment の登録をまとめるクラス。
 */
public final class ModAttachments {

	/**
	 * プレイヤーの現在の {@link PickupMode}。
	 *
	 * <p>{@code persistent(...)} を指定しているため、プレイヤーのセーブデータ（NBT）に
	 * 書き込まれ、ワールドの再読み込みやサーバーの再起動をまたいで保持される。
	 * 値を持たない場合の初期値は {@link PickupMode#PICKUP}。</p>
	 */
	public static final AttachmentType<PickupMode> PICKUP_MODE = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(PickupTweaks.MOD_ID, "pickup_mode"),
			builder -> builder
					.initializer(() -> PickupMode.PICKUP)
					.persistent(PickupMode.CODEC)
	);

	private ModAttachments() {
	}

	/** クラス初期化を強制するための空メソッド。共通エントリポイントから呼ぶ。 */
	public static void init() {
	}
}
