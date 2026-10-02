# NowUs 设计历史：88–93

当前正式原型仍是仓库 `prototype/` 中的第 88 轮。本目录用于浏览、对照和恢复，不会自动替换当前首页。

在仓库根目录运行 `python -m http.server 8765 --bind 127.0.0.1`，打开 http://127.0.0.1:8765/design-history/2026-10-01/ 。默认并排显示 88 与 93，可切换任意两版，或查看全部版本。点击“体验这一版”可在 375×812 的预览中操作。

每个 `round-N/` 的 `home.png` 是原始评审截图，`source/prototype/` 是对应完整源码。89–93 各有 `review.md`、`capture.json`、`source-hashes.json`。88 使用恢复后的同一基线截图；历史评分 8.1 来自用户，89 对同一实现的独立评分是 7。其余 90–93 均为 7。

恢复任意历史版本时，先备份当前 `prototype/`，然后将指定版本 `source/prototype/` 中的文件复制回仓库 `prototype/`，只作用于该目录，保留其他工作区改动。不要从截图重新生成源码。

`manifest.json` 保存本目录文件 SHA256，可检查归档是否被修改。已有 artifacts 下的原始记录继续保留。本目录不受 artifacts 的 Git 忽略规则影响，应与原型及续用文档一并提交；同步状态以 Git 记录为准。`.gitattributes` 对本目录关闭换行转换，确保跨工作区检出后仍能验证原始哈希。

本次只找到了 88–93 轮记录，没有更早 1–87 轮的可确认快照。续用提示词与复盘在本目录也保留了一份；正式文档分别位于仓库 docs/design-review-continuation-prompt.md 与 docs/design-review-retrospective-2026-10-02.md。

在任何检出的工作区运行 `python design-history/2026-10-01/verify.py`，即可检查历史文件与 manifest.json 的原始哈希是否一致。该命令只读，不会修改当前原型或历史版本。

`continuation-prompt.md` 是可用于其他 Agent 的最新完整提示词；`continuation-prompt-at-archive.md` 保留首次整理归档时的提示词文本。
