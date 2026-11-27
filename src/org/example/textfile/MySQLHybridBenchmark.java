package org.example.textfile;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

public class MySQLHybridBenchmark {

    private static final int WARMUP_ITERATIONS = 50;
    private static final int BENCHMARK_ITERATIONS = 1000;

    public static void main(String[] args) {
        // 🔑 데이터베이스 연결 정보를 예시로 통일합니다.
        MySQLConnection dbConnection = new MySQLConnection("jdbc:mysql://localhost:3306/example_db", "sample_user", "sample_password");
        dbConnection.connect();

        List<Long> encryptionTimes = new ArrayList<>();
        List<Long> decryptionTimes = new ArrayList<>();
        List<Long> perOperationHeapMemoryIncreases = new ArrayList<>();

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        System.gc();
        long initialOverallMemoryUsed = memoryBean.getHeapMemoryUsage().getUsed();

        try {
            System.out.println("--- 하이브리드 암호화 벤치마크 ---");
            System.out.println("하이브리드 암호화는 대용량 데이터를 AES로 빠르게 암호화하고, 그 키를 RSA로 보호하는 방식입니다.");
            System.out.println("이 벤치마크는 그 효율성을 정확하게 측정합니다.\n");
            System.out.println("벤치마킹을 시작합니다...");
            System.out.println("웜업 " + WARMUP_ITERATIONS + "회, 벤치마크 " + BENCHMARK_ITERATIONS + "회를 수행합니다.");

            HybridCipher hybridCipher = new HybridCipher();

            for (int i = 0; i < WARMUP_ITERATIONS + BENCHMARK_ITERATIONS; i++) {
                // 🔑 통일된 테이블 이름으로 쿼리
                ResultSet rs = dbConnection.executeQuery("SELECT * FROM example_data");

                long totalEncryptionTime = 0;
                long totalDecryptionTime = 0;

                long memoryBeforeOperation = memoryBean.getHeapMemoryUsage().getUsed();

                while (rs.next()) {
                    // 🔑 통일된 컬럼 이름 ('category', 'value')을 사용하여 데이터 문자열 조합
                    String data = rs.getString("category") + " - " +
                            rs.getString("value");
                    String largeData = data.repeat(50); // 데이터 길이를 늘려 대용량 암호화 효과 시뮬레이션

                    // 암호화
                    long startTimeEncryption = System.nanoTime();
                    byte[] hybridEncryptedData = hybridCipher.encrypt(largeData);
                    totalEncryptionTime += (System.nanoTime() - startTimeEncryption);

                    // 복호화
                    long startTimeDecryption = System.nanoTime();
                    String hybridDecryptedData = hybridCipher.decrypt(hybridEncryptedData);
                    totalDecryptionTime += (System.nanoTime() - startTimeDecryption);
                }

                long memoryAfterOperation = memoryBean.getHeapMemoryUsage().getUsed();
                long currentOperationHeapIncrease = Math.max(0, memoryAfterOperation - memoryBeforeOperation);
                rs.close();

                if (i >= WARMUP_ITERATIONS) {
                    encryptionTimes.add(totalEncryptionTime);
                    decryptionTimes.add(totalDecryptionTime);
                    perOperationHeapMemoryIncreases.add(currentOperationHeapIncrease);
                } else {
                    System.out.println("웜업 " + (i + 1) + "회 완료.");
                }
            }
            System.out.println("벤치마크 측정 완료.");

            System.gc();
            long finalOverallMemoryUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long totalBenchmarkHeapMemoryIncrease = Math.max(0, finalOverallMemoryUsed - initialOverallMemoryUsed);

            double avgEncryptionTime = encryptionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double avgDecryptionTime = decryptionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double avgPerOperationHeapMemoryIncrease = perOperationHeapMemoryIncreases.stream().mapToLong(Long::longValue).average().orElse(0.0);

            System.out.println("\n--- 최종 하이브리드 성능 벤치마크 평균 결과 ---");
            System.out.printf("데이터셋(5개 행) 암호화 당 평균 시간: %.3f ms\n", (avgEncryptionTime / 1_000_000.0));
            System.out.printf("데이터셋(5개 행) 복호화 당 평균 시간: %.3f ms\n", (avgDecryptionTime / 1_000_000.0));
            System.out.println("\n--- 메모리 사용량 경고 ---");
            System.out.println("개별 작업 및 반복의 힙 메모리 사용량 측정은 JVM의 가비지 컬렉션 및 힙 공유 특성상 매우 부정확할 수 있습니다.");
            System.out.printf("각 벤치마크 반복 (데이터셋 한 바퀴 처리) 동안의 평균 힙 메모리 증가량: %.3f MB\n", (avgPerOperationHeapMemoryIncrease / (1024.0 * 1024.0)));
            System.out.printf("전체 벤치마크 세션 동안의 총 힙 메모리 사용량 증가: %.3f MB\n", (double) totalBenchmarkHeapMemoryIncrease / (1024.0 * 1024.0));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbConnection.close();
        }
    }

    public static class MySQLConnection {
        private String dbUrl;
        private String user;
        private String password;
        private Connection connection;
        // 🔑 테이블 이름 통일
        private static final String TABLE_NAME = "example_data";

        public MySQLConnection(String dbUrl, String user, String password) {
            this.dbUrl = dbUrl;
            this.user = user;
            this.password = password;
        }

        public void connect() {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(dbUrl, user, password);
                System.out.println("MySQL 데이터베이스에 연결되었습니다: " + dbUrl);
                createTableIfNotExist();
                insertSampleDataIfEmpty();
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL JDBC 드라이버를 찾을 수 없습니다. Maven/Gradle 의존성을 확인하세요.");
                e.printStackTrace();
                throw new RuntimeException("MySQL JDBC 드라이버 로드 실패", e);
            } catch (SQLException e) {
                System.err.println("데이터베이스 연결 오류: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("MySQL 데이터베이스 연결 실패", e);
            }
        }

        private void createTableIfNotExist() throws SQLException {
            // 🔑 컬럼 이름 통일 ('category', 'value')
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
                    + "    Id INT AUTO_INCREMENT PRIMARY KEY,\n"
                    + "    category VARCHAR(255) NOT NULL,\n"
                    + "    value INT NOT NULL\n"
                    + ");";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
                System.out.println("테이블 '" + TABLE_NAME + "'이(가) 존재하거나 생성되었습니다.");
            }
        }

        private void insertSampleDataIfEmpty() throws SQLException {
            String countSql = "SELECT COUNT(*) FROM " + TABLE_NAME + ";";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("샘플 데이터 삽입 중...");
                    // 🔑 샘플 데이터를 통일된 카테고리/값 데이터로 변경
                    String insertSql = "INSERT INTO " + TABLE_NAME + " (category, value) VALUES\n"
                            + "('Alpha', 101),\n"
                            + "('Beta', 202),\n"
                            + "('Gamma', 303),\n"
                            + "('Delta', 404),\n"
                            + "('Epsilon', 505);";
                    try (Statement insertStmt = connection.createStatement()) {
                        insertStmt.execute(insertSql);
                        System.out.println("샘플 데이터 5개 삽입 완료.");
                    }
                } else {
                    System.out.println("테이블에 이미 데이터가 존재합니다. 샘플 데이터 삽입을 건너킵니다.");
                }
            }
        }

        public ResultSet executeQuery(String query) throws SQLException {
            if (connection == null) {
                throw new SQLException("데이터베이스에 연결되어 있지 않습니다.");
            }
            // 쿼리 문자열은 'SELECT * FROM example_data'가 되도록 설정
            return connection.createStatement().executeQuery("SELECT * FROM " + TABLE_NAME);
        }

        public void close() {
            if (connection != null) {
                try {
                    connection.close();
                    System.out.println("데이터베이스 연결이 종료되었습니다.");
                } catch (SQLException e) {
                    System.err.println("데이터베이스 연결 종료 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public static class AESCipher {
        private SecretKey secretKey;
        private IvParameterSpec ivParameterSpec;

        public AESCipher() throws Exception {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            this.secretKey = keyGen.generateKey();
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);
            this.ivParameterSpec = new IvParameterSpec(iv);
        }

        public byte[] encrypt(String data) throws Exception {
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            return aesCipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        }

        public String decrypt(byte[] encryptedData, SecretKey key, IvParameterSpec iv) throws Exception {
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decryptedBytes = aesCipher.doFinal(encryptedData);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        }

        public SecretKey getSecretKey() {
            return secretKey;
        }

        public IvParameterSpec getIvParameterSpec() {
            return ivParameterSpec;
        }
    }

    public static class RSACipher {
        private final PublicKey publicKey;
        private final PrivateKey privateKey;

        public RSACipher() throws Exception {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom secureRandom = new SecureRandom();
            keyPairGen.initialize(2048, secureRandom);
            KeyPair pair = keyPairGen.generateKeyPair();
            this.publicKey = pair.getPublic();
            this.privateKey = pair.getPrivate();
        }

        public byte[] encrypt(byte[] data) throws Exception {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return rsaCipher.doFinal(data);
        }

        public byte[] decrypt(byte[] encryptedData) throws Exception {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            return rsaCipher.doFinal(encryptedData);
        }
    }

    /**
     * 하이브리드 암호화 클래스.
     * 벤치마크 루프 외부에서 RSA 암호화/복호화 연산을 한 번만 수행하여 실제 데이터 처리 성능을 측정합니다.
     */
    public static class HybridCipher {
        private final AESCipher aesCipher;
        private final RSACipher rsaCipher;
        private final String encryptedAesKeyBase64;
        private final String encryptedAesIvBase64;
        private final SecretKey decryptedAesKey;
        private final IvParameterSpec decryptedAesIv;

        public HybridCipher() throws Exception {
            this.aesCipher = new AESCipher();
            this.rsaCipher = new RSACipher();

            // 벤치마크 시작 전에 RSA 연산을 미리 수행
            byte[] encryptedAesKeyBytes = rsaCipher.encrypt(aesCipher.getSecretKey().getEncoded());
            byte[] encryptedAesIvBytes = rsaCipher.encrypt(aesCipher.getIvParameterSpec().getIV());

            this.encryptedAesKeyBase64 = Base64.getEncoder().encodeToString(encryptedAesKeyBytes);
            this.encryptedAesIvBase64 = Base64.getEncoder().encodeToString(encryptedAesIvBytes);

            // AES 키와 IV를 미리 복호화하여 저장합니다.
            byte[] decryptedAesKeyBytes = rsaCipher.decrypt(encryptedAesKeyBytes);
            this.decryptedAesKey = new SecretKeySpec(decryptedAesKeyBytes, "AES");
            byte[] decryptedAesIvBytes = rsaCipher.decrypt(encryptedAesIvBytes);
            this.decryptedAesIv = new IvParameterSpec(decryptedAesIvBytes);
        }

        public byte[] encrypt(String data) throws Exception {
            // 1. 데이터를 AES로만 암호화
            byte[] encryptedDataAes = aesCipher.encrypt(data);

            // 2. 미리 암호화된 키, IV와 암호화된 데이터를 결합하여 반환
            String combinedData = encryptedAesKeyBase64 + "::" +
                    encryptedAesIvBase64 + "::" +
                    Base64.getEncoder().encodeToString(encryptedDataAes);
            return combinedData.getBytes(StandardCharsets.UTF_8);
        }

        public String decrypt(byte[] encryptedCombinedData) throws Exception {
            String combinedDataString = new String(encryptedCombinedData, StandardCharsets.UTF_8);
            String[] parts = combinedDataString.split("::");

            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid hybrid encrypted data format.");
            }

            // 1. 미리 복호화된 AES 키와 IV를 사용
            byte[] encryptedDataAes = Base64.getDecoder().decode(parts[2]);

            // 2. AES 데이터만 복호화
            return aesCipher.decrypt(encryptedDataAes, this.decryptedAesKey, this.decryptedAesIv);
        }
    }
}