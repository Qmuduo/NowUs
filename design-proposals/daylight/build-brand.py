"""Build original path-based vector assets and raster exports for NowUs."""
from pathlib import Path
import cairosvg
import re
import zipfile

out=Path(__file__).parent/'brand'
out.mkdir(exist_ok=True)
night='#2d425d'; dawn='#efc88f'; teal='#246b65'; cream='#f5f1df'; mint='#a6c7b9'
n='M30 88V50C30 24 64 24 64 50V78'
u='M64 50V78C64 104 98 104 98 78V40'
def mark(a=night,b=dawn,c=teal):
    return f'<g fill="none" stroke-width="18" stroke-linecap="round" stroke-linejoin="round"><path d="{n}" stroke="{a}"/><path d="{u}" stroke="{b}"/><path d="M64 54V74" stroke="{c}"/></g>'
def word(color=night):
    return f'''<g fill="none" stroke="{color}" stroke-width="6" stroke-linecap="round" stroke-linejoin="round">
    <path d="M4 46V17M4 29C4 10 30 10 30 29V46"/>
    <ellipse cx="56" cy="31" rx="14" ry="16"/>
    <path d="M83 16L91 46L102 22L113 46L121 16M137 16V33C137 51 163 51 163 33V16M201 19C177 5 172 30 190 31C211 32 204 54 180 44"/></g>'''
def svg(body,w=128,h=128,title='NowUs'):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" role="img" aria-label="{title}"><title>{title}</title>{body}</svg>'
assets={
    'mark.svg':svg(mark(),title='NowUs · 相接的 n 与 u'),
    'mark-mono.svg':svg(mark(night,night,night)),
    'mark-reverse.svg':svg(mark(cream,dawn,mint)),
    'wordmark.svg':svg(word(),211,64,'nowus 字标'),
    'logo.svg':svg('<g transform="translate(0 0) scale(.75)">'+mark()+'</g><g transform="translate(112 14) scale(1.08)">'+word()+'</g>',350,96,'NowUs 完整标志'),
    'logo-mono.svg':svg('<g transform="scale(.75)">'+mark(night,night,night)+'</g><g transform="translate(112 14) scale(1.08)">'+word()+'</g>',350,96),
    'logo-reverse.svg':svg('<g transform="scale(.75)">'+mark(cream,dawn,mint)+'</g><g transform="translate(112 14) scale(1.08)">'+word(cream)+'</g>',350,96),
    'app-icon.svg':svg(f'<rect width="128" height="128" rx="28" fill="{night}"/><g transform="translate(12.8 12.8) scale(.8)">'+mark(cream,dawn,mint)+'</g>',title='NowUs 应用图标'),
    'adaptive-foreground.svg':svg('<g transform="translate(15.6 15.6) scale(.6)">'+mark(cream,dawn,mint)+'</g>',108,108),
    'adaptive-background.svg':svg(f'<rect width="108" height="108" fill="{night}"/>',108,108),
}
for name,source in assets.items():
    (out/name).write_text(source,encoding='utf-8')
for size in [32,48,72,96,144,192,512,1024]:
    cairosvg.svg2png(bytestring=assets['app-icon.svg'].encode(),write_to=str(out/f'app-icon-{size}.png'),output_width=size,output_height=size)
cairosvg.svg2png(bytestring=assets['logo.svg'].encode(),write_to=str(out/'logo.png'),output_width=1400,output_height=384)
cairosvg.svg2png(bytestring=assets['mark.svg'].encode(),write_to=str(out/'mark.png'),output_width=512,output_height=512)
(out/'adaptive-foreground.xml').write_text(f'''<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
<group android:translateX="15.6" android:translateY="15.6" android:scaleX="0.6" android:scaleY="0.6">
<path android:pathData="{n}" android:strokeColor="{cream}" android:strokeWidth="18" android:strokeLineCap="round" android:fillColor="#00000000"/>
<path android:pathData="{u}" android:strokeColor="{dawn}" android:strokeWidth="18" android:strokeLineCap="round" android:fillColor="#00000000"/>
<path android:pathData="M64 54V74" android:strokeColor="{mint}" android:strokeWidth="18" android:strokeLineCap="round" android:fillColor="#00000000"/>
</group></vector>''',encoding='utf-8')
print(f'Exported {len(assets)} SVG assets, 10 PNG assets, and Android foreground XML to {out}')

source=(out.parent/'app.js').read_text(encoding='utf-8')
literal=re.search(r'const icons = \{(.*?)\n\};',source,re.S).group(1)
icons=dict(re.findall(r"(\w+): '([^']*)'",literal))
icons.update(dict(re.findall(r"icons\.(\w+)='([^']*)';",source)))
(out/'icons').mkdir(exist_ok=True)
for name,paths in icons.items():
    (out/'icons'/f'{name}.svg').write_text(svg(f'<g fill="none" stroke="{night}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">{paths}</g>',24,24,name),encoding='utf-8')
symbols=''.join(f'<symbol id="{name}" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">{paths}</g></symbol>' for name,paths in icons.items())
(out/'icons.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg">{symbols}</svg>',encoding='utf-8')
with zipfile.ZipFile(out/'nowus-brand-kit.zip','w',zipfile.ZIP_DEFLATED) as archive:
    for file in sorted(out.rglob('*')):
        if file.is_file() and file.suffix not in ['.zip','.jpg']:
            archive.write(file,'nowus-brand/'+str(file.relative_to(out)))
print(f'Exported {len(icons)} UI icons and nowus-brand-kit.zip')
