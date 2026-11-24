package game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** All-time best scores, kept sorted and saved in a text file as "name;score" lines. */
public class Leaderboard {
    private final Path file;
    private final List<String[]> entries = new ArrayList<>();

    public Leaderboard(String fileName) {
        file = Paths.get(fileName);
        try {
            if (Files.exists(file))
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String[] p = line.split(";");
                    if (p.length == 2 && p[1].trim().matches("-?\\d+")) entries.add(p);
                }
        } catch (IOException ignored) { /* start empty */ }
    }

    public synchronized void add(String name, int score) {
        entries.add(new String[]{name.replace(";", ","), String.valueOf(score)});
        entries.sort((a, b) -> Integer.parseInt(b[1].trim()) - Integer.parseInt(a[1].trim()));
        List<String> lines = new ArrayList<>();
        for (String[] e : entries) lines.add(e[0] + ";" + e[1]);
        try { Files.write(file, lines, StandardCharsets.UTF_8); } catch (IOException ignored) { }
    }

    /** The best n entries as {name, score} pairs (for the leaderboard screen). */
    public synchronized List<String[]> topEntries(int n) {
        List<String[]> out = new ArrayList<>();
        for (int i = 0; i < Math.min(n, entries.size()); i++) out.add(entries.get(i).clone());
        return out;
    }

    public synchronized String topText(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, entries.size()); i++)
            sb.append("  ").append(i + 1).append(". ").append(entries.get(i)[0])
              .append("  ").append(entries.get(i)[1]).append('\n');
        return sb.toString();
    }
}
