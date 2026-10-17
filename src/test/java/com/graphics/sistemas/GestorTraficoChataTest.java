package com.graphics.sistemas;

import com.graphics.entidades.Automovil;
import com.graphics.entidades.Chata;
import com.graphics.entidades.Huella;
import java.util.Collections;
import junit.framework.TestCase;

public class GestorTraficoChataTest extends TestCase {
    public void testTraficoFrenaAnteChataAntesDeAlcanzarAlAuto() {
        Automovil jugador = new Automovil(new float[][]{{0, 0}, {0, -20}}, 0, 1, 0, 0);
        Chata chata = new Chata(jugador);
        Automovil npc = new Automovil(new float[][]{{-2, 12}, {-2, -20}}, 10, 0, 0, 1);
        GestorTrafico gestor = new GestorTrafico(new Automovil[]{npc}, new float[0][], bits -> 1);
        gestor.setAutomovilJugador(jugador);
        gestor.setChataJugador(chata);
        for (int i = 0; i < 60; i++) {
            gestor.actualizar(1.0f / 60, Collections.emptyList());
            if (npc.getVelocidad() == 0) break;
        }
        assertEquals(0.0f, npc.getVelocidad(), 0.00001f);
        assertTrue("Debe frenar por la chata antes del alcance de frenado del auto", npc.getZ() > 7.5f);
        Huella auto = new Huella(npc.getXCarril(), npc.getZCarril(), npc.getAngulo(), 0.92f, 1.2f);
        for (Huella huella : chata.getHuellas(chata.getEstado())) assertTrue(auto.solapamientoCon(huella) < 0);
    }

    public void testPasoGrandeNoPermiteAtravesarLaChata() {
        Automovil jugador = new Automovil(new float[][]{{0, 0}, {0, -20}}, 0, 1, 0, 0);
        Chata chata = new Chata(jugador);
        Automovil npc = new Automovil(new float[][]{{-2, 12}, {-2, -20}}, 20, 0, 0, 1);
        GestorTrafico gestor = new GestorTrafico(new Automovil[]{npc}, new float[0][], bits -> 1);
        gestor.setAutomovilJugador(jugador);
        gestor.setChataJugador(chata);
        gestor.actualizar(1.0f, Collections.emptyList());
        assertTrue("Un frame largo no debe saltarse el remolque", npc.getZ() > 5);
        assertEquals(0.0f, npc.getVelocidad(), 0.00001f);
        // El movimiento rechazado no debe adelantar el indice de la ruta.
        assertEquals(1, npc.getSiguientePunto());
    }
}
