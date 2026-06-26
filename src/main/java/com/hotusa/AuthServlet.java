package com.hotusa;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;

/**
 * Servlet de autenticación de usuario contra Redis.
 *
 * GET /auth?codigousu=X&clausu=Y&afiliacion=Z
 *
 * Comprueba si existe la key:
 *   users:<codigousu>|<clausu>|<afiliacion>
 * en la base de datos Redis 1.
 *
 * Responde:
 *   200 OK         → key existe (usuario autenticado)
 *   401 Unauthorized → key no existe
 *   400 Bad Request  → parámetros ausentes
 *   503 Service Unavailable → error de conexión Redis
 */
@WebServlet(name = "AuthServlet", urlPatterns = "/auth")
public class AuthServlet extends HttpServlet {


    @Override
    public void init() throws ServletException {
        // Inicializar el pool al arrancar el servlet (eager init)
        try {
            RedisPoolManager.getInstance();
        } catch (Exception e) {
            throw new ServletException("No se pudo inicializar el pool de Redis", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // ── 1. Leer y validar parámetros ──────────────────────────────────
        String codigousu  = req.getParameter("codigousu");
        String clausu     = req.getParameter("clausu");
        String afiliacion = req.getParameter("afiliacion");

        if (isBlank(codigousu) || isBlank(clausu) || isBlank(afiliacion)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Parámetros obligatorios: codigousu, clausu, afiliacion");
            return;
        }

        // ── 2. Construir la key de Redis ──────────────────────────────────
        String redisKey = "users:" + codigousu + "|" + clausu + "|" + afiliacion;

        // ── 3. Consultar Redis ────────────────────────────────────────────
        JedisPool pool = RedisPoolManager.getInstance().getPool();

        try (Jedis jedis = pool.getResource()) {

            boolean exists = jedis.exists(redisKey);

            if (exists) {
                System.out.println("AUTH OK → key=" + redisKey);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"authenticated\":true}");
            } else {
                System.out.println("AUTH FAIL → key no encontrada: " + redisKey);
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"authenticated\":false}");
            }

        } catch (JedisException e) {
            System.out.println("Error conectando a Redis");
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Error de conexión con Redis");
        }
    }

    @Override
    public void destroy() {
        RedisPoolManager.getInstance().close();
    }

    // ------------------------------------------------------------------ //

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
