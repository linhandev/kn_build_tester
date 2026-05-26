# ALI-38: Incremental Build Artifact Size Growth — OH Version Variant Test

## Purpose

Re-run the Round 1 reproduction on Kotlin **2.2.21-OH.0.1.0-06** to compare against the baseline (2.2.21-0.2.0-12).

## Round 1 Baseline (Kotlin 2.2.21-0.2.0-12)

| Configuration | Size | Growth |
|---|---|---|
| Incremental clean | 255,481,768 (244 MB) | — |
| Incremental + 1 edit (no cache) | 255,481,784 | +16 B |
| Incremental + 1 edit (w/ cache) | 255,356,096 | +3,088 B |
| Control (cacheKind=none) | 140,007,552 (134 MB) | 0 B |
| **Baseline penalty** | **115,474,216 (82%)** | |

## Quick Start

```bash
chmod +x reproduce-oh.sh
./reproduce-oh.sh
```

## Prerequisites

- macOS aarch64
- OHOS SDK installed at `~/Library/OpenHarmony/Sdk`
- Git access to `https://gitcode.com/CPF-KMP-CMP/kmp-cmp-test-demo.git`
- ~2 GB disk for Kotlin/Native toolchain download

## What Changed

Only `gradle/libs.versions.toml` line 13:
```
kotlin = "2.2.21-OH.0.1.0-06"  # was "2.2.21-0.2.0-12"
```

## Expected Results (from our run)

| Configuration | Size | Growth |
|---|---|---|
| Incremental clean | 132,047,080 (126 MB) | — |
| Incremental + 1 edit (no cache) | 132,047,080 | **0 B** |
| Incremental + 1 edit (w/ cache) | 132,047,080 | **0 B** |
| Control (cacheKind=none) | 123,562,832 (118 MB) | 0 B |
| **Baseline penalty** | **8,484,248 (6.9%)** | |

**Improvement**: Baseline penalty dropped from 82% to 6.9%. Per-build growth eliminated entirely.
