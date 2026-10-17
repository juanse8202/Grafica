package com.graphics.entidades;

import junit.framework.TestCase;

public class ChataTest extends TestCase {
    private Automovil auto() {
        return new Automovil(new float[][]{{0, 0}, {0, -20}}, 0, 1, 0, 0);
    }

    private void comprobarEnganche(Chata.Estado estado, Automovil auto) {
        assertEquals(auto.getX() + Math.sin(auto.getAngulo()) * Chata.ENGANCHE_AUTO,
                estado.engancheX(), 0.00001);
        assertEquals(auto.getZ() + Math.cos(auto.getAngulo()) * Chata.ENGANCHE_AUTO,
                estado.engancheZ(), 0.00001);
        assertEquals(Chata.DISTANCIA_EJE_ENGANCHE,
                Math.hypot(estado.x() - estado.engancheX(), estado.z() - estado.engancheZ()), 0.00001);
        assertEquals(estado.engancheX(), Chata.mundoX(estado, 0, -Chata.DISTANCIA_EJE_ENGANCHE), 0.00001);
        assertEquals(estado.engancheZ(), Chata.mundoZ(estado, 0, -Chata.DISTANCIA_EJE_ENGANCHE), 0.00001);
    }

    public void testGiraConRetrasoYSeEnderezaAlAvanzar() {
        Automovil auto = auto();
        Chata chata = new Chata(auto);
        for (int i = 1; i <= 100; i++) {
            float angulo = i * 0.01f;
            float x = 8 * ((float) Math.cos(angulo) - 1);
            float z = -8 * (float) Math.sin(angulo);
            Chata.Estado propuesta = chata.predecir(x, z, angulo);
            auto.setPosicion(x, z);
            auto.setAngulo(angulo);
            chata.confirmar(propuesta);
            comprobarEnganche(chata.getEstado(), auto);
        }
        float articulacion = auto.getAngulo() - chata.getEstado().angulo();
        assertTrue("Debe verse un angulo entre el auto y la chata", articulacion > 0.1f);
        for (int i = 0; i < 400; i++) {
            float x = auto.getX() - (float) Math.sin(auto.getAngulo()) * 0.05f;
            float z = auto.getZ() - (float) Math.cos(auto.getAngulo()) * 0.05f;
            chata.confirmar(chata.predecir(x, z, auto.getAngulo()));
            auto.setPosicion(x, z);
        }
        assertEquals(auto.getAngulo(), chata.getEstado().angulo(), 0.002f);
        comprobarEnganche(chata.getEstado(), auto);
    }

    public void testPrediccionNoMueveLaChataHastaConfirmarla() {
        Chata chata = new Chata(auto());
        Chata.Estado anterior = chata.getEstado();
        Chata.Estado propuesta = chata.predecir(-1, -2, 0.4f);
        assertSame(anterior, chata.getEstado());
        chata.confirmar(propuesta);
        assertSame(propuesta, chata.getEstado());
    }

    public void testMarchaAtrasMantieneLaUnionYLimitaElPlegado() {
        Automovil auto = auto();
        Chata chata = new Chata(auto);
        for (int i = 1; i <= 100; i++) {
            float angulo = -i * 0.006f;
            float x = auto.getX() + (float) Math.sin(angulo) * 0.04f;
            float z = auto.getZ() + (float) Math.cos(angulo) * 0.04f;
            Chata.Estado anterior = chata.getEstado();
            chata.confirmar(chata.predecir(x, z, angulo));
            auto.setPosicion(x, z);
            auto.setAngulo(angulo);
            comprobarEnganche(chata.getEstado(), auto);
            assertTrue(Math.abs(Chata.normalizarAngulo(chata.getEstado().angulo() - angulo))
                    <= Chata.ANGULO_MAXIMO + 0.00001f);
            assertTrue("El remolque no debe saltar", Math.hypot(chata.getEstado().x() - anterior.x(),
                    chata.getEstado().z() - anterior.z()) < 0.1);
        }
        auto.setPosicion(2, 4);
        auto.setAngulo((float) Math.PI);
        chata.reiniciar();
        assertEquals(auto.getAngulo(), chata.getEstado().angulo(), 0.00001);
        comprobarEnganche(chata.getEstado(), auto);
    }

    public void testHuellasIncluyenCajaRuedasYBarraRotadas() {
        Automovil auto = auto();
        Chata chata = new Chata(auto);
        Chata.Estado estado = chata.predecir(-1, -2, 0.5f);
        chata.confirmar(estado);
        assertTrue(chata.contiene(estado.x(), estado.z(), 0));
        assertTrue(chata.contiene(Chata.mundoX(estado, 0, Chata.CENTRO_BARRA),
                Chata.mundoZ(estado, 0, Chata.CENTRO_BARRA), 0));
        assertTrue(chata.contiene(Chata.mundoX(estado, 0.78f, 0), Chata.mundoZ(estado, 0.78f, 0), 0));
        assertFalse(chata.contiene(Chata.mundoX(estado, 1.2f, 0), Chata.mundoZ(estado, 1.2f, 0), 0));
        assertTrue(chata.distanciaFirmada(estado.x(), estado.z()) < 0);
        Huella lateral = new Huella(Chata.mundoX(estado, 0.7f, 0),
                Chata.mundoZ(estado, 0.7f, 0), estado.angulo() + 0.5f, 0.25f, 0.25f);
        assertTrue(chata.getHuellas(estado).stream().anyMatch(h -> h.solapamientoCon(lateral) > 0));
    }
}
