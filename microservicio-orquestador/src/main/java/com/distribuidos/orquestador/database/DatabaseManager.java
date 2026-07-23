package com.distribuidos.orquestador.database;

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

/**
 * Gestor de Persistencia SQLite para el Orquestador.
 * Registra la auditoría de transiciones del Circuit Breaker en la tabla circuit_log.
 */
@Component
public class DatabaseManager {

    /**
     * Resuelve dinámicamente la ruta del archivo nodos.db para compartir
     * el mismo archivo SQLite en la raíz del proyecto.
     */
    private static String resolveDbPath() {
        File parentDb = new File("../nodos.db");
        if (new File("../frontend").exists() || new File("../microservicio-orquestador").exists()) {
            return parentDb.getAbsolutePath();
        }
        return new File("nodos.db").getAbsolutePath();
    }

    private static final String DB_PATH = resolveDbPath();
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    public DatabaseManager() {
        initDb();
    }

    /**
     * Inicializa la tabla circuit_log si no existe previamente.
     */
    public synchronized void initDb() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
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
            System.err.println("[SQLite Init Circuit Error] " + e.getMessage());
        }
    }

    /**
     * Inserta una nueva transición de estado en la tabla circuit_log.
     * 
     * @param servicio Nombre del servicio ("ServicioOrquestador").
     * @param estadoAnterior Estado previo (CLOSED, OPEN, HALF_OPEN).
     * @param nuevoEstado Nuevo estado al que commuta el circuito.
     * @param motivo Explicación técnica de la causa de la transición.
     */
    public synchronized void logCircuitTransition(String servicio, String estadoAnterior, String nuevoEstado, String motivo) {
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String sql = """
            INSERT INTO circuit_log (servicio, estado_anterior, nuevo_estado, motivo, timestamp)
            VALUES (?, ?, ?, ?, ?);
        """;

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, servicio);
            pstmt.setString(2, estadoAnterior);
            pstmt.setString(3, nuevoEstado);
            pstmt.setString(4, motivo != null ? motivo : "");
            pstmt.setString(5, nowStr);
            pstmt.executeUpdate();

        } catch (Exception e) {
            System.err.println("[SQLite Circuit Log Error] " + e.getMessage());
        }
    }

    /**
     * Consulta las últimas 50 transiciones registradas en la tabla circuit_log.
     */
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
