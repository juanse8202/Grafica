package com.graphics.entidades;

/** Rectangulo orientado en el plano XZ, compartido por dibujo y colisiones. */
public record Huella(float x, float z, float angulo, float semiancho, float semilargo) {
    public float radioEnEje(float ejeX, float ejeZ) {
        float seno = (float) Math.sin(angulo);
        float coseno = (float) Math.cos(angulo);
        return semiancho * Math.abs(ejeX * coseno - ejeZ * seno)
                + semilargo * Math.abs(ejeX * seno + ejeZ * coseno);
    }

    /** Penetracion minima por SAT; un valor negativo indica separacion. */
    public float solapamientoCon(Huella otra) {
        float[][] ejes = {
            {(float) Math.cos(angulo), -(float) Math.sin(angulo)},
            {(float) Math.sin(angulo), (float) Math.cos(angulo)},
            {(float) Math.cos(otra.angulo), -(float) Math.sin(otra.angulo)},
            {(float) Math.sin(otra.angulo), (float) Math.cos(otra.angulo)}
        };
        float minimo = Float.POSITIVE_INFINITY;
        for (float[] eje : ejes) {
            float distancia = Math.abs((otra.x - x) * eje[0] + (otra.z - z) * eje[1]);
            float penetracion = radioEnEje(eje[0], eje[1])
                    + otra.radioEnEje(eje[0], eje[1]) - distancia;
            if (penetracion < 0) return -1;
            minimo = Math.min(minimo, penetracion);
        }
        return minimo;
    }

    public boolean contiene(float puntoX, float puntoZ, float margen) {
        float dx = puntoX - x;
        float dz = puntoZ - z;
        float lateral = dx * (float) Math.cos(angulo) - dz * (float) Math.sin(angulo);
        float longitudinal = dx * (float) Math.sin(angulo) + dz * (float) Math.cos(angulo);
        return Math.abs(lateral) <= semiancho + margen
                && Math.abs(longitudinal) <= semilargo + margen;
    }

    /** Negativa dentro, cero en el borde y positiva fuera de la huella. */
    public double distanciaFirmada(float puntoX, float puntoZ) {
        float dx = puntoX - x;
        float dz = puntoZ - z;
        double lateral = Math.abs(dx * Math.cos(angulo) - dz * Math.sin(angulo)) - semiancho;
        double longitudinal = Math.abs(dx * Math.sin(angulo) + dz * Math.cos(angulo)) - semilargo;
        return Math.hypot(Math.max(lateral, 0), Math.max(longitudinal, 0))
                + Math.min(Math.max(lateral, longitudinal), 0);
    }
}
