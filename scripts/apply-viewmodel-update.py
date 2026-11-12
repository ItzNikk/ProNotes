#!/usr/bin/env python3
"""Apply the Pro Notes v9.5.1 maths/answer-animation integration safely.

Run from any directory after extracting the update ZIP into the repository root:
    python scripts/apply-viewmodel-update.py

The script only changes the targeted lines in EditorViewModel.kt. It refuses to
write if the expected upstream source is missing or has drifted.
"""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "app/src/main/java/com/pronotes/app/editor/EditorViewModel.kt"

REPLACEMENTS = [
    (
        "val good = guesses.firstOrNull { MathSolver.solve(it) != null } ?: guesses.first()",
        'val good = MathSolver.bestCandidate(guesses) ?: run { say("Couldn\'t read that sum accurately"); return@launch }',
    ),
    (
        'val text = if (raw.trimEnd().endsWith("=")) ans else "= $ans"',
        'val hasCompletedEquality = raw.trim().contains("=") && !raw.trimEnd().endsWith("=")\n'
        '        val text = if (raw.trimEnd().endsWith("=") || hasCompletedEquality) ans else "= $ans"',
    ),
    (
        "mathSrc[final.id] = RectF(b)\n        viewModelScope.launch {\n            val cur = pages.getOrNull(page) ?: return@launch",
        "mathSrc[final.id] = RectF(b)\n        viewModelScope.launch {\n"
        "            val cur = pages.getOrNull(page) ?: return@launch\n"
        "            val animationStart = android.os.SystemClock.uptimeMillis()\n"
        "            val answerRect = RectF(ax, b.top - size * 1.15f, ax + text.length * size * 0.62f, b.top + size * 0.3f)\n"
        "            mathAnim = MathAnim(page, RectF(b), answerRect, animationStart)",
    ),
    (
        "if (now.items.none { it.id == final.id }) return@launch",
        "if (now.items.none { it.id == final.id }) { if (mathAnim?.start == animationStart) mathAnim = null; return@launch }",
    ),
    (
        "scheduleSave()\n        }\n        return true",
        "scheduleSave()\n"
        "            delay(maxOf(SWEEP_MS + 650L, text.length * 84L + 160L))\n"
        "            if (mathAnim?.start == animationStart) mathAnim = null\n"
        "        }\n"
        "        return true",
    ),
    (
        'val raw = g.firstOrNull { MathSolver.solve(it) != null } ?: g.firstOrNull() ?: run { say("Couldn\'t read that"); return@launch }\n'
        '            val pretty = MathSolver.normalize(raw).replace("*", " × ").replace("/", " ÷ ").replace("+", " + ")',
        'val raw = MathSolver.bestCandidate(g) ?: run { say("Couldn\'t read that accurately"); return@launch }\n'
        '            val pretty = MathSolver.normalize(raw).trimEnd(\'=\', \'?\').replace("*", " × ").replace("/", " ÷ ").replace("+", " + ")',
    ),
    (
        'val raw = guesses.firstOrNull { it.trimEnd().endsWith("=") && MathSolver.solve(it) != null }',
        'val raw = MathSolver.bestCandidate(guesses.filter { it.trimEnd().endsWith("=") })',
    ),
]


def main() -> int:
    if not TARGET.is_file():
        print(f"ERROR: {TARGET} not found. Extract this ZIP into your ProNotes repository root first.", file=sys.stderr)
        return 1

    original_bytes = TARGET.read_bytes()
    newline = "\r\n" if b"\r\n" in original_bytes else "\n"
    source = original_bytes.decode("utf-8").replace("\r\n", "\n")
    already_applied = all(new in source and old not in source for old, new in REPLACEMENTS)
    if already_applied:
        print("EditorViewModel.kt already contains the v9.5.1 integration; no changes made.")
        return 0

    counts = [(old, source.count(old)) for old, _ in REPLACEMENTS]
    bad = [(old[:80].replace("\n", " "), count) for old, count in counts if count != 1]
    if bad:
        print("ERROR: source differs from the expected Pro Notes base; no changes were written.", file=sys.stderr)
        for snippet, count in bad:
            print(f"  expected one occurrence, found {count}: {snippet}", file=sys.stderr)
        return 2

    updated = source
    for old, new in REPLACEMENTS:
        updated = updated.replace(old, new, 1)

    # Guard against applying the integration to a source file without the canvas hook.
    if "data class MathAnim" not in updated or "@Volatile var mathAnim: MathAnim? = null" not in updated:
        print("ERROR: expected math-animation bridge is missing; no changes were written.", file=sys.stderr)
        return 3

    TARGET.write_bytes(updated.replace("\n", newline).encode("utf-8"))
    print("Applied the v9.5.1 maths-candidate and answer-animation integration to EditorViewModel.kt.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
