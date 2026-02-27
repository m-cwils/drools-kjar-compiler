# drools-kjar-compiler

A self-contained Maven project that compiles Drools `.drl` rule files (recursively from a
folder) into a deployable **KJar**, and provides a Java utility class to load that KJar and
execute rules via a `StatelessKieSession`.

DSL (Domain Specific Language) and DSLR (Domain Specific Language Rule) files are also
supported. `.dsl` files define the language mappings, and `.dslr` files contain rules written
in that domain-specific language. Both file types are collected and compiled together with
`.drl` files.

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
│   │   │   ├── KJarCompiler.java   ← compile a rules folder (.drl/.dsl/.dslr) → KJar
│   │   │   └── KJarLoader.java     ← load a KJar → StatelessKieSession
│   │   └── resources/META-INF/
│   │       └── kmodule.xml
│   └── test/
│       ├── java/com/example/drools/
│       │   ├── KJarCompilerTest.java
│       │   └── model/Person.java
│       └── resources/
│           ├── sample-rules/
│           │   └── sample.drl
│           └── sample-dsl-rules/
│               ├── person.dsl
│               └── person.dslr
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
| `compile(String rulesFolder, String outputJar)` | Recursively collects `.drl`, `.dsl`, and `.dslr` files, compiles them with `KieBuilder.buildAll()`, and writes the resulting KJar bytes to disk. Throws `IllegalStateException` if there are compilation errors. |
| `collectDrlFiles(File directory)` | Returns all `.drl` files found recursively under a directory. |
| `collectRulesFiles(File directory)` | Returns all `.drl`, `.dsl`, and `.dslr` files found recursively under a directory. |
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

## DSL and DSLR support

Drools' Domain Specific Language (DSL) feature lets you write rules in a more business-friendly
syntax. Two file types work together:

- **`.dsl`** – defines the language mappings between business phrases and DRL patterns/actions.
- **`.dslr`** – contains rules written using those phrases, referencing the DSL via an
  `expander` directive.

Both file types are discovered by `KJarCompiler` and written into the `KieFileSystem` before
compilation. Example `.dslr`:

```
package com.example.rules;
expander person.dsl
import com.example.drools.model.Person;

rule "Mark Adult via DSL"
when
    there is a person aged 18 or older
then
    mark the person as an adult
end
```
