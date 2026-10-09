# NowUs · 相接的我们

原创品牌提案，延续「两地天光」页面的夜蓝、晨光及青色。两条圆角曲线分别构成 n 与 u，共用的中间一笔表达共同联系时间。没有使用爱心、地球或钟面作为应用图标主体，避免细节在小尺寸消失。

## 文件

- `app-icon.svg`：带圆角背景的独立图标。
- `app-icon-{32,48,72,96,144,192,512,1024}.png`：带透明圆角的展示、Web、非自适应图标导出。
- `mark.svg` / `mark-mono.svg` / `mark-reverse.svg`：彩色、单色、深底标记。
- `logo.svg` / `logo-mono.svg` / `logo-reverse.svg`：完整标志，字标全部由路径构成，无字体安装依赖。
- `wordmark.svg`：独立 nowus 字标。
- `logo.png` / `mark.png`：透明 PNG 备用导出。
- `icons/`：24px 网格、1.8px 描边的页面图标；`icons.svg` 为 symbol 集合。
- `adaptive-foreground.svg` / `adaptive-background.svg`：108 × 108 自适应分层源文件；前景已缩入中央安全范围。
- `adaptive-foreground.xml`：Android VectorDrawable 前景候选。背景用 `#2D425D`；由工程的 adaptive-icon 资源引用前景与背景。
- `index.html`：品牌展示与下载页。

## 使用

保持原始比例；标记四周至少留出标记宽度的 1/8 空白。彩色标记用于浅底；深底使用反白版本；小尺寸或单色场景使用单色版。不要给字标追加描边、拉伸、投影或替换内部颜色。

图标 PNG 自带圆角，仅适合展示或非自适应入口；Android 自适应图标须使用分层文件，由系统裁切。XML 是交付素材，未写入已有 Android 工程，也未宣称经过 Android 编译验证。

界面图标中，「此刻」呼应 n/u 标记，「一天」是两条平行时间轨道，「留话」改为折角便签。其它操作图标保持统一圆角线条和语义，不用品牌图形替换所有功能符号。

运行上级目录 `build-brand.py` 可从路径母版重新生成素材与 ZIP。需要 Python 与 CairoSVG。素材为当前设计提案，不包含商标检索或注册结论。
