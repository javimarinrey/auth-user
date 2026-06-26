# Autentificación de usuario con Redis

## Cómo funciona

`RedisPoolManager` - Singleton con doble check (`volatile` + `synchronized`) que:

- Lee `redis.properties` del classpath en el primer acceso
- Construye un `JedisPool` con los parámetros de conexión y el pool (maxTotal/maxIdle/minIdle)
- Selecciona `database=1` directamente en el constructor del pool

`AuthServlet` - En el doGet:

- Valida que los tres parámetros no estén en blanco → `400` si faltan
- Construye la key `users:<codigousu>|<clausu>|<afiliacion>`
- Saca un `Jedis` del pool con try-with-resources (se devuelve automáticamente al pool)
- Hace `jedis.exists(key)` → `200 {authenticated:true}` / `401 {authenticated:false}`
- Captura `JedisException` → `503` si Redis no está disponible

## Puntos claves

- `try (Jedis jedis = pool.getResource())` garantiza que la conexión vuelve al pool aunque haya excepción
- El pool se inicializa en `init()` del servlet (eager), no en el primer request
- Si `redis.password` está vacío en el properties, no se pasa contraseña al pool
- El `destroy()` del servlet cierra el pool limpiamente al apagar el servidor