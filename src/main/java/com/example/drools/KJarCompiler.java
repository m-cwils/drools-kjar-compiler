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
 * Compiles all .drl files found recursively under a source directory into a KJar file.
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
            + "<kmodule xmlns=\"http://www.drools.org/xsd/kmodule\">\n"
            + "  <kbase name=\"defaultKBase\">\n"
            + "    <ksession name=\"defaultKSession\" type=\"stateless\"/>\n"
            + "  </kbase>\n"
            + "</kmodule>\n";

    /**
     * Entry point for command-line use.
     *
     * @param args {@code args[0]} – path to the folder containing .drl files;
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
     * Compiles all .drl files found recursively under {@code rulesFolderPath} into a KJar
     * and writes the resulting JAR bytes to {@code outputJarPath}.
     *
     * @param rulesFolderPath path to the folder containing .drl source files
     * @param outputJarPath   destination file for the compiled KJar
     * @throws IOException              if a .drl file cannot be read or the output cannot be written
     * @throws IllegalArgumentException if {@code rulesFolderPath} does not exist or is not a directory
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

        // Collect and add all .drl files recursively
        List<File> drlFiles = collectDrlFiles(rulesFolder);
        if (drlFiles.isEmpty()) {
            throw new IllegalArgumentException("No .drl files found under: " + rulesFolderPath);
        }

        for (File drl : drlFiles) {
            String relativePath = rulesFolder.toURI().relativize(drl.toURI()).getPath();
            String kfsPath = "src/main/resources/rules/" + relativePath;
            byte[] content = Files.readAllBytes(drl.toPath());
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
}
