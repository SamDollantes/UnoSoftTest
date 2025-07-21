import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

public class CsvUnionGroups {
    private static final String OUTPUT_FILE = "out.txt";
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^(?:\"[^\"]*\"|[^\";]*)(?:;(?:\"[^\"]*\"|[^\";]*))*$"
    );


    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("Usage: java -jar csv-union-groups.jar <path-to-input-file>");
            System.exit(1);
        }
        String inputPath = args[0];

        Instant t0 = Instant.now();
        List<String> rawLines = Files.readAllLines(Paths.get(inputPath), StandardCharsets.UTF_8);

        List<String> validLines = new ArrayList<>();
        for (String line : rawLines) {
            if (LINE_PATTERN.matcher(line).matches()) {
                validLines.add(line);

            }
        }
        int n = validLines.size();

        if (n == 0) {
            System.out.println(0);
            return;
        }

        DSU dsu = new DSU(n);
        Map<String,Integer> firstOccurrence = new HashMap<>();

        for (int i = 0; i < n; i++) {
            String[] cols = validLines.get(i).split(";", -1);
            for (int col = 0; col < cols.length; col++) {
                String token = cols[col];
                if ("\"\"".equals(token) || token.isEmpty()) continue;
                String key = col + token;
                Integer prev = firstOccurrence.get(key);
                if (prev == null) {
                    firstOccurrence.put(key, i);
                } else {
                    dsu.union(i, prev);
                }
            }
        }

        Map<Integer, List<String>> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = dsu.find(i);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(validLines.get(i));
        }

        List<List<String>> result = new ArrayList<>(groups.values());
        result.sort((a, b) -> Integer.compare(b.size(), a.size()));

        int countNonSingleton = 0;
        try (BufferedWriter out = Files.newBufferedWriter(Paths.get(OUTPUT_FILE), StandardCharsets.UTF_8)) {
            for (List<String> group : result) {
                out.write(String.valueOf(group.size()));
                out.newLine();
                if (group.size() > 1) {
                    countNonSingleton++;
                }
                for (String row : group) {
                    out.write(row);
                    out.newLine();
                }
                out.newLine();
            }
        }
        LocalDate end = LocalDate.now();

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
            if (rank[rx] == rank[ry]) {
                rank[rx]++;
                parent[ry] = rx;
            } else if (rank[rx] > rank[ry]) {
                parent[ry] = rx;
            } else {
                parent[rx] = ry;
            }
        }
    }
}
