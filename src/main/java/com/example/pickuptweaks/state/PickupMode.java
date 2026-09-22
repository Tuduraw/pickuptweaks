package com.example.pickuptweaks.state;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * プレイヤーごとの自動ピックアップの動作モード。
 *
 * <p>{@link #PICKUP} が既定値（バニラ挙動）。切り替えキーの押下によって
 * {@link #DISABLED} と行き来し、スニーク中に押した場合のみ {@link #DISCARD} に入る。</p>
 */
public enum PickupMode implements StringRepresentable {

	/** バニラ通りに拾う。 */
	PICKUP("pickup"),

	/** 一切拾わない。 */
	DISABLED("disabled"),

	/**
	 * 範囲内のアイテムに触れた時点でインベントリに加えず破棄する。
	 *
	 * <p>クリエイティブ・サバイバルを問わず動作する。誤操作による事故を防ぐため、
	 * このモードへはスニーク中に切り替えキーを押した場合にのみ入ることができる。</p>
	 */
	DISCARD("discard");

	/**
	 * プレイヤーのセーブデータ（NBT）へ永続化する際に使うコーデック。
	 * 名前ベースにしているのは、将来 enum の並び順を変えても
	 * 既存のセーブデータが壊れないようにするため。
	 */
	public static final Codec<PickupMode> CODEC = StringRepresentable.fromEnum(PickupMode::values);

	private static final PickupMode[] VALUES = values();

	private final String key;

	PickupMode(String key) {
		this.key = key;
	}

	@Override
	public String getSerializedName() {
		return key;
	}

	/** ネットワーク送受信用。1バイトのordinalとの相互変換に使う。 */
	public static PickupMode byOrdinal(int ordinal) {
		if (ordinal < 0 || ordinal >= VALUES.length) {
			return PICKUP;
		}

		return VALUES[ordinal];
	}
}
