# Database_Security

데이터 유형(텍스트 / 이미지)과 데이터베이스 시스템(MySQL / SQLite)에 따라
암호 시스템(AES / RSA / 하이브리드)의 **암호화·복호화 시간**과 **메모리 사용량**을 비교하는 Java 벤치마크 프로젝트입니다.

## 비교 조건

| 구분 | 항목 |
|---|---|
| 데이터 유형 | 텍스트 데이터, 이미지 데이터(BLOB, 1MB 이하) |
| 데이터베이스 | MySQL, SQLite |
| 암호 방식 | AES, RSA, 하이브리드(AES + RSA) |

총 **2 × 2 × 3 = 12가지** 조합을 각각의 벤치마크 클래스로 측정합니다.

### 암호 방식 설정

| 방식 | 알고리즘 / 모드 | 키 길이 |
|---|---|---|
| AES | `AES/CBC/PKCS5Padding` | 128bit |
| RSA | `RSA/ECB/PKCS1Padding` | 2048bit (한 번에 최대 245바이트) |
| 하이브리드 | 데이터는 AES로 암호화하고, AES 키와 IV는 RSA로 암호화합니다. 이미지 버전은 `SHA256withRSA` 서명을 함께 사용합니다. | AES 128bit + RSA 2048bit |

### 측정 방식

- 워밍업 **50회** 실행 후 본 측정 **1,000회** 반복
- 암호화·복호화 시간: `System.nanoTime()`으로 측정한 평균 (ms)
- 메모리 사용량: `MemoryMXBean`의 힙 사용량 변화 (MB)

## 프로젝트 구조

```
Database_Security/
├─ src/
│  └─ org/example/
│     ├─ textfile/                 # 텍스트 데이터 벤치마크
│     │  ├─ AESCipher.java
│     │  ├─ RSACipher.java
│     │  ├─ HybridCipher.java
│     │  ├─ MySQLConnection.java
│     │  ├─ SQLiteConnection.java
│     │  ├─ MySQLAESBenchmark.java      ▶ 실행
│     │  ├─ MySQLRSABenchmark.java      ▶ 실행
│     │  ├─ MySQLHybridBenchmark.java   ▶ 실행
│     │  ├─ SQLiteAESBenchmark.java     ▶ 실행
│     │  ├─ SQLiteRSABenchmark.java     ▶ 실행
│     │  └─ SQLiteHybridBenchmark.java  ▶ 실행
│     └─ imagefile/                # 이미지 데이터 벤치마크 (파일 구성 동일)
├─ .gitignore
└─ README.md
```

- `*Cipher.java`: 암호화·복호화 구현
- `*Connection.java`: DB 연결 및 테이블 생성
- `*Benchmark.java`: `main` 메서드가 있는 실행 클래스

## 실행 환경

- **JDK 21**
- **IntelliJ IDEA**
- **MySQL 8.x** (MySQL 벤치마크 실행 시)
- **JDBC 드라이버**
  - `com.mysql:mysql-connector-j`
  - `org.xerial:sqlite-jdbc`

## 실행 방법

### 1. 프로젝트 가져오기

```bash
git clone https://github.com/<계정명>/Database_Security.git
```

IntelliJ에서 폴더를 연 뒤 다음을 설정합니다.

1. **File → Project Structure → Project**에서 SDK를 JDK 21로 지정합니다.
2. **Modules → Sources**에서 `src` 폴더를 **Sources**로 지정합니다.
3. **Libraries → `+` → From Maven…**에서 위의 JDBC 드라이버 두 개를 추가합니다.

### 2. 환경 설정 변경 (필수)

코드에는 예시 값이 들어 있으므로, 실행 전에 본인 환경에 맞게 수정해야 합니다.

| 항목 | 위치 | 예시 값 |
|---|---|---|
| MySQL 주소 / DB 이름 | `MySQLConnection.java`, `MySQL*Benchmark.java` | `jdbc:mysql://localhost:3306/example_db` |
| MySQL 계정 / 비밀번호 | 위와 동일 | `sample_user` / `sample_password` |
| SQLite DB 파일 경로 | `SQLiteConnection.java`, `SQLite*Benchmark.java` | `jdbc:sqlite:/path/to/example_db.db` |
| 테이블 / 컬럼 이름 | `imagefile` 패키지의 SQL 문 | `테이블 이름`, `튜플 이름` |
| 이미지 데이터셋 경로 | `imagefile` 패키지 | 본인 데이터셋 위치 |

> ⚠️ 실제 DB 비밀번호를 넣은 상태로 커밋하지 마세요.

### 3. 벤치마크 실행

원하는 `*Benchmark.java` 파일을 열고 `main` 메서드 옆의 ▶ 버튼을 누릅니다.
실행이 끝나면 콘솔에 평균 암호화·복호화 시간과 메모리 사용량이 출력됩니다.
