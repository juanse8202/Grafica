package com.graphics.sistemas;

import com.graphics.entidades.Automovil;
import com.graphics.entidades.Chata;
import com.graphics.entidades.Huella;
import com.graphics.entidades.EstadoPersona;
import com.graphics.entidades.Persona;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/** Actualiza el trafico sin realizar llamadas de renderizado OpenGL. */
public class GestorTrafico {
    private final List<Automovil> automoviles;
    private final float[][] semaforos;
    private final IntUnaryOperator consultarEstadoSemaforo;
    private final Map<Automovil, Float> tiempoBloqueadoPorJugador = new IdentityHashMap<>();
    private Automovil automovilJugador;
    private Chata chataJugador;
    private static final float TIEMPO_ANTES_DE_ESQUIVAR = 2.0f;
    private static final float DESVIO_LATERAL = 3.6f;

    public GestorTrafico(Automovil[] automoviles, float[][] semaforos,
            IntUnaryOperator consultarEstadoSemaforo) {
        this.automoviles = new ArrayList<>();
        Collections.addAll(this.automoviles, automoviles);
        this.semaforos = copiarSemaforos(semaforos);
        this.consultarEstadoSemaforo = consultarEstadoSemaforo;
    }

    public List<Automovil> getAutomoviles() {
        return Collections.unmodifiableList(automoviles);
    }

    /** Registra el auto del jugador para que el trafico tambien respete su espacio. */
    public void setAutomovilJugador(Automovil automovilJugador) {
        this.automovilJugador = automovilJugador;
    }

    public void setChataJugador(Chata chataJugador) {
        this.chataJugador = chataJugador;
    }

    public void actualizar(float deltaTime, Collection<Persona> peatones) {
        for (Automovil automovil : automoviles) {
            boolean semaforo = debeDetenerseEnSemaforo(automovil);
            boolean peaton = debeDetenerseAntePeaton(automovil, peatones);
            boolean otroAuto = debeDetenerseAnteOtroAuto(automovil);
            boolean autoALaIzquierda = debeCederAnteAutoALaIzquierda(automovil);
            boolean jugadorBloquea = debeDetenerseAnteJugador(automovil);

            if (automovil.estaEsquivando()) {
                if (!semaforo && !peaton && !otroAuto && !autoALaIzquierda && !jugadorBloquea) {
                    avanzarSinAtravesarChata(automovil, deltaTime);
                } else {
                    automovil.setVelocidad(0.0f);
                }
                continue;
            }

            // Un auto abandonado se rodea tras una breve espera, solo si el carril
            // lateral y la zona de reincorporacion estan libres.
            if (!semaforo && !peaton && !otroAuto && !autoALaIzquierda && jugadorBloquea
                    && automovilJugador.getConductor() == null
                    && Math.abs(automovilJugador.getVelocidad()) < 0.25f) {
                float espera = tiempoBloqueadoPorJugador.getOrDefault(automovil, 0.0f) + deltaTime;
                tiempoBloqueadoPorJugador.put(automovil, espera);
                if (espera >= TIEMPO_ANTES_DE_ESQUIVAR) {
                    float ladoPreferido = ladoLibreParaEsquivar(automovil);
                    float[] lados = {ladoPreferido, -ladoPreferido};
                    for (float lado : lados) {
                        if (puedeEsquivar(automovil, peatones, lado)
                                && automovil.iniciarEvasion(lado,
                                        automovilJugador.getX(), automovilJugador.getZ())) {
                            tiempoBloqueadoPorJugador.remove(automovil);
                            avanzarSinAtravesarChata(automovil, deltaTime);
                            break;
                        }
                    }
                    if (!automovil.estaEsquivando()) automovil.setVelocidad(0.0f);
                } else {
                    automovil.setVelocidad(0.0f);
                }
                continue;
            }

            tiempoBloqueadoPorJugador.remove(automovil);
            if (!semaforo && !peaton && !otroAuto && !autoALaIzquierda && !jugadorBloquea) {
                avanzarSinAtravesarChata(automovil, deltaTime);
            } else {
                automovil.setVelocidad(0.0f);
            }
        }
    }

    private void avanzarSinAtravesarChata(Automovil auto, float deltaTime) {
        int pasos = Math.max(1, (int) Math.ceil(auto.getVelocidadRuta() * Math.max(0, deltaTime) / 0.10f));
        for (int i = 0; i < pasos; i++) {
            Huella anterior = huellaAuto(auto);
            boolean libre = auto.actualizarSi(deltaTime / pasos, candidato -> {
                if (chataJugador == null) return true;
                Huella propuesta = huellaAuto(candidato);
                for (Huella chata : chataJugador.getHuellas(chataJugador.getEstado())) {
                    float nuevo = propuesta.solapamientoCon(chata);
                    float previo = anterior.solapamientoCon(chata);
                    if (nuevo > 0.00001f && (previo <= 0 || nuevo >= previo - 0.00001f)) return false;
                }
                return true;
            });
            if (!libre) {
                auto.setVelocidad(0);
                return;
            }
        }
        auto.setVelocidad(auto.getVelocidadRuta());
    }

    private Huella huellaAuto(Automovil auto) {
        return new Huella(auto.getXCarril(), auto.getZCarril(), auto.getAngulo(), 0.92f, 1.20f);
    }

    private boolean debeDetenerseAnteJugador(Automovil auto) {
        if (automovilJugador == null || auto == automovilJugador) return false;
        float frenteX = auto.getObjetivoX() - auto.getX();
        float frenteZ = auto.getObjetivoZ() - auto.getZ();
        float longitud = (float) Math.sqrt(frenteX * frenteX + frenteZ * frenteZ);
        if (longitud < 0.01f) return false;
        frenteX /= longitud;
        frenteZ /= longitud;
        float dx = automovilJugador.getX() - auto.getXCarril();
        float dz = automovilJugador.getZ() - auto.getZCarril();
        float adelante = dx * frenteX + dz * frenteZ;
        float lateral = Math.abs(dx * frenteZ - dz * frenteX);
        // Espera hasta estar cerca; a 12 m el rodeo se ve prematuro aunque el
        // auto estacionado todavia no represente un obstaculo inmediato.
        boolean bloqueaAuto = adelante > -0.8f && adelante < 7.5f && lateral < 2.0f;
        if (bloqueaAuto || chataJugador == null) return bloqueaAuto;
        for (Huella chata : chataJugador.getHuellas(chataJugador.getEstado())) {
            float haciaX = chata.x() - auto.getXCarril();
            float haciaZ = chata.z() - auto.getZCarril();
            float adelanteChata = haciaX * frenteX + haciaZ * frenteZ;
            float lateralChata = Math.abs(haciaX * frenteZ - haciaZ * frenteX);
            float radioFrente = chata.radioEnEje(frenteX, frenteZ);
            float radioLateral = chata.radioEnEje(frenteZ, -frenteX);
            if (adelanteChata > -radioFrente && adelanteChata < radioFrente + 4.2f
                    && lateralChata < radioLateral + 1.05f) return true;
        }
        return false;
    }

    private float ladoLibreParaEsquivar(Automovil auto) {
        float frenteX = auto.getObjetivoX() - auto.getX();
        float frenteZ = auto.getObjetivoZ() - auto.getZ();
        float longitud = Math.max(0.01f, (float) Math.sqrt(frenteX * frenteX + frenteZ * frenteZ));
        frenteX /= longitud;
        frenteZ /= longitud;
        float dx = automovilJugador.getX() - auto.getXCarril();
        float dz = automovilJugador.getZ() - auto.getZCarril();
        float ladoJugador = dx * frenteZ - dz * frenteX;
        return ladoJugador >= 0.0f ? -DESVIO_LATERAL : DESVIO_LATERAL;
    }

    private boolean puedeEsquivar(Automovil auto, Collection<Persona> peatones,
            float lateral) {
        float frenteX = auto.getObjetivoX() - auto.getX();
        float frenteZ = auto.getObjetivoZ() - auto.getZ();
        float longitud = (float) Math.sqrt(frenteX * frenteX + frenteZ * frenteZ);
        if (longitud < 0.01f) return false;
        frenteX /= longitud;
        frenteZ /= longitud;
        float ladoX = frenteZ * lateral;
        float ladoZ = -frenteX * lateral;
        float[] distanciasAdelante = {0.0f, 4.0f, 8.0f, 11.0f};
        for (float adelante : distanciasAdelante) {
            float x = auto.getX() + frenteX * adelante + ladoX;
            float z = auto.getZ() + frenteZ * adelante + ladoZ;
            if (automovilJugador != null
                    && distanciaCuadrada(x, z, automovilJugador.getX(), automovilJugador.getZ()) < 10.0f) {
                return false;
            }
            if (chataJugador != null) {
                Huella propuesta = new Huella(x, z, auto.getAngulo(), 0.92f, 1.20f);
                for (Huella huella : chataJugador.getHuellas(chataJugador.getEstado())) {
                    if (propuesta.solapamientoCon(huella) >= 0) return false;
                }
            }
            for (Automovil otro : automoviles) {
                if (otro != auto && distanciaCuadrada(x, z, otro.getXCarril(), otro.getZCarril()) < 16.0f) {
                    return false;
                }
            }
            for (Persona peaton : peatones) {
                if (distanciaCuadrada(x, z, peaton.getX(), peaton.getZ()) < 6.25f) {
                    return false;
                }
            }
        }
        return true;
    }

    private float distanciaCuadrada(float x1, float z1, float x2, float z2) {
        float dx = x1 - x2;
        float dz = z1 - z2;
        return dx * dx + dz * dz;
    }

    /** Conserva una distancia basica cuando dos rutas se encuentran. */
    private boolean debeDetenerseAnteOtroAuto(Automovil auto) {
        float frenteX = auto.getObjetivoX() - auto.getX();
        float frenteZ = auto.getObjetivoZ() - auto.getZ();
        float longitud = (float) Math.sqrt(frenteX * frenteX + frenteZ * frenteZ);
        if (longitud < 0.01f) return false;
        frenteX /= longitud;
        frenteZ /= longitud;

        for (Automovil otro : automoviles) {
            if (otro == auto) continue;
            // La distancia de seguridad aplica a autos que siguen el mismo carril.
            // Frenar ante cualquier auto cercano bloqueaba dos rutas perpendiculares
            // cuando ambos llegaban a una interseccion al mismo tiempo.
            float otroFrenteX = otro.getObjetivoX() - otro.getX();
            float otroFrenteZ = otro.getObjetivoZ() - otro.getZ();
            float otroLongitud = (float) Math.sqrt(otroFrenteX * otroFrenteX + otroFrenteZ * otroFrenteZ);
            if (otroLongitud < 0.01f) continue;
            otroFrenteX /= otroLongitud;
            otroFrenteZ /= otroLongitud;
            float mismoSentido = frenteX * otroFrenteX + frenteZ * otroFrenteZ;
            if (mismoSentido < 0.85f) continue;

            float dx = otro.getXCarril() - auto.getXCarril();
            float dz = otro.getZCarril() - auto.getZCarril();
            float adelante = dx * frenteX + dz * frenteZ;
            float lateral = Math.abs(dx * frenteZ - dz * frenteX);
            if (adelante > 0.0f && adelante < 3.4f && lateral < 1.35f) return true;
        }
        return false;
    }

    /** Cede cuando una trayectoria de conflicto se aproxima desde el lado izquierdo. */
    private boolean debeCederAnteAutoALaIzquierda(Automovil auto) {
        float frenteX = -(float) Math.sin(auto.getAngulo());
        float frenteZ = -(float) Math.cos(auto.getAngulo());
        float izquierdaX = frenteZ;
        float izquierdaZ = -frenteX;
        float velocidadAuto = Math.max(0.0f, auto.getVelocidad());

        for (Automovil otro : automoviles) {
            if (otro == auto || otro.getVelocidad() < 0.05f) continue;

            float diferenciaX = otro.getXCarril() - auto.getXCarril();
            float diferenciaZ = otro.getZCarril() - auto.getZCarril();
            if (diferenciaX * diferenciaX + diferenciaZ * diferenciaZ > 18.0f * 18.0f) continue;

            float adelantePropio = diferenciaX * frenteX + diferenciaZ * frenteZ;
            float aLaIzquierda = diferenciaX * izquierdaX + diferenciaZ * izquierdaZ;
            if (adelantePropio < -2.0f || adelantePropio > 14.0f || aLaIzquierda <= 0.0f) continue;

            float otroFrenteX = -(float) Math.sin(otro.getAngulo());
            float otroFrenteZ = -(float) Math.cos(otro.getAngulo());
            float alineacion = frenteX * otroFrenteX + frenteZ * otroFrenteZ;
            // El seguimiento y el trafico en sentidos opuestos tienen reglas propias.
            if (Math.abs(alineacion) > 0.85f) continue;
            float adelanteOtro = -diferenciaX * otroFrenteX - diferenciaZ * otroFrenteZ;
            if (adelanteOtro < -2.0f || adelanteOtro > 14.0f) continue;

            float velocidadOtro = Math.max(0.0f, otro.getVelocidad());
            float velocidadRelativaX = otroFrenteX * velocidadOtro - frenteX * velocidadAuto;
            float velocidadRelativaZ = otroFrenteZ * velocidadOtro - frenteZ * velocidadAuto;
            float velocidadRelativaCuadrada = velocidadRelativaX * velocidadRelativaX
                    + velocidadRelativaZ * velocidadRelativaZ;
            if (velocidadRelativaCuadrada < 0.01f) continue;

            float tiempoEncuentro = -(diferenciaX * velocidadRelativaX
                    + diferenciaZ * velocidadRelativaZ) / velocidadRelativaCuadrada;
            if (tiempoEncuentro < 0.0f || tiempoEncuentro > 2.5f) continue;

            float separacionX = diferenciaX + velocidadRelativaX * tiempoEncuentro;
            float separacionZ = diferenciaZ + velocidadRelativaZ * tiempoEncuentro;
            if (separacionX * separacionX + separacionZ * separacionZ < 3.4f * 3.4f) {
                return true;
            }
        }
        return false;
    }

    public void reiniciar() {
        tiempoBloqueadoPorJugador.clear();
        for (Automovil automovil : automoviles) {
            automovil.reiniciar();
        }
    }

    private boolean debeDetenerseEnSemaforo(Automovil auto) {
        float frenteX = auto.getObjetivoX() - auto.getX();
        float frenteZ = auto.getObjetivoZ() - auto.getZ();
        float longitud = (float) Math.sqrt(frenteX * frenteX + frenteZ * frenteZ);

        if (longitud < 0.01f) {
            return false;
        }

        frenteX /= longitud;
        frenteZ /= longitud;

        for (float[] semaforo : semaforos) {
            if (consultarEstadoSemaforo.applyAsInt(Float.floatToIntBits(semaforo[2])) != 0) {
                continue;
            }

            float mismoSentido = frenteX * semaforo[3] + frenteZ * semaforo[4];
            if (mismoSentido < 0.95f) {
                continue;
            }

            float haciaSemaforoX = semaforo[0] - auto.getX();
            float haciaSemaforoZ = semaforo[1] - auto.getZ();
            float adelante = haciaSemaforoX * frenteX + haciaSemaforoZ * frenteZ;
            float lateral = Math.abs(haciaSemaforoX * frenteZ - haciaSemaforoZ * frenteX);

            // Mantiene la misma distancia de parada anterior a la esquina.
            if (adelante > 0.4f && adelante < 3.5f && lateral < 5.2f) {
                return true;
            }
        }
        return false;
    }

    /** Evita que el trafico avance hacia una persona que esta cruzando. */
    private boolean debeDetenerseAntePeaton(Automovil auto, Collection<Persona> peatones) {
        float frenteX = auto.getObjetivoX() - auto.getX();
        float frenteZ = auto.getObjetivoZ() - auto.getZ();
        float longitud = (float) Math.sqrt(frenteX * frenteX + frenteZ * frenteZ);
        if (longitud < 0.01f) return false;
        frenteX /= longitud;
        frenteZ /= longitud;

        for (Persona peaton : peatones) {
            EstadoPersona estadoPeaton = peaton.getEstado();
            // Un peatón que espera permanece en la acera y no debe detener
            // toda la fila de autos; el semáforo ya regula cuándo puede cruzar.
            boolean cruzando = estadoPeaton == EstadoPersona.CRUZANDO;
            if (!cruzando) continue;
            float haciaX = peaton.getX() - auto.getXCarril();
            float haciaZ = peaton.getZ() - auto.getZCarril();
            float adelante = haciaX * frenteX + haciaZ * frenteZ;
            float lateral = Math.abs(haciaX * frenteZ - haciaZ * frenteX);
            // Solo frena si el peatón está realmente delante y dentro del
            // corredor del auto; evita congelar rutas cercanas pero paralelas.
            // El auto cede ante una persona solo cuando la trayectoria se
            // superpone de verdad con su huella. Un radio amplio hacia delante
            // hace que cada peaton del cruce detenga varias cuadras de trafico.
            float distanciaFrenado = 4.2f;
            if (adelante > -0.4f && adelante < distanciaFrenado && lateral < 1.55f) {
                return true;
            }
        }
        return false;
    }

    private float[][] copiarSemaforos(float[][] originales) {
        float[][] copia = new float[originales.length][];
        for (int i = 0; i < originales.length; i++) {
            copia[i] = originales[i].clone();
        }
        return copia;
    }
}
