package com.graphics.sistemas;

import com.graphics.AppCiudad;
import com.graphics.entidades.EstadoPersona;
import com.graphics.entidades.Persona;
import com.graphics.entidades.Huella;
import java.util.function.BiPredicate;
import junit.framework.TestCase;

public class GestorPersonasTest extends TestCase {
    private GestorPersonas crearGestor(int estadoSemaforo) {
        float[][] semaforos = new float[27][5];
        for (int i = 0; i < semaforos.length; i++) {
            // Cruce horizontal de (6, 10) a (14, 10).
            semaforos[i] = new float[]{5.5f, 12.3f, 0.0f, 0.0f, 1.0f};
        }
        return new GestorPersonas(semaforos, bits -> estadoSemaforo,
                new float[0][], new int[][]{{2}}, 50.0f, 100.0f);
    }

    private boolean dentro(float x, float z, float centroX, float centroZ,
            float semiancho, float semilargo) {
        return Math.abs(x - centroX) <= semiancho && Math.abs(z - centroZ) <= semilargo;
    }

    public void testRodeaAutoYContinuaSinAtravesarloNiSaltar() {
        GestorPersonas gestor = crearGestor(0);
        BiPredicate<Float, Float> auto = (x, z) -> dentro(x, z, 8, 10, 0.75f, 1.5f);
        gestor.setDetectorVehiculos(auto);
        Persona principal = gestor.getPeatones().get(5);
        Persona acompanante = gestor.getPeatones().get(6);
        float mayorDesvio = 0;
        float dt = 1.0f / 60;
        for (int i = 0; i < 720; i++) {
            float x0 = principal.getX();
            float z0 = principal.getZ();
            float ax0 = acompanante.getX();
            float az0 = acompanante.getZ();
            gestor.actualizar(dt);
            assertFalse("El peaton atraviesa el auto", auto.test(principal.getX(), principal.getZ()));
            assertFalse("El acompanante atraviesa el auto", auto.test(acompanante.getX(), acompanante.getZ()));
            assertTrue("El peaton salta de posicion", Math.hypot(principal.getX() - x0,
                    principal.getZ() - z0) <= 1.16f * dt);
            assertTrue("El acompanante salta de posicion", Math.hypot(acompanante.getX() - ax0,
                    acompanante.getZ() - az0) <= 1.28f * dt);
            mayorDesvio = Math.max(mayorDesvio, Math.abs(principal.getZ() - 10));
        }
        assertTrue("Debe apartarse por un costado", mayorDesvio > 1.5f);
        assertTrue("Debe continuar despues del auto", principal.getX() > 10);
        assertTrue("Su acompanante tambien debe continuar", acompanante.getX() > 9);
    }

    public void testEscogeElCostadoLibreCuandoHayDosAutos() {
        GestorPersonas gestor = crearGestor(0);
        gestor.setDetectorVehiculos((x, z) -> dentro(x, z, 8, 10, 0.75f, 1.5f)
                || dentro(x, z, 8, 12, 1.2f, 0.8f));
        Persona persona = gestor.getPeatones().get(0);
        float menorZ = persona.getZ();
        for (int i = 0; i < 720; i++) {
            gestor.actualizar(1.0f / 60);
            menorZ = Math.min(menorZ, persona.getZ());
        }
        assertTrue("Debe elegir el lado opuesto al segundo auto", menorZ < 8.5f);
        assertTrue("Debe superar ambos autos", persona.getX() > 10);
    }

    public void testSaleDeUnaHuellaInicialConPasosPequenos() {
        GestorPersonas gestor = crearGestor(0);
        BiPredicate<Float, Float> auto = (x, z) -> dentro(x, z, 5, 10, 0.7f, 1.3f);
        gestor.setDetectorVehiculos(auto);
        gestor.setMedidorDistanciaVehiculos((x, z) -> Math.hypot(x - 5, z - 10));
        Persona persona = gestor.getPeatones().get(0);
        assertTrue(auto.test(persona.getX(), persona.getZ()));
        boolean salio = false;
        double distanciaAnterior = Math.hypot(persona.getX() - 5, persona.getZ() - 10);
        for (int i = 0; i < 2400; i++) {
            gestor.actualizar(1.0f / 240);
            double distancia = Math.hypot(persona.getX() - 5, persona.getZ() - 10);
            if (auto.test(persona.getX(), persona.getZ())) {
                assertFalse("No debe volver a entrar al auto", salio);
                assertTrue("Debe alejarse del centro", distancia >= distanciaAnterior - 0.00001);
            } else {
                salio = true;
            }
            distanciaAnterior = distancia;
        }
        assertTrue("Debe salir del espacio ocupado por el auto", salio);
        assertTrue("Debe retomar su recorrido", persona.getX() > 7);
    }

    public void testEsperaSiNoHayPasoYContinuaCuandoSeLibera() {
        GestorPersonas gestor = crearGestor(0);
        boolean[] bloqueado = {true};
        gestor.setDetectorVehiculos((x, z) -> bloqueado[0] && x >= 7 && x <= 9);
        Persona persona = gestor.getPeatones().get(0);
        for (int i = 0; i < 300; i++) gestor.actualizar(1.0f / 60);
        assertTrue("No debe atravesar un paso cerrado", persona.getX() < 7);
        assertEquals(0.0f, persona.getVelocidad(), 0.0001f);
        bloqueado[0] = false;
        for (int i = 0; i < 300; i++) gestor.actualizar(1.0f / 60);
        assertTrue("Debe avanzar cuando el auto libera el paso", persona.getX() > 10);
    }

    public void testSigueEsperandoElSemaforoAntesDeCruzar() {
        GestorPersonas gestor = crearGestor(1);
        Persona persona = gestor.getPeatones().get(0);
        float x = persona.getX();
        float z = persona.getZ();
        for (int i = 0; i < 120; i++) gestor.actualizar(1.0f / 60);
        assertEquals(x, persona.getX(), 0.0001f);
        assertEquals(z, persona.getZ(), 0.0001f);
        assertEquals(EstadoPersona.ESPERANDO_SEMAFORO, persona.getEstado());
    }

    public void testRecalculaSiOtroAutoBloqueaElDesvio() {
        GestorPersonas gestor = crearGestor(0);
        float[] ladoBloqueado = {0};
        BiPredicate<Float, Float> autos = (x, z) -> dentro(x, z, 8, 10, 0.75f, 1.5f)
                || (ladoBloqueado[0] != 0 && dentro(x, z, 8, 10 + ladoBloqueado[0] * 2, 0.75f, 0.6f));
        gestor.setDetectorVehiculos(autos);
        Persona persona = gestor.getPeatones().get(0);
        for (int i = 0; i < 1100; i++) {
            gestor.actualizar(1.0f / 60);
            if (ladoBloqueado[0] == 0 && Math.abs(persona.getZ() - 10) > 0.2f) {
                ladoBloqueado[0] = Math.signum(persona.getZ() - 10);
            }
            assertFalse("Debe comprobar el camino tambien despues de planearlo",
                    autos.test(persona.getX(), persona.getZ()));
            if (persona.getX() > 10) break;
        }
        assertTrue("La prueba debe bloquear un desvio ya iniciado", ladoBloqueado[0] != 0);
        assertTrue("Debe encontrar otro camino y continuar", persona.getX() > 10);
    }

    public void testPaseanteEntreParquesRodeaNodosOcupadosDentroDelPasoCebra() {
        GestorPersonas gestor = new GestorPersonas(
                new float[][]{{-4.5f, -7.7f, 0, 0, 1}}, bits -> 0,
                new float[][]{{-10, -10}, {10, -10}},
                new int[][]{{2, 0, 2}, {0, 0, 0}, {0, 0, 0}}, 15, 10);
        BiPredicate<Float, Float> auto = (x, z) -> dentro(x, z, 0, -8, 0.75f, 1.1f);
        gestor.setDetectorVehiculos(auto);
        Persona persona = gestor.getPeatones().get(0);
        boolean llegoAlOtroParque = false;
        for (int i = 0; i < 6000; i++) {
            gestor.actualizar(1.0f / 60);
            assertFalse("El paseante no debe atravesar el auto", auto.test(persona.getX(), persona.getZ()));
            if (Math.abs(persona.getX()) < 5) {
                assertTrue("Debe permanecer dentro del paso cebra",
                        Math.abs(persona.getZ() + 10) <= 1.25f);
            }
            if (persona.getX() > 6) {
                llegoAlOtroParque = true;
                break;
            }
        }
        assertTrue("Debe superar los nodos ocupados y continuar hacia el otro parque: "
                + persona.getX() + ", " + persona.getZ() + ", " + persona.getEstado(), llegoAlOtroParque);
    }

    private void comprobarPasoPorLadoLibre(float ladoAuto, float autoX) {
        float[][] semaforos = new float[27][5];
        for (int i = 0; i < semaforos.length; i++) {
            semaforos[i] = new float[]{-4.5f, 2.3f, 0, 0, 1};
        }
        GestorPersonas gestor = new GestorPersonas(semaforos, bits -> 0, new float[0][],
                new int[][]{{0, 0, 0}, {2, 0, 2}, {0, 0, 0}}, 15, 10);
        BiPredicate<Float, Float> auto = (x, z) -> dentro(x, z, autoX, ladoAuto * 1.2f, 0.75f, 1.5f);
        gestor.setDetectorVehiculos(auto);
        Persona principal = gestor.getPeatones().get(5);
        Persona acompanante = gestor.getPeatones().get(6);
        boolean pasoPorLadoLibre = false;
        boolean llegaron = false;
        for (int i = 0; i < 1200; i++) {
            gestor.actualizar(1.0f / 60);
            for (Persona persona : new Persona[]{principal, acompanante}) {
                assertFalse("Debe rodear el auto", auto.test(persona.getX(), persona.getZ()));
                if (Math.abs(persona.getX()) < 5) {
                    assertTrue("Debe pisar el ancho dibujado del paso cebra: " + persona.getZ(),
                            Math.abs(persona.getZ()) <= 1.25001f);
                }
            }
            if (Math.abs(principal.getX() - autoX) < 0.75f) {
                pasoPorLadoLibre |= principal.getZ() * ladoAuto < -0.3f;
            }
            if (principal.getX() > 5 && acompanante.getX() > 5) {
                llegaron = true;
                break;
            }
        }
        assertTrue("Debe aprovechar el espacio del lado opuesto al auto", pasoPorLadoLibre);
        assertTrue("Ambos deben completar el cruce, incluso con la llegada ocupada: "
                + principal.getX() + ", " + principal.getZ() + " / "
                + acompanante.getX() + ", " + acompanante.getZ(), llegaron);
    }

    public void testUsaHuecoIzquierdoDentroDelPasoDibujado() {
        comprobarPasoPorLadoLibre(1, -2);
    }

    public void testUsaHuecoDerechoDentroDelPasoDibujado() {
        comprobarPasoPorLadoLibre(-1, -2);
    }

    public void testLlegadaOcupadaPermiteSalirPorLaIzquierda() {
        comprobarPasoPorLadoLibre(1, 4);
    }

    public void testLlegadaOcupadaPermiteSalirPorLaDerecha() {
        comprobarPasoPorLadoLibre(-1, 4);
    }

    public void testAnchoDibujadoConservaLasRutasDelMapaReal() {
        assertNotNull(new CiudadDePrueba().crearGestor());
    }

    private void comprobarFilaQueTapaTodoElPaso(float sentidoFila) {
        float[][] semaforos = new float[27][5];
        for (int i = 0; i < semaforos.length; i++) semaforos[i] = new float[]{-4.5f, 2.3f, 0, 0, 1};
        int[] estadoSemaforo = {0};
        GestorPersonas gestor = new GestorPersonas(semaforos, bits -> estadoSemaforo[0], new float[0][],
                new int[][]{{0, 0, 0}, {2, 0, 2}, {0, 0, 0}}, 15, 10);
        Huella[] autos = new Huella[3];
        for (int i = 0; i < autos.length; i++) {
            // Dimensiones reales del NPC mas el margen de 0.32 m del peaton.
            autos[i] = new Huella(-2, sentidoFila * 2.8f * i, 0, 1.05f, 1.50f);
        }
        gestor.setDetectorVehiculos((x, z) -> {
            for (Huella huella : autos) if (huella.contiene(x, z, 0)) return true;
            return false;
        });
        gestor.setMedidorDistanciaVehiculos((x, z) -> {
            double distancia = Double.POSITIVE_INFINITY;
            for (Huella huella : autos) distancia = Math.min(distancia, huella.distanciaFirmada(x, z));
            return distancia;
        });
        Persona guia = gestor.getPeatones().get(5);
        Persona acompanante = gestor.getPeatones().get(6);
        boolean rodeoElFrente = false;
        boolean llegaron = false;
        for (int i = 0; i < 1400; i++) {
            float x = guia.getX();
            float z = guia.getZ();
            gestor.actualizar(1.0f / 60);
            assertTrue("El rodeo debe caminar, sin saltos", Math.hypot(guia.getX() - x, guia.getZ() - z) < 0.02);
            for (Persona persona : new Persona[]{guia, acompanante}) {
                for (Huella huella : autos) assertFalse("No debe atravesar los autos",
                        huella.contiene(persona.getX(), persona.getZ(), 0));
                if (Math.abs(persona.getX()) < 5) assertTrue("El rodeo debe ser corto",
                        Math.abs(persona.getZ()) <= 1.90001f);
                if (persona.getX() > 3 && persona.getX() < 5) assertTrue("Debe volver al paso cebra",
                        Math.abs(persona.getZ()) <= 1.25001f);
            }
            if (Math.abs(guia.getX() + 2) < 1) rodeoElFrente |= guia.getZ() * sentidoFila < -1.5f;
            // El guia puede esperar el siguiente semaforo sin inmovilizar a quien viene detras.
            if (guia.getX() > 5) estadoSemaforo[0] = 1;
            if (guia.getX() > 5 && acompanante.getX() > 5) {
                llegaron = true;
                break;
            }
        }
        assertTrue("Debe apartarse un poco por el frente libre de la fila", rodeoElFrente);
        assertTrue("El guia y su acompanante deben superar la fila: " + guia.getX() + ", " + guia.getZ()
                + " / " + acompanante.getX() + ", " + acompanante.getZ(), llegaron);
    }

    public void testRodeaFilaCentradaPorElBordeIzquierdoYRetomaElCruce() {
        comprobarFilaQueTapaTodoElPaso(1);
    }

    public void testRodeaFilaCentradaPorElBordeDerechoYRetomaElCruce() {
        comprobarFilaQueTapaTodoElPaso(-1);
    }

    public void testAcompananteAtrasadoTerminaAunqueElGuiaEspereOtroSemaforo() {
        float[][] semaforos = new float[27][5];
        for (int i = 0; i < semaforos.length; i++) semaforos[i] = new float[]{-4.5f, 2.3f, 0, 0, 1};
        int[] estado = {0};
        GestorPersonas gestor = new GestorPersonas(semaforos, bits -> estado[0], new float[0][],
                new int[][]{{0, 0, 0}, {2, 0, 2}, {0, 0, 0}}, 15, 10);
        Persona guia = gestor.getPeatones().get(5);
        Persona acompanante = gestor.getPeatones().get(6);
        boolean quedoAtras = false;
        for (int i = 0; i < 1400; i++) {
            gestor.actualizar(1.0f / 60);
            if (!quedoAtras && guia.getX() > 3.8f) {
                // Representa un acompanante demorado por un obstaculo anterior.
                acompanante.setPosicion(0, 0, 0);
                estado[0] = 1;
                quedoAtras = true;
            }
        }
        assertTrue(quedoAtras);
        assertEquals(EstadoPersona.ESPERANDO_SEMAFORO, guia.getEstado());
        assertTrue("Debe salir de la calzada mientras el guia espera: " + acompanante.getX(),
                acompanante.getX() >= 5);
    }

    private static class CiudadDePrueba extends AppCiudad {
        GestorPersonas crearGestor() {
            java.util.List<float[]> parques = new java.util.ArrayList<>();
            for (int fila = 0; fila < MAPA.length; fila++) {
                for (int columna = 0; columna < MAPA[fila].length; columna++) {
                    if (MAPA[fila][columna] == 2) parques.add(new float[]{centro(columna), centro(fila)});
                }
            }
            return new GestorPersonas(semaforos, bits -> 0, parques.toArray(new float[0][]),
                    MAPA, LIMITE, CELDA);
        }
    }
}
