package nl.leonw.competencymatrix.tools;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * One-time migration tool to split competencies.yaml into multiple files.
 * This is a test utility, not part of the main application.
 */
public class YamlSplitter {

    public static void main(String[] args) {
        String sourceFile = "src/main/resources/seed/competencies.yaml";
        String categoriesDir = "src/main/resources/seed/categories";
        String rolesDir = "src/main/resources/seed/roles";

        System.out.println("=== Competencies YAML Splitter ===");
        System.out.println("Source: " + sourceFile);
        System.out.println("Categories output: " + categoriesDir);
        System.out.println("Roles output: " + rolesDir);
        System.out.println();

        try {
            // Create output directories
            Files.createDirectories(Paths.get(categoriesDir));
            Files.createDirectories(Paths.get(rolesDir));

            // Load source YAML
            Yaml yaml = new Yaml();
            Map<String, Object> data;

            try (InputStream input = new FileInputStream(sourceFile)) {
                data = yaml.load(input);
            }

            // Split categories
            if (data.containsKey("categories")) {
                List<Map<String, Object>> categories = (List<Map<String, Object>>) data.get("categories");
                System.out.println("Found " + categories.size() + " categories");

                for (Map<String, Object> category : categories) {
                    String name = (String) category.get("name");
                    String filename = toFilename(name);
                    Path outputPath = Paths.get(categoriesDir, filename);

                    System.out.println("  Writing " + name + " -> " + filename);

                    try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                        yaml.dump(category, writer);
                    }
                }
            }

            // Split roles
            if (data.containsKey("roles")) {
                List<Map<String, Object>> roles = (List<Map<String, Object>>) data.get("roles");
                System.out.println("Found " + roles.size() + " roles");

                for (Map<String, Object> role : roles) {
                    String name = (String) role.get("name");
                    String filename = toFilename(name);
                    Path outputPath = Paths.get(rolesDir, filename);

                    System.out.println("  Writing " + name + " -> " + filename);

                    try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                        yaml.dump(role, writer);
                    }
                }
            }

            // Write progressions to separate file
            if (data.containsKey("progressions")) {
                List<Map<String, Object>> progressions = (List<Map<String, Object>>) data.get("progressions");
                System.out.println("Found " + progressions.size() + " progressions");

                Path progressionsFile = Paths.get("src/main/resources/seed/progressions.yaml");
                System.out.println("  Writing progressions -> progressions.yaml");

                try (FileWriter writer = new FileWriter(progressionsFile.toFile())) {
                    yaml.dump(progressions, writer);
                }
            }

            System.out.println("\n✓ Split completed successfully");

            // Verification
            long categoryCount = Files.list(Paths.get(categoriesDir))
                    .filter(p -> p.toString().endsWith(".yaml"))
                    .count();
            long roleCount = Files.list(Paths.get(rolesDir))
                    .filter(p -> p.toString().endsWith(".yaml"))
                    .count();

            System.out.println("\n=== Verification ===");
            System.out.println("Categories created: " + categoryCount);
            System.out.println("Roles created: " + roleCount);
            System.out.println("Progressions file: " + Files.exists(Paths.get("src/main/resources/seed/progressions.yaml")));

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Convert display name to filename (lowercase, hyphens, no special chars)
     */
    private static String toFilename(String name) {
        return name.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                + ".yaml";
    }
}
