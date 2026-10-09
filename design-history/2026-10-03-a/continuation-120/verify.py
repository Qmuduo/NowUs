"""Read-only verification of new evidence, original history, and adopted source."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parent
repo = root.parents[2]
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8-sig"))
prior = json.loads((root / "prior-history-hashes.json").read_text(encoding="utf-8-sig"))
baseline = json.loads((root / "baseline-119/source-hashes.json").read_text(encoding="utf-8-sig"))
failures = []
for rel, expected in manifest["files"].items():
    p = root / rel
    if not p.is_file() or sha(p) != expected:
        failures.append(f"New archive changed: {rel}")
for rel, expected in prior.items():
    p = repo / rel
    if not p.is_file() or sha(p) != expected:
        failures.append(f"Original history changed: {rel}")
for rel, expected in baseline.items():
    p = repo / "prototype" / rel
    if not p.is_file() or sha(p) != expected:
        failures.append(f"Adopted round 119 changed: {rel}")
candidate = root / "candidate/source/prototype"
changed = sorted(rel for rel, expected in baseline.items() if sha(candidate / rel) != expected)
if changed != ["app.mjs", "styles.css"]:
    failures.append(f"Unexpected candidate changed files: {changed}")
for label, source in [("A", "baseline-119"), ("B", "candidate")]:
    if sha(root / f"review-materials/{label}.png") != sha(root / source / "home.png"):
        failures.append(f"Review image {label} differs from screenshot")
if sha(root / "review-materials/A.png") == sha(root / "review-materials/B.png"):
    failures.append("Review images are identical")
if failures:
    raise SystemExit("\n".join(failures))
print(f"Verified {len(manifest['files'])} new files, {len(prior)} original history files, "
      f"{len(baseline)} adopted source files; candidate changes exactly {changed}.")
print("Round 120 is unreviewed; adopted prototype is still round 119.")
