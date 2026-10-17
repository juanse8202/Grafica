package com.graphics.sistemas;

import com.graphics.entidades.Automovil;
import com.graphics.entidades.Huella;
import com.graphics.entidades.Persona;
import junit.framework.TestCase;

/** Reproduce el orden real: primero se mueve el trafico y despues los peatones. */
public class CruceConTraficoTest extends TestCase {
    public void testParejaRodeaLaFilaDetenidaSobreElPasoSinBloquearseMutuamente() {
        float[][] semaforos = new float[28][5];
        for (int i = 0; i < 27; i++) semaforos[i] = new float[]{-4.5f, 2.3f, 0, 0, 1};
        semaforos[27] = new float[]{-4, -1, 0, 0, -1};
        GestorPersonas personas = new GestorPersonas(semaforos, bits -> 0, new float[0][],
                new int[][]{{0, 0, 0}, {2, 0, 2}, {0, 0, 0}}, 15, 10);
        Automovil[] autos = new Automovil[3];
        for (int i = 0; i < autos.length; i++) {
            autos[i] = new Automovil(new float[][]{{-4, i * 3.3f}, {-4, -20}}, 4, 1, 0, 0);
        }
        GestorTrafico trafico = new GestorTrafico(autos, semaforos, bits -> 0);
        personas.setDetectorVehiculos((x, z) -> {
            for (Automovil auto : autos) if (huella(auto).contiene(x, z, 0.32f)) return true;
            return false;
        });
        personas.setMedidorDistanciaVehiculos((x, z) -> {
            double distancia = Double.POSITIVE_INFINITY;
            for (Automovil auto : autos) distancia = Math.min(distancia, huella(auto).distanciaFirmada(x, z));
            return distancia;
        });
        Persona guia = personas.getPeatones().get(5);
        Persona acompanante = personas.getPeatones().get(6);
        boolean completaron = false;
        for (int i = 0; i < 1400; i++) {
            trafico.actualizar(1.0f / 60, personas.getPeatones());
            personas.actualizar(1.0f / 60);
            for (Persona persona : new Persona[]{guia, acompanante}) {
                for (Automovil auto : autos) assertFalse("Debe rodear la carroceria en la simulacion conjunta",
                        huella(auto).contiene(persona.getX(), persona.getZ(), 0));
            }
            if (guia.getX() > 5 && acompanante.getX() > 5) {
                completaron = true;
                break;
            }
        }
        assertTrue("Ambos deben superar el auto que sigue detenido en el cruce: "
                + guia.getX() + ", " + guia.getZ() + " / " + acompanante.getX() + ", " + acompanante.getZ(), completaron);
        assertEquals(0.0f, autos[0].getVelocidad(), 0.00001f);
        assertEquals(0.0f, autos[0].getZ(), 0.00001f);
    }

    private static Huella huella(Automovil auto) {
        return new Huella(auto.getXCarril(), auto.getZCarril(), auto.getAngulo(), 0.73f, 1.18f);
    }
}
