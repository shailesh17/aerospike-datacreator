## Repo snapshot for AI coding agents

This repository is a small Java-based data generator that writes and reads records to Aerospike using JSON-driven configs.

Keep guidance concise and actionable. When making changes, prefer minimal, well-tested edits and preserve public behavior.

Key places to inspect when editing behavior or adding features:
- `README.md` — primary user-facing examples and CLI usage (run jar with a config file). Use the examples/ JSON files as canonical test inputs.
- `src/main/java/com/aerospike/ps/priv/dc/GenericDataCreator.java` — program entry point and orchestration of writers/readers, metrics, and thread lifecycle.
- `src/main/java/com/aerospike/ps/priv/dc/Config.java` — JSON-mapped configuration classes (`Write`, `Read`, `BinSpec`, `Bin`) and Aerospike client creation. Changes here affect JSON config shape.
- `src/main/java/com/aerospike/ps/priv/dc/Writer.java` and `Reader.java` — core write/read logic, rate limiting, pre-generation, and how keys/bins are produced.
- `src/main/java/com/aerospike/ps/priv/dc/RecordTemplate.java` and `KeyQueue.java` — record composition and inter-thread recycling queues.

Architecture and dataflow (short):
- The app loads a JSON `Config` (see `examples/sample.json`) and constructs KeyGenerators and a set of `Writer` and `Reader` worker instances.
- `Writer` constructs `RecordTemplate` objects according to `Config.Write.binSpecs` and calls Aerospike `put` or `operate` depending on `useOperations`.
- On successful write, `Writer` notifies the `GenericDataCreator` (a `Listener`) which enqueues the created `Key` into a `KeyQueue` for potential reads/recycling.
- `Reader` requests keys from its `KeyGenerator` (or reuses keys from the queue when configured) and performs Aerospike `get` calls; hits update metrics.

Project-specific conventions and gotchas:
- JSON config shapes map directly to `Config` nested classes. Adding or renaming fields must be done carefully and kept backward compatible.
- Default host/port are in `Config.java` (127.0.0.1:3000). Tests and examples assume a local Aerospike instance unless config overrides are provided.
- Key generation is pluggable via class name strings in config (`Write.keyGenerator` / `Read.keyGenerator`). Instantiation uses reflection (Class.forName(...).getConstructor().newInstance()). Keep constructors parameterless.
- `Writer` may use `preGenerate` to pre-build templates. These are re-used when `sameRecordDifferentKey` is set — watch for shared mutable state when changing `RecordTemplate`.
- `RecordTemplate.getOps(...)` converts map bins into a single MapOperation put; operations array is cached per template — mutating `bins` after ops creation can produce inconsistencies.

Developer workflows and commands:
- Build a runnable jar (maven):
  mvn -DskipTests package
- Run locally with an example config (jar generated under `target/`):
  java -jar target/DataCreator-2.0.1-jar-with-dependencies.jar examples/sample.json
- Use the `examples/` JSON files to reproduce behavior quickly (`sample.json`, `employees.json`, `mapandlist.json`).

Testing and verification tips for agents:
- Prefer adding small unit tests around utility code (key generators, record templating) rather than large end-to-end Aerospike integration tests.
- When changing JSON shapes, update `README.md` examples and any sample files in `examples/`.
- Keep changes minimal to avoid affecting metrics naming: `GenericDataCreator.metrics.register("\u2211 " + mId + ".writes", ...)` — tests that assert metric names may break if mId changes.

Integration points and external dependencies:
- Aerospike Java client is used dire- Aerospike Java client is used directly (`com.aerospike.client.*`). Code instantiates `AerospikeClient` in `Config.getClient()`.
  ctly (`com.aerospike.client.*`). Code instantiates `AerospikeClient` in `Config.getClient()`.
- Jackson is used for JSON config mapping; `ClientPolicy` has a MixIn to ignore TLS policy during pretty printing in `GenericDataCreator`.
- Rate limiting uses Guava `RateLimiter` and metrics are from `com.codahale.metrics`.

Change heuristics for AI edits:
- For behavioral changes (read/write semantics), update `Writer.java`/`Reader.java` and add focused tests.
- For config/schema changes, update `Config.java`, `README.md`, and at least one example JSON in `examples/`.
- For performance or metrics changes, preserve metric names or add backward-compatible aliases.

If unsure, run `mvn -DskipTests package` and exercise with `examples/sample.json` before proposing a PR.

Areas I couldn't discover automatically (ask the human):
- CI commands (no CI config detected in repo). Ask where builds/PR checks run if you need to modify CI-sensitive files.
- Intended compatibility guarantees (semver policy) — ask before making breaking API/config changes.

Feedback request: please review these instructions and tell me if you'd like more detail on build/CI, test scaffolding, or hotspots to avoid when changing concurrency/templating logic.
