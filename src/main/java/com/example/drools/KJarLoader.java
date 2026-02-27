package com.example.drools;

import org.kie.api.KieServices;
import org.kie.api.builder.KieRepository;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;

import java.io.File;

/**
 * Loads a compiled KJar from the local file system and exposes a {@link StatelessKieSession}
 * ready to execute Drools rules.
 *
 * <p>Example usage:
 * <pre>{@code
 * KJarLoader loader = new KJarLoader("/path/to/rules.jar");
 * StatelessKieSession session = loader.newStatelessKieSession();
 * session.execute(Arrays.asList(myFact1, myFact2));
 * }</pre>
 */
public class KJarLoader {

    private final KieContainer kieContainer;

    /**
     * Creates a new {@code KJarLoader} that reads the KJar from the given path and registers
     * it with the local {@link KieRepository}.
     *
     * @param kjarPath path to the KJar file produced by {@link KJarCompiler}
     * @throws IllegalArgumentException if the file does not exist
     */
    public KJarLoader(String kjarPath) {
        File kjarFile = new File(kjarPath);
        if (!kjarFile.exists()) {
            throw new IllegalArgumentException("KJar file not found: " + kjarPath);
        }

        KieServices ks = KieServices.Factory.get();
        KieRepository repo = ks.getRepository();

        // Register the KJar so the container can resolve it
        org.kie.api.builder.KieModule kieModule =
                repo.addKieModule(ks.getResources().newFileSystemResource(kjarFile));

        kieContainer = ks.newKieContainer(kieModule.getReleaseId());
    }

    /**
     * Returns a new {@link StatelessKieSession} from the default KIE session defined in the
     * loaded KJar ({@code defaultKSession}).
     *
     * @return a fresh {@link StatelessKieSession}
     */
    public StatelessKieSession newStatelessKieSession() {
        return kieContainer.newStatelessKieSession("defaultKSession");
    }

    /**
     * Returns the underlying {@link KieContainer} for advanced use cases.
     *
     * @return the {@link KieContainer} wrapping the loaded KJar
     */
    public KieContainer getKieContainer() {
        return kieContainer;
    }
}
