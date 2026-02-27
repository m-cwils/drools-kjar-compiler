# drools-kjar-compiler

A self-contained Maven project that compiles Drools `.drl` rule files (recursively from a
folder) into a deployable **KJar**, and provides a Java utility class to load that KJar and
execute rules via a `StatelessKieSession`.

## Requirements

| Tool | Version |
|------|---------|
| Java | 8+ |
| Maven | 3.5+ |
| Drools | 6.5.0.Final |

## Project structure

```
drools-kjar-compiler/
├── pom.xml
├── build-kjar.sh
├── src/
│   ├── main/
│   │   ├── java/com/example/drools/
│   │   │   ├── KJarCompiler.java   ← compile a .drl folder → KJar
│   │   │   └── KJarLoader.java     ← load a KJar → StatelessKieSession
│   │   └── resources/META-INF/
│   │       └── kmodule.xml
│   └── test/
│       ├── java/com/example/drools/
│       │   ├── KJarCompilerTest.java
│       │   └── model/Person.java
│       └── resources/sample-rules/
│           └── sample.drl
└── README.md
```

## Quick start

### 1. Build the fat jar

```bash
mvn package -DskipTests
```

### 2. Compile a folder of rules into a KJar

```bash
# Using the convenience script:
./build-kjar.sh /path/to/my-rules /path/to/output.jar

# Or directly:
java -jar target/drools-kjar-compiler-1.0.0-jar-with-dependencies.jar \
     /path/to/my-rules /path/to/output.jar
```

### 3. Load and execute the KJar from Java

```java
KJarLoader loader = new KJarLoader("/path/to/output.jar");
StatelessKieSession session = loader.newStatelessKieSession();
session.execute(Arrays.asList(myFact1, myFact2));
```

### 4. Run the tests

```bash
mvn test
```

## Key classes

### `KJarCompiler`

| Method | Description |
|--------|-------------|
| `compile(String rulesFolder, String outputJar)` | Recursively collects `.drl` files, compiles them with `KieBuilder.buildAll()`, and writes the resulting KJar bytes to disk. Throws `IllegalStateException` if there are compilation errors. |
| `collectDrlFiles(File directory)` | Returns all `.drl` files found recursively under a directory. |
| `main(String[] args)` | CLI entry point: `args[0]` = rules folder, `args[1]` = output jar path. |

### `KJarLoader`

| Method | Description |
|--------|-------------|
| `KJarLoader(String kjarPath)` | Registers the KJar with the local `KieRepository` and creates a `KieContainer`. |
| `newStatelessKieSession()` | Returns a fresh `StatelessKieSession` (using `defaultKSession`). |
| `getKieContainer()` | Returns the underlying `KieContainer` for advanced use cases. |

## Notes

- The `kmodule.xml` generated programmatically by `KJarCompiler` defines a single `kbase` that
  picks up all packages (no `packages` attribute means all packages are included) and exposes a
  `stateless` session named `defaultKSession`.
- The JBoss public Maven repository is declared in `pom.xml` to resolve Drools 6.5 artifacts.
