#!/usr/bin/env python3
"""Turns a cold-start Macrobenchmark result into a short Markdown table.

    python3 -I scripts/startup-report.py <...-benchmarkData.json>

It reads StartupBenchmark's coldStartWithoutProfile and coldStartWithProfile runs (time to initial
display) and prints the median, fastest and slowest, and what the Baseline Profile saves. Emulator
numbers vary from run to run and are only comparable with each other; read them as a trend.
"""
import json
import sys


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    data = json.load(open(sys.argv[1], encoding="utf-8"))
    device = data.get("context", {}).get("build", {})
    runs = {}
    for benchmark in data.get("benchmarks", []):
        metric = benchmark.get("metrics", {}).get("timeToInitialDisplayMs")
        if metric:
            runs[benchmark["name"]] = metric
    rows = [("Without the Baseline Profile", runs.get("coldStartWithoutProfile")),
            ("With the Baseline Profile", runs.get("coldStartWithProfile"))]
    print("### Cold start (time to first display)\n")
    print(f"Device: {device.get('model', 'unknown')}, API {device.get('version', {}).get('sdk', '?')}. "
          "Emulator numbers vary between runs; compare them with other runs, not with phones.\n")
    print("| | Median | Fastest | Slowest | Runs |")
    print("|---|---:|---:|---:|---:|")
    for label, metric in rows:
        if metric is None:
            print(f"| {label} | not measured | | | |")
        else:
            print(f"| {label} | {metric['median']:.0f} ms | {metric['minimum']:.0f} ms | "
                  f"{metric['maximum']:.0f} ms | {len(metric.get('runs', []))} |")
    without, with_profile = rows[0][1], rows[1][1]
    if without and with_profile and without["median"] > 0:
        saved = without["median"] - with_profile["median"]
        share = abs(saved) / without["median"]
        if saved >= 0:
            print(f"\nThe Baseline Profile saves {saved:.0f} ms ({share:.0%}) at the median.")
        else:
            print(f"\nWith the Baseline Profile the median is {-saved:.0f} ms ({share:.0%}) slower in this run.")


if __name__ == "__main__":
    main()
