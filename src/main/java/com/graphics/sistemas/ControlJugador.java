package com.graphics.sistemas;

import com.graphics.entidades.Automovil;
import com.graphics.entidades.EstadoPersona;
import com.graphics.entidades.Persona;
import java.util.function.BiPredicate;

/** Coordina si las entradas del usuario controlan a la persona o a su auto. */
public class ControlJugador {
    private final Persona jugador;
    private final Automovil automovil;

    public ControlJugador(Persona jugador, Automovil automovil) {
        this.jugador = jugador;
        this.automovil = automovil;
        automovil.setPerteneceAlJugador(true);
        automovil.setConductor(jugador);
        jugador.setEstado(EstadoPersona.CONDUCIENDO);
    }

    public Persona getJugador() { return jugador; }
    public Automovil getAutomovil() { return automovil; }
    public boolean estaConduciendo() {
        return jugador.getEstado() == EstadoPersona.CONDUCIENDO;
    }

    /** Intenta salir por un costado libre, incluso si el auto sigue avanzando. */
    public boolean salirDelAutomovil(BiPredicate<Float, Float> posicionValida) {
        if (!estaConduciendo()) return false;

        float lateralX = (float) Math.cos(automovil.getAngulo());
        float lateralZ = -(float) Math.sin(automovil.getAngulo());
        // Prueba primero junto a la puerta y solo se aleja si la acera u otro
        // obstaculo ocupa ese espacio inmediato.
        for (float distancia : new float[]{1.7f, 1.95f, 2.25f, 2.6f}) {
            for (int lado : new int[]{1, -1}) {
                float x = automovil.getX() + lateralX * distancia * lado;
                float z = automovil.getZ() + lateralZ * distancia * lado;
                if (posicionValida.test(x, z)) {
                    jugador.setPosicion(x, 0.0f, z);
                    jugador.setAngulo(automovil.getAngulo());
                    jugador.setEstado(EstadoPersona.CAMINANDO);
                    automovil.setConductor(null);
                    return true;
                }
            }
        }
        return false;
    }

    /** Entra solo al auto propio y cuando la persona esta cerca. */
    public boolean entrarAlAutomovil() {
        if (estaConduciendo()) {
            return false;
        }

        float dx = jugador.getX() - automovil.getX();
        float dz = jugador.getZ() - automovil.getZ();
        if (dx * dx + dz * dz > 3.6f * 3.6f) {
            return false;
        }

        jugador.setEstado(EstadoPersona.CONDUCIENDO);
        automovil.setConductor(jugador);
        return true;
    }

    /** Actualiza el movimiento a pie y bloquea la posicion si no es valida. */
    public void actualizarCaminata(float ejeX, float ejeZ, float deltaTime, boolean correr,
            BiPredicate<Float, Float> posicionValida) {
        if (estaConduciendo()) {
            return;
        }

        float magnitud = (float) Math.sqrt(ejeX * ejeX + ejeZ * ejeZ);
        if (magnitud < 0.001f) {
            jugador.setVelocidad(0.0f);
            jugador.actualizarAnimacion(deltaTime, false);
            return;
        }

        ejeX /= magnitud;
        ejeZ /= magnitud;
        float velocidad = correr ? 5.0f : 3.0f;
        float siguienteX = jugador.getX() + ejeX * velocidad * deltaTime;
        float siguienteZ = jugador.getZ() + ejeZ * velocidad * deltaTime;
        jugador.setAngulo((float) Math.atan2(-ejeX, -ejeZ));
        jugador.setVelocidad(velocidad);
        if (posicionValida.test(siguienteX, siguienteZ)) {
            jugador.setPosicion(siguienteX, 0.0f, siguienteZ);
        }
        jugador.actualizarAnimacion(deltaTime, true);
    }

    public void reiniciar() {
        automovil.reiniciar();
        automovil.setVelocidad(0.0f);
        automovil.setConductor(jugador);
        jugador.setPosicion(automovil.getX(), 0.0f, automovil.getZ());
        jugador.setAngulo(automovil.getAngulo());
        jugador.setVelocidad(0.0f);
        jugador.setEstado(EstadoPersona.CONDUCIENDO);
        jugador.actualizarAnimacion(0.0f, false);
    }
}
