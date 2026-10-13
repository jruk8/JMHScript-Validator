"""Example CLI adapter: run the validator jar and map its JSON report.

Requires java on PATH and the versioned CLI jar (see the pin
file next to your tester). Asserts the pinned versions, maps
stable codes into local records, and never parses message
text for meaning. Adapt freely; keep the version asserts.
"""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

REPORT_VERSION = 1


def run_validator(jar: Path, registry: Path, expect_validator: str,
                  expect_engine: str, paths: list[Path]) -> dict:
    """Run the CLI and return the parsed report, versions asserted."""
    actual_validator = cli_version(jar)
    if actual_validator != expect_validator:
        raise SystemExit(
            f"validator {actual_validator} != pinned {expect_validator}; "
            "refresh the CLI jar to the pinned version")
    proc = subprocess.run(
        ["java", "-jar", str(jar), "--registry", str(registry),
         "--format", "json", "--expect-engine-version", expect_engine,
         *(str(p) for p in paths)],
        capture_output=True, text=True, check=False)
    if proc.returncode == 2:
        raise SystemExit(f"validator setup error: {proc.stderr.strip()}")
    report = json.loads(proc.stdout or "{}")
    if report.get("reportVersion") != REPORT_VERSION:
        raise SystemExit(
            f"report version {report.get('reportVersion')} != {REPORT_VERSION}")
    return report


def cli_version(jar: Path) -> str:
    """Read the CLI version via --version."""
    proc = subprocess.run(["java", "-jar", str(jar), "--version"],
                          capture_output=True, text=True, check=False)
    if proc.returncode != 0:
        raise SystemExit(f"cannot run validator jar: {proc.stderr.strip()}")
    return proc.stdout.strip()


if __name__ == "__main__":
    here = Path(__file__).resolve().parent
    print("example adapter (import run_validator); contract:", here / "report-schema.json")
    sys.exit(0)
