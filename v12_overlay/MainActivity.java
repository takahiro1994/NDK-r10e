package com.xperiatk.lightart;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_IMPORT = 1001;
    private static final int REQ_EXPORT = 1002;

    private static final int BG = 0xff090c14;
    private static final int PANEL = 0xff141925;
    private static final int PANEL_2 = 0xff1b2130;
    private static final int BUTTON = 0xff252c3d;
    private static final int PRIMARY = 0xff405de6;
    private static final int DANGER = 0xff5b2630;
    private static final int TEXT = 0xffeef2ff;
    private static final int MUTED = 0xff9ba7bf;

    private final float[] speedValues = {0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 4.0f};
    private final String[] speedLabels = {"0.25×", "0.5×", "1×", "1.5×", "2×", "4×"};

    private ProjectStore store;
    private String pendingExport;
    private String searchQuery = "";
    private boolean favoritesOnly;

    private ProjectStore.Project editing;
    private EditText editorName;
    private EditText editorScript;

    private ArtGLSurfaceView player;
    private LinearLayout playerControls;
    private Button pauseButton;
    private Button speedButton;
    private int speedIndex = 2;
    private boolean displayOnly;
    private boolean playerReturnToEditor;
    private String playingName;
    private String playingScript;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ProjectStore(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        showProjectList();
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private Button button(String text, View.OnClickListener listener) {
        return button(text, BUTTON, listener);
    }

    private Button primaryButton(String text, View.OnClickListener listener) {
        return button(text, PRIMARY, listener);
    }

    private Button dangerButton(String text, View.OnClickListener listener) {
        return button(text, DANGER, listener);
    }

    private Button button(String text, int color, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(TEXT);
        b.setTextSize(14);
        b.setMinHeight(dp(44));
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setBackground(rounded(color, 10));
        b.setOnClickListener(listener);
        return b;
    }

    private TextView label(String text, int sp) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(TEXT);
        return v;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(12), dp(10), dp(12), dp(10));
        l.setBackgroundColor(BG);
        return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout topBar(String title, View.OnClickListener back, View.OnClickListener menu) {
        LinearLayout bar = horizontal();
        bar.setPadding(0, dp(2), 0, dp(8));
        if (back != null) {
            Button b = button("‹", back);
            b.setTextSize(27);
            bar.addView(b, fixed(dp(50), dp(46), 0, 0, dp(8), 0));
        }
        TextView t = label(title, 22);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(t, new LinearLayout.LayoutParams(0, dp(48), 1));
        if (menu != null) {
            Button b = button("☰", menu);
            b.setTextSize(20);
            bar.addView(b, fixed(dp(56), dp(46), dp(8), 0, 0, 0));
        }
        return bar;
    }

    private void normalizeWindow() {
        displayOnly = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
    }

    private void showProjectList() {
        stopPlayer();
        normalizeWindow();
        editing = null;
        editorName = null;
        editorScript = null;

        LinearLayout root = vertical();
        root.addView(topBar("LightArt Studio", null, v -> showAppMenu()));

        Button create = primaryButton("＋  新しい作品を作る", v -> chooseTemplate());
        root.addView(create, matchWrapMargins(0, 0, 0, dp(10)));

        LinearLayout searchRow = horizontal();
        EditText search = new EditText(this);
        search.setHint("作品を検索");
        search.setHintTextColor(0xff6f7990);
        search.setTextColor(TEXT);
        search.setSingleLine(true);
        search.setBackground(rounded(PANEL_2, 10));
        search.setPadding(dp(12), 0, dp(12), 0);
        search.setText(searchQuery);
        search.setSelection(search.length());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) { searchQuery = s.toString(); }
            @Override public void afterTextChanged(Editable s) {
                View holder = root.findViewWithTag("project_holder");
                View count = root.findViewWithTag("project_count");
                if (holder instanceof LinearLayout) fillProjectHolder((LinearLayout) holder, count instanceof TextView ? (TextView) count : null);
            }
        });
        searchRow.addView(search, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button filter = button(favoritesOnly ? "★" : "すべて", v -> { favoritesOnly = !favoritesOnly; showProjectList(); });
        searchRow.addView(filter, fixed(dp(72), dp(48), dp(8), 0, 0, 0));
        root.addView(searchRow, matchWrapMargins(0, 0, 0, dp(6)));

        TextView count = label("", 12);
        count.setTag("project_count");
        count.setTextColor(MUTED);
        count.setPadding(dp(4), dp(2), dp(4), dp(8));
        root.addView(count);

        LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setTag("project_holder");
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(holder);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
        fillProjectHolder(holder, count);
    }

    private void fillProjectHolder(LinearLayout holder, TextView countView) {
        holder.removeAllViews();
        String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        List<ProjectStore.Project> filtered = new ArrayList<>();
        for (ProjectStore.Project p : store.list()) {
            if (favoritesOnly && !p.favorite) continue;
            if (!q.isEmpty() && (p.name == null || !p.name.toLowerCase(Locale.ROOT).contains(q))) continue;
            filtered.add(p);
        }
        if (countView != null) countView.setText(filtered.size() + " 件" + (favoritesOnly ? "  ・ お気に入りのみ" : ""));
        if (filtered.isEmpty()) {
            TextView empty = label(favoritesOnly ? "お気に入りの作品はまだありません。" : "作品がありません。上のボタンから作成できます。", 15);
            empty.setTextColor(MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(48), dp(18), dp(48));
            holder.addView(empty);
            return;
        }
        for (ProjectStore.Project p : filtered) holder.addView(projectCard(p));
    }

    private View projectCard(ProjectStore.Project p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(rounded(PANEL, 14));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardLp);

        LinearLayout head = horizontal();
        TextView name = label((p.favorite ? "★  " : "") + (p.name == null ? "Untitled" : p.name), 18);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        head.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button more = button("⋮", v -> showProjectMenu(p));
        more.setTextSize(21);
        head.addView(more, fixed(dp(52), dp(42), dp(8), 0, 0, 0));
        card.addView(head);

        TextView date = label(new SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.JAPAN).format(new Date(p.updatedAt)), 12);
        date.setTextColor(MUTED);
        date.setPadding(0, 0, 0, dp(9));
        card.addView(date);

        LinearLayout mainActions = horizontal();
        mainActions.addView(primaryButton("▶  再生", v -> showPlayer(p.name, p.script, false)), weightedWithMargins(0, 0, dp(5), 0));
        mainActions.addView(button("編集", v -> showEditor(p.copy())), weightedWithMargins(dp(5), 0, 0, 0));
        card.addView(mainActions);

        card.setOnLongClickListener(v -> { showProjectMenu(p); return true; });
        return card;
    }

    private void showAppMenu() {
        String filterText = favoritesOnly ? "お気に入り表示を解除" : "お気に入りだけ表示";
        String[] items = {"Import", "全作品をExport", filterText, "使い方", "このアプリについて"};
        new AlertDialog.Builder(this).setTitle("メニュー").setItems(items, (d, which) -> {
            if (which == 0) openImport();
            else if (which == 1) exportAll();
            else if (which == 2) { favoritesOnly = !favoritesOnly; showProjectList(); }
            else if (which == 3) showHelp();
            else showAbout();
        }).show();
    }

    private void showProjectMenu(ProjectStore.Project p) {
        String favorite = p.favorite ? "★ お気に入りから外す" : "☆ お気に入りに追加";
        String[] items = {favorite, "複製", "Export", "構文確認", "削除"};
        new AlertDialog.Builder(this).setTitle(p.name).setItems(items, (d, which) -> {
            try {
                if (which == 0) { p.favorite = !p.favorite; store.save(p); showProjectList(); }
                else if (which == 1) { ProjectStore.Project copy = store.duplicate(p); showEditor(copy.copy()); }
                else if (which == 2) exportProject(p);
                else if (which == 3) validateScript(p.script);
                else confirmDelete(p);
            } catch (Exception e) { toastError(e); }
        }).show();
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle("使い方")
                .setMessage("1. 『新しい作品を作る』からテンプレートを選びます。\n\n" +
                        "2. 編集画面で数式を変更し、『保存して再生』を押します。\n\n" +
                        "3. 再生画面の『画面のみ』で、ボタン・ステータスバー・ナビゲーションバーを全部隠せます。作品だけの表示になります。\n\n" +
                        "4. 画面のみモード中は、画面を1回タップすると操作メニューが戻ります。\n\n" +
                        "5. Import / Export はホーム右上の☰から使えます。")
                .setPositiveButton("OK", null).show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("LightArt Studio v1.2")
                .setMessage("C製数式VM + OpenGL ES 2.0\n\n作品はJSONで保存・Import / Exportできます。再生中は画面を消灯させない設定を自動で有効にします。")
                .setPositiveButton("OK", null).show();
    }

    private void chooseTemplate() {
        String[] names = {"Spiral", "Organic Ribbon", "Wave", "Flower", "最小テンプレート"};
        new AlertDialog.Builder(this).setTitle("テンプレートを選択").setItems(names, (d, which) -> {
            ProjectStore.Project p;
            if (which == 1) p = store.newProject("Organic Ribbon", ProjectStore.ribbonScript());
            else if (which == 2) p = store.newProject("Wave", ProjectStore.waveScript());
            else if (which == 3) p = store.newProject("Flower", ProjectStore.flowerScript());
            else if (which == 4) p = store.newProject("Untitled", ProjectStore.minimalScript());
            else p = store.newProject();
            showEditor(p);
        }).show();
    }

    private void showEditor(ProjectStore.Project p) {
        stopPlayer();
        normalizeWindow();
        editing = p;

        LinearLayout root = vertical();
        root.addView(topBar("編集", v -> saveEditorAndReturn(), v -> showEditorMenu()));

        editorName = new EditText(this);
        editorName.setSingleLine(true);
        editorName.setHint("作品名");
        editorName.setHintTextColor(0xff6f7990);
        editorName.setTextColor(TEXT);
        editorName.setText(p.name);
        editorName.setBackground(rounded(PANEL_2, 10));
        editorName.setPadding(dp(12), 0, dp(12), 0);
        root.addView(editorName, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        TextView hint = label("LightArt DSL", 12);
        hint.setTextColor(MUTED);
        hint.setPadding(dp(3), dp(8), 0, dp(5));
        root.addView(hint);

        editorScript = new EditText(this);
        editorScript.setText(p.script);
        editorScript.setTextColor(0xffdbe6ff);
        editorScript.setBackground(rounded(0xff070a10, 10));
        editorScript.setGravity(Gravity.TOP | Gravity.START);
        editorScript.setTextSize(13);
        editorScript.setTypeface(Typeface.MONOSPACE);
        editorScript.setHorizontallyScrolling(true);
        editorScript.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(editorScript, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        root.addView(makeHelperBar(editorScript), matchWrapMargins(0, dp(7), 0, dp(7)));

        LinearLayout actions = horizontal();
        actions.addView(button("保存", v -> saveEditor(true)), weightedWithMargins(0, 0, dp(5), 0));
        actions.addView(primaryButton("▶  保存して再生", v -> saveAndPlay()), weightedWithMargins(dp(5), 0, 0, 0));
        root.addView(actions);
        setContentView(root);
    }

    private HorizontalScrollView makeHelperBar(EditText target) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = horizontal();
        String[] helpers = {"sin(", "cos(", "PI", "let ", "mag(", "mod(", "clamp(", "points ", "speed "};
        for (String h : helpers) {
            Button b = button(h, v -> insertAtCursor(target, h));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
            lp.setMargins(0, 0, dp(6), 0);
            row.addView(b, lp);
        }
        scroll.addView(row);
        return scroll;
    }

    private void insertAtCursor(EditText target, String text) {
        int pos = Math.max(0, target.getSelectionStart());
        target.getText().insert(pos, text);
        target.requestFocus();
    }

    private void showEditorMenu() {
        if (editing == null || editorName == null || editorScript == null) return;
        String favorite = editing.favorite ? "★ お気に入りから外す" : "☆ お気に入りに追加";
        String[] items = {"構文確認", "Export", favorite, "複製して編集", "テンプレートへ戻す"};
        new AlertDialog.Builder(this).setTitle("編集メニュー").setItems(items, (d, which) -> {
            if (which == 0) validateScript(editorScript.getText().toString());
            else if (which == 1) { updateEditingFromViews(); exportProject(editing); }
            else if (which == 2) {
                editing.favorite = !editing.favorite;
                saveEditor(false);
                Toast.makeText(this, editing.favorite ? "お気に入りに追加しました" : "お気に入りから外しました", Toast.LENGTH_SHORT).show();
            } else if (which == 3) {
                try {
                    updateEditingFromViews();
                    ProjectStore.Project copy = store.duplicate(editing);
                    showEditor(copy.copy());
                } catch (Exception e) { toastError(e); }
            } else {
                new AlertDialog.Builder(this).setTitle("テンプレートへ戻す")
                        .setMessage("現在のスクリプトを標準Spiralに置き換えます。")
                        .setNegativeButton("キャンセル", null)
                        .setPositiveButton("置き換える", (x, y) -> editorScript.setText(ProjectStore.defaultScript()))
                        .show();
            }
        }).show();
    }

    private void updateEditingFromViews() {
        if (editing == null || editorName == null || editorScript == null) return;
        String n = editorName.getText().toString().trim();
        editing.name = n.isEmpty() ? "Untitled" : n;
        editing.script = editorScript.getText().toString();
    }

    private boolean saveEditor(boolean notify) {
        if (editing == null) return false;
        updateEditingFromViews();
        try {
            store.save(editing);
            if (notify) Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            toastError(e);
            return false;
        }
    }

    private void saveEditorAndReturn() {
        saveEditor(false);
        showProjectList();
    }

    private void saveAndPlay() {
        updateEditingFromViews();
        String error = NativeArt.validate(editing.script);
        if (error != null && !error.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("構文エラー").setMessage(error).setPositiveButton("OK", null).show();
            return;
        }
        if (!saveEditor(false)) return;
        showPlayer(editing.name, editing.script, true);
    }

    private void showPlayer(String name, String script, boolean returnToEditor) {
        stopPlayer();
        normalizeWindow();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playingName = name == null ? "LightArt" : name;
        playingScript = script == null ? "" : script;
        playerReturnToEditor = returnToEditor;
        speedIndex = 2;

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.BLACK);
        player = new ArtGLSurfaceView(this, playingScript);
        player.setClickable(true);
        player.setOnClickListener(v -> { if (displayOnly) exitDisplayOnly(); });
        frame.addView(player, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        playerControls = horizontal();
        playerControls.setPadding(dp(6), dp(6), dp(6), dp(6));
        playerControls.setBackground(rounded(0xcc080a0f, 14));

        Button back = button("‹", v -> returnFromPlayer());
        back.setTextSize(25);
        playerControls.addView(back, weightedWithMargins(0, 0, dp(4), 0));

        pauseButton = button("Ⅱ", v -> togglePause());
        pauseButton.setTextSize(20);
        playerControls.addView(pauseButton, weightedWithMargins(dp(4), 0, dp(4), 0));

        speedButton = button(speedLabels[speedIndex], v -> cycleSpeed());
        playerControls.addView(speedButton, weightedWithMargins(dp(4), 0, dp(4), 0));

        playerControls.addView(primaryButton("画面のみ", v -> enterDisplayOnly()), weightedWithMargins(dp(4), 0, dp(4), 0));

        Button more = button("⋮", v -> showPlayerMenu());
        more.setTextSize(21);
        playerControls.addView(more, weightedWithMargins(dp(4), 0, 0, 0));

        FrameLayout.LayoutParams controlLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        controlLp.setMargins(dp(9), dp(9), dp(9), dp(9));
        frame.addView(playerControls, controlLp);

        setContentView(frame);
    }

    private void togglePause() {
        if (player == null) return;
        boolean next = !player.isPaused();
        player.setPaused(next);
        if (pauseButton != null) pauseButton.setText(next ? "▶" : "Ⅱ");
    }

    private void cycleSpeed() {
        if (player == null) return;
        speedIndex = (speedIndex + 1) % speedValues.length;
        applySpeed();
    }

    private void applySpeed() {
        if (player != null) player.setSpeedScale(speedValues[speedIndex]);
        if (speedButton != null) speedButton.setText(speedLabels[speedIndex]);
    }

    private void chooseSpeed() {
        new AlertDialog.Builder(this).setTitle("再生速度").setSingleChoiceItems(speedLabels, speedIndex, (d, which) -> {
            speedIndex = which;
            applySpeed();
            d.dismiss();
        }).show();
    }

    private void showPlayerMenu() {
        String pause = player != null && player.isPaused() ? "再開" : "一時停止";
        List<String> itemList = new ArrayList<>();
        itemList.add("画面のみモード");
        itemList.add("再生速度を選ぶ");
        itemList.add("最初から再生");
        itemList.add(pause);
        itemList.add("構文確認");
        if (editing != null) itemList.add("編集画面へ戻る");
        String[] items = itemList.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle(playingName).setItems(items, (d, which) -> {
            if (which == 0) enterDisplayOnly();
            else if (which == 1) chooseSpeed();
            else if (which == 2 && player != null) player.resetAnimation();
            else if (which == 3) togglePause();
            else if (which == 4) validateScript(playingScript);
            else returnFromPlayer();
        }).show();
    }

    private void enterDisplayOnly() {
        if (player == null) return;
        displayOnly = true;
        if (playerControls != null) playerControls.setVisibility(View.GONE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        Toast.makeText(this, "画面をタップすると操作メニューが戻ります", Toast.LENGTH_SHORT).show();
    }

    private void exitDisplayOnly() {
        displayOnly = false;
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (playerControls != null) playerControls.setVisibility(View.VISIBLE);
    }

    private void returnFromPlayer() {
        boolean toEditor = playerReturnToEditor && editing != null;
        stopPlayer();
        normalizeWindow();
        if (toEditor) showEditor(editing.copy());
        else showProjectList();
    }

    private void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
        playerControls = null;
        pauseButton = null;
        speedButton = null;
    }

    private void validateScript(String script) {
        String err = NativeArt.validate(script);
        new AlertDialog.Builder(this)
                .setTitle(err == null || err.isEmpty() ? "構文OK" : "構文エラー")
                .setMessage(err == null || err.isEmpty() ? "Cエンジンで正常にコンパイルできます。" : err)
                .setPositiveButton("OK", null).show();
    }

    private void confirmDelete(ProjectStore.Project p) {
        new AlertDialog.Builder(this).setTitle("削除")
                .setMessage("『" + p.name + "』を削除しますか？")
                .setNegativeButton("キャンセル", null)
                .setPositiveButton("削除", (d, w) -> { store.delete(p); showProjectList(); })
                .show();
    }

    private void openImport() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        startActivityForResult(i, REQ_IMPORT);
    }

    private void exportProject(ProjectStore.Project p) {
        try {
            pendingExport = store.exportOne(p);
            createExportDocument(safeName(p.name) + ".lightart.json");
        } catch (Exception e) { toastError(e); }
    }

    private void exportAll() {
        try {
            pendingExport = store.exportAll();
            createExportDocument("LightArtStudio-backup.json");
        } catch (Exception e) { toastError(e); }
    }

    private void createExportDocument(String filename) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(i, REQ_EXPORT);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == REQ_IMPORT) {
                int n = store.importJson(readUri(uri));
                Toast.makeText(this, n + "件Importしました", Toast.LENGTH_SHORT).show();
                showProjectList();
            } else if (requestCode == REQ_EXPORT) {
                try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                    if (out == null) throw new IllegalStateException("出力先を開けません");
                    out.write((pendingExport == null ? "" : pendingExport).getBytes(StandardCharsets.UTF_8));
                }
                Toast.makeText(this, "Exportしました", Toast.LENGTH_SHORT).show();
                pendingExport = null;
            }
        } catch (Exception e) { toastError(e); }
    }

    private String readUri(Uri uri) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IllegalStateException("ファイルを開けません");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    private LinearLayout.LayoutParams weightedWithMargins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private LinearLayout.LayoutParams matchWrapMargins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private LinearLayout.LayoutParams fixed(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private void toastError(Exception e) {
        Toast.makeText(this, e.getMessage() == null ? e.toString() : e.getMessage(), Toast.LENGTH_LONG).show();
    }

    private String safeName(String s) {
        return (s == null ? "LightArt" : s).replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onPause() {
        if (player != null) player.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (player != null) player.onResume();
    }

    @Override public void onBackPressed() {
        if (player != null) {
            if (displayOnly) exitDisplayOnly();
            else returnFromPlayer();
        } else if (editing != null) {
            saveEditorAndReturn();
        } else {
            super.onBackPressed();
        }
    }
}
