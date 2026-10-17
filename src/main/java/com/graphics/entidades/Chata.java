package com.graphics.entidades;

import java.util.List;

/** Remolque de un eje: la caja sigue al enganche con orientacion independiente. */
public final class Chata {
    public static final float ENGANCHE_AUTO = 1.35f;
    public static final float DISTANCIA_EJE_ENGANCHE = 1.65f;
    public static final float SEMILARGO_CAJA = 0.85f;
    public static final float LARGO_BARRA = DISTANCIA_EJE_ENGANCHE - SEMILARGO_CAJA;
    public static final float CENTRO_BARRA = -(DISTANCIA_EJE_ENGANCHE + SEMILARGO_CAJA) * 0.5f;
    public static final float ANGULO_MAXIMO = (float) Math.toRadians(60);

    public record Estado(float x, float z, float angulo, float engancheX, float engancheZ) {}

    private final Automovil automovil;
    private Estado estado;
    private List<Huella> huellas;

    public Chata(Automovil automovil) {
        this.automovil = automovil;
        reiniciar();
    }

    public Estado getEstado() { return estado; }
    public void confirmar(Estado siguiente) {
        estado = siguiente;
        huellas = null;
    }

    public void reiniciar() {
        float angulo = automovil.getAngulo();
        float seno = (float) Math.sin(angulo);
        float coseno = (float) Math.cos(angulo);
        float engancheX = automovil.getX() + seno * ENGANCHE_AUTO;
        float engancheZ = automovil.getZ() + coseno * ENGANCHE_AUTO;
        confirmar(new Estado(engancheX + seno * DISTANCIA_EJE_ENGANCHE,
                engancheZ + coseno * DISTANCIA_EJE_ENGANCHE, angulo, engancheX, engancheZ));
    }

    /** No modifica el remolque hasta que se validen las colisiones del conjunto. */
    public Estado predecir(float autoX, float autoZ, float anguloAuto) {
        float engancheX = autoX + (float) Math.sin(anguloAuto) * ENGANCHE_AUTO;
        float engancheZ = autoZ + (float) Math.cos(anguloAuto) * ENGANCHE_AUTO;
        float dx = estado.x - engancheX;
        float dz = estado.z - engancheZ;
        float angulo = Math.hypot(dx, dz) < 0.00001 ? estado.angulo : (float) Math.atan2(dx, dz);
        float articulacion = normalizarAngulo(angulo - anguloAuto);
        articulacion = Math.max(-ANGULO_MAXIMO, Math.min(ANGULO_MAXIMO, articulacion));
        angulo = normalizarAngulo(anguloAuto + articulacion);
        return new Estado(engancheX + (float) Math.sin(angulo) * DISTANCIA_EJE_ENGANCHE,
                engancheZ + (float) Math.cos(angulo) * DISTANCIA_EJE_ENGANCHE,
                angulo, engancheX, engancheZ);
    }

    public static float normalizarAngulo(float angulo) {
        return (float) Math.atan2(Math.sin(angulo), Math.cos(angulo));
    }

    public static float mundoX(Estado estado, float x, float z) {
        return estado.x + (float) Math.cos(estado.angulo) * x + (float) Math.sin(estado.angulo) * z;
    }

    public static float mundoZ(Estado estado, float x, float z) {
        return estado.z - (float) Math.sin(estado.angulo) * x + (float) Math.cos(estado.angulo) * z;
    }

    /** Caja, dos ruedas y barra; evita ensanchar toda la caja por las ruedas. */
    public List<Huella> getHuellas(Estado posicion) {
        if (posicion == estado && huellas != null) return huellas;
        List<Huella> resultado = List.of(new Huella(posicion.x, posicion.z, posicion.angulo, 0.65f, SEMILARGO_CAJA),
                huellaLocal(posicion, -0.73f, 0, 0.10f, 0.25f),
                huellaLocal(posicion, 0.73f, 0, 0.10f, 0.25f),
                huellaLocal(posicion, 0, CENTRO_BARRA, 0.06f, LARGO_BARRA * 0.5f));
        if (posicion == estado) huellas = resultado;
        return resultado;
    }

    private Huella huellaLocal(Estado posicion, float x, float z, float semiancho, float semilargo) {
        return new Huella(mundoX(posicion, x, z), mundoZ(posicion, x, z),
                posicion.angulo, semiancho, semilargo);
    }

    public boolean contiene(float x, float z, float margen) {
        for (Huella huella : getHuellas(estado)) {
            if (huella.contiene(x, z, margen)) return true;
        }
        return false;
    }

    public double distanciaFirmada(float x, float z) {
        double distancia = Double.POSITIVE_INFINITY;
        for (Huella huella : getHuellas(estado)) {
            distancia = Math.min(distancia, huella.distanciaFirmada(x, z));
        }
        return distancia;
    }
}
