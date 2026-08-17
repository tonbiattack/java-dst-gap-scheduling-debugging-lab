# Java DST gap scheduling debugging lab

夏時間の開始で存在しないローカル時刻を `LocalDateTime.atZone` へ渡したとき、時刻が自動で先送りされて予定として保存される不具合を再現します。

## 前提

- Java 21 以上
- 外部ライブラリは不要

## 実行

```bash
./run-tests.sh
```

バグ状態では、`Europe/Berlin` の `2024-03-31T02:30` が `2024-03-31T03:30+02:00[Europe/Berlin]` に変換され、予定が1件保存されます。修正後は入力を拒否し、予定を保存しません。
