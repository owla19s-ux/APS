#!/usr/bin/env python3
"""Run APS's existing debug checks and retain evidence outside the source tree."""

import argparse
import datetime
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
TASKS = [
    ":app:testDebugUnitTest",
    ":app:compileDebugAndroidTestKotlin",
    ":app:compileDebugAndroidTestJavaWithJavac",
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True,
                        help="New evidence directory outside the repository")
    args = parser.parse_args()
    output = args.output.resolve()
    if output == ROOT or ROOT in output.parents:
        parser.error("Evidence must be saved outside the repository")
    output.mkdir(parents=True, exist_ok=False)
    records = []
    checks = {}

    def git(*arguments):
        return subprocess.check_output(["git", *arguments], cwd=ROOT, text=True).strip()

    evidence = {
        "started_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "commit": git("rev-parse", "HEAD"),
        "branch": git("branch", "--show-current"),
        "working_tree_status": git("status", "--porcelain"),
        "checks": checks,
        "commands": records,
        "result": "in_progress",
        "instrumentation_execution": "not_executed",
    }
    (output / "working-tree.patch").write_text(git("diff", "HEAD", "--", "."))

    def save():
        (output / "summary.json").write_text(
            json.dumps(evidence, ensure_ascii=False, indent=2) + "\n")

    def run(name, command):
        log = output / (name + ".log")
        print("Running: " + " ".join(command), flush=True)
        with log.open("w") as stream:
            try:
                result = subprocess.run(command, cwd=ROOT, stdout=stream,
                                        stderr=subprocess.STDOUT)
                code = result.returncode
            except FileNotFoundError:
                code = 127
                stream.write(f"Executable not found: {command[0]}\n")
        records.append({"command": command, "exit_code": code, "log": log.name})
        save()
        print(f"Exit: {code}; log: {log}", flush=True)
        return code, log.read_text(errors="replace")

    java = shutil.which("java") or "java"
    javac = shutil.which("javac") or "javac"
    gradle = shutil.which("gradle") or "gradle"
    java_code, java_text = run("java", [java, "-version"])
    javac_code, javac_text = run("javac", [javac, "-version"])
    gradle_code, gradle_text = run("gradle", [gradle, "--version"])
    checks["java_17"] = java_code == 0 and bool(re.search(r'version "17\.', java_text))
    checks["javac_17"] = javac_code == 0 and bool(re.search(r"javac 17\.", javac_text))
    checks["gradle_8_10"] = gradle_code == 0 and bool(
        re.search(r"^Gradle 8\.10\s*$", gradle_text, re.MULTILINE))
    checks["gradle_jvm_17"] = bool(
        re.search(r"^Launcher JVM:\s+17\.", gradle_text, re.MULTILINE))
    sdk_home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    sdk = Path(sdk_home) if sdk_home else None
    checks["android_platform_35"] = bool(sdk and (sdk / "platforms/android-35/android.jar").is_file())
    checks["android_build_tools_35_0_0"] = bool(sdk and (sdk / "build-tools/35.0.0/aapt2").is_file())
    evidence["sdk_home"] = sdk_home
    if not all(checks.values()):
        evidence["result"] = "environment_blocked"
        save()
        print(json.dumps(checks, ensure_ascii=False, indent=2))
        return 2

    # Force unit-test execution, so an old XML report is never treated as this run's evidence.
    code, _ = run("android-tests", [gradle, *TASKS, "--stacktrace", "--console=plain",
                                     "--no-daemon", "--max-workers=2", "--rerun-tasks"])
    counts = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    reports = sorted((ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
    # On a failed build, old reports cannot establish the outcome of this run.
    if code == 0:
        for report in reports:
            suite = ET.parse(report).getroot()
            for key in counts:
                counts[key] += int(suite.attrib.get(key, 0))
            shutil.copy2(report, output / report.name)
    evidence["unit_test_counts"] = counts if code == 0 else None
    evidence["result"] = (
        "passed" if code == 0 and counts["tests"] > 0 and
        counts["failures"] == 0 and counts["errors"] == 0 else "failed"
    )
    evidence["finished_utc"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    save()
    print(f"Result: {evidence['result']}; unit tests: {evidence['unit_test_counts']}")
    return 0 if evidence["result"] == "passed" else (code or 1)


if __name__ == "__main__":
    sys.exit(main())
