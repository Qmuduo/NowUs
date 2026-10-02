"""Archive the current prototype as a reviewed round: screenshot, source snapshot, hashes."""
import hashlib, json, pathlib, shutil, subprocess, sys

round_no = sys.argv[1]
batch = sys.argv[2] if len(sys.argv) > 2 else '2026-10-02-e'
root = pathlib.Path('.').resolve()
out = root / 'design-history' / batch / f'round-{round_no}'
(out / 'source').mkdir(parents=True, exist_ok=True)

subprocess.run(['node', f'design-history/{batch}/tools/capture.cjs', f'round-{round_no}', f'round{round_no}'], check=True)

src = root / 'prototype'
dst = out / 'source' / 'prototype'
if dst.exists():
    shutil.rmtree(dst)
shutil.copytree(src, dst)

files = {f.name: hashlib.sha256(f.read_bytes()).hexdigest() for f in sorted(src.iterdir()) if f.is_file()}
(out / 'source-hashes.json').write_text(
    json.dumps({'round': int(round_no), 'label': '本轮已评实现', 'prototype': files}, ensure_ascii=False, indent=2),
    encoding='utf-8')
print(f'round-{round_no} archived with {len(files)} source files')
