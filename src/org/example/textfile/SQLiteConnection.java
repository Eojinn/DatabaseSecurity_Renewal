package org.example.textfile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteConnection {
    private String dbUrl;
    private Connection connection;
    // 🔑 테이블 이름 통일
    private static final String TABLE_NAME = "example_data";

    public SQLiteConnection(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            // 🔑 DB URL은 생성자에서 설정되므로 그대로 둡니다.
            connection = DriverManager.getConnection(dbUrl);
            System.out.println("데이터베이스에 연결되었습니다: " + dbUrl);
            createTableIfNotExist();
            insertSampleDataIfEmpty();
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC 드라이버를 찾을 수 없습니다. Maven/Gradle 의존성을 확인하세요.");
            e.printStackTrace();
            throw new RuntimeException("SQLite JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            System.err.println("데이터베이스 연결 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("SQLite 데이터베이스 연결 실패", e);
        }
    }

    private void createTableIfNotExist() throws SQLException {
        // 🔑 컬럼 이름 통일 ('category', 'value')
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " category TEXT NOT NULL,\n"
                + " value INTEGER NOT NULL\n"
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
                // 🔑 샘플 데이터를 통일된 카테고리/값 데이터로 변경 (5개 행)
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
                System.out.println("테이블에 이미 데이터가 존재합니다. 샘플 데이터 삽입을 건너뜁니다.");
            }
        }
    }

    public ResultSet executeQuery(String query) throws SQLException {
        if (connection == null) {
            throw new SQLException("데이터베이스에 연결되어 있지 않습니다.");
        }
        // 쿼리 문자열은 'SELECT * FROM example_data'가 되도록 설정
        // 이 클래스는 쿼리를 받아들이지만, 벤치마크 코드의 일관성을 위해 쿼리를 직접 수정하지 않고 TABLE_NAME을 사용하도록 허용합니다.
        // 벤치마크 코드는 "SELECT * FROM example_data"를 직접 전달해야 합니다.
        return connection.createStatement().executeQuery(query);
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