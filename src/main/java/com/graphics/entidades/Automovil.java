package com.graphics.entidades;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Representa los datos y el movimiento de un automovil de la ciudad.
 * El renderizado OpenGL se conserva en SimuladorCiudad.
 */
public class Automovil {
    private static final int PUNTOS_POR_GIRO = 32;
    // Los autos circulan desplazados 2 m del centro de la ruta. Un radio
    // menor dejaba la trayectoria interior casi sin curva y parecia un pivote.
    private static final float RADIO_MAXIMO_GIRO = 5.0f;
    private final float[][] ruta;
    private final float velocidadRuta;
    private float velocidad;
    private final float r;
    private final float g;
    private final float b;
    private final float xInicial;
    private final float zInicial;

    private float x;
    private float z;
    private float angulo;
    private int siguientePunto;
    private boolean perteneceAlJugador;
    private Persona conductor;
    private float[][] rutaEvasion;
    private int siguientePuntoEvasion;

    public Automovil(float[][] ruta, float velocidad, float r, float g, float b) {
        if (ruta == null || ruta.length < 2) {
            throw new IllegalArgumentException("La ruta debe tener al menos dos puntos");
        }

        this.ruta = crearRutaSuave(copiarRuta(ruta));
        this.velocidadRuta = velocidad;
        this.velocidad = velocidad;
        this.r = r;
        this.g = g;
        this.b = b;
        xInicial = this.ruta[0][0];
        zInicial = this.ruta[0][1];
        reiniciar();
    }

    /** Reemplaza las esquinas rectas de la ruta por arcos cubicos cortos. */
    private float[][] crearRutaSuave(float[][] original) {
        if (original.length < 3) return original;
        float[][] entrada = new float[original.length][2];
        float[][] salida = new float[original.length][2];
        float[][] controlEntrada = new float[original.length][2];
        float[][] controlSalida = new float[original.length][2];

        for (int i = 0; i < original.length; i++) {
            float[] previo = original[(i + original.length - 1) % original.length];
            float[] actual = original[i];
            float[] siguiente = original[(i + 1) % original.length];
            float largoEntrada = distancia(previo, actual);
            float largoSalida = distancia(actual, siguiente);
            float inX = (actual[0] - previo[0]) / Math.max(largoEntrada, 0.001f);
            float inZ = (actual[1] - previo[1]) / Math.max(largoEntrada, 0.001f);
            float outX = (siguiente[0] - actual[0]) / Math.max(largoSalida, 0.001f);
            float outZ = (siguiente[1] - actual[1]) / Math.max(largoSalida, 0.001f);
            float producto = inX * outX + inZ * outZ;
            float radio = producto > 0.98f ? 0.0f
                    : Math.min(RADIO_MAXIMO_GIRO, Math.min(largoEntrada, largoSalida) * 0.25f);

            entrada[i][0] = actual[0] - inX * radio;
            entrada[i][1] = actual[1] - inZ * radio;
            salida[i][0] = actual[0] + outX * radio;
            salida[i][1] = actual[1] + outZ * radio;
            float control = radio * 0.5523f;
            controlEntrada[i][0] = entrada[i][0] + inX * control;
            controlEntrada[i][1] = entrada[i][1] + inZ * control;
            controlSalida[i][0] = salida[i][0] - outX * control;
            controlSalida[i][1] = salida[i][1] - outZ * control;
        }

        List<float[]> puntos = new ArrayList<>();
        puntos.add(salida[0]);
        for (int esquina = 1; esquina < original.length; esquina++) {
            puntos.add(entrada[esquina]);
            if (distancia(entrada[esquina], salida[esquina]) > 0.001f) {
                agregarCurvaRuta(puntos, entrada[esquina], controlEntrada[esquina],
                        controlSalida[esquina], salida[esquina], PUNTOS_POR_GIRO);
            }
        }
        puntos.add(entrada[0]);
        // La ruta se cierra con el ultimo tramo de la curva inicial, sin duplicar
        // su punto final: al envolver el indice, el auto sigue esa misma curva.
        if (distancia(entrada[0], salida[0]) > 0.001f) {
            agregarCurvaRuta(puntos, entrada[0], controlEntrada[0],
                    controlSalida[0], salida[0], PUNTOS_POR_GIRO - 1);
        }
        return puntos.toArray(new float[0][]);
    }

    private void agregarCurvaRuta(List<float[]> puntos, float[] inicio, float[] control1,
            float[] control2, float[] fin, int pasos) {
        for (int i = 1; i <= pasos; i++) {
            float t = i / (float) PUNTOS_POR_GIRO;
            float inverso = 1.0f - t;
            float a = inverso * inverso * inverso;
            float b = 3.0f * inverso * inverso * t;
            float c = 3.0f * inverso * t * t;
            float d = t * t * t;
            puntos.add(new float[]{a * inicio[0] + b * control1[0]
                    + c * control2[0] + d * fin[0],
                a * inicio[1] + b * control1[1]
                    + c * control2[1] + d * fin[1]});
        }
    }

    private float distancia(float[] a, float[] b) {
        return (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
    }

    private float[][] copiarRuta(float[][] original) {
        float[][] copia = new float[original.length][2];
        for (int i = 0; i < original.length; i++) {
            copia[i][0] = original[i][0];
            copia[i][1] = original[i][1];
        }
        return copia;
    }

    public float getX() { return x; }
    public float getZ() { return z; }
    public float getAngulo() { return angulo; }
    /** Centro fisico del auto autonomo, desplazado al carril que tambien dibuja el simulador. */
    public float getXCarril() { return x + (float) Math.cos(angulo) * 2.0f; }
    public float getZCarril() { return z - (float) Math.sin(angulo) * 2.0f; }
    public float getVelocidad() { return velocidad; }
    public float getVelocidadRuta() { return velocidadRuta; }
    public void setVelocidad(float velocidad) { this.velocidad = velocidad; }
    public void setPosicion(float x, float z) { this.x = x; this.z = z; }
    public void setAngulo(float angulo) { this.angulo = angulo; }
    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }
    public int getSiguientePunto() { return siguientePunto; }
    public float getObjetivoX() { return ruta[siguientePunto][0]; }
    public float getObjetivoZ() { return ruta[siguientePunto][1]; }
    public boolean perteneceAlJugador() { return perteneceAlJugador; }
    public void setPerteneceAlJugador(boolean perteneceAlJugador) {
        this.perteneceAlJugador = perteneceAlJugador;
    }
    public Persona getConductor() { return conductor; }
    public void setConductor(Persona conductor) { this.conductor = conductor; }

    /** Restablece el auto al inicio de su ruta, igual que AutoTrafico. */
    public void reiniciar() {
        x = xInicial;
        z = zInicial;
        velocidad = velocidadRuta;
        siguientePunto = 1;
        rutaEvasion = null;
        siguientePuntoEvasion = 0;
        actualizarOrientacion();
    }

    /** Inicia una maniobra temporal para rodear un vehiculo detenido. */
    public boolean iniciarEvasion(float desplazamientoLateral,
            float obstaculoX, float obstaculoZ) {
        if (rutaEvasion != null || velocidadRuta <= 0.0f) return false;
        float frenteX = ruta[siguientePunto][0] - x;
        float frenteZ = ruta[siguientePunto][1] - z;
        float longitud = (float) Math.sqrt(frenteX * frenteX + frenteZ * frenteZ);
        if (longitud < 0.01f) return false;
        frenteX /= longitud;
        frenteZ /= longitud;
        float ladoX = frenteZ;
        float ladoZ = -frenteX;
        float haciaObstaculoX = obstaculoX - x;
        float haciaObstaculoZ = obstaculoZ - z;
        float distanciaObstaculo = haciaObstaculoX * frenteX + haciaObstaculoZ * frenteZ;
        // No improvisa una diagonal a traves de una esquina: solo rebasa si cabe
        // la maniobra completa antes del siguiente giro de la ruta.
        if (distanciaObstaculo < 1.0f || distanciaObstaculo + 6.0f > longitud) return false;

        float inicioCambio = Math.max(2.0f, distanciaObstaculo - 5.0f);
        float finPaso = distanciaObstaculo + 2.7f;
        float finCambio = distanciaObstaculo + 6.0f;
        float[] inicio = {x, z};
        float[] ladoObstaculo = {
            x + frenteX * inicioCambio + ladoX * desplazamientoLateral,
            z + frenteZ * inicioCambio + ladoZ * desplazamientoLateral
        };
        float[] despuesObstaculo = {
            x + frenteX * finPaso + ladoX * desplazamientoLateral,
            z + frenteZ * finPaso + ladoZ * desplazamientoLateral
        };
        float[] reincorporacion = {
            x + frenteX * finCambio,
            z + frenteZ * finCambio
        };

        List<float[]> puntos = new ArrayList<>();
        agregarCurva(puntos, inicio,
                new float[]{x + frenteX * inicioCambio * 0.34f,
                    z + frenteZ * inicioCambio * 0.34f},
                new float[]{ladoObstaculo[0] - frenteX * inicioCambio * 0.34f,
                    ladoObstaculo[1] - frenteZ * inicioCambio * 0.34f},
                ladoObstaculo, 10);
        agregarLinea(puntos, ladoObstaculo, despuesObstaculo, 6);
        agregarCurva(puntos, despuesObstaculo,
                new float[]{despuesObstaculo[0] + frenteX * 1.2f,
                    despuesObstaculo[1] + frenteZ * 1.2f},
                new float[]{reincorporacion[0] - frenteX * 1.2f
                        + ladoX * desplazamientoLateral,
                    reincorporacion[1] - frenteZ * 1.2f
                        + ladoZ * desplazamientoLateral},
                reincorporacion, 10);
        rutaEvasion = puntos.toArray(new float[0][]);
        siguientePuntoEvasion = 0;
        return true;
    }

    private void agregarLinea(List<float[]> puntos, float[] inicio, float[] fin, int pasos) {
        for (int i = 1; i <= pasos; i++) {
            float t = i / (float) pasos;
            puntos.add(new float[]{inicio[0] + (fin[0] - inicio[0]) * t,
                inicio[1] + (fin[1] - inicio[1]) * t});
        }
    }

    private void agregarCurva(List<float[]> puntos, float[] inicio, float[] control1,
            float[] control2, float[] fin, int pasos) {
        for (int i = 1; i <= pasos; i++) {
            float t = i / (float) pasos;
            float inverso = 1.0f - t;
            float a = inverso * inverso * inverso;
            float b = 3.0f * inverso * inverso * t;
            float c = 3.0f * inverso * t * t;
            float d = t * t * t;
            puntos.add(new float[]{a * inicio[0] + b * control1[0]
                + c * control2[0] + d * fin[0],
                a * inicio[1] + b * control1[1]
                + c * control2[1] + d * fin[1]});
        }
    }

    public boolean estaEsquivando() { return rutaEvasion != null; }

    /** Avanza solo si la posicion es valida, conservando tambien los indices de ruta al bloquearse. */
    public boolean actualizarSi(float deltaTime, Predicate<Automovil> posicionValida) {
        float xAnterior = x;
        float zAnterior = z;
        float anguloAnterior = angulo;
        int puntoAnterior = siguientePunto;
        float[][] evasionAnterior = rutaEvasion;
        int puntoEvasionAnterior = siguientePuntoEvasion;
        actualizar(deltaTime);
        if (posicionValida.test(this)) return true;
        x = xAnterior;
        z = zAnterior;
        angulo = anguloAnterior;
        siguientePunto = puntoAnterior;
        rutaEvasion = evasionAnterior;
        siguientePuntoEvasion = puntoEvasionAnterior;
        return false;
    }

    /** Avanza hacia el punto actual de la ruta usando deltaTime. */
    public void actualizar(float deltaTime) {
        if (rutaEvasion != null) {
            actualizarEvasion(deltaTime);
            return;
        }
        float avanceRestante = velocidadRuta * deltaTime;
        int puntosRecorridos = 0;
        while (avanceRestante > 0.0001f && puntosRecorridos < ruta.length) {
            float destinoX = ruta[siguientePunto][0];
            float destinoZ = ruta[siguientePunto][1];
            float dx = destinoX - x;
            float dz = destinoZ - z;
            float distancia = (float) Math.hypot(dx, dz);
            if (distancia < 0.0001f) {
                siguientePunto = (siguientePunto + 1) % ruta.length;
                puntosRecorridos++;
                continue;
            }

            angulo = (float) Math.atan2(-dx, -dz);
            if (distancia <= avanceRestante) {
                x = destinoX;
                z = destinoZ;
                avanceRestante -= distancia;
                siguientePunto = (siguientePunto + 1) % ruta.length;
                puntosRecorridos++;
            } else {
                x += dx / distancia * avanceRestante;
                z += dz / distancia * avanceRestante;
                avanceRestante = 0.0f;
            }
        }
    }

    private void actualizarEvasion(float deltaTime) {
        float avanceRestante = velocidadRuta * deltaTime;
        int puntosRecorridos = 0;
        while (rutaEvasion != null && avanceRestante > 0.0001f
                && puntosRecorridos < rutaEvasion.length) {
            float destinoX = rutaEvasion[siguientePuntoEvasion][0];
            float destinoZ = rutaEvasion[siguientePuntoEvasion][1];
            float dx = destinoX - x;
            float dz = destinoZ - z;
            float distancia = (float) Math.hypot(dx, dz);
            if (distancia < 0.0001f) {
                siguientePuntoEvasion++;
                puntosRecorridos++;
            } else {
                angulo = (float) Math.atan2(-dx, -dz);
                if (distancia <= avanceRestante) {
                    x = destinoX;
                    z = destinoZ;
                    avanceRestante -= distancia;
                    siguientePuntoEvasion++;
                    puntosRecorridos++;
                } else {
                    x += dx / distancia * avanceRestante;
                    z += dz / distancia * avanceRestante;
                    avanceRestante = 0.0f;
                }
            }

            if (siguientePuntoEvasion >= rutaEvasion.length) {
                rutaEvasion = null;
                actualizarOrientacion();
            }
        }
    }

    private void actualizarOrientacion() {
        float dx = ruta[siguientePunto][0] - x;
        float dz = ruta[siguientePunto][1] - z;
        angulo = (float) Math.atan2(-dx, -dz);
    }
}
