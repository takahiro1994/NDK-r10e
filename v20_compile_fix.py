from pathlib import Path

p = Path('LightArtStudio/app/src/main/java/com/xperiatk/lightart/ScriptWebView.java')
s = p.read_text()

# v2.0 patch originally targeted an older field spelling. The actual v1.8/v1.9 source has `private boolean paused;`.
if 'private boolean renderProcessGone' not in s:
    target = '    private boolean paused;\n'
    if target not in s:
        raise SystemExit('paused field target missing')
    s = s.replace(target, target + '    private boolean renderProcessGone = false;\n', 1)

# Inside the anonymous WebViewClient, `this` is the client, not ScriptWebView.
s = s.replace('''                post(() -> {
                    try {
                        android.view.ViewParent parent = getParent();
                        if (parent instanceof android.view.ViewGroup) ((android.view.ViewGroup) parent).removeView(this);
                    } catch (Exception ignored) {}
                    try { destroy(); } catch (Exception ignored) {}
                    notifyError(m);
                });''', '''                ScriptWebView.this.post(() -> {
                    try {
                        android.view.ViewParent parent = ScriptWebView.this.getParent();
                        if (parent instanceof android.view.ViewGroup) ((android.view.ViewGroup) parent).removeView(ScriptWebView.this);
                    } catch (Exception ignored) {}
                    try { ScriptWebView.this.destroy(); } catch (Exception ignored) {}
                    notifyError(m);
                });''')

p.write_text(s)

assert 'private boolean renderProcessGone = false;' in s
assert 'removeView(ScriptWebView.this)' in s
assert 'ScriptWebView.this.destroy()' in s
