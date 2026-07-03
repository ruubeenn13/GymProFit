package es.pmdm.gymprofit.network;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

// ============================================================
// BooleanNumericAdapter — TypeAdapter Gson tolerante para campos booleanos.
// Los endpoints de admin/jOOQ (AdminUsuarioDTO, EjercicioJooqDTO, AlimentoJooqDTO)
// serializan el estado "activo" como un Byte (0/1) en lugar de true/false. El
// adaptador estándar de Gson lanza al leer un número en un campo boolean, por lo
// que se aplica este adaptador (vía @JsonAdapter) a los campos "activo" de los
// POJOs de dominio: acepta tanto el boolean clásico de los endpoints normales
// como el número 0/1 de admin, siendo un superconjunto retrocompatible.
// ============================================================
public class BooleanNumericAdapter extends TypeAdapter<Boolean> {

    // Lectura tolerante: booleano nativo, número (0=false, ≠0=true), cadena o null.
    @Override
    public Boolean read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch (token) {
            case BOOLEAN:
                return in.nextBoolean();
            case NUMBER:
                return in.nextInt() != 0;
            case STRING:
                String s = in.nextString();
                return "1".equals(s) || "true".equalsIgnoreCase(s);
            case NULL:
                in.nextNull();
                return false;
            default:
                in.skipValue();
                return false;
        }
    }

    // Escritura estándar (los cuerpos de escritura viajan como Map, no usan este adaptador).
    @Override
    public void write(JsonWriter out, Boolean value) throws IOException {
        if (value == null) out.nullValue();
        else out.value(value);
    }
}
