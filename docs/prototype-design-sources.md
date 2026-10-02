# NowUs 原型的设计来源地图

回答一个问题：**想改原型的哪一部分，应该动哪个文件。**

## 一、直接规定原型的源文件（都在 `prototype/`，改设计就是改这几个）

| 文件 | 行数 | 规定什么 |
| --- | --- | --- |
| `index.html` | 62 | **骨架与 DOM**：状态栏、页头（wordmark / 演示切换）、四个视图容器（`#home-view` `#day-view` `#rhythm-view` `#setup-view`）、底部导航、弹窗（`#sheet`）、遮罩。视图内容由 JS 注入，这里只有壳。 |
| `styles.css` | 533 | **视觉规范的唯一来源**：`:root` 里的设计令牌（页面/面板/墨色/强调色/昼夜色/边框/阴影/圆角/字体族/缓动），以及所有布局、排版、响应式规则。**改颜色、字号、间距、层级、圆角，全部在这里。** 首页的样式集中在 `#home-view` 前缀的规则里。 |
| `app.mjs` | 352 | **渲染、交互与全部中文文案**：`renderHome` / `renderDay` / `renderRhythm`、各空状态分支、免责说明「按双方联系偏好估计，尚未约定」、`data-action` 事件处理、`state`。**页面上的每一句字都在这里。** |
| `model.mjs` | 196 | **领域模型与演示数据**：`PEOPLE`（阿远/纽约、我/北京）、`SCENARIOS`（五个演示场景的时刻）、`parts` / `dayBounds`、`routineAt`、`commonWindows`、`nextWindow`、`canContact`。**它决定页面上「有什么信息」，以及时间关系的算法。** |
| `rhythm.mjs` | 58 | 「我的节奏」表单：字段、标签、校验、持久化、摘要文案。 |
| `onboarding.mjs` | 72 | 首次使用与配对流程及其全部文案。 |
| `pairing-model.mjs` | 40 | 配对数据模型（城市、时区、邀请状态）。 |
| `model.test.mjs` / `pairing-model.test.mjs` | 16 项 | 模型的**行为契约**。改 `model.mjs` 必须让它们继续通过。 |
| `preview-*.png`（6 张） | — | 静态预览图，只作展示，**不是规范**。 |

**一句话**：视觉规范 = `styles.css`（尤其 `:root` 令牌）；内容与文案 = `app.mjs` + `model.mjs`；结构 = `index.html`。

## 二、约束「设计怎么改」的规则文件（不在 prototype 里，但决定演进方式）

| 文件 | 作用 |
| --- | --- |
| `docs/design-review-continuation-prompt-v2.md` | 当前评审协议：两条轨道、固定场景与使用途径、每轮只改一处、隔离规则、刻度与停止/恢复规则。 |
| `design-history/*/thesis.md` | 每一批的整页设计论点，按协议要求整批保持。 |
| `design-history/*/review.md`、`findings.md` | 每一轮的评审原文与审计结论。 |
| `docs/design-review-score-analysis-2026-10-02.md` | 为什么早期 20 轮分数偏低，以及同图对照实验。 |

## 三、上游产品设计文档（原型的依据）

`prototype/index.html` 的评审面板里「查看产品设计文档」直接指向：

- **`docs/superpowers/specs/2026-09-29-nowus-product-design.md`**

同目录下还有与原型直接相关的：

- `2026-09-29-nowus-prototype-direction.md`（原型方向）
- `2026-09-29-nowus-prototype-v2.md`
- `2026-09-29-nowus-mvp-acceptance.md`（验收标准）
- `2026-09-30-nowus-routine-templates.md`（作息模板，对应「我的节奏」）
- `2026-09-29-nowus-onboarding-design.md`（首次使用与配对）

## 四、历史快照（不规定当前设计，是记录）

`design-history/<批次>/round-<N>/source/prototype/` 保存每一轮送评时的完整 15 个文件，可用 `verify.py` 校验未被改动。当前 `prototype/` = 第 114 轮。
