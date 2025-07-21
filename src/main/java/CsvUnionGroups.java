import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

public class CsvUnionGroups {
    private static final String OUTPUT_FILE = "out.txt";
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^(?:\"[^\"]*\"|[^\";]*)(?:;(?:\"[^\"]*\"|[^\";]*))*$"
    );

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java -jar <NameOfFile>.jar <path-to-input-file>");
            System.exit(1);
        }
        Path input = Paths.get(args[0]);
        if (!Files.exists(input) || !Files.isReadable(input)) {
            System.err.println("File not found or unreadable: " + input);
            System.exit(1);
        }

        Instant t0 = Instant.now();

        // 1) Подсчёт валидных строк
        long validCount;
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            validCount = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (LINE_PATTERN.matcher(line).matches()) {
                    validCount++;
                }
            }
        }
        int n = (int) validCount;
        if (n == 0) {
            System.out.println(0);
            return;
        }

        DSU dsu = new DSU(n);
        Map<String, Integer> firstOccurrence = new HashMap<>(n * 2);
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            int id = 0;
            while ((line = reader.readLine()) != null) {
                if (!LINE_PATTERN.matcher(line).matches()) continue;
                String[] cols = line.split(";", -1);
                for (int col = 0; col < cols.length; col++) {
                    String raw = cols[col];
                    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
                        raw = raw.substring(1, raw.length() - 1);
                    }
                    if (raw.isEmpty()) continue;
                    String key = col + "|" + raw;
                    Integer prev = firstOccurrence.putIfAbsent(key, id);
                    if (prev != null) {
                        dsu.union(id, prev);
                    }
                }
                id++;
            }
        }

        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = dsu.find(i);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(i);
        }
        List<List<Integer>> result = new ArrayList<>();
        for (List<Integer> g : groups.values()) {
            if (g.size() > 1) result.add(g);
        }
        result.sort((a, b) -> Integer.compare(b.size(), a.size()));

        int[] idToGroup = new int[n];
        Arrays.fill(idToGroup, -1);
        for (int gi = 0; gi < result.size(); gi++) {
            for (int idx : result.get(gi)) {
                idToGroup[idx] = gi;
            }
        }

        int countNonSingleton = result.size();
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             BufferedWriter out = Files.newBufferedWriter(Paths.get(OUTPUT_FILE), StandardCharsets.UTF_8)) {

            String line;
            int id = 0;
            boolean[] headerWritten = new boolean[result.size()];
            while ((line = reader.readLine()) != null) {
                if (!LINE_PATTERN.matcher(line).matches()) continue;
                int gi = idToGroup[id++];
                if (gi >= 0) {
                    if (!headerWritten[gi]) {
                        out.write(String.valueOf(result.get(gi).size()));
                        out.newLine();
                        headerWritten[gi] = true;
                    }
                    out.write(line);
                    out.newLine();
                }
            }
        }

        Duration elapsed = Duration.between(t0, Instant.now());
        System.out.println(elapsed.toSeconds());
        System.out.println(countNonSingleton);
    }

    static class DSU {
        private final int[] parent;
        private final int[] rank;
        public DSU(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
        }
        public int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]);
            return parent[x];
        }
        public void union(int x, int y) {
            int rx = find(x), ry = find(y);
            if (rx == ry) return;
            if (rank[rx] < rank[ry]) {
                parent[rx] = ry;
            } else if (rank[rx] > rank[ry]) {
                parent[ry] = rx;
            } else {
                parent[ry] = rx;
                rank[rx]++;
            }
        }
    }
}
