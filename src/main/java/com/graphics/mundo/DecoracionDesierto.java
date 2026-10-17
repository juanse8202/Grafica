package com.graphics.mundo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Rocas y pinos bajos colocados de forma determinista en el exterior árido. */
public final class DecoracionDesierto {
    private static final float SUELO = -0.25f;
    private final List<ElementoPaisaje> elementos;

    public DecoracionDesierto(float limiteCiudad) {
        List<ElementoPaisaje> decoracion = new ArrayList<>();
        int indice = 0;

        for (int i = 0; i < 6; i++) {
            float x = -175.0f + i * 70.0f;
            float distancia = limiteCiudad + 14.0f + (i % 3) * 25.0f;
            indice = agregarDecoracion(decoracion, x, distancia, indice);
            indice = agregarDecoracion(decoracion, x + 18.0f, -distancia - 8.0f, indice);
        }
        for (int i = 0; i < 5; i++) {
            float z = -140.0f + i * 56.0f;
            float distancia = limiteCiudad + 16.0f + (i % 3) * 23.0f;
            indice = agregarDecoracion(decoracion, distancia, z, indice);
            indice = agregarDecoracion(decoracion, -distancia - 10.0f, z + 14.0f, indice);
        }

        elementos = Collections.unmodifiableList(decoracion);
    }

    public List<ElementoPaisaje> getElementos() {
        return elementos;
    }

    private int agregarDecoracion(List<ElementoPaisaje> lista, float x, float z, int indice) {
        if (indice % 2 == 0) agregarPino(lista, x, z, indice);
        else agregarRoca(lista, x, z, indice);
        return indice + 1;
    }

    private void agregarPino(List<ElementoPaisaje> lista, float x, float z, int variante) {
        float escala = 0.82f + (variante % 3) * 0.12f;
        agregar(lista, x, SUELO + 0.80f * escala, z,
                0.42f * escala, 1.60f * escala, 0.42f * escala,
                0.34f, 0.20f, 0.10f, 0.0f);
        agregar(lista, x, SUELO + 1.23f * escala, z,
                1.95f * escala, 1.05f * escala, 1.95f * escala,
                0.12f, 0.34f, 0.16f, 0.18f);
        agregar(lista, x, SUELO + 1.87f * escala, z,
                1.48f * escala, 1.02f * escala, 1.48f * escala,
                0.10f, 0.29f, 0.14f, -0.12f);
        agregar(lista, x, SUELO + 2.47f * escala, z,
                0.92f * escala, 0.92f * escala, 0.92f * escala,
                0.08f, 0.25f, 0.12f, 0.08f);
    }

    private void agregarRoca(List<ElementoPaisaje> lista, float x, float z, int variante) {
        float escala = 0.80f + (variante % 4) * 0.16f;
        float giro = (variante * 0.47f) % 1.4f;
        float rojo = 0.48f + (variante % 3) * 0.035f;
        agregar(lista, x, SUELO + 0.48f * escala, z,
                1.75f * escala, 0.96f * escala, 1.42f * escala,
                rojo, 0.39f, 0.25f, giro);
        agregar(lista, x - 0.58f * escala, SUELO + 0.38f * escala,
                z + 0.42f * escala, 0.92f * escala, 0.76f * escala,
                0.96f * escala, 0.43f, 0.35f, 0.24f, -giro * 0.7f);
        agregar(lista, x + 0.57f * escala, SUELO + 0.32f * escala,
                z - 0.32f * escala, 0.80f * escala, 0.64f * escala,
                0.82f * escala, 0.55f, 0.45f, 0.31f, giro * 0.5f);
    }

    private void agregar(List<ElementoPaisaje> lista, float x, float y, float z,
            float ancho, float alto, float profundidad,
            float rojo, float verde, float azul, float giro) {
        lista.add(new ElementoPaisaje(x, y, z, ancho, alto, profundidad,
                rojo, verde, azul, giro));
    }
}
