# 🔍 HashChecker

Fast Minecraft mod verification tool using Modrinth API. Validates mod files by checking their SHA-1 hashes against the Modrinth database.

## 📋 Features

- **SHA-1 Hash Verification** — validates mod files against Modrinth database
- **Real-time Progress** — live TPS monitoring and file count tracking
- **Rate Limit Tracking** — displays API usage and remaining quota
- **Smart Rate Limiter** — automatic adaptation to API limits
- **Colored Output** — easy-to-read results with color coding
- **Retry Logic** — automatic retry on rate limit errors

## 📦 Installation

### Prerequisites

- Java 11 or higher
- Gradle (included via wrapper)

### Build from Source

1. Clone the repository:
```bash
git clone https://github.com/Pa-dej/HashChecker2.git
cd HashChecker2
```

2. Build the project:
```bash
./gradlew jar
```

3. The JAR file will be created at:
```
build/libs/HashChecker-1.0.0.jar
```

## 🚀 Usage

### Check Mods Folder

Verify all mod files in a directory:

```bash
java -jar HashChecker-1.0.0.jar <mods_folder>
```

Example:
```bash
java -jar HashChecker-1.0.0.jar mods/
```

### Check API Rate Limit

View current API rate limit status:

```bash
java -jar HashChecker-1.0.0.jar --limit
```

## 📊 Example Output

```
Проверка модов: test_mods

[OK] iris-fabric-1.10.5+mc1.21.11.jar
[OK] sodium-neoforge-0.8.4+mc1.21.11.jar
[NOT FOUND] custom-mod.jar
[OK] modmenu-17.0.0-beta.2.jar

TPS: 3.50 | Pending: 2

OK: 7
NOT FOUND: 3
Время: 2.5 сек
Средний TPS: 4.00

RATE LIMIT STATUS
API calls made: 10 | Used: 10/300 (3.3%) | Remaining: 290 | Reset in: 54s
```

## 🔧 How It Works

1. **Hash Calculation** — computes SHA-1 hash for each mod file
2. **API Request** — sends hash to Modrinth API endpoint `/v2/version_file/{hash}`
3. **Rate Limiting** — respects Modrinth's 300 requests/minute limit
4. **Result Display** — shows verification status with color coding:
   - 🟢 **[OK]** — mod found in Modrinth database
   - 🟡 **[NOT FOUND]** — mod not found or invalid hash
   - 🔴 **[429 RATE LIMIT]** — rate limit exceeded, retrying

## 🛠️ Technical Details

- **Language:** Java 11+
- **Build Tool:** Gradle
- **API:** Modrinth API v2
- **HTTP Client:** Java HTTP/2 client
- **JSON Parser:** Gson 2.10.1

### Rate Limiting

The application implements adaptive rate limiting based on Modrinth API headers:

- `X-Ratelimit-Limit` — maximum requests per minute (300)
- `X-Ratelimit-Remaining` — requests remaining in current window
- `X-Ratelimit-Reset` — seconds until rate limit resets

Rate limiter automatically adjusts speed:
- **> 50% remaining** → 6 requests/sec
- **20-50% remaining** → 3 requests/sec
- **< 20% remaining** → 1.5 requests/sec
- **429 error** → 0.5 requests/sec + retry

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Credits

Created by **Padej_**

## 🔗 Links

- [GitHub Repository](https://github.com/Pa-dej/HashChecker2)
- [Modrinth API Documentation](https://docs.modrinth.com/api/)

---

*Made with ☕ by Padej_*
