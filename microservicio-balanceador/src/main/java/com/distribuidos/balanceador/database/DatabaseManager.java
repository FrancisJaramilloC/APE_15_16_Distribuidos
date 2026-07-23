package com.distribuidos.balanceador.database;

import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseManager {

    private static String resolveDbPath() {
        File parentDb = new File("../nodos.db");
        if (new File("../frontend").exists() || new File("../microservicio-balanceador").exists()) {
            return parentDb.getAbsolutePath();
        }
        return new File("nodos.db").getAbsolutePath();
    }

    private static final String DB_PATH = resolveDbPath();
    private static final String URL = "jdbc:sqlite:" + DB_PATH + "?busy_timeout=5000";

    public DatabaseManager() {
        initDb();
    }

    public synchronized void initDb() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA busy_timeout=5000;");
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS estado_nodos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nodo TEXT NOT NULL UNIQUE,
                    puerto INTEGER NOT NULL,
                    estado TEXT NOT NULL,
                    latencia REAL DEFAULT 0.0,
                    ultima_actualizacion TEXT NOT NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS circuit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    servicio TEXT NOT NULL,
                    estado_anterior TEXT NOT NULL,
                    nuevo_estado TEXT NOT NULL,
                    motivo TEXT,
                    timestamp TEXT NOT NULL
                );
            """);

        } catch (Exception e) {
            System.err.println("[SQLite Init Error] " + e.getMessage());
        }
    }

    public synchronized void syncNodes(List<BackendNode> activeNodes) {
        if (activeNodes == null || activeNodes.isEmpty()) return;
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            StringBuilder inClause = new StringBuilder();
            for (int i = 0; i < activeNodes.size(); i++) {
                if (i > 0) inClause.append(",");
                inClause.append("'").append(activeNodes.get(i).getAddress()).append("'");
            }
            stmt.executeUpdate("DELETE FROM estado_nodos WHERE nodo NOT IN (" + inClause.toString() + ");");
        } catch (Exception e) {
            System.err.println("[SQLite Sync Nodes Error] " + e.getMessage());
        }
    }

    public synchronized void updateNodeStatus(String nodo, int puerto, String estado, double latencia) {
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String sql = """
            INSERT INTO estado_nodos (nodo, puerto, estado, latencia, ultima_actualizacion)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(nodo) DO UPDATE SET
                puerto = excluded.puerto,
                estado = excluded.estado,
                latencia = excluded.latencia,
                ultima_actualizacion = excluded.ultima_actualizacion;
        """;

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nodo);
            pstmt.setInt(2, puerto);
            pstmt.setString(3, estado);
            pstmt.setDouble(4, Math.round(latencia * 100.0) / 100.0);
            pstmt.setString(5, nowStr);
            pstmt.executeUpdate();

        } catch (Exception e) {
            System.err.println("[SQLite Update Node Error] " + e.getMessage());
        }
    }

    public synchronized List<Map<String, Object>> getAllNodes() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM estado_nodos ORDER BY puerto ASC;";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("nodo", rs.getString("nodo"));
                row.put("puerto", rs.getInt("puerto"));
                row.put("estado", rs.getString("estado"));
                row.put("latencia", rs.getDouble("latencia"));
                row.put("ultima_actualizacion", rs.getString("ultima_actualizacion"));
                list.add(row);
            }

        } catch (Exception e) {
            System.err.println("[SQLite Get Nodes Error] " + e.getMessage());
        }

        return list;
    }

    public synchronized List<Map<String, Object>> getCircuitLogs() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM circuit_log ORDER BY id DESC LIMIT 50;";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("servicio", rs.getString("servicio"));
                row.put("estado_anterior", rs.getString("estado_anterior"));
                row.put("nuevo_estado", rs.getString("nuevo_estado"));
                row.put("motivo", rs.getString("motivo"));
                row.put("timestamp", rs.getString("timestamp"));
                list.add(row);
            }

        } catch (Exception e) {
            System.err.println("[SQLite Get Circuit Logs Error] " + e.getMessage());
        }

        return list;
    }
}
