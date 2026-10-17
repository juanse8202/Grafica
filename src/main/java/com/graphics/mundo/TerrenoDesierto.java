package com.graphics.mundo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Suelo arenoso que rodea el borde de la ciudad sin modificar su mapa. */
public final class TerrenoDesierto {
    private static final float BORDE_EXTERIOR = 220.0f;
    private final List<ElementoPaisaje> elementos;

    public TerrenoDesierto(float limiteCiudad) {
        float anchoFranja = BORDE_EXTERIOR - limiteCiudad;
        float centroFranja = (BORDE_EXTERIOR + limiteCiudad) * 0.5f;
        List<ElementoPaisaje> suelo = new ArrayList<>();

        // Las cuatro franjas se unen en las esquinas y dejan libres las calles.
        agregar(suelo, 0.0f, -0.50f, centroFranja, BORDE_EXTERIOR * 2.0f,
                0.50f, anchoFranja, 0.72f, 0.57f, 0.34f, 0.0f);
        agregar(suelo, 0.0f, -0.50f, -centroFranja, BORDE_EXTERIOR * 2.0f,
                0.50f, anchoFranja, 0.72f, 0.57f, 0.34f, 0.0f);
        agregar(suelo, centroFranja, -0.50f, 0.0f, anchoFranja,
                0.50f, limiteCiudad * 2.0f, 0.72f, 0.57f, 0.34f, 0.0f);
        agregar(suelo, -centroFranja, -0.50f, 0.0f, anchoFranja,
                0.50f, limiteCiudad * 2.0f, 0.72f, 0.57f, 0.34f, 0.0f);

        // Variaciones planas de arena rompen la apariencia de un piso uniforme.
        for (int i = 0; i < 8; i++) {
            float x = -190.0f + i * 54.0f;
            float z = (i % 2 == 0 ? 1.0f : -1.0f) * (124.0f + (i % 3) * 28.0f);
            agregar(suelo, x, -0.235f, z, 22.0f, 0.02f, 8.0f,
                    0.78f, 0.63f, 0.39f, (i % 3 - 1) * 0.18f);
        }
        elementos = Collections.unmodifiableList(suelo);
    }

    public List<ElementoPaisaje> getElementos() {
        return elementos;
    }

    private void agregar(List<ElementoPaisaje> lista, float x, float y, float z,
            float ancho, float alto, float profundidad,
            float rojo, float verde, float azul, float giro) {
        lista.add(new ElementoPaisaje(x, y, z, ancho, alto, profundidad,
                rojo, verde, azul, giro));
    }
}
