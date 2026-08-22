from pathlib import Path

root = Path('LightArtStudio')

p = root / 'app/build.gradle'
s = p.read_text()
s = s.replace('versionCode 9', 'versionCode 10').replace("versionName '1.8'", "versionName '1.9'")
p.write_text(s)

p = root / 'app/src/main/java/com/xperiatk/lightart/ScriptWebView.java'
s = p.read_text()
s = s.replace(
    'import android.webkit.JavascriptInterface;\n',
    'import android.webkit.JavascriptInterface;\nimport android.webkit.RenderProcessGoneDetail;\n'
)
old_point = '"P(p5.prototype);const C=p5.prototype.createCanvas;p5.prototype.createCanvas=function(...a){const r=C.apply(this,a);let q=r;while(q&&q!==Object.prototype){P(q);q=Object.getPrototypeOf(q)}return r};" +\n                "})();" +'
new_point = '"P(p5.prototype);" +\n                "const __laPointOriginal=p5.prototype.point;p5.prototype.point=function(x,y,z){const r=this._renderer;if(r&&r._doStroke&&typeof x===\\\'number\\\'&&typeof y===\\\'number\\\'){if(r.isP3D){if(typeof z===\\\'number\\\')r.point(x,y,z);else return __laPointOriginal.apply(this,arguments)}else r.point(x,y);return this;}return __laPointOriginal.apply(this,arguments)};" +\n                "const C=p5.prototype.createCanvas;p5.prototype.createCanvas=function(...a){const r=C.apply(this,a);let q=r;while(q&&q!==Object.prototype){P(q);q=Object.getPrototypeOf(q)}return r};" +\n                "})();" +'
if old_point not in s:
    raise SystemExit('point patch target missing')
s = s.replace(old_point, new_point)
old_client = '''                if (u.equals("https://lightart.local/p5.webgpu.min.js")) return asset("p5.webgpu.min.js", "application/javascript");
                if (u.equals("https://lightart.local/twigl_noise.glsl")) return asset("twigl_noise.glsl", "text/plain");
                return super.shouldInterceptRequest(view, request);
            }
        });'''
new_client = '''                if (u.equals("https://lightart.local/p5.webgpu.min.js")) return asset("p5.webgpu.min.js", "application/javascript");
                if (u.equals("https://lightart.local/twigl_noise.glsl")) return asset("twigl_noise.glsl", "text/plain");
                return super.shouldInterceptRequest(view, request);
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                final String m = detail != null && detail.didCrash()
                        ? "描画プロセスがクラッシュしました。再生を終了しました。"
                        : "描画プロセスがメモリ不足などで終了しました。再生を終了しました。";
                post(() -> notifyError(m));
                return true;
            }
        });'''
if old_client not in s:
    raise SystemExit('WebViewClient patch target missing')
s = s.replace(old_client, new_client)
p.write_text(s)

p = root / 'app/src/main/java/com/xperiatk/lightart/MainActivity.java'
s = p.read_text().replace('LightArt Studio v1.8', 'LightArt Studio v1.9').replace('v1.8では検出のみです', 'v1.9では検出のみです')
p.write_text(s)

(root / 'BUILD_NOTES.md').write_text('''# v1.9 build notes
- Fixes stalls/crashes on #つぶやきProcessing sketches with 10k-30k numeric point() calls per frame.
- Keeps p5 Renderer2D/RendererGL rendering semantics while bypassing only the expensive public parameter-validation layer for numeric point calls.
- Handles WebView render-process termination so a renderer failure does not terminate the whole app.
- Keeps v1.8 orientation/aspect-fit and manual GLSL resolution controls.
- versionCode 10 / versionName 1.9.
''')

assert 'versionCode 10' in (root / 'app/build.gradle').read_text()
assert "versionName '1.9'" in (root / 'app/build.gradle').read_text()
assert '__laPointOriginal' in (root / 'app/src/main/java/com/xperiatk/lightart/ScriptWebView.java').read_text()
assert 'onRenderProcessGone' in (root / 'app/src/main/java/com/xperiatk/lightart/ScriptWebView.java').read_text()
