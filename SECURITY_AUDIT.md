# Security Audit Report — DodgeGame

| | |
|---|---|
| **Project** | DodgeGame (internal save namespace: `dotch_*`) |
| **Audit date** | 2026-06-30 |
| **Auditor** | Automated multi-agent security review (Claude Code) |
| **Scope** | 39 Java source files (~12,250 LOC) under `src/`, bundled third-party JARs in `libs/jars/`, bundled native binaries in `libs/lwjgl/native/`, and IDE/build configuration |
| **Commit / Branch** | `main` (HEAD `09346c4`) |
| **Stack** | Java 8, AWT/Swing rendering, LWJGL 2.x, Slick2D, JOrbis/JOgg (OGG audio), JInput |

---

## 1. Executive Summary

DodgeGame is a local, offline, single-player 2D dodge game. It has **no network code, no server, no multiplayer, no authentication, and no Java native (`ObjectInputStream`) deserialization** — eliminating the entire class of remote and gadget-chain remote-code-execution risk by construction. All persistent state is stored as length/version-prefixed binary via `DataInputStream`/`DataOutputStream`. The dominant realistic attack surface is therefore the parsing of crafted/tampered `.dat` save files (a victim handed a "shared save," or a local user editing their own file).

The review confirmed **one Medium and three Low** findings, all concentrated in `Profile.java`. The root cause is a single class: `Profile.load()` parses an untrusted file with no value validation, allocates an array directly from an attacker-controlled 32-bit count, and wraps the whole parse in an `IOException`-only `catch` while running inside a class **static initializer**. The combination turns ordinary malformed-input failures into uncaught `RuntimeException`/`Error` that escape as `ExceptionInInitializerError` and crash the game **on every launch** until the offending file is deleted. The impact is bounded to local denial of service (crash / freeze) on the victim's own machine and is fully recoverable by deleting the regenerable `.dat` file — there is no code execution, data exfiltration, memory corruption, or privilege escalation anywhere in the verified set.

### Severity counts

| Severity | Count |
|----------|:-----:|
| Critical | 0 |
| High     | 0 |
| Medium   | 1 |
| Low      | 3 |
| Info     | 0 |

---

## 2. Scope & Methodology

The following were reviewed:

- **39 Java source files (~12.2k LOC)** under `src/` — game loop, rendering, input, persistence, audio, and asset loading.
- **Bundled dependencies** in `libs/jars/`: `lwjgl.jar`, `slick.jar`, `jorbis-0.0.15.jar`, `jogg-0.0.7.jar`, `jinput.jar`.
- **Bundled native binaries** in `libs/lwjgl/native/{windows,linux,macosx,solaris}` and the `-Djava.library.path` configuration that loads them.
- **Build / IDE configuration** (`.idea/`, `*.iml`, `README.md`) — there is no Maven/Gradle/Ant build.

Findings were produced across **nine review dimensions**: malformed-save parsing (DoS/OOM/memory safety), save-data tampering & integrity, untrusted input parsing & robustness, native-library loading, media decoding, dependency/EOL status, file/path handling, randomness/anti-cheat, and general error handling. Every candidate finding was **adversarially verified against the actual source** (file and line cited), and inflated or non-security observations were dismissed (see Appendix A). Reported severities are the **adjusted severities** after that verification pass.

---

## 3. Threat Model

**DodgeGame is a local, single-player, offline desktop game.** There is no server, no multiplayer, no remote endpoint, and no authentication. Severity is calibrated against that reality — issues that would be High/Critical in a networked service are routinely Low/Info here because there is no remote attacker and no cross-user trust boundary.

The realistic trust boundaries are:

- **(a) Save/config `.dat` files** — the primary surface. An attacker could hand a victim a crafted `.dat` (a shared "save" or modded profile), or a local user could tamper to cheat. Files are stored as **relative paths in the current working directory**: `dotch_profile.dat`, `dotch_settings.dat`, `dotch_achievements.dat`, `dotch_daily.dat`, `dotch_stats.dat` (legacy). Crafted-file parsing must be robust against OOM, huge/negative allocations, infinite loops, and array-index errors. **All confirmed findings live here.**
- **(b) Native library loading on Windows** — DLL search-order / planting if natives are resolved from CWD or a writable relative path. No application code performs native loading; resolution is delegated to the launch-time `-Djava.library.path`.
- **(c) Media decoding** — OGG (JOrbis/JOgg) and image (ImageIO) decoders are EOL and historically CVE-prone, but here **all media is bundled** (hardcoded asset paths), so this is weighted down accordingly.
- **(d) Outdated/EOL dependencies** — LWJGL 2.x, Slick2D, JOrbis, JOgg, JInput are all unmaintained; noted as inventory/hygiene risk (Section 6).

**Explicitly out of scope as RCE:** there is no `ObjectInputStream`/`Serializable` deserialization in this codebase — persistence is binary `DataInputStream`. No gadget-chain RCE claim is made. Save tampering for self-cheating is treated as Low/Info because the "attacker" and "victim" are the same local user and no trust boundary is crossed.

---

## 4. Findings

Ordered most-severe first. All four findings reside in `src/Profile.java`, the only persistence loader that allocates from a file-controlled count and indexes arrays with file-controlled values.

---

### [MEDIUM] Unbounded length-prefixed allocation in `Profile.load()` — crafted profile causes persistent crash-on-launch (OOM / NegativeArraySizeException)

| | |
|---|---|
| **Severity** | Medium (adjusted) |
| **CWE** | CWE-789: Memory Allocation with Excessive Size Value |
| **Location** | `src/Profile.java:436-438` (allocation), amplified by `:442` (catch) and `:59` (static init) |
| **Confidence** | High — confirmed in code across three independent review dimensions |

**Description.** In the `version >= 3` block of `Profile.load()`, a 32-bit item count is read directly from the file and used as an array size with **zero validation**:

```java
// src/Profile.java:435-439
coins = in.readLong();
int itemCount = in.readInt();                 // attacker-controlled, unvalidated
boolean[] purchased = new boolean[itemCount]; // line 437 — allocate before any further read
for (int i = 0; i < itemCount; i++) purchased[i] = in.readBoolean();
CoinShop.loadPurchased(purchased);
```

`itemCount` is never bounded against the only legitimate value (`CoinShop.ITEM_COUNT`, 25). The allocation happens **before** the read loop, so the file does not need to be large to trigger it — only the 4-byte count drives the allocation size.

**Impact.** Two deterministic failure modes from a single crafted byte sequence:
- A value near `Integer.MAX_VALUE` → an attempted ~2 GB `boolean[]` allocation → `OutOfMemoryError` (an `Error`).
- A negative value (e.g. `0x80000000`) → `NegativeArraySizeException` (a `RuntimeException`) — heap-independent and fully reliable.

Neither is an `IOException`, so the `catch (IOException e)` at line 442 does not catch it. Because `load()` runs from the static initializer `static { load(); }` (line 59-61), the escaping throwable becomes an **`ExceptionInInitializerError`** the first time the `Profile` class is touched — early in startup. Since `load()` never completes, the file is never rewritten, so **the crash recurs on every launch** (subsequent accesses throw `NoClassDefFoundError`) until the user manually deletes `dotch_profile.dat`. The downstream consumer `CoinShop.loadPurchased` is itself safely bounded (`Math.min(data.length, ITEM_COUNT)`), but it runs *after* the unsafe allocation, so it cannot prevent the crash.

This maps directly to trust boundary **(a)** — a crafted/shared save handed to a victim. The persistent (self-perpetuating) nature of the DoS is what raises it from Low to **Medium**; an ordinary recoverable parse crash would be Low.

**Evidence.**
```text
src/Profile.java:436   int itemCount = in.readInt();
src/Profile.java:437   boolean[] purchased = new boolean[itemCount];
src/Profile.java:438   for (int i = 0; i < itemCount; i++) purchased[i] = in.readBoolean();
src/Profile.java:442   } catch (IOException e) {   // misses OOM / NegativeArraySizeException
src/Profile.java:59    static { load(); }          // turns the escape into ExceptionInInitializerError
```

**Exploit / Failure Scenario.** An attacker crafts a `dotch_profile.dat`: `writeInt(version=3)`, then all valid v1/v2 fields, then `writeLong(coins)`, then `writeInt(-1)` (or `writeInt(Integer.MAX_VALUE)`) as `itemCount`. The victim places this "shared save" in the game's working directory and launches. At `Profile.java:437`, `new boolean[-1]` throws `NegativeArraySizeException`, which escapes the `IOException` handler, propagates out of the static initializer as `ExceptionInInitializerError`, and crashes the game at startup. The game remains unlaunchable on every subsequent run until the user locates and deletes the file.

**Remediation.** Validate `itemCount` before allocating, and never size an array directly from a raw `readInt()`:
```java
int itemCount = in.readInt();
if (itemCount < 0 || itemCount > CoinShop.ITEM_COUNT) throw new IOException("bad item count");
boolean[] purchased = new boolean[itemCount];
```
Mirror the safe pattern already used in `Achievements.load()` (clamp the populate count with `Math.min`, EOF-bound any surplus reads). Additionally broaden the `catch` (see next finding) so any malformed field falls back to defaults instead of killing the static initializer.

---

### [LOW] `Profile.load()` catches only `IOException`, so RuntimeException/Error from crafted input escapes the static initializer

| | |
|---|---|
| **Severity** | Low (adjusted) |
| **CWE** | CWE-755: Improper Handling of Exceptional Conditions |
| **Location** | `src/Profile.java:442` |
| **Confidence** | High — verified in code |

**Description.** `load()` wraps the entire parse in `try { ... } catch (IOException e) {}`, with the comment "Corrupted or old format — defaults are fine." But every non-IO failure a crafted file can induce — `NegativeArraySizeException`/`OutOfMemoryError` from line 437, and `ArrayIndexOutOfBoundsException` from unvalidated history indices used later — is a `RuntimeException`/`Error`, **not** an `IOException`. This narrow catch is the amplifier that converts otherwise-recoverable parse failures into hard crashes that defeat the documented "defaults are fine" fallback.

**Impact.** A malformed `dotch_profile.dat` that *should* degrade gracefully to default state instead aborts class initialization (via `static { load(); }` at line 59) and terminates the game on startup, or crashes later at runtime when the unvalidated values are used. The same `IOException`-only idiom is repeated in the other loaders (`Settings`, `Achievements`, `DailyChallenge`, `Stats`), each running in a static initializer, so the pattern is systemic.

**Evidence.**
```java
// src/Profile.java:442-444
} catch (IOException e) {
    // Corrupted or old format — defaults are fine
}
```

**Exploit / Failure Scenario.** Identical delivery to the Medium finding: a crafted `version=3` profile with `itemCount = -1`. On startup, `new boolean[-1]` throws `NegativeArraySizeException`, which is not an `IOException`, escapes line 442, and surfaces as `ExceptionInInitializerError`, crashing the game at launch rather than loading defaults.

**Remediation.** Broaden to `catch (IOException | RuntimeException e)` so a single bad field can never crash startup, and prevent the `OutOfMemoryError` path by bounds-checking the allocation (previous finding) rather than relying on catching `Error`. Apply the same broadening to the other static-initializer loaders.

---

### [LOW] Unvalidated `historyIndex` / `historyCount` drive `ArrayIndexOutOfBoundsException` at run-end and in the stats UI

| | |
|---|---|
| **Severity** | Low (adjusted) |
| **CWE** | CWE-129: Improper Validation of Array Index |
| **Location** | `src/Profile.java:422-423` (load); used at `:191-193` and `:275` |
| **Confidence** | High — both vectors confirmed in code |

**Description.** `historyIndex` (line 422) and `historyCount` (line 423) are read straight from the file with **no clamp** to `[0, HISTORY_SIZE = 20)`. Two distinct vectors result:

1. **Run-end write (`endRun`).** `historyIndex` is used as a direct array index *before* it is recomputed modulo `HISTORY_SIZE`:
   ```java
   // src/Profile.java:191-194
   recentScores[historyIndex] = score;          // no bounds check
   recentLevels[historyIndex] = levelReached;
   recentDifficulty[historyIndex] = difficulty;
   historyIndex = (historyIndex + 1) % HISTORY_SIZE; // normalization comes too late
   ```
   A loaded value outside `0..19` (e.g. 50 or -5) throws `ArrayIndexOutOfBoundsException` on the very first run-end.

2. **Stats UI read (`getRecentScore/Level/Difficulty`).** These compute a modular index guarded only by `ago >= historyCount`:
   ```java
   // src/Profile.java:274-276
   if (ago >= historyCount) return 0;
   int idx = (historyIndex - 1 - ago + HISTORY_SIZE * 2) % HISTORY_SIZE;
   return recentScores[idx];
   ```
   The Recent Runs panel (`Menu.java:1557`, `:1565`) loops `for (i = 0; i < getHistoryCount(); i++)`. A crafted `historyCount` (e.g. 1,000,000) keeps the guard from tripping; once `ago` exceeds ~`historyIndex + 20`, the dividend goes negative and Java's `%` yields a **negative** remainder → `recentScores[-1]` → `ArrayIndexOutOfBoundsException` while rendering.

**Impact.** An uncaught `ArrayIndexOutOfBoundsException` (a `RuntimeException`, missed by the `IOException` catch) crashes the game thread either when the player finishes a run (`endRun`, line 191) or when the history/stats screen is opened. A crafted save reliably bricks the first post-load run. DoS only — no memory corruption, no information disclosure.

**Evidence.**
```text
src/Profile.java:422   historyIndex = in.readInt();   // no clamp
src/Profile.java:423   historyCount = in.readInt();   // no clamp
src/Profile.java:191   recentScores[historyIndex] = score;                       // AIOOBE
src/Profile.java:275   int idx = (historyIndex - 1 - ago + HISTORY_SIZE*2) % 20; // can be negative
```

**Exploit / Failure Scenario.** A crafted/shared `dotch_profile.dat` sets `historyIndex = 50`. On the victim's next death, `Profile.endRun` executes `recentScores[50] = score` and crashes. Alternatively, with a valid `historyIndex` but `historyCount = 1000000`, opening the Recent Runs screen drives `idx` to `-1` and crashes the render path.

**Remediation.** Clamp/normalize immediately after reading:
```java
historyCount = Math.max(0, Math.min(HISTORY_SIZE, historyCount));
historyIndex = ((historyIndex % HISTORY_SIZE) + HISTORY_SIZE) % HISTORY_SIZE;
```
Also make the modular math in `getRecent*` defensively non-negative (add `HISTORY_SIZE` and re-mod).

---

### [LOW] Unvalidated `level` / `totalXp` drive O(level) per-frame and O(level²) loops — UI freeze / hang

| | |
|---|---|
| **Severity** | Low (adjusted) |
| **CWE** | CWE-1284: Improper Validation of Specified Quantity in Input |
| **Location** | `src/Profile.java:396-397` (load); consumed at `:93`, `:107` and `Menu.java:590,674,2990` |
| **Confidence** | High — mechanism and per-frame call sites confirmed |

**Description.** `totalXp` (line 396, `readLong`) and `level` (line 397, `readInt`) are read with no upper bound. `xpInCurrentLevel()` runs a loop linear in `level`, calling `Math.pow` each iteration:

```java
// src/Profile.java:91-97
public static long xpInCurrentLevel() {
    long xp = totalXp;
    for (int i = 1; i < level; i++) xp -= xpForNextLevel(i); // O(level), Math.pow each step
    return Math.max(0, xp);
}
```

`levelProgress()` calls it, and both are invoked from **per-frame render code**: the profile card / HUD XP bar (`Menu.java:590`, `:674`) and the level-up popup (`Menu.java:2990`), all reached from `render(Graphics g)`. Compounding this, `addXp()` is O(level²) over loaded input:

```java
// src/Profile.java:105-111
private static void addXp(int xp) {
    totalXp += xp;
    while (xpInCurrentLevel() >= xpForNextLevel(level)) { level++; ... } // each iter itself O(level)
}
```

**Impact.** A crafted `level` near `Integer.MAX_VALUE` forces a ~2-billion-iteration `Math.pow` loop on **every frame**, freezing the UI as soon as any menu drawing the XP bar is entered. Alternatively, a huge `totalXp` with a small `level` makes the first `endRun()` spin the `addXp()` while-loop millions of times (~O(level²) total work), producing a multi-minute hang on death. Impact is bounded to the victim's own offline client and is undone by deleting the file.

**Evidence.**
```text
src/Profile.java:396   totalXp = in.readLong();   // no bound
src/Profile.java:397   level   = in.readInt();    // no bound
src/Profile.java:93    for (int i = 1; i < level; i++) xp -= xpForNextLevel(i);   // O(level) per frame
src/Profile.java:107   while (xpInCurrentLevel() >= xpForNextLevel(level)) { ... } // O(level^2)
Menu.java:590 / 674 / 2990  // per-frame render call sites of levelProgress()/xpInCurrentLevel()
```

**Exploit / Failure Scenario.** A crafted `dotch_profile.dat` with `level = 2000000000`. On the first main-menu render, `Menu.java:590 → levelProgress() → xpInCurrentLevel()` runs ~2 billion `Math.pow` iterations to paint a single frame, freezing the UI indefinitely.

**Remediation.** Clamp `level` to a realistic maximum on load (`level = Math.max(1, Math.min(level, 9999))`) and treat `totalXp` as bounded, or replace the linear `xpInCurrentLevel()` loop and the `addXp()` while-loop with closed-form arithmetic that cannot iterate an attacker-controlled number of times.

---

## 5. Dependency Security Analysis

The project has **no dependency manager or build script** (no `pom.xml` / `build.gradle` / `build.xml`). It builds only inside IntelliJ via one module (`DodgeGame.iml`) referencing the project library defined in `.idea/libraries/jars.xml`, which points at five opaque, pre-compiled binary JARs committed under `libs/jars/` — no checksums, no SBOM, no upstream coordinates. All are End-of-Life.

| Dependency | Version | Built / Released | Maintenance status | Risk in this codebase |
|---|---|---|---|---|
| LWJGL | 2.8.4 | 2012-05-30 | **EOL** — entire 2.x line abandoned for LWJGL 3 | JNI bindings to OpenGL/OpenAL; loads native DLLs. No untrusted input reaches it (it receives already-decoded PCM, not container bytes). Risk = EOL/no-patch + native search-order (boundary b). No CVE cited. |
| Slick2D | build 274 | 2010-06-26 | **EOL** — unmaintained | Wraps OGG playback via `org.newdawn.slick.Music` (`AudioPlayer.java:54`). Resolves resources classpath-first, then **filesystem-relative to CWD** (a theoretical planting fallback). Image/texture loaders present but unreachable (`new Sound` never called). No CVE cited. |
| JOrbis | 0.0.15 | ~2005 | **EOL** (JCraft) | Pure-Java Vorbis decoder — the real media-parsing surface (boundary c). Only ever fed the 6 hardcoded bundled `.ogg` tracks; no file picker, import, or network source exists. Historically CVE-prone class of code. |
| JOgg | 0.0.7 | ~2005 | **EOL** (JCraft); empty MANIFEST | Ogg container/page parser feeding JOrbis; page/segment-length fields drive buffer sizing. Same bundled-media-only exposure as JOrbis. |
| JInput | 2.0.0 | 2009-03 | **EOL** | **Dead code** — no `src/` file imports `net.java.games.input`; input is pure AWT (`KeyInput`/`Menu` use `java.awt.event`). Pulls in `jinput-*.dll`. Footprint/hygiene only; no reachable code path. |

**Native binaries.** `libs/lwjgl/native/windows/` ships `lwjgl.dll`, `lwjgl64.dll`, `OpenAL32.dll`, `OpenAL64.dll`, and `jinput-*.dll` at a relative repo path, loaded via launch-time `-Djava.library.path` (per project memory note). No `System.load`/`loadLibrary` exists in application source — resolution is delegated to `org.lwjgl.Sys`. Committed binaries cannot be diff-verified against canonical upstream releases, though Git content-addressing does provide a tamper-evident baseline within the repo.

**Overall dependency risk:** Low-to-informational under this threat model. The EOL status is genuine technical debt, but no confirmed untrusted-input path reaches any decoder, and no specific advisory was demonstrated against the pinned versions.

---

## 6. Positive Observations

The codebase does several things correctly, materially shrinking the attack surface:

- **No `ObjectInputStream` / Java native serialization** anywhere. All persistence uses length/version-prefixed binary `DataInputStream`/`DataOutputStream`, so there is **no deserialization gadget-chain RCE** risk.
- **No network attack surface.** A repo-wide search for `java.net`, `Socket`, `HttpURLConnection`, `URL.openConnection`, `InetAddress`, `DatagramSocket` returns zero matches. No server, no leaderboard, no telemetry — scores never leave the machine.
- **No `Runtime.exec` / reflection-based command execution** and no hardcoded secrets/credentials (none required — the game is fully offline).
- **Validated consumers of loaded data.** `CoinShop.loadPurchased` (`CoinShop.java:172`) bounds its copy with `Math.min(data.length, ITEM_COUNT)`, and `PlayerSkins.loadSelection` range-checks shape/color against their counts and falls back to defaults — so the purchase-array and skin-id sinks cannot overflow even though `Profile` reads them raw.
- **Safe loaders elsewhere.** `Achievements.load` reads an unvalidated count but never allocates from it (`Math.min` against `ALL.size()`, EOF-bounded skip loop). `DailyChallenge` and `Stats` use only fixed-size arrays. `Settings.load` is fixed-layout (no count-driven loops or content-driven allocation) and resets to defaults on `IOException`.
- **No untrusted input reaches the `Integer.parseInt`/`split` parsing** flagged by static heuristics — the `enemies`/`controls` RGB tables in `Menu.java` are compile-time `String[][]` literals, not file/user data.
- **Input layer is inert.** Keyboard handling (`KeyInput.java`) only compares `getKeyCode()` to constant `VK_*` values; mouse coordinates are clamped to `0..1` for sliders and never used as array indices. There is no free-text/player-name entry anywhere.
- **Native loading is explicitly pinned** in the working IDE run configuration via an absolute `-Djava.library.path`, which *replaces* the default search path (removing `.`/CWD) for the primary LWJGL DLL — the recommended defense against search-order planting.

---

## 7. Prioritized Remediation Roadmap

Ordered by priority and effort (all are small, localized changes in `Profile.java`):

1. **Bound the array allocation (fixes the Medium).** In `Profile.load()`, validate `itemCount` against `[0, CoinShop.ITEM_COUNT]` before `new boolean[itemCount]`; throw `IOException` (or clamp) on a bad count. Eliminates the OOM / `NegativeArraySizeException` crash-on-launch. *(Lowest effort, highest impact.)*
2. **Broaden the loader catch.** Change `catch (IOException e)` to `catch (IOException | RuntimeException e)` in `Profile.load()` (and the other static-initializer loaders: `Settings`, `Achievements`, `DailyChallenge`, `Stats`) so any malformed field degrades to defaults instead of becoming an `ExceptionInInitializerError`.
3. **Clamp history indices on load.** Normalize `historyIndex` into `[0, HISTORY_SIZE)` and clamp `historyCount` to `[0, HISTORY_SIZE]` immediately after reading; harden the modular math in `getRecentScore/Level/Difficulty` to never yield a negative index.
4. **Clamp `level` and treat `totalXp` as bounded on load**, or replace the linear `xpInCurrentLevel()` / `addXp()` loops with closed-form arithmetic, so a crafted value cannot drive an unbounded per-frame or per-run loop.
5. **(Hardening, optional)** Validate the version field, and consider writing saves atomically (temp file + rename) to avoid silent progress loss on a mid-write crash. *(Reliability, not security.)*
6. **(Hygiene, optional)** Track dependency provenance: record versions/hashes for the five committed JARs and plan migration off the EOL LWJGL 2.x / Slick2D / JOrbis stack if the project is revived. Remove the unreachable JInput dependency and the stray `dotchgame.iml` / nested `windows.iml` IDE artifacts.

---

## 8. Appendix A: Dismissed / Out-of-Scope Findings

The following were considered and ruled out after adversarial verification. They are listed to document rigor; none is a genuine, exploitable vulnerability under the stated threat model.

| # | Candidate (location) | Why dismissed |
|---|---|---|
| 1 | NaN volume bypasses `clamp()` into audio engine (`Settings.java:115`) | Real Java NaN-propagation, but consequence is at most silenced/undefined local audio gain. No DoS/OOM/index/memory effect; cosmetic robustness nit, not security. |
| 2 | No integrity/authenticity check; non-atomic truncate-then-write saves (`Profile.java:330`) | No trust boundary an HMAC/checksum could protect (local offline; any key ships with the client). Non-atomic write is a reliability nit; corrupt reads fall back to defaults. Info. |
| 3 | Coins/XP/level/unlocks/achievements trivially forgeable (`Profile.java:329`) | Pure self-cheating in a single-player offline game — attacker and victim are the same user. Threat model rates this Low/Info; tamper-evidence is security theater here. |
| 4 | Unvalidated `lastPlayedEpochDay` enables streak forgery + cycle-reset overflow (`DailyChallenge.java:183`) | Fixed-size arrays, no allocation/index primitive. Only effect is cosmetic local daily-board numbers; no crash, no boundary crossed. Info. |
| 5 | `.dat` files at relative CWD paths (`Profile.java:10`, others) | Filenames are compile-time constants (no traversal). Relocating to APPDATA wouldn't change crafted-file reachability. Anyone who can write CWD already has local-user access. Info. |
| 6 | Non-atomic saves corrupt file / discard progress (`Profile.java:330`) | No attacker input; requires victim's own crash/power-loss in a sub-millisecond window. Availability/reliability nit, not security. Info. |
| 7 | Check-then-open TOCTOU in every `load()` (`Profile.java:388`) | No privilege boundary crossed; failure mode is `FileNotFoundException` → load defaults. Winning the race grants no capability beyond simply handing over a crafted file. Info. |
| 8 | README launch omits `java.library.path` → default DLL search order (`README.md:165`) | Not a code defect; the documented command actually crashes (`UnsatisfiedLinkError`) because bundled natives live elsewhere. Generic platform behavior, not introduced by this code. |
| 9 | `java.library.path` points at writable project-relative native dir (`.idea/workspace.xml:52`) | File is gitignored/dev-local, not shipped. An explicit absolute path is the *defense* against search-order hijacking, not the cause. Overwriting a DLL there needs write access that already subsumes the box. |
| 10 | No `SetDllDirectory` / path validation / integrity check in app code (`Game.java:347`) | Absence of optional defense-in-depth, not a defect. Recommended Win32 APIs aren't callable from pure Java; LWJGL2 lacks the LWJGL3 property. Derivative, informational. |
| 11 | EOL JOrbis 0.0.15 + JOgg 0.0.7 decode at startup (`jars.xml:8`) | Only the 6 hardcoded bundled `.ogg` files are decoded; no file picker/import/network. Filesystem-fallback planting needs pre-existing CWD write access. Informational EOL note. |
| 12 | EOL Slick2D build 274 (`jars.xml:5`) | No untrusted-media path reaches it; image/`Sound` loaders never exercised. No CVE cited. Informational EOL note. |
| 13 | EOL LWJGL 2.8.4 (`jars.xml:7`) | Thin JNI binding; parses no trust-boundary data (gets decoded PCM, not container bytes). No CVE cited; native search-order counted separately. Informational. |
| 14 | EOL JInput 2.0.0 on classpath (`jars.xml:6`) | Dead/unreachable code — zero imports in `src/`; input is pure AWT. Would need physical HID access to feed it. Hygiene only. |
| 15 | Opaque committed JARs/DLLs, no SBOM (`lwjgl.dll`) | Not a code defect; Git content-addressing provides a tamper-evident baseline. Attacker scenario presupposes repo/filesystem write access. Process-hygiene note. Info. |
| 16 | NaN/Infinity volume reaches audio engine (`Settings.java:115`) | Duplicate of #1; at startup `tracks` is empty so OpenAL isn't even reached. Trivial correctness nit. Info. |
| 17 | Inconsistent `language` clamp: load 0..5 vs setter 0..2 (`Settings.java:123`) | `getLanguage()` has no callers — write-only dead state; out-of-range value indexes nothing today. Speculative/forward-looking. Info. |
| 18 | `Integer.parseInt` on enemy RGB strings (`Menu.java:2594`) | Parsed data is a compile-time `String[][]` literal, not external input. No trust boundary. No fix needed. |
| 19 | Daily Challenge runs not reproducible — enemy behaviour uses unseeded RNG (`HardEnemy.java:73`) | Correctness/fairness defect only; no leaderboard/score submission exists. No attacker, no boundary. Info. |
| 20 | Daily seed is predictable function of constants + date (`DailyChallenge.java:58`) | Determinism is the intended feature (shared daily board); no network/leaderboard. `java.util.Random` used as content generator, not security primitive. Info. |
| 21 | Non-crypto `java.util.Random`/`Math.random` everywhere (`Game.java:119`) | No token/key/nonce/session/password anywhere (zero `SecureRandom`/secret matches). RNG is cosmetic/gameplay only. Info. |
| 22 | `BossLaser` spawns stationary `(0,0)` bullets that never despawn (`BossLaser.java:249`) | Self-limiting (beam deals lethal contact damage; boss death sweeps all bullets) and bounded to tens-to-low-hundreds of objects. Benign gameplay quirk, not DoS. Info. |

---

## 9. Disclaimer

This report was produced by an automated multi-agent security review (Claude Code). While findings were grounded in specific source lines and adversarially verified, automated review is **not a substitute for a manual penetration test or a formal code audit**, and **no guarantee of completeness** is made. The absence of additional findings does not prove the absence of vulnerabilities. Severity ratings are calibrated to the stated threat model (a local, offline, single-player desktop game) and should be re-evaluated if the application's trust boundaries change — for example, if networking, multiplayer, a shared leaderboard, score submission, or any untrusted-media import feature is ever added, several Info/Low items (save integrity, predictable daily seed, EOL media decoders) would warrant immediate re-rating upward.