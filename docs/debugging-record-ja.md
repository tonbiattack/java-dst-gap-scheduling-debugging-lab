# デバッグ記録: 夏時間の欠落時刻が予定として先送り保存される

## 対象の不具合

`Europe/Berlin` で夏時間が始まる `2024-03-31T02:30` は存在しないローカル時刻である。しかし scheduler が `LocalDateTime.atZone` の結果をそのまま保存したため、入力を拒否せず `03:30+02:00` の予定を1件作成した。利用者が入力したローカル時刻を一意に解決できない場合は、予定を作成せず明示的に拒否することが契約である。

| 観測点 | 期待値 | バグ状態の実際値 |
| --- | --- | --- |
| 境界応答または例外 | `IllegalArgumentException` | `2024-03-31T03:30+02:00[Europe/Berlin]` が返る |
| 最終状態 | 予定数 `0` | 予定数 `1` |
| 保持対象 | 通常日の `02:30` を変更せず保存 | 通常日は正常 |

## 再現条件

バグ状態のコミットは `566ef966b747bfc92d51a2091c793eda51f71586` です。

```bash
git checkout 566ef96
./run-tests.sh
```

```text
Exception in thread "main" java.lang.AssertionError: 夏時間の欠落時刻は拒否する expected=IllegalArgumentException actual=2024-03-31T03:30+02:00[Europe/Berlin] storedAppointments=1
    at lab.AppointmentSchedulerTest.rejectsNonexistentLocalTimeInsteadOfShiftingIt(AppointmentSchedulerTest.java:22)
    at lab.AppointmentSchedulerTest.main(AppointmentSchedulerTest.java:9)
```

## 調査

| 確認対象 | 観測結果 | 判断 |
| --- | --- | --- |
| 入力 | `2024-03-31T02:30` と `Europe/Berlin` | 夏時間開始日の gap を意図的に指定している。 |
| 境界出力 | `atZone` の結果は `2024-03-31T03:30+02:00[Europe/Berlin]` | 入力時刻を保持せず、1時間先へ調整している。 |
| 最終状態 | `appointments.add(resolved)` が実行され、件数は `1` | 表示値だけでなく、誤った予定が保存される。 |
| 実装 | `requestedLocalTime.atZone(zone)` の前に valid offset 数を検証していない | 直接原因を採用。 |
| 仕様 | `ZoneRules.getValidOffsets` は gap で size 0、normal で size 1、overlap で size 2 を返す。`ZonedDateTime.of` は gap 内のローカル時刻を gap 長だけ先へ調整する。[1] [2] | 観測した先送りは仕様どおり。 |

## 原因

`LocalDateTime` はタイムゾーンを持たないため、`ZoneId` と組み合わせる段階でそのローカル時刻が実在するかを判定する必要がある。対象日時は summer time への切替 gap に入り、valid offset は0件である。にもかかわらず `atZone` に委譲したため、Java の既定解決方針に従って `03:30` に先送りされ、その値が保存された。

## 修正

`zone.getRules().getValidOffsets(requestedLocalTime).size() != 1` を先に検証し、gap と overlap のどちらも `IllegalArgumentException` で拒否するようにした。修正コミットは `2c754d5a61e643249be3401c876a40d0973ddcb0` である。これは「一意に決められない予約時刻は受け付けない」という業務契約を scheduler の境界で明確にする最小変更である。

## 回帰確認

```bash
git checkout main
./run-tests.sh
```

```text
PASS: all tests
```

gap の入力は例外となり保存件数0を確認し、通常日の `2024-03-30T02:30` は入力時刻を変えず保存件数1となることを同じテストスイートで確認した。

## 設計上の制約

この実装は一意に解決できない時刻を全て拒否する。秋の overlap で「早い offset」または「遅い offset」を選ばせる要件、タイムゾーンDBの更新、利用者への候補時刻提示は扱わない。要件がそれらを必要とする場合は、`ZoneRules.getTransition` や offset 選択を含む別の明示的なUI/API契約が必要である。
