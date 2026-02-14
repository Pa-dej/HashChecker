# 🔍 HashChecker

Fast Minecraft mod verification tool using Modrinth API. Validates mod files by checking their SHA-512 hashes against the Modrinth database for enhanced security.

## 📋 Features

- **SHA-512 Hash Verification** — production-grade cryptographic validation
- **Batch Processing** — up to 100 mods per API request (100x faster)
- **Real-time Progress** — live TPS monitoring and file count tracking
- **Security Checks** — detects unknown, modified, and private mods
- **Rate Limit Tracking** — displays API usage and remaining quota
- **Smart Rate Limiter** — automatic adaptation to API limits
- **Colored Output** — easy-to-read results with color coding
- **Symlink Protection** — canonical path validation against attacks

## 🔒 Security

### What This Tool Detects

✅ **Private mods** — not published on Modrinth  
✅ **Modified jars** — tampered or altered mod files  
✅ **Unknown files** — files not in Modrinth database  
✅ **Renamed cheats** — files with mismatched hashes

### What This Tool Cannot Detect

❌ **Runtime injection** — mods loaded after startup  
❌ **JavaAgent injection** — JVM-level modifications  
❌ **Memory injection** — runtime bytecode manipulation  
❌ **Embedded cheats** — cheats hidden inside legitimate mods

### Effectiveness

This tool catches approximately **95% of common cheat clients** by verifying file integrity against Modrinth's database. For complete protection, combine with server-side anticheat systems.

## 📦 Installation

### Prerequisites

- Java 11 or higher

### Download JAR from Releases

1. Go to the [Releases page](https://github.com/Pa-dej/HashChecker/releases)  
2. Download the latest `.jar` file (e.g., `HashChecker-1.0.0.jar`)  
3. Place it in your preferred directory

### Build from Source (Optional)

1. Clone the repository:
```bash
git clone https://github.com/Pa-dej/HashChecker.git
cd HashChecker
````

2. Build the project:

```bash
./gradlew jar
```

3. The JAR file will be created at:

```
build/libs/HashChecker-1.0.0.jar
```

## 🚀 Usage

### Batch Mode (Recommended)

Verify all mod files in a directory using batch API:

```bash
java -jar HashChecker-1.0.0.jar <mods_folder>
```

Example:

```bash
java -jar HashChecker-1.0.0.jar mods/
```

### Single File Mode

Check mods one by one (slower, for debugging):

```bash
java -jar HashChecker-1.0.0.jar --single <mods_folder>
```

### Check API Rate Limit

View current API rate limit status:

```bash
java -jar HashChecker-1.0.0.jar --limit
```

## 📊 Example Output

```
<<<<<<< HEAD
Checking mods: C:\Users\User\mods
Found .jar files: 10
=======
Checking mods: test_mods
>>>>>>> 1b8e48d77f4d395b789af829bc8ea35269c0c725

[OK] iris-fabric-1.10.5+mc1.21.11.jar
[OK] sodium-neoforge-0.8.4+mc1.21.11.jar
[NOT FOUND] custom-mod.jar
[OK] modmenu-17.0.0-beta.2.jar

VERIFIED: 7
UNKNOWN: 3

<<<<<<< HEAD
WARNING: 3 unknown/modified files detected!
These files are NOT verified by Modrinth:
- Private mods
- Modified jars
- Potential cheats

Time: 0.8 sec
Average TPS: 12.50
=======
OK: 7
NOT FOUND: 3
Time: 2.5s
Average TPS: 4.00
>>>>>>> 1b8e48d77f4d395b789af829bc8ea35269c0c725

RATE LIMIT STATUS
API calls made: 1 | Used: 1/300 (0.3%) | Remaining: 299 | Reset in: 54s

SECURITY SUMMARY
Hash Algorithm: SHA-512
Files Checked: 10
Verification Rate: 70.0%
```

## 🔧 How It Works

<<<<<<< HEAD
1. **File Discovery** — scans directory for `.jar` files only
2. **Canonical Path Check** — validates paths to prevent symlink attacks
3. **SHA-512 Hashing** — computes cryptographic hash for each file
4. **Batch API Request** — sends up to 100 hashes per request to Modrinth
5. **Verification** — compares hashes against Modrinth database
6. **Security Report** — displays detailed results with warnings

## 🛠️ Technical Details

- **Language:** Java 11+
- **Build Tool:** Gradle
- **API:** Modrinth API v2
- **HTTP Client:** Java HTTP/2 client
- **JSON Parser:** Gson 2.10.1
- **Hash Algorithm:** SHA-512 (production-grade)
=======
1. **Hash Calculation** — computes SHA-1 hash for each mod file
2. **API Request** — sends hash to Modrinth API endpoint `/v2/version_file/{hash}`
3. **Rate Limiting** — respects Modrinth's 300 requests/minute limit
4. **Result Display** — shows verification status with color coding:

   * 🟢 **[OK]** — mod found in Modrinth database
   * 🟡 **[NOT FOUND]** — mod not found or invalid hash
   * 🔴 **[429 RATE LIMIT]** — rate limit exceeded, retrying

## 🛠️ Technical Details

* **Language:** Java 11+
* **Build Tool:** Gradle
* **API:** Modrinth API v2
* **HTTP Client:** Java HTTP/2 client
* **JSON Parser:** Gson 2.10.1
>>>>>>> 1b8e48d77f4d395b789af829bc8ea35269c0c725

### Rate Limiting

The application implements adaptive rate limiting based on Modrinth API headers:

* `X-Ratelimit-Limit` — maximum requests per minute (300)
* `X-Ratelimit-Remaining` — requests remaining in current window
* `X-Ratelimit-Reset` — seconds until rate limit resets

Rate limiter automatically adjusts speed:

* **> 50% remaining** → 6 requests/sec
* **20-50% remaining** → 3 requests/sec
* **< 20% remaining** → 1.5 requests/sec
* **429 error** → 0.5 requests/sec + retry

### Security Improvements

**v1.0.0 Security Features:**
- SHA-512 instead of SHA-1 (cryptographically secure)
- Batch processing (100 files per request)
- Only checks `.jar` files (ignores configs, logs, etc.)
- Canonical path validation (prevents symlink attacks)
- Unknown file counting and warnings
- Verification rate percentage

## 🎯 Use Cases

- **Modpack Verification** — ensure all mods are legitimate
- **Server Administration** — validate client mods before joining
- **Launcher Integration** — automated mod integrity checks
- **Security Audits** — detect modified or unknown files

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Credits

Created by **Padej_**

## 🔗 Links

* [GitHub Repository](https://github.com/Pa-dej/HashChecker)
* [Modrinth API Documentation](https://docs.modrinth.com/api/)

## ⚠️ Disclaimer

This tool provides file integrity verification but cannot detect all types of cheats or malicious modifications. For complete protection, use in combination with server-side anticheat systems and runtime monitoring.

---

*Made with 🍵 by Padej_*
