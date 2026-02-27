package com.example.drools;

import com.example.drools.model.Person;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kie.api.runtime.StatelessKieSession;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link KJarCompiler} and {@link KJarLoader}.
 */
public class KJarCompilerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // -----------------------------------------------------------------------
    // collectDrlFiles
    // -----------------------------------------------------------------------

    @Test
    public void collectDrlFiles_findsFilesRecursively() throws Exception {
        File root = tempFolder.newFolder("rules");
        File subDir = new File(root, "sub");
        subDir.mkdirs();

        File drl1 = new File(root, "rule1.drl");
        File drl2 = new File(subDir, "rule2.drl");
        File other = new File(root, "readme.txt");

        Files.write(drl1.toPath(), "// drl1".getBytes());
        Files.write(drl2.toPath(), "// drl2".getBytes());
        Files.write(other.toPath(), "not a rule".getBytes());

        List<File> found = KJarCompiler.collectDrlFiles(root);

        assertEquals("Should find exactly 2 .drl files", 2, found.size());
        assertTrue("Should contain rule1.drl", found.contains(drl1));
        assertTrue("Should contain rule2.drl", found.contains(drl2));
        assertFalse("Should not contain readme.txt", found.contains(other));
    }

    @Test
    public void collectDrlFiles_emptyDirectory_returnsEmptyList() throws Exception {
        File root = tempFolder.newFolder("empty");
        List<File> found = KJarCompiler.collectDrlFiles(root);
        assertNotNull(found);
        assertTrue("Expected empty list for empty directory", found.isEmpty());
    }

    // -----------------------------------------------------------------------
    // compile
    // -----------------------------------------------------------------------

    @Test
    public void compile_sampleRules_producesKJar() throws Exception {
        String rulesFolder = getSampleRulesFolder();
        File outputJar = new File(tempFolder.getRoot(), "rules.jar");

        KJarCompiler.compile(rulesFolder, outputJar.getAbsolutePath());

        assertTrue("KJar file should have been created", outputJar.exists());
        assertTrue("KJar file should not be empty", outputJar.length() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void compile_nonExistentFolder_throwsIllegalArgumentException() throws IOException {
        KJarCompiler.compile("/no/such/folder", "/tmp/out.jar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void compile_folderWithNoDrlFiles_throwsIllegalArgumentException() throws Exception {
        File emptyFolder = tempFolder.newFolder("no-drls");
        KJarCompiler.compile(emptyFolder.getAbsolutePath(), tempFolder.newFile("out.jar").getAbsolutePath());
    }

    @Test(expected = IllegalStateException.class)
    public void compile_invalidDrl_throwsIllegalStateException() throws Exception {
        File rulesFolder = tempFolder.newFolder("bad-rules");
        File badDrl = new File(rulesFolder, "bad.drl");
        // A DRL with a Java type error in the RHS causes a Drools compilation error.
        String badDrlContent = "package com.example.invalid;\n"
                + "rule \"Compile Error\"\n"
                + "when\n"
                + "    eval(true)\n"
                + "then\n"
                + "    int x = \"not a valid int literal\";\n"
                + "end\n";
        Files.write(badDrl.toPath(), badDrlContent.getBytes());

        File outputJar = new File(tempFolder.getRoot(), "bad.jar");
        KJarCompiler.compile(rulesFolder.getAbsolutePath(), outputJar.getAbsolutePath());
    }

    // -----------------------------------------------------------------------
    // KJarLoader – end-to-end rule execution
    // -----------------------------------------------------------------------

    @Test
    public void kjarLoader_executeRules_marksAdultAndMinorCorrectly() throws Exception {
        String rulesFolder = getSampleRulesFolder();
        File outputJar = new File(tempFolder.getRoot(), "rules.jar");
        KJarCompiler.compile(rulesFolder, outputJar.getAbsolutePath());

        KJarLoader loader = new KJarLoader(outputJar.getAbsolutePath());
        StatelessKieSession session = loader.newStatelessKieSession();
        assertNotNull("StatelessKieSession should not be null", session);

        Person adult = new Person("Alice", 30);
        Person minor = new Person("Bob", 15);

        session.execute(Arrays.asList(adult, minor));

        assertTrue("Alice (age 30) should be marked as adult", adult.isAdult());
        assertFalse("Bob (age 15) should not be marked as adult", minor.isAdult());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String getSampleRulesFolder() {
        URL resource = getClass().getResource("/sample-rules");
        assertNotNull("sample-rules test resource must exist", resource);
        return new File(resource.getFile()).getAbsolutePath();
    }
}
