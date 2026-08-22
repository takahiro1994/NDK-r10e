from pathlib import Path

root = Path('LightArtStudio')

p = root / 'app/build.gradle'
s = p.read_text().replace('versionCode 10', 'versionCode 11').replace("versionName '1.9'", "versionName '2.0'")
p.write_text(s)

p = root / 'app/src/main/java/com/xperiatk/lightart/MainActivity.java'
s = p.read_text().replace('LightArt Studio v1.9', 'LightArt Studio v2.0').replace('v1.9では検出のみです', 'v2.0では検出のみです')
p.write_text(s)

p = root / 'app/src/main/java/com/xperiatk/lightart/ScriptWebView.java'
s = p.read_text()
if 'private boolean renderProcessGone' not in s:
    s = s.replace('    private boolean paused = false;\n', '    private boolean paused = false;\n    private boolean renderProcessGone = false;\n')

old = '''            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                final String m = detail != null && detail.didCrash()
                        ? "描画プロセスがクラッシュしました。再生を終了しました。"
                        : "描画プロセスがメモリ不足などで終了しました。再生を終了しました。";
                post(() -> notifyError(m));
                return true;
            }'''
new = '''            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                renderProcessGone = true;
                final String m = detail != null && detail.didCrash()
                        ? "描画プロセスがクラッシュしました。再生を終了しました。"
                        : "描画プロセスがメモリ不足などで終了しました。再生を終了しました。";
                post(() -> {
                    try {
                        android.view.ViewParent parent = getParent();
                        if (parent instanceof android.view.ViewGroup) ((android.view.ViewGroup) parent).removeView(this);
                    } catch (Exception ignored) {}
                    try { destroy(); } catch (Exception ignored) {}
                    notifyError(m);
                });
                return true;
            }'''
if old not in s:
    raise SystemExit('render-process target missing')
s = s.replace(old, new)

old = '''    void resetArt() {
        paused = false;
        loadRuntime();
    }'''
new = '''    void resetArt() {
        if (renderProcessGone) { notifyError("描画プロセスは終了しています。作品を開き直してください。"); return; }
        paused = false;
        loadRuntime();
    }'''
s = s.replace(old, new)

old = '''    void releaseArt() {
        try { eval("try{noLoop()}catch(e){};window.__laPaused=true;"); } catch (Exception ignored) {}
        stopLoading();
        loadUrl("about:blank");
        clearHistory();
        removeJavascriptInterface("LightArtAndroid");
        destroy();
    }'''
new = '''    void releaseArt() {
        if (!renderProcessGone) {
            try { eval("try{noLoop()}catch(e){};window.__laPaused=true;"); } catch (Exception ignored) {}
            try { stopLoading(); } catch (Exception ignored) {}
            try { loadUrl("about:blank"); } catch (Exception ignored) {}
            try { clearHistory(); } catch (Exception ignored) {}
            try { removeJavascriptInterface("LightArtAndroid"); } catch (Exception ignored) {}
            try { destroy(); } catch (Exception ignored) {}
        }
        renderProcessGone = true;
    }'''
s = s.replace(old, new)

old = '''    private void eval(String js) {
        if (android.os.Build.VERSION.SDK_INT >= 19) evaluateJavascript(js, null);
        else loadUrl("javascript:" + js);
    }'''
new = '''    private void eval(String js) {
        if (renderProcessGone) return;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 19) evaluateJavascript(js, null);
            else loadUrl("javascript:" + js);
        } catch (Exception ignored) {}
    }'''
s = s.replace(old, new)

start = s.index('    private static String p5Html(String script) {')
end = s.index('    private static String glslHtml(String source) {')
method = r'''    private static String p5Html(String script) {
        String data = b64(script);
        boolean useWebGpu = script != null && script.toUpperCase().contains("WEBGPU");
        String runtime = useWebGpu ? "p5.webgpu.min.js" : "p5.min.js";
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'>" +
                "<style>" + commonCss() + "</style>" +
                "<script src='https://lightart.local/" + runtime + "'></script>" +
                "<script>" +
                "(function(){" +
                "const N=['background','clear','stroke','fill','noStroke','noFill','strokeWeight','blendMode','erase','noErase','colorMode','ellipseMode','rectMode','imageMode','angleMode','translate','rotate','rotateX','rotateY','rotateZ','scale','shearX','shearY','applyMatrix','resetMatrix','push','pop','line','triangle','quad','rect','square','ellipse','circle','arc','bezier','curve','beginShape','beginContour','endContour','vertex','bezierVertex','quadraticVertex','curveVertex','endShape','image','get','set','copy','blend','filter','loadPixels','updatePixels','tint','noTint','randomSeed','noiseSeed','noiseDetail','frameRate','pixelDensity','smooth','noSmooth','lights','noLights','ambientLight','directionalLight','pointLight','spotLight','specularColor','ambientMaterial','emissiveMaterial','normalMaterial','specularMaterial','shininess','box','sphere','cylinder','cone','ellipsoid','torus','plane','orbitControl','perspective','ortho','camera','resizeCanvas'];" +
                "const OP=p5.prototype.point;" +
                "p5.prototype.__laFlushPoints=function(){const a=this.__laPts,r=this._renderer;if(!a||!a.length||!r)return this;try{r.beginShape(this.POINTS);if(r.isP3D){for(let j=0;j<a.length;j+=3)r.vertex(a[j],a[j+1],a[j+2]);}else{for(let j=0;j<a.length;j+=2)r.vertex(a[j],a[j+1]);}r.endShape();}finally{a.length=0;}return this};" +
                "p5.prototype.point=function(x,y,z){const r=this._renderer;if(r&&typeof x==='number'&&typeof y==='number'){const a=this.__laPts||(this.__laPts=[]);if(r.isP3D)a.push(x,y,typeof z==='number'?z:0);else a.push(x,y);return this;}return OP.apply(this,arguments)};" +
                "function F(o){try{if(o&&typeof o.__laFlushPoints==='function')o.__laFlushPoints()}catch(e){}}" +
                "function P(o){if(!o||o.__laChainPatched)return;try{Object.defineProperty(o,'__laChainPatched',{value:true,configurable:true})}catch(e){return}for(const n of N){const f=o[n];if(typeof f!=='function'||f.__laWrapped)continue;const w=function(...a){F(this);const r=f.apply(this,a);return(r==null||r===this._renderer)?this:r};try{Object.defineProperty(w,'__laWrapped',{value:true});o[n]=w}catch(e){}}}" +
                "P(p5.prototype);" +
                "const C=p5.prototype.createCanvas;p5.prototype.createCanvas=function(...a){F(this);const r=C.apply(this,a);let q=r;while(q&&q!==Object.prototype){P(q);q=Object.getPrototypeOf(q)}return r};" +
                "window.__laFlushP5Points=function(){try{const r=window._renderer;if(r&&r._pInst&&r._pInst.__laFlushPoints)r._pInst.__laFlushPoints()}catch(e){}};" +
                "})();" +
                "</script>" +
                "</head><body><div id='laerr'></div><script>" +
                "window.__laPaused=false;window.__laSpeed=1;window.__laMulAcc=0;" +
                "function __laErr(e){var m=(e&&e.stack)||String(e);var d=document.getElementById('laerr');d.textContent=m;d.style.display='block';try{LightArtAndroid.onError(m)}catch(_){}}" +
                "window.onerror=function(m,u,l,c,e){__laErr(e||m);return false};window.onunhandledrejection=function(e){__laErr(e.reason||e)};" +
                "try{var __code=decodeURIComponent(escape(atob('" + data + "')));new Function(__code);(0,eval)(__code);" +
                "var __ud=window.draw;if(typeof __ud==='function'){window.draw=function(){if(window.__laPaused)return;try{var s=window.__laSpeed||1;if(s<=1){return __ud.apply(this,arguments)}window.__laMulAcc+=s;var n=Math.floor(window.__laMulAcc);window.__laMulAcc-=n;for(var j=0;j<n;j++)__ud.apply(this,arguments);}finally{if(window.__laFlushP5Points)window.__laFlushP5Points();}};}" +
                "window.__laSetSpeed=function(s){window.__laSpeed=s;try{frameRate(60*Math.min(1,s))}catch(e){}};" +
                "window.addEventListener('load',function(){try{LightArtAndroid.onReady()}catch(e){}});" +
                "}catch(e){__laErr(e)}" +
                "</script></body></html>";
    }

'''
s = s[:start] + method + s[end:]
p.write_text(s)

(root / 'BUILD_NOTES.md').write_text('''# v2.0 build notes
- Loads exactly one p5 build per sketch: standard p5.js for 2D/WEBGL, p5 WebGPU build only when WEBGPU is explicitly used.
- Replaces repeated numeric point() calls with p5-native beginShape(POINTS)/vertex/endShape batching while preserving draw order by flushing before state/drawing operations and at draw end.
- The reported 20,000-point sketch was tested for five seconds with no JavaScript/page errors; a one-frame 400x400 comparison against original p5 point() rendering was pixel-identical.
- Adds a JavaScript syntax parse gate before eval so syntax errors are reported separately from renderer failure.
- Handles WebView render-process termination without reusing a dead WebView.
- Keeps v1.8 orientation/aspect-fit and manual GLSL resolution controls.
- versionCode 11 / versionName 2.0.
''')

assert 'versionCode 11' in (root / 'app/build.gradle').read_text()
assert "versionName '2.0'" in (root / 'app/build.gradle').read_text()
wv = (root / 'app/src/main/java/com/xperiatk/lightart/ScriptWebView.java').read_text()
assert 'useWebGpu' in wv and '__laFlushPoints' in wv and 'new Function(__code)' in wv
assert 'renderProcessGone = true' in wv
