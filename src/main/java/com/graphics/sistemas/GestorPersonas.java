package com.graphics.sistemas;

import com.graphics.entidades.CrucePeatonal;
import com.graphics.entidades.EstadoPersona;
import com.graphics.entidades.Persona;
import com.graphics.entidades.TipoPersona;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.IntUnaryOperator;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleBiFunction;

/** Actualiza peatones, aceras y cruces; SimuladorCiudad realiza el dibujo OpenGL. */
public class GestorPersonas {
    private static final int SEMAFORO_ROJO = 0;
    // AppCiudad dibuja franjas de 2.5 m de ancho.
    private static final float SEMIANCHO_PASO_CEBRA = 1.25f;
    private static final float MARGEN_MAXIMO_RODEO = 0.65f;
    private final List<Persona> peatones = new ArrayList<>();
    private final List<CrucePeatonal> cruces = new ArrayList<>();
    private final List<CrucePeatonal> pasosCebra = new ArrayList<>();
    private final List<RecorridoPeaton> recorridos = new ArrayList<>();
    private final List<PaseoParque> paseosParque = new ArrayList<>();
    private final IntUnaryOperator consultarEstadoSemaforo;
    private final float limiteCiudad;
    private final float tamanoCelda;
    private final int[][] mapa;
    private RutaParque[] rutasInterParque;
    private BiPredicate<Float, Float> ocupadoPorVehiculo = (x, z) -> false;
    private ToDoubleBiFunction<Float, Float> distanciaVehiculo = (x, z) -> Double.POSITIVE_INFINITY;
    private record DesvioPeaton(List<float[]> puntos, CrucePeatonal cruce, float margen) {}
    private final Map<Persona, DesvioPeaton> rutasEvasion = new IdentityHashMap<>();
    private final Map<Persona, Float> esperaReintentoEvasion = new IdentityHashMap<>();

    public GestorPersonas(float[][] semaforos, IntUnaryOperator consultarEstadoSemaforo,
            float[][] parques, int[][] mapa, float limiteCiudad, float tamanoCelda) {
        this.consultarEstadoSemaforo = consultarEstadoSemaforo;
        this.limiteCiudad = limiteCiudad;
        this.tamanoCelda = tamanoCelda;
        this.mapa = mapa;
        crearCruces(semaforos);
        crearPeatones();
        rutasInterParque = crearRutasInterParque(parques);
        crearPaseantesParques(parques, rutasInterParque);
    }

    public List<Persona> getPeatones() {
        return Collections.unmodifiableList(peatones);
    }

    public List<CrucePeatonal> getCruces() {
        return Collections.unmodifiableList(cruces);
    }

    /** Conecta la simulación de peatones con las huellas reales de los vehículos. */
    public void setDetectorVehiculos(BiPredicate<Float, Float> detector) {
        ocupadoPorVehiculo = detector != null ? detector : (x, z) -> false;
    }

    /** Separacion respecto al vehiculo mas cercano; permite salir de una huella inicial. */
    public void setMedidorDistanciaVehiculos(ToDoubleBiFunction<Float, Float> medidor) {
        distanciaVehiculo = medidor != null ? medidor : (x, z) -> Double.POSITIVE_INFINITY;
    }

    /** Los peatones esperan fuera del paso y completan el cruce iniciado. */
    public void actualizar(float deltaTime) {
        if (deltaTime <= 0.0f) return;
        for (RecorridoPeaton recorrido : recorridos) {
            SegmentoPeatonal segmento = recorrido.segmentos.get(recorrido.indice);
            Persona persona = recorrido.persona;

            if (segmento.cruce != null && persona.getEstado() == EstadoPersona.ESPERANDO_SEMAFORO) {
                persona.actualizarAnimacion(deltaTime, false);
                int estado = consultarEstadoSemaforo.applyAsInt(
                        Float.floatToIntBits(segmento.cruce.getDesfaseSemaforo()));
                // Al ponerse rojo para los autos, inicia el cruce. Los autos
                // autonomos ceden al estado CRUZANDO; la verificacion de huella
                // y la evasion mantienen seguro el paso si un auto quedo encima.
                if (estado == SEMAFORO_ROJO) persona.setEstado(EstadoPersona.CRUZANDO);
                actualizarAcompanantes(recorrido, deltaTime);
                continue;
            }

            if (segmento.cruce == null) {
                persona.setEstado(EstadoPersona.CAMINANDO);
            }
            caminarHacia(persona, segmento.destinoX, segmento.destinoZ,
                    deltaTime, recorrido.velocidad);
            actualizarAcompanantes(recorrido, deltaTime);

            float dx = segmento.destinoX - persona.getX();
            float dz = segmento.destinoZ - persona.getZ();
            boolean llego = dx * dx + dz * dz < 0.04f;
            if (segmento.cruce != null) {
                // Un rodeo puede terminar al costado del punto original. Basta
                // alcanzar su extremo por dentro de la franja del mismo cruce.
                CrucePeatonal cruce = segmento.cruce;
                float ejeX = cruce.getFinX() - cruce.getInicioX();
                float ejeZ = cruce.getFinZ() - cruce.getInicioZ();
                float signo = (segmento.destinoX - (cruce.getInicioX() + cruce.getFinX()) * 0.5f)
                        * ejeX + (segmento.destinoZ - (cruce.getInicioZ() + cruce.getFinZ()) * 0.5f) * ejeZ;
                float pendiente = (dx * ejeX + dz * ejeZ) * Math.signum(signo);
                llego |= pendiente <= 0.12f && puntoDentroDelPaso(cruce, persona.getX(), persona.getZ());
            }
            if (llego) {
                if (segmento.cruce != null) {
                    persona.setEstado(EstadoPersona.CAMINANDO);
                }
                recorrido.indice = (recorrido.indice + 1) % recorrido.segmentos.size();
                SegmentoPeatonal siguiente = recorrido.segmentos.get(recorrido.indice);
                if (siguiente.cruce != null) {
                    persona.setEstado(EstadoPersona.ESPERANDO_SEMAFORO);
                }
            }
        }
        for (PaseoParque paseo : paseosParque) {
            paseo.actualizar(deltaTime, consultarEstadoSemaforo);
        }
    }

    private boolean segmentoLibre(float x0, float z0, float x1, float z1,
            CrucePeatonal cruce, float margen) {
        float distancia = (float) Math.hypot(x1 - x0, z1 - z0);
        int muestras = Math.max(1, (int) Math.ceil(distancia / 0.20f));
        boolean comienzaDentroDeAuto = ocupadoPorVehiculo.test(x0, z0);
        double distanciaAnteriorAuto = distanciaVehiculo.applyAsDouble(x0, z0);
        for (int muestra = 1; muestra <= muestras; muestra++) {
            float t = muestra / (float) muestras;
            float x = x0 + (x1 - x0) * t;
            float z = z0 + (z1 - z0) * t;
            boolean dentroDeAuto = ocupadoPorVehiculo.test(x, z);
            // Si una animacion o un auto en movimiento dejo al peaton dentro de
            // una huella, permite solamente el movimiento que lo saque de ella.
            double distanciaActualAuto = distanciaVehiculo.applyAsDouble(x, z);
            boolean sigueSaliendo = comienzaDentroDeAuto && dentroDeAuto
                    && distanciaActualAuto > distanciaAnteriorAuto + 0.0000001;
            if (!esTransitableEnRodeo(x, z, cruce, margen) || (dentroDeAuto && !sigueSaliendo)) {
                return false;
            }
            if (!dentroDeAuto) comienzaDentroDeAuto = false;
            distanciaAnteriorAuto = distanciaActualAuto;
        }
        return true;
    }

    /** A* local: rodea las huellas de los autos sin salir de la zona transitable. */
    private DesvioPeaton buscarRutaEvasion(float x0, float z0, float destinoX, float destinoZ,
            CrucePeatonal cruce, float margen) {
        final float paso = 0.2f;
        final int radio = 40; // Busca hasta 8 metros; detecta huecos de ambos lados.
        final int ancho = radio * 2 + 1;
        float distanciaDestino = (float) Math.hypot(destinoX - x0, destinoZ - z0);
        if (distanciaDestino > 6.0f) {
            destinoX = x0 + (destinoX - x0) * 6.0f / distanciaDestino;
            destinoZ = z0 + (destinoZ - z0) * 6.0f / distanciaDestino;
        }
        List<float[]> destinos = destinosLibresDeEvasion(x0, z0, destinoX, destinoZ);
        if (destinos.isEmpty()) return null;

        int cantidad = ancho * ancho;
        float[] costes = new float[cantidad];
        int[] anterior = new int[cantidad];
        boolean[] cerrado = new boolean[cantidad];
        Arrays.fill(costes, Float.POSITIVE_INFINITY);
        Arrays.fill(anterior, -1);
        int inicio = radio * ancho + radio;
        costes[inicio] = 0.0f;
        PriorityQueue<NodoRuta> abiertos = new PriorityQueue<>();
        abiertos.add(new NodoRuta(inicio, 0.0f));
        int fin = -1;
        float[] destinoFinal = null;
        while (!abiertos.isEmpty()) {
            int actual = abiertos.poll().indice;
            if (cerrado[actual]) continue;
            cerrado[actual] = true;
            int columna = actual % ancho;
            int fila = actual / ancho;
            float x = x0 + (columna - radio) * paso;
            float z = z0 + (fila - radio) * paso;
            for (float[] destino : destinos) {
                if (Math.hypot(destino[0] - x, destino[1] - z) <= paso
                        && segmentoLibre(x, z, destino[0], destino[1], cruce, margen)) {
                    fin = actual;
                    destinoFinal = destino;
                    break;
                }
            }
            if (fin >= 0) break;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) continue;
                    int nx = columna + dx;
                    int nz = fila + dz;
                    if (nx < 0 || nx >= ancho || nz < 0 || nz >= ancho) continue;
                    int siguiente = nz * ancho + nx;
                    float coste = costes[actual] + paso * (dx != 0 && dz != 0 ? 1.414214f : 1.0f);
                    if (cerrado[siguiente] || coste >= costes[siguiente]) continue;
                    float siguienteX = x0 + (nx - radio) * paso;
                    float siguienteZ = z0 + (nz - radio) * paso;
                    if (!segmentoLibre(x, z, siguienteX, siguienteZ, cruce, margen)) continue;
                    costes[siguiente] = coste;
                    anterior[siguiente] = actual;
                    float estimado = Float.POSITIVE_INFINITY;
                    for (float[] destino : destinos) {
                        estimado = Math.min(estimado, (float) Math.hypot(
                                destino[0] - siguienteX, destino[1] - siguienteZ));
                    }
                    abiertos.add(new NodoRuta(siguiente, coste + estimado));
                }
            }
        }
        if (fin < 0) return null;
        List<float[]> ruta = new ArrayList<>();
        ruta.add(destinoFinal);
        for (int nodo = fin; nodo != inicio; nodo = anterior[nodo]) {
            ruta.add(new float[]{x0 + (nodo % ancho - radio) * paso,
                    z0 + (nodo / ancho - radio) * paso});
        }
        Collections.reverse(ruta);
        return new DesvioPeaton(ruta, cruce, margen);
    }

    /** Prueba ambos lados aunque un auto ocupe el punto exacto de llegada. */
    private List<float[]> destinosLibresDeEvasion(float x0, float z0, float destinoX, float destinoZ) {
        List<float[]> destinos = new ArrayList<>();
        if (esTransitable(destinoX, destinoZ) && !ocupadoPorVehiculo.test(destinoX, destinoZ)) {
            destinos.add(new float[]{destinoX, destinoZ});
            return destinos;
        }
        float dx = destinoX - x0;
        float dz = destinoZ - z0;
        float longitud = (float) Math.hypot(dx, dz);
        if (longitud < 0.001f) return destinos;
        float ladoX = -dz / longitud;
        float ladoZ = dx / longitud;
        for (float lateral = 0.25f; lateral <= 2.5f; lateral += 0.25f) {
            for (int signo : new int[]{-1, 1}) {
                float x = destinoX + ladoX * lateral * signo;
                float z = destinoZ + ladoZ * lateral * signo;
                if (esTransitable(x, z) && !ocupadoPorVehiculo.test(x, z)) {
                    destinos.add(new float[]{x, z});
                }
            }
        }
        return destinos;
    }

    private void moverHaciaEvasion(Persona persona, DesvioPeaton desvio,
            float deltaTime, float velocidad) {
        List<float[]> ruta = desvio.puntos();
        float tiempoRestante = deltaTime;
        while (!ruta.isEmpty() && tiempoRestante > 0.000001f) {
            float[] destino = ruta.get(0);
            float distancia = (float) Math.hypot(destino[0] - persona.getX(),
                    destino[1] - persona.getZ());
            if (distancia < 0.0001f) {
                ruta.remove(0);
                continue;
            }
            if (!darPaso(persona, destino[0], destino[1], tiempoRestante, velocidad,
                    desvio.cruce(), desvio.margen())) {
                // Recalcula si otro auto ocupa el desvio mientras lo recorre.
                rutasEvasion.remove(persona);
                return;
            }
            tiempoRestante -= Math.min(distancia / velocidad, tiempoRestante);
            if (Math.hypot(destino[0] - persona.getX(), destino[1] - persona.getZ()) < 0.0001) {
                ruta.remove(0);
            }
        }
        if (ruta.isEmpty()) rutasEvasion.remove(persona);
        persona.actualizarAnimacion(deltaTime, persona.getVelocidad() > 0.0f);
    }

    private boolean darPaso(Persona persona, float destinoX, float destinoZ,
            float deltaTime, float velocidad) {
        return darPaso(persona, destinoX, destinoZ, deltaTime, velocidad, null, 0);
    }

    private boolean darPaso(Persona persona, float destinoX, float destinoZ,
            float deltaTime, float velocidad, CrucePeatonal cruce, float margen) {
        float dx = destinoX - persona.getX();
        float dz = destinoZ - persona.getZ();
        float distancia = (float) Math.hypot(dx, dz);
        if (distancia < 0.0001f) return true;
        float avance = Math.min(velocidad * deltaTime, distancia);
        float x = persona.getX() + dx / distancia * avance;
        float z = persona.getZ() + dz / distancia * avance;
        if (!segmentoLibre(persona.getX(), persona.getZ(), x, z, cruce, margen)) {
            persona.setVelocidad(0.0f);
            persona.actualizarAnimacion(deltaTime, false);
            return false;
        }
        persona.setPosicion(x, 0.0f, z);
        persona.setAngulo((float) Math.atan2(-dx, -dz));
        persona.setVelocidad(velocidad);
        return true;
    }

    public void reiniciar() {
        rutasEvasion.clear();
        esperaReintentoEvasion.clear();
        for (RecorridoPeaton recorrido : recorridos) {
            recorrido.reiniciar();
        }
        for (PaseoParque paseo : paseosParque) paseo.reiniciar();
    }

    /** Añade 10 parejas y 10 paseantes individuales, repartidos por todos los parques. */
    private void crearPaseantesParques(float[][] parques, RutaParque[] rutas) {
        if (parques == null || parques.length == 0) return;
        float[][] colores = {
            {0.88f, 0.24f, 0.18f}, {0.15f, 0.55f, 0.92f},
            {0.20f, 0.72f, 0.34f}, {0.88f, 0.68f, 0.12f},
            {0.62f, 0.28f, 0.82f}, {0.12f, 0.72f, 0.72f},
            {0.92f, 0.42f, 0.12f}, {0.85f, 0.45f, 0.60f}
        };
        int[] porParque = new int[parques.length];

        // Veinte personas en parejas (10 parejas), alternadas entre los parques.
        for (int pareja = 0; pareja < 10; pareja++) {
            int parque = pareja % parques.length;
            float[] centro = parques[parque];
            float lateral = (porParque[parque]++ % 3 - 1) * 0.75f;
            Persona hombre = nuevaPersonaParque(centro, colores[pareja % colores.length], TipoPersona.HOMBRE);
            Persona mujer = nuevaPersonaParque(centro, colores[(pareja + 3) % colores.length], TipoPersona.MUJER);
            peatones.add(hombre);
            peatones.add(mujer);
            paseosParque.add(new PaseoParque(parques,
                    parque, lateral, new Persona[]{hombre, mujer},
                    new float[]{0.0f, 0.86f}, pareja, rutas));
        }

        // Diez personas solas también recorren y descansan en los distintos parques.
        for (int sola = 0; sola < 10; sola++) {
            int parque = (sola + parques.length / 2) % parques.length;
            float[] centro = parques[parque];
            float lateral = (porParque[parque]++ % 3 - 1) * 0.75f;
            Persona persona = nuevaPersonaParque(centro, colores[(sola + 5) % colores.length],
                    sola % 2 == 0 ? TipoPersona.HOMBRE : TipoPersona.MUJER);
            peatones.add(persona);
            paseosParque.add(new PaseoParque(parques,
                    parque, lateral, new Persona[]{persona}, new float[]{0.0f}, sola + 10,
                    rutas));
        }
    }

    /** Precalcula caminos entre parques usando aceras y pasos cebra dibujados en el mapa. */
    private RutaParque[] crearRutasInterParque(float[][] parques) {
        RutaParque[] rutas = new RutaParque[parques.length];
        for (int i = 0; i < parques.length; i++) {
            int siguiente = (i + 1) % parques.length;
            rutas[i] = buscarRutaPeatonal(parques[i], parques[siguiente]);
        }
        return rutas;
    }

    private RutaParque buscarRutaPeatonal(float[] origen, float[] destino) {
        int minimo = (int) Math.ceil(-limiteCiudad + 1.0f);
        int maximo = (int) Math.floor(limiteCiudad - 1.0f);
        int ancho = maximo - minimo + 1;
        int cantidad = ancho * ancho;
        int[] anterior = new int[cantidad];
        float[] coste = new float[cantidad];
        boolean[] cerrado = new boolean[cantidad];
        Arrays.fill(anterior, -1);
        Arrays.fill(coste, Float.POSITIVE_INFINITY);

        int inicioX = limitar(Math.round(origen[0]), minimo, maximo);
        int inicioZ = limitar(Math.round(origen[1] + 3.35f), minimo, maximo);
        int finX = limitar(Math.round(destino[0]), minimo, maximo);
        int finZ = limitar(Math.round(destino[1]), minimo, maximo);
        int inicio = indiceNodo(inicioX, inicioZ, minimo, ancho);
        int fin = indiceNodo(finX, finZ, minimo, ancho);
        PriorityQueue<NodoRuta> abiertos = new PriorityQueue<>();
        coste[inicio] = 0.0f;
        abiertos.add(new NodoRuta(inicio, distanciaManhattan(inicioX, inicioZ, finX, finZ)));
        int[] pasoX = {1, -1, 0, 0};
        int[] pasoZ = {0, 0, 1, -1};

        while (!abiertos.isEmpty()) {
            NodoRuta actual = abiertos.poll();
            if (cerrado[actual.indice]) continue;
            if (actual.indice == fin) break;
            cerrado[actual.indice] = true;
            int x = minimo + actual.indice % ancho;
            int z = minimo + actual.indice / ancho;

            for (int i = 0; i < pasoX.length; i++) {
                int siguienteX = x + pasoX[i];
                int siguienteZ = z + pasoZ[i];
                if (siguienteX < minimo || siguienteX > maximo
                        || siguienteZ < minimo || siguienteZ > maximo
                        || !esTransitable(siguienteX, siguienteZ)) continue;
                int siguiente = indiceNodo(siguienteX, siguienteZ, minimo, ancho);
                if (cerrado[siguiente] || coste[siguiente] <= coste[actual.indice] + 1.0f) continue;
                coste[siguiente] = coste[actual.indice] + 1.0f;
                anterior[siguiente] = actual.indice;
                float estimado = coste[siguiente]
                        + distanciaManhattan(siguienteX, siguienteZ, finX, finZ);
                abiertos.add(new NodoRuta(siguiente, estimado));
            }
        }

        if (inicio != fin && anterior[fin] < 0) {
            throw new IllegalStateException("No se encontro un camino por aceras entre los parques ["
                    + origen[0] + ", " + origen[1] + "] y ["
                    + destino[0] + ", " + destino[1] + "].");
        }
        List<PuntoRuta> camino = new ArrayList<>();
        for (int nodo = fin; nodo >= 0; nodo = anterior[nodo]) {
            camino.add(new PuntoRuta(minimo + nodo % ancho, minimo + nodo / ancho));
            if (nodo == inicio) break;
        }
        Collections.reverse(camino);
        return comprimirCruces(camino);
    }

    private boolean esTransitable(float x, float z) {
        int columna = (int) Math.floor((x + limiteCiudad) / tamanoCelda);
        int fila = (int) Math.floor((z + limiteCiudad) / tamanoCelda);
        if (fila < 0 || fila >= mapa.length || columna < 0
                || columna >= mapa[fila].length) return false;
        float centroX = -limiteCiudad + tamanoCelda * (columna + 0.5f);
        float centroZ = -limiteCiudad + tamanoCelda * (fila + 0.5f);
        int tipo = mapa[fila][columna];
        if (tipo == 1) {
            return Math.abs(x - centroX) >= 3.8f || Math.abs(z - centroZ) >= 3.8f;
        }
        if (tipo == 2) {
            // Evita los dos arboles que ocupan la franja norte del parque.
            boolean arbolIzquierdo = Math.abs(x - (centroX - 2.4f)) < 1.35f
                    && Math.abs(z - (centroZ - 2.2f)) < 1.35f;
            boolean arbolDerecho = Math.abs(x - (centroX + 2.3f)) < 1.35f
                    && Math.abs(z - (centroZ - 2.1f)) < 1.35f;
            return !arbolIzquierdo && !arbolDerecho;
        }
        return estaDentroDeUnPasoCebra(x, z);
    }

    private boolean esCalzada(float x, float z) {
        int columna = (int) Math.floor((x + limiteCiudad) / tamanoCelda);
        int fila = (int) Math.floor((z + limiteCiudad) / tamanoCelda);
        return fila >= 0 && fila < mapa.length && columna >= 0
                && columna < mapa[fila].length && mapa[fila][columna] == 0;
    }

    private boolean esTransitableEnRodeo(float x, float z, CrucePeatonal cruce, float margen) {
        if (esTransitable(x, z)) return true;
        // Excepcion local para un auto que tapa toda la pintura. La ruta debe
        // terminar de nuevo dentro del paso; nunca habilita otras calles ni edificios.
        return cruce != null && margen > 0 && esCalzada(x, z)
                && puntoDentroDelPaso(cruce, x, z, margen);
    }

    private CrucePeatonal cruceParaRodeo(float x, float z) {
        CrucePeatonal cercano = null;
        double menorDistancia = Double.POSITIVE_INFINITY;
        for (CrucePeatonal cruce : pasosCebra) {
            float medioX = (cruce.getInicioX() + cruce.getFinX()) * 0.5f;
            float medioZ = (cruce.getInicioZ() + cruce.getFinZ()) * 0.5f;
            boolean horizontal = Math.abs(cruce.getFinX() - cruce.getInicioX())
                    >= Math.abs(cruce.getFinZ() - cruce.getInicioZ());
            float lateral = horizontal ? Math.abs(z - medioZ) : Math.abs(x - medioX);
            float longitudinal = horizontal ? Math.abs(x - medioX) : Math.abs(z - medioZ);
            double distancia = Math.hypot(x - medioX, z - medioZ);
            if (lateral <= SEMIANCHO_PASO_CEBRA + MARGEN_MAXIMO_RODEO
                    && longitudinal <= 6.5f && distancia < menorDistancia) {
                cercano = cruce;
                menorDistancia = distancia;
            }
        }
        return cercano;
    }

    private boolean estaDentroDeUnPasoCebra(float x, float z) {
        for (CrucePeatonal cruce : pasosCebra) {
            if (puntoDentroDelPaso(cruce, x, z)) return true;
        }
        return false;
    }

    private boolean puntoDentroDelPaso(CrucePeatonal cruce, float x, float z) {
        return puntoDentroDelPaso(cruce, x, z, 0);
    }

    private boolean puntoDentroDelPaso(CrucePeatonal cruce, float x, float z, float margen) {
        boolean horizontal = Math.abs(cruce.getFinX() - cruce.getInicioX())
                >= Math.abs(cruce.getFinZ() - cruce.getInicioZ());
        if (horizontal) {
            float medioX = (cruce.getInicioX() + cruce.getFinX()) * 0.5f;
            return Math.abs(x - medioX) <= 5.3f
                    && Math.abs(z - cruce.getInicioZ()) <= SEMIANCHO_PASO_CEBRA + margen;
        }
        float medioZ = (cruce.getInicioZ() + cruce.getFinZ()) * 0.5f;
        return Math.abs(x - cruce.getInicioX()) <= SEMIANCHO_PASO_CEBRA + margen
                && Math.abs(z - medioZ) <= 5.3f;
    }

    private RutaParque comprimirCruces(List<PuntoRuta> camino) {
        List<PuntoRuta> puntos = new ArrayList<>();
        List<CrucePeatonal> crucesRuta = new ArrayList<>();
        if (camino.isEmpty()) return new RutaParque(new float[0][2], new CrucePeatonal[0]);
        // Conserva cada nodo de A*: al saltar varios nodos se creaban diagonales
        // que atravesaban la calzada fuera de las franjas del paso de cebra.
        // Cada nodo de calzada queda asociado a su cruce para esperar el semáforo;
        // una vez iniciada la travesía, el peatón sigue nodo a nodo hasta la acera.
        for (PuntoRuta punto : camino) {
            puntos.add(punto);
            crucesRuta.add(cruceEnCalzada(punto));
        }

        float[][] coordenadas = new float[puntos.size()][2];
        for (int j = 0; j < puntos.size(); j++) {
            coordenadas[j][0] = puntos.get(j).x;
            coordenadas[j][1] = puntos.get(j).z;
        }
        return new RutaParque(coordenadas, crucesRuta.toArray(new CrucePeatonal[0]));
    }

    private CrucePeatonal cruceEnCalzada(PuntoRuta punto) {
        int columna = (int) Math.floor((punto.x + limiteCiudad) / tamanoCelda);
        int fila = (int) Math.floor((punto.z + limiteCiudad) / tamanoCelda);
        if (fila < 0 || fila >= mapa.length || columna < 0
                || columna >= mapa[fila].length || mapa[fila][columna] != 0) return null;
        for (CrucePeatonal cruce : pasosCebra) {
            if (puntoDentroDelPaso(cruce, punto.x, punto.z)) return cruce;
        }
        return null;
    }

    private static int indiceNodo(int x, int z, int minimo, int ancho) {
        return (z - minimo) * ancho + (x - minimo);
    }

    private static int limitar(int valor, int minimo, int maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    private static float distanciaManhattan(int x1, int z1, int x2, int z2) {
        return Math.abs(x1 - x2) + Math.abs(z1 - z2);
    }

    private static final class NodoRuta implements Comparable<NodoRuta> {
        private final int indice;
        private final float costeEstimado;

        private NodoRuta(int indice, float costeEstimado) {
            this.indice = indice;
            this.costeEstimado = costeEstimado;
        }

        @Override
        public int compareTo(NodoRuta otro) {
            return Float.compare(costeEstimado, otro.costeEstimado);
        }
    }

    private static final class PuntoRuta {
        private final float x;
        private final float z;

        private PuntoRuta(float x, float z) {
            this.x = x;
            this.z = z;
        }
    }

    private static final class RutaParque {
        private final float[][] puntos;
        private final CrucePeatonal[] cruces;

        private RutaParque(float[][] puntos, CrucePeatonal[] cruces) {
            this.puntos = puntos;
            this.cruces = cruces;
        }
    }

    private Persona nuevaPersonaParque(float[] centro, float[] color, TipoPersona tipo) {
        return new Persona(centro[0], 0.0f, centro[1], 0.0f,
                color[0], color[1], color[2], false, EstadoPersona.CAMINANDO, tipo);
    }

    private void crearCruces(float[][] semaforos) {
        // Cada interseccion aporta los pasos cebra de ambos sentidos de circulacion.
        for (float[] semaforo : semaforos) {
            if (semaforo[3] == 0.0f && semaforo[4] == 1.0f) {
                float centroX = semaforo[0] + 4.5f;
                float zPasoCebra = semaforo[1] - 2.3f;
                CrucePeatonal cruce = new CrucePeatonal(
                        centroX - 4.0f, zPasoCebra,
                        centroX + 4.0f, zPasoCebra,
                        semaforo[2], semaforo[3], semaforo[4]);
                cruces.add(cruce);
                pasosCebra.add(cruce);
                pasosCebra.add(new CrucePeatonal(
                        centroX - 4.0f, zPasoCebra + 12.6f,
                        centroX + 4.0f, zPasoCebra + 12.6f,
                        semaforo[2], semaforo[3], semaforo[4]));
            } else if (semaforo[3] == 1.0f && semaforo[4] == 0.0f) {
                float xPasoCebra = semaforo[0] - 2.3f;
                float centroZ = semaforo[1] - 4.5f;
                pasosCebra.add(new CrucePeatonal(
                        xPasoCebra, centroZ - 4.0f,
                        xPasoCebra, centroZ + 4.0f,
                        semaforo[2], semaforo[3], semaforo[4]));
                pasosCebra.add(new CrucePeatonal(
                        xPasoCebra + 12.6f, centroZ - 4.0f,
                        xPasoCebra + 12.6f, centroZ + 4.0f,
                        semaforo[2], semaforo[3], semaforo[4]));
            }
        }
    }

    private void crearPeatones() {
        if (cruces.size() < 18) return;

        float[][] colores = {
            {0.88f, 0.24f, 0.18f}, {0.15f, 0.55f, 0.92f},
            {0.20f, 0.72f, 0.34f}, {0.88f, 0.68f, 0.12f},
            {0.62f, 0.28f, 0.82f}, {0.12f, 0.72f, 0.72f},
            {0.92f, 0.42f, 0.12f}, {0.85f, 0.45f, 0.60f}
        };

        // Distribuye recorridos por toda la ciudad: cuatro grupos de avenidas y cinco
        // cruces separados por grupo para mantener espacio entre los peatones.
        int cantidadFilasCruces = cruces.size() / 9;
        int indicePersona = 0;
        for (int fila = 0; fila + 1 < cantidadFilasCruces; fila += 2) {
            for (int columna = 0; columna < 9; columna += 2) {
                int indiceA = fila * 9 + columna;
                int indiceB = indiceA + 9;
                if (indiceB >= cruces.size()) continue;

                CrucePeatonal a = cruces.get(indiceA);
                CrucePeatonal b = cruces.get(indiceB);
                List<SegmentoPeatonal> ruta = new ArrayList<>();
                // Cruza la calle y se incorpora a la acera que bordea la manzana.
                ruta.add(new SegmentoPeatonal(a.getFinX(), a.getFinZ(), a));
                ruta.add(new SegmentoPeatonal(a.getFinX() + 2.0f, a.getFinZ(), null));
                ruta.add(new SegmentoPeatonal(b.getFinX() + 2.0f, b.getFinZ(), null));
                float[] esperaB = puntoExteriorDelPaso(b, b.getFinX(), b.getFinZ());
                ruta.add(new SegmentoPeatonal(esperaB[0], esperaB[1], null));
                // Vuelve a cruzar y sigue por la acera del lado opuesto de la avenida.
                ruta.add(new SegmentoPeatonal(b.getInicioX(), b.getInicioZ(), b));
                ruta.add(new SegmentoPeatonal(b.getInicioX() - 2.0f, b.getInicioZ(), null));
                ruta.add(new SegmentoPeatonal(a.getInicioX() - 2.0f, a.getInicioZ(), null));
                float[] esperaA = puntoExteriorDelPaso(a, a.getInicioX(), a.getInicioZ());
                ruta.add(new SegmentoPeatonal(esperaA[0], esperaA[1], null));

                float[] color = colores[indicePersona % colores.length];
                float velocidad = 1.15f + (indicePersona % 6) * 0.12f;
                float[] inicioAcera = puntoExteriorDelPaso(a, a.getInicioX(), a.getInicioZ());
                Persona persona = new Persona(inicioAcera[0], 0.0f, inicioAcera[1],
                        velocidad, color[0], color[1], color[2], false,
                        EstadoPersona.ESPERANDO_SEMAFORO);
                peatones.add(persona);
                recorridos.add(new RecorridoPeaton(persona, ruta, velocidad,
                        inicioAcera[0], inicioAcera[1]));
                indicePersona++;
            }
        }

        // Agrega diez parejas de adultos y diez grupos de madre con hijo en otros cruces.
        int indicePareja = 0;
        for (int fila = 1; fila < cantidadFilasCruces; fila += 2) {
            for (int columna = 0; columna < 9; columna += 2) {
                int indiceA = fila * 9 + columna;
                int indiceB = indiceA + 9;
                if (indiceB >= cruces.size()) continue;

                CrucePeatonal a = cruces.get(indiceA);
                CrucePeatonal b = cruces.get(indiceB);
                List<SegmentoPeatonal> ruta = crearRuta(a, b);
                float[] colorPrincipal = colores[indicePareja % colores.length];
                float[] colorAcompanante = colores[(indicePareja + 3) % colores.length];
                float velocidad = 1.15f + (indicePareja % 6) * 0.12f;
                boolean familia = indicePareja >= 10;

                float[] inicioAcera = puntoExteriorDelPaso(a, a.getInicioX(), a.getInicioZ());
                Persona principal = new Persona(inicioAcera[0], 0.0f, inicioAcera[1],
                        velocidad, colorPrincipal[0], colorPrincipal[1], colorPrincipal[2],
                        false, EstadoPersona.ESPERANDO_SEMAFORO,
                        familia ? TipoPersona.MUJER : TipoPersona.HOMBRE);
                Persona acompanante = new Persona(inicioAcera[0], 0.0f, inicioAcera[1],
                        velocidad, colorAcompanante[0], colorAcompanante[1], colorAcompanante[2],
                        false, EstadoPersona.ESPERANDO_SEMAFORO,
                        familia ? TipoPersona.NINO : TipoPersona.MUJER);

                peatones.add(principal);
                peatones.add(acompanante);
                RecorridoPeaton recorrido = new RecorridoPeaton(principal, ruta, velocidad,
                        inicioAcera[0], inicioAcera[1]);
                recorrido.agregarAcompanante(acompanante,
                        familia ? 0.22f : 0.48f,
                        familia ? 0.45f : 0.0f);
                recorridos.add(recorrido);
                indicePareja++;
            }
        }
    }

    private List<SegmentoPeatonal> crearRuta(CrucePeatonal a, CrucePeatonal b) {
        List<SegmentoPeatonal> ruta = new ArrayList<>();
        // Cruza la calle y se incorpora a la acera que bordea la manzana.
        ruta.add(new SegmentoPeatonal(a.getFinX(), a.getFinZ(), a));
        ruta.add(new SegmentoPeatonal(a.getFinX() + 2.0f, a.getFinZ(), null));
        ruta.add(new SegmentoPeatonal(b.getFinX() + 2.0f, b.getFinZ(), null));
        float[] esperaB = puntoExteriorDelPaso(b, b.getFinX(), b.getFinZ());
        ruta.add(new SegmentoPeatonal(esperaB[0], esperaB[1], null));
        // Vuelve a cruzar y sigue por la acera del lado opuesto de la avenida.
        ruta.add(new SegmentoPeatonal(b.getInicioX(), b.getInicioZ(), b));
        ruta.add(new SegmentoPeatonal(b.getInicioX() - 2.0f, b.getInicioZ(), null));
        ruta.add(new SegmentoPeatonal(a.getInicioX() - 2.0f, a.getInicioZ(), null));
        float[] esperaA = puntoExteriorDelPaso(a, a.getInicioX(), a.getInicioZ());
        ruta.add(new SegmentoPeatonal(esperaA[0], esperaA[1], null));
        return ruta;
    }

    /** Mantiene los puntos de espera fuera de la franja de calzada del cruce. */
    private float[] puntoExteriorDelPaso(CrucePeatonal cruce, float x, float z) {
        boolean horizontal = Math.abs(cruce.getFinX() - cruce.getInicioX())
                >= Math.abs(cruce.getFinZ() - cruce.getInicioZ());
        if (horizontal) {
            float medioX = (cruce.getInicioX() + cruce.getFinX()) * 0.5f;
            return new float[]{x + (x <= medioX ? -1.4f : 1.4f), z};
        }
        float medioZ = (cruce.getInicioZ() + cruce.getFinZ()) * 0.5f;
        return new float[]{x, z + (z <= medioZ ? -1.4f : 1.4f)};
    }

    private void actualizarAcompanantes(RecorridoPeaton recorrido, float deltaTime) {
        Persona principal = recorrido.persona;
        float coseno = (float) Math.cos(principal.getAngulo());
        float seno = (float) Math.sin(principal.getAngulo());
        boolean guiaCaminando = principal.getEstado() != EstadoPersona.ESPERANDO_SEMAFORO;

        for (Acompanante grupo : recorrido.acompanantes) {
            Persona persona = grupo.persona;
            float x = principal.getX() + coseno * grupo.lateral + seno * grupo.atras;
            float z = principal.getZ() - seno * grupo.lateral + coseno * grupo.atras;
            // El guia puede llegar a la acera antes que su acompanante. Este
            // completa su propio cruce aunque el guia espere el siguiente semaforo.
            boolean caminando = guiaCaminando || (esCalzada(persona.getX(), persona.getZ())
                    && Math.hypot(x - persona.getX(), z - persona.getZ()) > 0.12);
            persona.setEstado(!guiaCaminando && caminando ? EstadoPersona.CRUZANDO : principal.getEstado());
            if (caminando) {
                caminarHacia(persona, x, z, deltaTime, recorrido.velocidad * 1.1f);
            } else {
                persona.setVelocidad(0.0f);
                persona.actualizarAnimacion(deltaTime, false);
            }
        }
    }

    private void caminarHacia(Persona persona, float destinoX, float destinoZ,
            float deltaTime, float velocidad) {
        if (deltaTime <= 0.0f || velocidad <= 0.0f) return;
        DesvioPeaton evasion = rutasEvasion.get(persona);
        if (evasion != null) {
            moverHaciaEvasion(persona, evasion, deltaTime, velocidad);
            return;
        }
        if (Math.hypot(destinoX - persona.getX(), destinoZ - persona.getZ()) < 0.001f) {
            persona.setVelocidad(0.0f);
            persona.actualizarAnimacion(deltaTime, false);
            return;
        }
        if (darPaso(persona, destinoX, destinoZ, deltaTime, velocidad)) {
            esperaReintentoEvasion.remove(persona);
            persona.actualizarAnimacion(deltaTime, true);
            return;
        }
        float espera = esperaReintentoEvasion.getOrDefault(persona, 0.0f) - deltaTime;
        if (espera > 0.0f) {
            esperaReintentoEvasion.put(persona, espera);
            return;
        }
        evasion = buscarRutaEvasion(persona.getX(), persona.getZ(), destinoX, destinoZ, null, 0);
        if (evasion == null) {
            CrucePeatonal cruce = cruceParaRodeo(persona.getX(), persona.getZ());
            if (cruce != null) {
                for (float margen : new float[]{0.35f, MARGEN_MAXIMO_RODEO}) {
                    evasion = buscarRutaEvasion(persona.getX(), persona.getZ(), destinoX, destinoZ, cruce, margen);
                    if (evasion != null) break;
                }
            }
        }
        if (evasion == null) {
            // Un paso completamente cerrado se vuelve a consultar sin buscar cada frame.
            esperaReintentoEvasion.put(persona, 0.4f);
            return;
        }
        esperaReintentoEvasion.remove(persona);
        rutasEvasion.put(persona, evasion);
        moverHaciaEvasion(persona, evasion, deltaTime, velocidad);
    }

    /** Grupo que pasea por un parque, descansa en un banco y luego retoma el paseo. */
    private final class PaseoParque {
        private final float[][] parques;
        private final Persona[] personas;
        private final float[] desplazamientos;
        private final float velocidad;
        private final float lateral;
        private final int parqueInicial;
        private final RutaParque[] rutasInterParque;
        private int indiceParque;
        private float[][] ruta;
        private CrucePeatonal[] crucesRuta;
        private int indice;
        private float tiempoSentado;
        private boolean viajandoAParque;

        private PaseoParque(float[][] parques, int indiceParque, float lateral, Persona[] personas,
                float[] desplazamientos, int semilla, RutaParque[] rutasInterParque) {
            this.parques = parques;
            this.parqueInicial = indiceParque;
            this.indiceParque = indiceParque;
            this.rutasInterParque = rutasInterParque;
            this.personas = personas;
            this.desplazamientos = desplazamientos;
            this.lateral = lateral;
            this.velocidad = 0.72f + (semilla % 5) * 0.09f;
            this.ruta = crearRutaDelParque(indiceParque);
            this.crucesRuta = new CrucePeatonal[ruta.length];
            reiniciar();
        }

        private float[][] crearRutaDelParque(int parque) {
            float x = parques[parque][0];
            float z = parques[parque][1];
            return new float[][]{
                {x + lateral, z + 3.35f},
                {x + lateral, z + 0.65f},
                {x - 2.0f + lateral * 0.35f, z + 2.5f}, // Centro del asiento del banco sur.
                {x + 0.7f + lateral, z + 0.45f},
                {x + lateral, z + 3.35f} // Sale hacia la calle para visitar otro parque.
            };
        }

        private void actualizar(float deltaTime, IntUnaryOperator consultarSemaforo) {
            Persona guia = personas[0];
            if (guia.getEstado() == EstadoPersona.SENTADA) {
                tiempoSentado -= deltaTime;
                actualizarAcompanantes(deltaTime);
                if (tiempoSentado <= 0.0f) {
                    indice = (indice + 1) % ruta.length;
                    guia.setEstado(EstadoPersona.CAMINANDO);
                } else {
                    for (Persona persona : personas) {
                        persona.actualizarAnimacion(deltaTime, false);
                    }
                    return;
                }
            }

            CrucePeatonal cruceActual = crucesRuta[indice];
            if (cruceActual != null && guia.getEstado() != EstadoPersona.CRUZANDO) {
                guia.setEstado(EstadoPersona.ESPERANDO_SEMAFORO);
                guia.setVelocidad(0.0f);
                guia.actualizarAnimacion(deltaTime, false);
                actualizarAcompanantes(deltaTime);
                int estado = consultarSemaforo.applyAsInt(
                        Float.floatToIntBits(cruceActual.getDesfaseSemaforo()));
                if (estado != SEMAFORO_ROJO) return;
                guia.setEstado(EstadoPersona.CRUZANDO);
            } else if (cruceActual == null) {
                guia.setEstado(EstadoPersona.CAMINANDO);
            }

            // Los recorridos entre parques tienen nodos cada metro. Si un auto
            // tapa uno, retoma el primer nodo libre del mismo cruce al rodearlo.
            if (viajandoAParque && cruceActual != null
                    && ocupadoPorVehiculo.test(ruta[indice][0], ruta[indice][1])) {
                for (int siguiente = indice + 1; siguiente < ruta.length; siguiente++) {
                    if (crucesRuta[siguiente] != null && crucesRuta[siguiente] != cruceActual) break;
                    if (Math.hypot(ruta[siguiente][0] - guia.getX(),
                            ruta[siguiente][1] - guia.getZ()) > 6.0f) break;
                    if (!ocupadoPorVehiculo.test(ruta[siguiente][0], ruta[siguiente][1])) {
                        indice = siguiente;
                        break;
                    }
                    if (crucesRuta[siguiente] == null) break;
                }
            }
            float[] destino = ruta[indice];
            float dx = destino[0] - guia.getX();
            float dz = destino[1] - guia.getZ();
            float distancia = (float) Math.sqrt(dx * dx + dz * dz);
            if (distancia < 0.12f) {
                if (!viajandoAParque && indice == 2) {
                    guia.setAngulo(0.0f); // Orienta al grupo hacia delante del banco.
                    guia.setEstado(EstadoPersona.SENTADA);
                    tiempoSentado = 4.0f + (personas.length == 2 ? 2.0f : 0.0f);
                    actualizarAcompanantes(deltaTime);
                    return;
                }
                avanzarRuta();
                return;
            }

            guia.setEstado(cruceActual == null
                    ? EstadoPersona.CAMINANDO : EstadoPersona.CRUZANDO);
            caminarHacia(guia, destino[0], destino[1], deltaTime, velocidad);
            actualizarAcompanantes(deltaTime);
        }

        private void actualizarAcompanantes(float deltaTime) {
            Persona guia = personas[0];
            for (int i = 1; i < personas.length; i++) {
                Persona acompanante = personas[i];
                boolean terminandoCruce = guia.getEstado() == EstadoPersona.ESPERANDO_SEMAFORO
                        && esCalzada(acompanante.getX(), acompanante.getZ())
                        && Math.hypot(guia.getX() + desplazamientos[i] - acompanante.getX(),
                                guia.getZ() - acompanante.getZ()) > 0.12;
                acompanante.setEstado(terminandoCruce ? EstadoPersona.CRUZANDO : guia.getEstado());
                boolean caminando = guia.getEstado() == EstadoPersona.CAMINANDO
                        || guia.getEstado() == EstadoPersona.CRUZANDO || terminandoCruce;
                if (caminando) {
                    caminarHacia(acompanante, guia.getX() + desplazamientos[i],
                            guia.getZ(), deltaTime, velocidad * 1.1f);
                } else if (guia.getEstado() == EstadoPersona.SENTADA) {
                    acompanante.setPosicion(guia.getX() + desplazamientos[i], 0.0f, guia.getZ());
                    acompanante.setVelocidad(0.0f);
                    acompanante.actualizarAnimacion(deltaTime, false);
                } else {
                    acompanante.setVelocidad(0.0f);
                    acompanante.actualizarAnimacion(deltaTime, false);
                }
            }
        }

        private void avanzarRuta() {
            if (indice < ruta.length - 1) {
                indice++;
                return;
            }
            indice = 0;
            if (viajandoAParque) {
                viajandoAParque = false;
                ruta = crearRutaDelParque(indiceParque);
                crucesRuta = new CrucePeatonal[ruta.length];
            } else {
                int siguiente = (indiceParque + 1) % parques.length;
                ruta = rutasInterParque[indiceParque].puntos;
                crucesRuta = rutasInterParque[indiceParque].cruces;
                indiceParque = siguiente;
                viajandoAParque = true;
            }
        }

        private void reiniciar() {
            indiceParque = parqueInicial;
            indice = 0;
            tiempoSentado = 0.0f;
            viajandoAParque = false;
            ruta = crearRutaDelParque(indiceParque);
            crucesRuta = new CrucePeatonal[ruta.length];
            for (int i = 0; i < personas.length; i++) {
                personas[i].setPosicion(ruta[0][0] + desplazamientos[i], 0.0f, ruta[0][1]);
                personas[i].setAngulo((float) Math.PI);
                personas[i].setVelocidad(velocidad);
                personas[i].setEstado(EstadoPersona.CAMINANDO);
                personas[i].actualizarAnimacion(0.0f, false);
            }
        }
    }

    private static final class SegmentoPeatonal {
        private final float destinoX;
        private final float destinoZ;
        private final CrucePeatonal cruce;

        private SegmentoPeatonal(float destinoX, float destinoZ, CrucePeatonal cruce) {
            this.destinoX = destinoX;
            this.destinoZ = destinoZ;
            this.cruce = cruce;
        }
    }

    private static final class RecorridoPeaton {
        private final Persona persona;
        private final List<SegmentoPeatonal> segmentos;
        private final float velocidad;
        private final float inicioX;
        private final float inicioZ;
        private final List<Acompanante> acompanantes = new ArrayList<>();
        private int indice;

        private RecorridoPeaton(Persona persona, List<SegmentoPeatonal> segmentos,
                float velocidad, float inicioX, float inicioZ) {
            this.persona = persona;
            this.segmentos = segmentos;
            this.velocidad = velocidad;
            this.inicioX = inicioX;
            this.inicioZ = inicioZ;
            reiniciar();
        }

        private void agregarAcompanante(Persona persona, float lateral, float atras) {
            acompanantes.add(new Acompanante(persona, lateral, atras));
        }

        private void reiniciar() {
            indice = 0;
            persona.setPosicion(inicioX, 0.0f, inicioZ);
            persona.setAngulo(0.0f);
            persona.setVelocidad(velocidad);
            persona.setEstado(EstadoPersona.ESPERANDO_SEMAFORO);
            persona.actualizarAnimacion(0.0f, false);
            float coseno = (float) Math.cos(persona.getAngulo());
            float seno = (float) Math.sin(persona.getAngulo());
            for (Acompanante grupo : acompanantes) {
                grupo.persona.setPosicion(
                        persona.getX() + coseno * grupo.lateral + seno * grupo.atras,
                        0.0f,
                        persona.getZ() - seno * grupo.lateral + coseno * grupo.atras);
                grupo.persona.setAngulo(persona.getAngulo());
                grupo.persona.setVelocidad(velocidad);
                grupo.persona.setEstado(EstadoPersona.ESPERANDO_SEMAFORO);
                grupo.persona.actualizarAnimacion(0.0f, false);
            }
        }
    }

    private static final class Acompanante {
        private final Persona persona;
        private final float lateral;
        private final float atras;

        private Acompanante(Persona persona, float lateral, float atras) {
            this.persona = persona;
            this.lateral = lateral;
            this.atras = atras;
        }
    }
}
