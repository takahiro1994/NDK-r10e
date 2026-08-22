package com.xperiatk.lightart;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class ProjectStore {
    static final String FORMAT = "LightArtProject";
    static final int VERSION = 3;

    static final class Project {
        String id;
        String name;
        String script;
        long updatedAt;
        boolean favorite;

        Project copy() {
            Project p = new Project();
            p.id = id; p.name = name; p.script = script; p.updatedAt = updatedAt; p.favorite = favorite;
            return p;
        }
    }

    private final File dir;

    ProjectStore(Context context) {
        dir = new File(context.getFilesDir(), "projects");
        if (!dir.exists()) dir.mkdirs();
    }

    Project newProject() { return newProject("Spiral", defaultScript()); }

    Project newProject(String name, String script) {
        Project p = new Project();
        p.id = UUID.randomUUID().toString();
        p.name = name;
        p.script = script;
        p.updatedAt = System.currentTimeMillis();
        return p;
    }

    Project duplicate(Project src) throws IOException, JSONException {
        Project p = src.copy();
        p.id = UUID.randomUUID().toString();
        p.name = (src.name == null ? "Art" : src.name) + " copy";
        p.favorite = false;
        save(p);
        return p;
    }

    void save(Project p) throws IOException, JSONException {
        if (p.id == null || p.id.isEmpty()) p.id = UUID.randomUUID().toString();
        p.updatedAt = System.currentTimeMillis();
        writeUtf8(fileFor(p.id), toJson(p).toString(2));
    }

    void delete(Project p) { if (p != null && p.id != null) fileFor(p.id).delete(); }

    List<Project> list() {
        List<Project> out = new ArrayList<>();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return out;
        for (File f : files) {
            try { out.add(fromJson(new JSONObject(readUtf8(f)))); } catch (Exception ignored) {}
        }
        Collections.sort(out, (a, b) -> {
            if (a.favorite != b.favorite) return a.favorite ? -1 : 1;
            return Long.compare(b.updatedAt, a.updatedAt);
        });
        return out;
    }

    String exportOne(Project p) throws JSONException { return toJson(p).toString(2); }

    String exportAll() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("format", "LightArtBundle"); root.put("version", VERSION); root.put("exportedAt", System.currentTimeMillis());
        JSONArray arr = new JSONArray();
        for (Project p : list()) arr.put(toJson(p));
        root.put("projects", arr);
        return root.toString(2);
    }

    int importJson(String text) throws JSONException, IOException {
        JSONObject root = new JSONObject(text); int count = 0;
        if ("LightArtBundle".equals(root.optString("format"))) {
            JSONArray arr = root.optJSONArray("projects");
            if (arr == null) throw new JSONException("projects array is missing");
            for (int i = 0; i < arr.length(); i++) {
                Project p = fromJson(arr.getJSONObject(i)); p.id = UUID.randomUUID().toString(); save(p); count++;
            }
        } else {
            Project p = fromJson(root); p.id = UUID.randomUUID().toString(); save(p); count = 1;
        }
        return count;
    }

    private File fileFor(String id) { return new File(dir, id.replaceAll("[^A-Za-z0-9_-]", "_") + ".json"); }

    private JSONObject toJson(Project p) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("format", FORMAT); o.put("version", VERSION); o.put("id", p.id);
        o.put("name", p.name == null ? "Untitled" : p.name); o.put("script", p.script == null ? "" : p.script);
        o.put("updatedAt", p.updatedAt); o.put("favorite", p.favorite);
        return o;
    }

    private Project fromJson(JSONObject o) throws JSONException {
        if (!FORMAT.equals(o.optString("format"))) throw new JSONException("Unsupported format: " + o.optString("format"));
        Project p = new Project();
        p.id = o.optString("id", UUID.randomUUID().toString()); p.name = o.optString("name", "Imported Art");
        p.script = o.getString("script"); p.updatedAt = o.optLong("updatedAt", System.currentTimeMillis()); p.favorite = o.optBoolean("favorite", false);
        return p;
    }

    static String readUtf8(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()]; int off = 0;
            while (off < buf.length) { int n = in.read(buf, off, buf.length - off); if (n < 0) break; off += n; }
            return new String(buf, 0, off, StandardCharsets.UTF_8);
        }
    }

    static void writeUtf8(File f, String text) throws IOException {
        try (FileOutputStream out = new FileOutputStream(f)) { out.write(text.getBytes(StandardCharsets.UTF_8)); }
    }

    static String defaultScript() {
        return "# LightArt DSL v1.2\ncanvas 400 400\nbackground 9\nstroke 255 96\npointSize 1.0\npoints 10000\nspeed PI/80\n\n" +
                "let u = i/285\nlet k = 5*cos(i/44)\nlet e = u/2-15\nlet d = mag(k,e)/3\nlet c = d/2-t/3+mod(i,2)*9\n" +
                "x = (79+d*d+k*k)*sin(c)+200\n" +
                "y = 99*cos(c/3)+9/d*sin(k*2)+u/(77*sin(e/2)+0.0001)*k*e+d*d*cos(t*3-d*d/4)+200\n";
    }

    static String minimalScript() {
        return "# Minimal\ncanvas 400 400\nbackground 9\nstroke 255 180\npointSize 1.5\npoints 3000\nspeed PI/80\n\n" +
                "let a = i/45+t\nlet r = 110\nx = 200+r*cos(a)\ny = 200+r*sin(a)\n";
    }

    static String ribbonScript() {
        return "# Organic Ribbon\ncanvas 400 400\nbackground 9\nstroke 255 96\npointSize 1.0\npoints 10000\nspeed PI/60\n\n" +
                "let u = i/295\nlet k = 4*cos(i/29)\nlet e = u/5-13\nlet d = mag(k,e)-4\nlet c = d-t/3\n" +
                "x = (d*d/0.7-k*k*2+u)*cos(c)+200\n" +
                "y = 3*sin(k*2)+cos(u)/(k+0.0001)+u/9*k*(3+sin(e*9-d*3+t))+79*sin(c/3)+200+d*d/3*cos(t-d*d/9)\n";
    }

    static String waveScript() {
        return "# Wave\ncanvas 400 400\nbackground 5 8 18\nstroke 70 210 255 180\npointSize 2\npoints 5000\nspeed PI/120\n\n" +
                "let a = i/55\nlet r = 115+35*sin(i/97+t)\nx = 200+r*cos(a)\ny = 200+r*sin(a)+22*sin(a*5-t*2)\n";
    }

    static String flowerScript() {
        return "# Flower\ncanvas 400 400\nbackground 8\nstroke 255 90 190 170\npointSize 1.7\npoints 8000\nspeed PI/100\n\n" +
                "let a = i/110\nlet r = 90+60*sin(5*a+t)\nx = 200+r*cos(a+t/6)\ny = 200+r*sin(a+t/6)\n";
    }
}
