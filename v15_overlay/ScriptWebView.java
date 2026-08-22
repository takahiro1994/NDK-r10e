package com.xperiatk.lightart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Base64;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;

import java.nio.charset.StandardCharsets;

final class ScriptWebView extends WebView {
    interface ErrorListener { void onError(String message); }

    private final String originalScript;
    private final ScriptRuntime.Mode mode;
    private ErrorListener errorListener;
    private boolean paused;
    private float speedScale = 1f;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    ScriptWebView(Context context, String script, ScriptRuntime.Mode mode) {
        super(context);
        this.originalScript = script == null ? "" : script;
        this.mode = mode;
        setBackgroundColor(Color.BLACK);
        WebSettings s = getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        if (android.os.Build.VERSION.SDK_INT >= 21) s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        addJavascriptInterface(new Bridge(), "LightArtAndroid");
        setWebViewClient(new WebViewClient() {
            private WebResourceResponse asset(String name, String mime) {
                try { return new WebResourceResponse(mime, "UTF-8", getContext().getAssets().open(name)); }
                catch (IOException e) { return null; }
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString();
                if (u.equals("https://lightart.local/p5.min.js")) return asset("p5.min.js", "application/javascript");
                if (u.equals("https://lightart.local/p5.webgpu.min.js")) return asset("p5.webgpu.min.js", "application/javascript");
                return super.shouldInterceptRequest(view, request);
            }
        });
        setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage cm) {
                if (cm.messageLevel() == ConsoleMessage.MessageLevel.ERROR) notifyError(cm.message());
                return true;
            }
        });
        loadRuntime();
    }

    void setErrorListener(ErrorListener listener) { this.errorListener = listener; }

    ScriptRuntime.Mode getMode() { return mode; }

    boolean isPausedArt() { return paused; }

    void setPausedArt(boolean value) {
        paused = value;
        if (mode == ScriptRuntime.Mode.GLSL) {
            eval("window.__laPaused=" + (value ? "true" : "false") + ";");
        } else {
            eval("window.__laPaused=" + (value ? "true" : "false") + ";try{" + (value ? "noLoop()" : "loop()") + ";}catch(e){}");
        }
    }

    void setSpeedScaleArt(float scale) {
        speedScale = scale;
        eval("if(window.__laSetSpeed)window.__laSetSpeed(" + scale + ");");
    }

    void resetArt() {
        paused = false;
        loadRuntime();
    }

    void releaseArt() {
        try { eval("try{noLoop()}catch(e){};window.__laPaused=true;"); } catch (Exception ignored) {}
        stopLoading();
        loadUrl("about:blank");
        clearHistory();
        removeJavascriptInterface("LightArtAndroid");
        destroy();
    }

    private void loadRuntime() {
        String html;
        if (mode == ScriptRuntime.Mode.GLSL) html = glslHtml(ScriptRuntime.cleanGlsl(originalScript));
        else if (mode == ScriptRuntime.Mode.PROCESSING_PY) html = pythonNoticeHtml();
        else html = p5Html(ScriptRuntime.cleanP5(originalScript));
        loadDataWithBaseURL("https://lightart.local/", html, "text/html", "UTF-8", null);
    }

    private void eval(String js) {
        if (android.os.Build.VERSION.SDK_INT >= 19) evaluateJavascript(js, null);
        else loadUrl("javascript:" + js);
    }

    private void notifyError(String msg) {
        if (errorListener != null && msg != null && !msg.isEmpty()) errorListener.onError(msg);
    }

    private final class Bridge {
        @JavascriptInterface public void onError(String message) { post(() -> notifyError(message)); }
        @JavascriptInterface public void onReady() { post(() -> setSpeedScaleArt(speedScale)); }
    }

    private static String b64(String s) {
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private static String commonCss() {
        return "html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#000;}" +
                "body{display:flex;align-items:center;justify-content:center;}" +
                "canvas{display:block;max-width:100vw!important;max-height:100vh!important;width:auto!important;height:auto!important;}" +
                "#laerr{position:fixed;left:10px;right:10px;top:10px;padding:10px;background:#b00020;color:#fff;font:12px monospace;white-space:pre-wrap;z-index:99999;display:none;border-radius:8px;}";
    }

    private static String p5Html(String script) {
        String data = b64(script);
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'>" +
                "<style>" + commonCss() + "</style>" +
                "<script src='https://lightart.local/p5.min.js'></script>" +
                "<script src='https://lightart.local/p5.webgpu.min.js'></script>" +
                "<script>" +
                "(function(){" +
                "const N=['background','clear','stroke','fill','noStroke','noFill','strokeWeight','blendMode','erase','noErase','colorMode','ellipseMode','rectMode','imageMode','angleMode','translate','rotate','rotateX','rotateY','rotateZ','scale','shearX','shearY','applyMatrix','resetMatrix','push','pop','point','line','triangle','quad','rect','square','ellipse','circle','arc','bezier','curve','beginShape','beginContour','endContour','vertex','bezierVertex','quadraticVertex','curveVertex','endShape','image','tint','noTint','randomSeed','noiseSeed','noiseDetail','frameRate','pixelDensity','smooth','noSmooth','lights','noLights','ambientLight','directionalLight','pointLight','spotLight','specularColor','ambientMaterial','emissiveMaterial','normalMaterial','specularMaterial','shininess','box','sphere','cylinder','cone','ellipsoid','torus','plane','orbitControl','perspective','ortho','camera'];" +
                "function P(o){if(!o||o.__laChainPatched)return;try{Object.defineProperty(o,'__laChainPatched',{value:true,configurable:true})}catch(e){return}for(const n of N){const f=o[n];if(typeof f!=='function'||f.__laWrapped)continue;const w=function(...a){const r=f.apply(this,a);return(r==null||r===this._renderer)?this:r};try{Object.defineProperty(w,'__laWrapped',{value:true});o[n]=w}catch(e){}}}" +
                "P(p5.prototype);const C=p5.prototype.createCanvas;p5.prototype.createCanvas=function(...a){const r=C.apply(this,a);let q=r;while(q&&q!==Object.prototype){P(q);q=Object.getPrototypeOf(q)}return r};" +
                "})();" +
                "</script>" +
                "</head><body><div id='laerr'></div><script>" +
                "window.__laPaused=false;window.__laSpeed=1;window.__laMulAcc=0;" +
                "function __laErr(e){var m=(e&&e.stack)||String(e);var d=document.getElementById('laerr');d.textContent=m;d.style.display='block';try{LightArtAndroid.onError(m)}catch(_){}}" +
                "window.onerror=function(m,u,l,c,e){__laErr(e||m);return false};window.onunhandledrejection=function(e){__laErr(e.reason||e)};" +
                "try{var __code=decodeURIComponent(escape(atob('" + data + "')));(0,eval)(__code);" +
                "var __ud=window.draw;if(typeof __ud==='function'){window.draw=function(){if(window.__laPaused)return;var s=window.__laSpeed||1;if(s<=1){return __ud.apply(this,arguments)}window.__laMulAcc+=s;var n=Math.floor(window.__laMulAcc);window.__laMulAcc-=n;for(var j=0;j<n;j++)__ud.apply(this,arguments);};}" +
                "window.__laSetSpeed=function(s){window.__laSpeed=s;try{frameRate(60*Math.min(1,s))}catch(e){}};" +
                "window.addEventListener('load',function(){try{LightArtAndroid.onReady()}catch(e){}});" +
                "}catch(e){__laErr(e)}" +
                "</script></body></html>";
    }

    private static String glslHtml(String source) {
        String data = b64(source);
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'><style>" + commonCss() + "</style></head>" +
                "<body><canvas id='c'></canvas><div id='laerr'></div><script>" +
                "const cvs=document.getElementById('c'),gl=cvs.getContext('webgl2',{antialias:false,preserveDrawingBuffer:true});window.__laPaused=false;window.__laSpeed=1;let st=performance.now(),held=0,last=st;" +
                "function err(m){m=String(m);let d=document.getElementById('laerr');d.textContent=m;d.style.display='block';try{LightArtAndroid.onError(m)}catch(e){}}" +
                "if(!gl){err('WebGL2 is not available on this device');throw 0;}" +
                "function rs(){let d=devicePixelRatio||1;cvs.width=Math.max(1,innerWidth*d);cvs.height=Math.max(1,innerHeight*d);cvs.style.width=innerWidth+'px';cvs.style.height=innerHeight+'px';gl.viewport(0,0,cvs.width,cvs.height)}addEventListener('resize',rs);rs();" +
                "let user=decodeURIComponent(escape(atob('" + data + "')));user=user.replace(/#つぶやきGLSL.*$/gm,'').trim();" +
                "const helpers=`vec3 _la_hsv(float h,float s,float v){vec3 c=clamp(abs(mod(h*6.+vec3(0.,4.,2.),6.)-3.)-1.,0.,1.);return v*mix(vec3(1),c,s);}mat2 _la_r2(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}mat3 _la_r3(float a,vec3 v){v=normalize(v);float c=cos(a),s=sin(a),C=1.-c;return mat3(c+v.x*v.x*C,v.x*v.y*C-v.z*s,v.x*v.z*C+v.y*s,v.y*v.x*C+v.z*s,c+v.y*v.y*C,v.y*v.z*C-v.x*s,v.z*v.x*C-v.y*s,v.z*v.y*C+v.x*s,c+v.z*v.z*C);}`;" +
                "if(!/void\\s+main\\s*\\(/.test(user))user='void main(){'+user+'}';user=user.replace(/void\\s+main\\s*\\([^)]*\\)\\s*\\{/,'void main(){o=vec4(0.,0.,0.,1.);');" +
                "let fs=`#version 300 es\\nprecision highp float;\\nuniform float t;uniform vec2 r;out vec4 o;\\n#define FC gl_FragCoord\\n#define hsv _la_hsv\\n#define rotate2D _la_r2\\n#define rotate3D _la_r3\\n${helpers}\\n${user}`;" +
                "function sh(type,src){let s=gl.createShader(type);gl.shaderSource(s,src);gl.compileShader(s);if(!gl.getShaderParameter(s,gl.COMPILE_STATUS))throw new Error(gl.getShaderInfoLog(s));return s}" +
                "try{let vs=sh(gl.VERTEX_SHADER,'#version 300 es\\nin vec2 p;void main(){gl_Position=vec4(p,0,1);}'),f=sh(gl.FRAGMENT_SHADER,fs),pr=gl.createProgram();gl.attachShader(pr,vs);gl.attachShader(pr,f);gl.linkProgram(pr);if(!gl.getProgramParameter(pr,gl.LINK_STATUS))throw new Error(gl.getProgramInfoLog(pr));gl.useProgram(pr);let b=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,b);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array([-1,-1,3,-1,-1,3]),gl.STATIC_DRAW);let a=gl.getAttribLocation(pr,'p');gl.enableVertexAttribArray(a);gl.vertexAttribPointer(a,2,gl.FLOAT,false,0,0);let ut=gl.getUniformLocation(pr,'t'),ur=gl.getUniformLocation(pr,'r');" +
                "window.__laSetSpeed=s=>window.__laSpeed=s;function loop(now){requestAnimationFrame(loop);if(window.__laPaused){last=now;return}let dt=now-last;last=now;held+=dt*(window.__laSpeed||1);gl.uniform1f(ut,held/1000);gl.uniform2f(ur,cvs.width,cvs.height);gl.drawArrays(gl.TRIANGLES,0,3)}requestAnimationFrame(loop);try{LightArtAndroid.onReady()}catch(e){}" +
                "}catch(e){err(e.stack||e)}" +
                "</script></body></html>";
    }

    private static String pythonNoticeHtml() {
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><style>" + commonCss() +
                "body{color:#fff;font:16px sans-serif;padding:24px;box-sizing:border-box;display:block}h2{margin-top:20vh}p{color:#cbd5e1;line-height:1.7}</style></head><body>" +
                "<h2>Processing.py legacy code</h2><p>このコードはp5.jsではなく、Processing Python Mode / Jython系です。LightArt v1.5では自動判定できますが、完全なJython/P3D互換ランタイムはまだ同梱していません。</p>" +
                "<p>現在の@yuruyurauのp5.jsコードと #つぶやきGLSL は直接実行できます。</p></body></html>";
    }
}
