-- ============================================================
-- GP-106 · La cuenta de servicio del bot pasa al dominio del producto.
--
-- Como admin y guest (V202609271000), la cuenta del bot de Discord se creó con un
-- correo de gymprofit.com, un dominio que no es del proyecto. Pasa a gymprofit.app.
-- El bot entra por nombre de usuario, así que no le afecta.
--
-- Mismas guardas que V202609271000: solo la fila cuyo correo sigue siendo
-- exactamente el de .com (comparación binaria) y solo si el destino está libre, para
-- que la UNIQUE del correo no tumbe el arranque de producción.
-- ============================================================

UPDATE usuarios
SET email = 'bot@gymprofit.app'
WHERE email = CAST('bot@gymprofit.com' AS BINARY)
  AND NOT EXISTS (SELECT 1
                  FROM (SELECT DISTINCT email FROM usuarios WHERE email = 'bot@gymprofit.app') AS ocupado);
