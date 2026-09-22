# Pickup Tweaks

Minecraft 1.21.11 / Fabric 向け、アイテムの自動ピックアップ挙動を変更する mod。
マッピングは **Mojang 公式マッピング（Mojmap）** を使用しています。

## 機能

自動ピックアップは3つのモードを持ち、1つのキーで切り替えます（既定は `V`）。

| モード | 動作 | 切り替え方法 |
| --- | --- | --- |
| 通常（Pickup） | バニラ通りに拾う | 既定値 |
| 無効（Disabled） | 一切拾わない | 通常押下で「通常」と往復 |
| 自動破棄（Discard） | 範囲内のアイテムに触れた時点でインベントリに加えず破棄する | **スニーク中に押下**した場合のみ |

自動破棄モードはクリエイティブ・サバイバルを問わず動作します。誤操作でアイテムを
失う事故を防ぐため、このモードへは通常の押下では入れず、必ずスニーク中に切り替えキーを
押した場合にのみ有効化されます。もう一度スニーク中に押すと通常モードへ戻ります。

**モードはプレイヤーのセーブデータに永続化されます。** ワールドを出て入り直しても、
サーバーを再起動しても、設定したモードは保持されます（詳細は後述）。

## ビルド

Gradle Wrapper（8.14.3）を同梱しています。Gradle 本体のインストールは不要です。

```bash
./gradlew build          # Windows は gradlew.bat build
```

生成物は `build/libs/pickuptweaks-1.0.0.jar` です。

**JDK 21 が必要です。** Loom は Java 21 を要求し、Gradle 8.14 系は Java 25 に未対応のため、
JDK 25 が既定になっていると `Unsupported class file major version` などで失敗します。
その場合は JDK 21 を明示してください。

```bash
./gradlew build -Dorg.gradle.java.home=/path/to/jdk-21
```

Unix 系で `Permission denied` になる場合は `chmod +x gradlew` を実行してください。

### Loom のバージョンについて

Loom は依存 mod の jar マニフェストに記録された `Fabric-Loom-Version` を読み、
自分より新しい Loom でビルドされた mod を拒否する。

```
Mod was built with a newer version of Loom (X.Y.Z), you are using Loom (A.B.C)
```

このエラーが出たら、`build.gradle` の Loom のバージョンを上げてください。
Loom 1.14 以降は Gradle 9.2 以上、1.16 以降は Gradle 9.4 以上を要求するため、
そちらへ上げる場合は `gradle/wrapper/gradle-wrapper.properties` の更新も必要です。

## コンフィグ

`config/pickuptweaks.json` に生成されます。

```json
{
  "pickupRange": 2.0,
  "scanIntervalTicks": 1
}
```

- **`pickupRange`** — プレイヤーの足元位置からの距離（ブロック）。範囲内であれば
  「通常」「自動破棄」いずれのモードでも対象になります（「無効」時は判定されません）。
  バニラの実効範囲はおよそ **1.4** なので、それより小さくすると拾える範囲が狭まります。
  `-1` を指定すると範囲判定そのものを行わず、完全にバニラの当たり判定に従います。
  上限は 16。
- **`scanIntervalTicks`** — 範囲を広げたときの走査間隔。負荷が気になる場合に 2〜4 へ。

モードの切り替えはプレイヤーごとの状態であり、`pickuptweaks.json` には含まれません
（プレイヤーのセーブデータに保存されます。詳細は「モードの永続化」を参照）。

### コマンド

権限レベル 2 相当（`LEVEL_GAMEMASTERS`）以上で使えます。

```
/pickuptweaks reload
/pickuptweaks range [<blocks>]
```

## モードの永続化

モードは Fabric API の **Data Attachment API** を使って、プレイヤーのセーブデータ
（NBT）に保存されます。ワールドごとのプレイヤーデータの一部として保存されるため、

- 同じワールドを出て入り直しても保持されます。
- サーバーを再起動しても保持されます。
- **別のワールド／サーバーに接続した場合は、そちらのセーブデータに保存されている
  （初回なら既定値の「通常」）モードになります** — あるワールドで設定したモードが
  無関係な別のワールドに引き継がれることはありません。

プレイヤーが参加すると、サーバーは永続化されているモードを `SyncPickupModePayload`
（S2C）でクライアントへ通知します。クライアントはこれを一時的な起点として保持し、
次にキーを押したときの遷移計算にのみ使います。クライアント側では何も保存しません —
権威は常にサーバー側のセーブデータにあります。

## 構成

```
src/main/java/com/example/pickuptweaks/
├── PickupTweaks.java          共通エントリポイント（通信・tick・コマンド登録）
├── PickupHandler.java         拾得可否・破棄の判定と拡張範囲の走査
├── config/PickupConfig.java   JSON コンフィグ（範囲・走査間隔のみ）
├── net/
│   ├── SetPickupModePayload.java   C2S モード通知
│   └── SyncPickupModePayload.java  S2C モード同期（参加時に送信）
└── state/
    ├── PickupMode.java        PICKUP / DISABLED / DISCARD の3値。永続化用Codec持ち
    ├── ModAttachments.java    Data Attachment の登録
    └── PickupState.java       プレイヤーの Attachment へのアクセスをまとめる

src/main/java/com/example/pickuptweaks/mixin/
├── ItemEntityMixin.java     playerTouch を HEAD でフック
└── ItemEntityAccessor.java  pickupDelay フィールドの参照

src/client/java/com/example/pickuptweaks/client/
└── PickupTweaksClient.java    キーバインドとモード遷移ロジック
```

### 設計上のポイント

拾得処理はサーバー側の `ItemEntity#playerTouch` に集約されているため、
モード判定と破棄処理をこの1メソッドのフックで完結させています。
範囲を広げるための毎 tick 走査も、見つけたアイテムに対して同じ `playerTouch` を呼ぶだけなので、
モード判定は自動的に適用され、拾得遅延・所有者制限・インベントリ合流・
取得アニメーションといったバニラの挙動もそのまま維持されます。

モードの決定はクライアント側で行い（`nextMode` メソッド）、結果をサーバーへ送信します。
サーバーはこの申告をそのまま採用します — この設定はプレイヤー自身の利便性のためのものであり、
公平性に関わる判定ではないため、クライアントを信頼して問題ありません。実際に保存され、
判定にも使われる値は常にサーバー側の Attachment です。

**クライアントとサーバーの両方に導入が必要です。** 拾得判定はサーバー側にしか存在しないため、
クライアント単独では動作しません。

## 1.21.11 で対応した変更

- `ResourceLocation` → `Identifier`（`net.minecraft.resources.Identifier`）。
  ファクトリメソッド名は変わらず `fromNamespaceAndPath` のままです
  （namespace の省略形は `withDefaultNamespace` など、こちらは従来通り）。
- `KeyMapping.Category` のファクトリメソッドは `create` ではなく **`register`** です
  （`KeyMapping.Category.register(Identifier.fromNamespaceAndPath(...))`）。
  `KeyBindingHelper.registerKeyBinding` 自体は 1.21.11 時点でまだ改名されていません
  （将来の 26.1 系で `KeyMappingHelper.registerKeyMapping` にリネームされる予定です）。
- 権限レベルの整数指定が廃止され、`PermissionCheck` ベースの新APIに置き換わりました。
  `source.hasPermission(2)` は `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` に相当します。

## 動作確認していない点

`compileJava` / `compileClientJava` ともにコンパイルは通る想定ですが、
実際に起動して検証したものではありません。特に以下は Mixin または実行時APIが
実際に解決されるかどうかに関わるため、コンパイルが通っても起動時に落ちる可能性があります。

1. **`@Accessor("pickupDelay")`** — フィールド名が変わっている場合は
   バニラの `ItemEntity` を `genSources` で展開して確認してください。
   `hasPickUpDelay()` が public なら Accessor を使わずそちらでも代用できます。
2. **`ItemEntity#playerTouch`** — メソッド名自体が変わっている可能性があります。
   Mixin の `@Inject(method = "playerTouch", ...)` が起動時に解決できない場合、
   `genSources` で実際のメソッド名を確認してください。
3. **Data Attachment API**（`net.fabricmc.fabric.api.attachment.v1`）— fabric-api
   0.141.6+1.21.11 に同梱されている想定ですが、実際にプレイヤーのNBTへ書き込まれ、
   ワールド再読み込み後も読み出せるかは未検証です。

## ライセンス

MIT
