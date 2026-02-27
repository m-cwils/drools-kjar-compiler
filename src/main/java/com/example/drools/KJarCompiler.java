package com.example.drools;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Compiles all .drl, .dsl, and .dslr files found recursively under a source directory into a
 * KJar file.
 *
 * <p>Usage from the command line:
 * <pre>
 *   java -jar drools-kjar-compiler-with-dependencies.jar &lt;rules-folder&gt; &lt;output.jar&gt;
 * </pre>
 */
public class KJarCompiler {

    /** Default KIE group identifier used when generating the kmodule release id. */
    static final String DEFAULT_GROUP_ID = "com.example";

    /** Default KIE artifact identifier used when generating the kmodule release id. */
    static final String DEFAULT_ARTIFACT_ID = "rules";

    /** Default KIE version used when generating the kmodule release id. */
    static final String DEFAULT_VERSION = "1.0.0";

    private static final String KMODULE_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<kmodule xmlns=\"http://jboss.org/kie/6.0.0/kmodule\">\n"
            + "  <kbase name=\"defaultKBase\">\n"
            + "    <ksession name=\"defaultKSession\" type=\"stateless\"/>\n"
            + "  </kbase>\n"
            + "</kmodule>\n";

    /*
     * Mitigate the XXE vulnerability present in drools-core <= 7.59.0.Final (including 6.5.x).
     * The three JAXP system properties below instruct every XML parser in the JVM to refuse
     * connections to external DTDs, schemas and stylesheets, eliminating the external-entity
     * resolution path that the vulnerability relies on.  Empty string means "no protocol
     * allowed" per the JAXP specification (JEP 185 / Java 7u45+).
     * This static block runs before any Drools class that parses XML is initialised.
     */
    static {
        System.setProperty("javax.xml.accessExternalDTD", "");
        System.setProperty("javax.xml.accessExternalSchema", "");
        System.setProperty("javax.xml.accessExternalStylesheet", "");
    }

    /**
     * Entry point for command-line use.
     *
     * @param args {@code args[0]} – path to the folder containing rule files (.drl/.dsl/.dslr);
     *             {@code args[1]} – path where the resulting KJar should be written.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: KJarCompiler <rules-folder> <output.jar>");
            System.exit(1);
        }
        compile(args[0], args[1]);
        System.out.println("KJar written to: " + args[1]);
    }

    /**
     * Compiles all .drl, .dsl, and .dslr files found recursively under {@code rulesFolderPath}
     * into a KJar and writes the resulting JAR bytes to {@code outputJarPath}.
     *
     * <p>.dsl files define the domain-specific language mappings. .dslr files contain rules
     * written in that domain-specific language (they reference the corresponding .dsl file via
     * the {@code expander} directive). .drl files are standard Drools rule files.
     *
     * @param rulesFolderPath path to the folder containing rule source files
     * @param outputJarPath   destination file for the compiled KJar
     * @throws IOException              if a rule file cannot be read or the output cannot be written
     * @throws IllegalArgumentException if {@code rulesFolderPath} does not exist, is not a
     *                                  directory, or contains no .drl or .dslr files
     * @throws IllegalStateException    if the Drools compilation produces errors
     */
    public static void compile(String rulesFolderPath, String outputJarPath) throws IOException {
        File rulesFolder = new File(rulesFolderPath);
        if (!rulesFolder.exists() || !rulesFolder.isDirectory()) {
            throw new IllegalArgumentException(
                    "Rules folder does not exist or is not a directory: " + rulesFolderPath);
        }

        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        // Write the kmodule.xml descriptor
        kfs.writeKModuleXML(KMODULE_XML);

        // Collect and add all rules files (DRL, DSL, DSLR) recursively
        List<File> rulesFiles = collectRulesFiles(rulesFolder);
        boolean hasRules = false;
        for (File f : rulesFiles) {
            String name = f.getName();
            if (name.endsWith(".drl") || name.endsWith(".dslr")) {
                hasRules = true;
                break;
            }
        }
        if (!hasRules) {
            throw new IllegalArgumentException("No .drl or .dslr files found under: " + rulesFolderPath);
        }

        for (File file : rulesFiles) {
            String relativePath = rulesFolder.toURI().relativize(file.toURI()).getPath();
            String kfsPath = "src/main/resources/rules/" + relativePath;
            byte[] content = Files.readAllBytes(file.toPath());
            kfs.write(kfsPath, content);
        }

        // Compile
        KieBuilder kieBuilder = ks.newKieBuilder(kfs);
        kieBuilder.buildAll();

        Results results = kieBuilder.getResults();
        List<Message> errors = results.getMessages(Message.Level.ERROR);
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Drools compilation errors:\n");
            for (Message msg : errors) {
                sb.append("  [ERROR] ").append(msg.getText()).append('\n');
            }
            throw new IllegalStateException(sb.toString());
        }

        // Extract and save the KJar bytes
        InternalKieModule kieModule = (InternalKieModule) kieBuilder.getKieModule();
        byte[] jarBytes = kieModule.getBytes();

        Path outputPath = Paths.get(outputJarPath);
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.write(outputPath, jarBytes);
    }

    /**
     * Recursively collects all files with a {@code .drl} extension under the given directory.
     *
     * @param directory root directory to search
     * @return list of .drl files (never {@code null})
     */
    static List<File> collectDrlFiles(File directory) {
        List<File> result = new ArrayList<>();
        collectDrlFilesRecursive(directory, result);
        return result;
    }

    private static void collectDrlFilesRecursive(File dir, List<File> result) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                collectDrlFilesRecursive(entry, result);
            } else if (entry.getName().endsWith(".drl")) {
                result.add(entry);
            }
        }
    }

    /**
     * Recursively collects all rule-related files ({@code .drl}, {@code .dsl}, {@code .dslr})
     * under the given directory.
     *
     * @param directory root directory to search
     * @return list of rule files (never {@code null})
     */
    static List<File> collectRulesFiles(File directory) {
        List<File> result = new ArrayList<>();
        collectRulesFilesRecursive(directory, result);
        return result;
    }

    private static void collectRulesFilesRecursive(File dir, List<File> result) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                collectRulesFilesRecursive(entry, result);
            } else {
                String name = entry.getName();
                if (name.endsWith(".drl") || name.endsWith(".dsl") || name.endsWith(".dslr")) {
                    result.add(entry);
                }
            }
        }
    }
}
