package com.example.pickuptweaks.config;

import com.example.pickuptweaks.PickupTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/pickuptweaks.json に保存される設定。
 *
 * <p>すべての値はサーバー側が権威を持つ。クライアントはキー入力を送るだけで、
 * 範囲や破棄の判定には一切関与しない。</p>
 */
public final class PickupConfig {

	/** 範囲判定を行わない（バニラの当たり判定に完全に任せる）ことを示す値。 */
	public static final double RANGE_VANILLA = -1.0D;

	/** 設定可能な最大範囲。これ以上はエンティティ走査のコストが跳ね上がるため制限する。 */
	public static final double MAX_RANGE = 16.0D;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("pickuptweaks.json");

	private static PickupConfig instance = new PickupConfig();

	// ------------------------------------------------------------------
	// 設定項目
	// ------------------------------------------------------------------

	/**
	 * 自動ピックアップの範囲（ブロック数）。プレイヤーの足元位置からの距離で判定する。
	 *
	 * <p>バニラの実効範囲はおよそ 1.4 ブロック。それより小さくすると拾える範囲が狭まり、
	 * 大きくすると毎tickの走査によって範囲が広がる。
	 * {@code -1} を指定すると範囲判定そのものを行わず、バニラの挙動になる。</p>
	 */
	public double pickupRange = 2.0D;

	/**
	 * 拡張範囲の走査を行う間隔（tick）。
	 * 1 なら毎tick。大きな範囲を設定していて負荷が気になる場合は 2〜4 程度に上げる。
	 */
	public int scanIntervalTicks = 1;

	// ------------------------------------------------------------------

	public static PickupConfig get() {
		return instance;
	}

	/** 設定ファイルを読み込む。存在しなければデフォルト値で新規作成する。 */
	public static void load() {
		PickupConfig loaded = new PickupConfig();

		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				PickupConfig parsed = GSON.fromJson(reader, PickupConfig.class);

				if (parsed != null) {
					loaded = parsed;
				}
			} catch (IOException | RuntimeException e) {
				PickupTweaks.LOGGER.error("設定の読み込みに失敗しました。デフォルト値を使用します。", e);
			}
		}

		loaded.clamp();
		instance = loaded;
		save();
	}

	/** 現在の設定をファイルへ書き出す。 */
	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());

			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			PickupTweaks.LOGGER.error("設定の保存に失敗しました。", e);
		}
	}

	/** 不正な値を安全な範囲へ丸める。 */
	public void clamp() {
		if (this.pickupRange < 0.0D) {
			this.pickupRange = RANGE_VANILLA;
		} else if (this.pickupRange > MAX_RANGE) {
			this.pickupRange = MAX_RANGE;
		}

		if (this.scanIntervalTicks < 1) {
			this.scanIntervalTicks = 1;
		} else if (this.scanIntervalTicks > 20) {
			this.scanIntervalTicks = 20;
		}
	}

	private PickupConfig() {
	}
}
