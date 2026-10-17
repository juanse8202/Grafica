package com.graphics;

import com.graphics.entidades.Automovil;
import com.graphics.entidades.Chata;
import com.graphics.entidades.Huella;
import com.graphics.entidades.Persona;
import com.graphics.sistemas.GestorPersonas;
import com.graphics.sistemas.GestorTrafico;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import junit.framework.TestCase;

public class SimuladorChataTest extends TestCase {
    private SimuladorCiudad ciudad;
    private Automovil auto;
    private Chata chata;
    private GestorTrafico trafico;
    private GestorPersonas personas;

    private <T> T campo(String nombre, Class<T> tipo) throws Exception {
        Field campo = SimuladorCiudad.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        return tipo.cast(campo.get(ciudad));
    }

    protected void setUp() throws Exception {
        ciudad = new SimuladorCiudad();
        auto = campo("automovilJugador", Automovil.class);
        chata = campo("chata", Chata.class);
        trafico = campo("gestorTrafico", GestorTrafico.class);
        personas = campo("gestorPersonas", GestorPersonas.class);
        for (Automovil npc : trafico.getAutomoviles()) npc.setPosicion(-100, -100);
        for (Persona persona : personas.getPeatones()) persona.setPosicion(-100, 0, -100);
    }

    public void testMovimientoDelConjuntoArticulaSinSepararElEnganche() {
        for (int i = 1; i <= 50; i++) {
            float angulo = i * 0.01f;
            assertTrue(ciudad.moverAutomovilConChata(8 * ((float) Math.cos(angulo) - 1),
                    -8 * (float) Math.sin(angulo), angulo));
        }
        assertTrue(auto.getAngulo() - chata.getEstado().angulo() > 0.1f);
        assertEquals(Chata.DISTANCIA_EJE_ENGANCHE, Math.hypot(
                chata.getEstado().x() - chata.getEstado().engancheX(),
                chata.getEstado().z() - chata.getEstado().engancheZ()), 0.00001);
    }

    public void testMarchaAtrasSeDetieneAntesDeMeterLaChataEnOtroAuto() {
        Automovil npc = trafico.getAutomoviles().get(0);
        npc.setAngulo(0);
        npc.setPosicion(-2, 5.2f); // Su centro visible esta en (0, 5.2).
        assertFalse(ciudad.moverAutomovilConChata(0, 2, 0));
        assertTrue(auto.getZ() < 0.2f);
        Huella otro = new Huella(npc.getXCarril(), npc.getZCarril(), 0, 0.92f, 1.20f);
        for (Huella huella : chata.getHuellas(chata.getEstado())) {
            assertTrue(huella.solapamientoCon(otro) <= 0.00001f);
        }
        assertTrue("Debe permitir alejarse del contacto", ciudad.moverAutomovilConChata(0, -1, 0));
    }

    public void testChataSeDetieneAntePeatonAunqueElAutoEsteLejos() {
        Persona persona = personas.getPeatones().get(0);
        persona.setPosicion(0, 0, 4.3f);
        assertFalse(ciudad.moverAutomovilConChata(0, 2, 0));
        assertTrue(auto.getZ() < 0.3f);
        assertFalse(chata.contiene(persona.getX(), persona.getZ(), 0.22f));
    }

    public void testChataColisionaConManzanaAntesQueElAuto() throws Exception {
        auto.setAngulo((float) Math.PI / 4);
        chata.reiniciar();
        assertFalse(ciudad.moverAutomovilConChata(2.5f, 2.5f, auto.getAngulo()));
        assertTrue(campo("motivoBloqueo", String.class).startsWith("chata junto a"));
    }

    public void testChataRespetaElLimiteDeLaCiudad() throws Exception {
        auto.setPosicion(0, 99);
        chata.reiniciar();
        assertFalse(ciudad.moverAutomovilConChata(0, 103, 0));
        assertEquals("chata en el limite de la ciudad", campo("motivoBloqueo", String.class));
    }

    public void testJugadorYPeatonesDetectanLaCajaYLaBarra() throws Exception {
        Method caminar = SimuladorCiudad.class.getDeclaredMethod("puedeCaminar", float.class, float.class);
        Method detectar = SimuladorCiudad.class.getDeclaredMethod("hayVehiculoCercaDelPeaton", float.class, float.class);
        caminar.setAccessible(true);
        detectar.setAccessible(true);
        for (float z : new float[]{3, 1.8f}) {
            assertEquals(false, caminar.invoke(ciudad, 0, z));
            assertEquals(true, detectar.invoke(ciudad, 0, z));
        }
    }

    public void testUnGiroCerradoNoPliegaLaChataDentroDelAuto() throws Exception {
        assertFalse(ciudad.moverAutomovilConChata(0, 0, 1.0f));
        assertEquals("angulo de la chata", campo("motivoBloqueo", String.class));
        Huella coche = new Huella(auto.getX(), auto.getZ(), auto.getAngulo(), 1.05f, 1.35f);
        java.util.List<Huella> huellas = chata.getHuellas(chata.getEstado());
        for (int i = 0; i < 3; i++) assertTrue(huellas.get(i).solapamientoCon(coche) <= 0.001f);
    }
}
