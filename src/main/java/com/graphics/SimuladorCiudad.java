package com.graphics;

import com.graphics.entidades.Automovil;
import com.graphics.entidades.EstadoPersona;
import com.graphics.entidades.Persona;
import com.graphics.entidades.TipoPersona;
import com.graphics.sistemas.ControlJugador;
import com.graphics.sistemas.GestorPersonas;
import com.graphics.sistemas.GestorTrafico;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class SimuladorCiudad extends AppCiudad {
    private Automovil automovilJugador;
    private Persona personaJugador;
    private ControlJugador controlJugador;
    private GestorPersonas gestorPersonas;
    private boolean cameraArea = false;  // Camara para primera y tercera persona
    private boolean farosEncendidos = false; //2.3
    private float anguloCamaraPeaton = 0.6f;
    private float alturaCamaraPeaton = 9.0f;
    private float deltaCamara = 1.0f / 60.0f;
    private float camaraPeatonX;
    private float camaraPeatonZ;
    private boolean camaraPeatonInicializada;
    private String motivoBloqueo = "";

    private boolean mostrarAyuda = false; //5.2: inicia con los controles contraidos

    private final GestorTrafico gestorTrafico;

    private static final float RADIO_AUTO = 1.65f; // Radio del vehiculo para las colisiones
    private static final float RADIO_ENTREGA = 2.5f;    //agregue 1.2
    private static final float RADIO_ENTREGA_A_PIE = 2.0f;
    private static final float RADIO_AUTO_CERCA = 3.6f;

    // Los marcadores se ubican en el centro de la calle para entregar desde el auto
    // o bajarse y llegar caminando al mismo punto.
    private final float[][] destinos = {
        {centro(2), centro(1)},
        {centro(7), centro(4)},
        {centro(12), centro(11)},
        {centro(17), centro(18)}
    };
    private int destinoActual = 0;
    private int entregasCompletadas = 0;        //agregue 1.2

    public SimuladorCiudad() {
        gestorTrafico = new GestorTrafico(crearAutosTrafico(), semaforos,
                bitsDesfase -> estadoSemaforo(Float.intBitsToFloat(bitsDesfase)));
        gestorPersonas = new GestorPersonas(semaforos,
                bitsDesfase -> estadoSemaforo(Float.intBitsToFloat(bitsDesfase)),
                obtenerPosicionesParques(), MAPA, LIMITE, CELDA);

        // El jugador es una sola persona que puede conducir su propio auto.
        automovilJugador = new Automovil(new float[][]{{0.0f, 0.0f}, {0.0f, -1.0f}},
                0.0f, 0.95f, 0.24f, 0.12f);
        gestorTrafico.setAutomovilJugador(automovilJugador);
        personaJugador = new Persona(0.0f, 0.0f, 0.0f, 0.0f,
                0.90f, 0.72f, 0.52f, true, EstadoPersona.CONDUCIENDO);
        controlJugador = new ControlJugador(personaJugador, automovilJugador);
        gestorPersonas.setDetectorVehiculos(this::hayVehiculoCercaDelPeaton);
        gestorPersonas.setMedidorDistanciaVehiculos(this::distanciaVehiculoMasCercano);

        // Cada auto NPC conserva una persona asociada como conductor. //5.1
        for (Automovil automovil : gestorTrafico.getAutomoviles()) {
            automovil.setConductor(new com.graphics.entidades.Persona(
                    automovil.getX(), 0.0f, automovil.getZ(), 0.0f,
                    0.90f, 0.72f, 0.52f, false,
                    com.graphics.entidades.EstadoPersona.CONDUCIENDO));
        }
    }

    private Automovil[] crearAutosTrafico() {
        // Las rutas mantienen las mismas posiciones, velocidades y colores del trafico original. //5.1
        Automovil[] base = new Automovil[]{
            new Automovil(new float[][]{{centro(2), centro(2)}, {centro(18), centro(2)}, {centro(18), centro(18)}, {centro(2), centro(18)}}, 5.0f, 0.95f, 0.75f, 0.10f),
            new Automovil(new float[][]{{centro(4), centro(4)}, {centro(16), centro(4)}, {centro(16), centro(16)}, {centro(4), centro(16)}}, 4.2f, 0.15f, 0.75f, 0.92f),
            new Automovil(new float[][]{{centro(2), centro(10)}, {centro(10), centro(10)}, {centro(10), centro(18)}, {centro(2), centro(18)}}, 5.8f, 0.90f, 0.18f, 0.25f),
            new Automovil(new float[][]{{centro(6), centro(2)}, {centro(14), centro(2)}, {centro(14), centro(10)}, {centro(6), centro(10)}}, 4.6f, 0.95f, 0.85f, 0.10f),
            new Automovil(new float[][]{{centro(18), centro(6)}, {centro(18), centro(14)}, {centro(10), centro(14)}, {centro(10), centro(6)}}, 5.2f, 0.65f, 0.15f, 0.95f),
            new Automovil(new float[][]{{centro(4), centro(18)}, {centro(4), centro(8)}, {centro(12), centro(8)}, {centro(12), centro(18)}}, 4.8f, 0.10f, 0.90f, 0.35f),
            new Automovil(new float[][]{{centro(16), centro(18)}, {centro(16), centro(8)}, {centro(8), centro(8)}, {centro(8), centro(18)}}, 5.4f, 0.95f, 0.35f, 0.15f),
            new Automovil(new float[][]{{centro(2), centro(14)}, {centro(8), centro(14)}, {centro(8), centro(4)}, {centro(2), centro(4)}}, 4.4f, 0.85f, 0.15f, 0.75f),
            new Automovil(new float[][]{{centro(18), centro(4)}, {centro(12), centro(4)}, {centro(12), centro(12)}, {centro(18), centro(12)}}, 5.6f, 0.15f, 0.85f, 0.85f),
            new Automovil(new float[][]{{centro(6), centro(16)}, {centro(14), centro(16)}, {centro(14), centro(6)}, {centro(6), centro(6)}}, 4.7f, 0.95f, 0.45f, 0.15f),
            new Automovil(new float[][]{{centro(10), centro(2)}, {centro(10), centro(18)}, {centro(4), centro(18)}, {centro(4), centro(2)}}, 5.1f, 0.45f, 0.35f, 0.95f),
            new Automovil(new float[][]{{centro(14), centro(14)}, {centro(6), centro(14)}, {centro(6), centro(4)}, {centro(14), centro(4)}}, 4.5f, 0.20f, 0.75f, 0.25f),
            new Automovil(new float[][]{{centro(2), centro(6)}, {centro(18), centro(6)}, {centro(18), centro(10)}, {centro(2), centro(10)}}, 5.3f, 0.90f, 0.25f, 0.55f)
        };
        int[][] iniciosAdicionales = {
            {2, 4}, {2, 8}, {2, 12}, {2, 16},
            {4, 2}, {4, 8}, {4, 12}, {4, 16},
            {8, 2}, {8, 6}, {8, 10}, {8, 14}, {8, 18},
            {10, 4}, {10, 8}, {10, 12}, {10, 16},
            {12, 2}, {12, 10}, {12, 18}
        };
        float[][] colores = {
            {0.92f, 0.52f, 0.12f}, {0.18f, 0.72f, 0.82f},
            {0.72f, 0.30f, 0.88f}, {0.86f, 0.72f, 0.18f},
            {0.28f, 0.68f, 0.32f}, {0.88f, 0.34f, 0.55f},
            {0.24f, 0.46f, 0.88f}, {0.80f, 0.48f, 0.22f},
            {0.42f, 0.78f, 0.70f}, {0.70f, 0.72f, 0.78f},
            {0.82f, 0.28f, 0.24f}, {0.30f, 0.78f, 0.48f},
            {0.92f, 0.58f, 0.72f}, {0.48f, 0.38f, 0.82f},
            {0.78f, 0.68f, 0.48f}, {0.20f, 0.62f, 0.88f},
            {0.88f, 0.42f, 0.18f}, {0.55f, 0.78f, 0.24f},
            {0.72f, 0.42f, 0.68f}, {0.30f, 0.68f, 0.72f}
        };
        Automovil[] autos = java.util.Arrays.copyOf(base, base.length + iniciosAdicionales.length);
        for (int i = 0; i < iniciosAdicionales.length; i++) {
            int fila = iniciosAdicionales[i][0];
            int columna = iniciosAdicionales[i][1];
            int filaFinal = fila <= 10 ? Math.min(18, fila + 4) : Math.max(2, fila - 4);
            int columnaFinal = columna <= 10
                    ? Math.min(18, columna + 4) : Math.max(2, columna - 4);
            float[][] ruta = {
                {centro(columna), centro(fila)},
                {centro(columnaFinal), centro(fila)},
                {centro(columnaFinal), centro(filaFinal)},
                {centro(columna), centro(filaFinal)}
            };
            float[] color = colores[i];
            autos[base.length + i] = new Automovil(ruta, 3.8f + (i % 5) * 0.35f,
                    color[0], color[1], color[2]);
        }
        return autos;
    }

    /** Obtiene el centro de cada parque dibujado en el mapa para distribuir sus visitantes. */
    private float[][] obtenerPosicionesParques() {
        int cantidad = 0;
        for (int[] fila : MAPA) {
            for (int celda : fila) if (celda == 2) cantidad++;
        }
        float[][] posiciones = new float[cantidad][2];
        int indice = 0;
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == 2) {
                    posiciones[indice][0] = centro(columna);
                    posiciones[indice][1] = centro(fila);
                    indice++;
                }
            }
        }
        return posiciones;
    }

    //---------------------TECLADO---------------------
    @Override
    protected void tecla(int key){
        super.tecla(key);

        if(key == GLFW_KEY_C) {
            cameraArea = !cameraArea;
        }

        if (key == GLFW_KEY_R) {
            reiniciar();
        }

        if (key == GLFW_KEY_F) { //2.3
            farosEncendidos = !farosEncendidos; //2.3
        } 

        if (key == GLFW_KEY_H) { //5.2
            mostrarAyuda = !mostrarAyuda;
        }

        if (key == GLFW_KEY_E) {
            if (controlJugador.estaConduciendo()) {
                if (controlJugador.salirDelAutomovil(this::puedeCaminar)) {
                    iniciarSeguimientoPeaton();
                }
            } else {
                if (puedeEntregarAPie()) {
                    completarEntrega();
                } else if (controlJugador.entrarAlAutomovil()) {
                    camaraPeatonInicializada = false;
                }
            }
        }

    }

    private void reiniciar() {
        controlJugador.reiniciar();

        cameraArea = false;  // Camara para primera y tercera persona punto 3.5
        orbita = 0.6f;  // Camara para primera y tercera persona punto 3.5
        anguloCamaraPeaton = 0.6f;
        alturaCamaraPeaton = 9.0f;
        camaraPeatonInicializada = false;

        destinoActual = 0;  //agregue   1.2
        entregasCompletadas = 0; //agregue 1.2

        farosEncendidos = false; // punto 3.5

        gestorTrafico.reiniciar();
        gestorPersonas.reiniciar();

    }



    //--------------------- Conduccion ---------------------
    @Override
    protected void actualizar(float deltaTime) {
        deltaCamara = Math.max(0.0f, deltaTime);
        if (!controlJugador.estaConduciendo()) motivoBloqueo = "";
        if (controlJugador.estaConduciendo()) {
            actualizarConduccion(deltaTime);
        } else {
            float adelante = 0.0f;
            float derecha = 0.0f;
            if (pulsada(GLFW_KEY_W)) adelante += 1.0f;
            if (pulsada(GLFW_KEY_S)) adelante -= 1.0f;
            if (pulsada(GLFW_KEY_A)) derecha -= 1.0f;
            if (pulsada(GLFW_KEY_D)) derecha += 1.0f;

            // Las flechas orbitan la camara; WASD mueve a la persona respecto a esa vista.
            if (pulsada(GLFW_KEY_LEFT)) anguloCamaraPeaton -= 1.8f * deltaTime;
            if (pulsada(GLFW_KEY_RIGHT)) anguloCamaraPeaton += 1.8f * deltaTime;
            if (pulsada(GLFW_KEY_UP)) alturaCamaraPeaton = Math.min(20.0f,
                    alturaCamaraPeaton + 7.0f * deltaTime);
            if (pulsada(GLFW_KEY_DOWN)) alturaCamaraPeaton = Math.max(4.0f,
                    alturaCamaraPeaton - 7.0f * deltaTime);

            float senoCamara = (float) Math.sin(anguloCamaraPeaton);
            float cosenoCamara = (float) Math.cos(anguloCamaraPeaton);
            float ejeX = -senoCamara * adelante + cosenoCamara * derecha;
            float ejeZ = -cosenoCamara * adelante - senoCamara * derecha;
            boolean correr = pulsada(GLFW_KEY_W)
                    && (pulsada(GLFW_KEY_LEFT_SHIFT) || pulsada(GLFW_KEY_RIGHT_SHIFT));
            controlJugador.actualizarCaminata(
                    ejeX, ejeZ, deltaTime, correr, this::puedeCaminar);
            actualizarAutoSinConductor(deltaTime);
        }

        gestorTrafico.actualizar(deltaTime, gestorPersonas.getPeatones()); //5.1
        gestorPersonas.actualizar(deltaTime);

        if (controlJugador.estaConduciendo()) {
            verificarEntrega(); // Las entregas siguen vinculadas al auto del jugador.
        }
        actualizarTitulo(); 
    }

    private void actualizarConduccion(float deltaTime) {
        float acelerador = 0.0f;
        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) acelerador += 1.0f;
        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) acelerador -= 1.0f;

        boolean turbo = (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP))
                && (pulsada(GLFW_KEY_LEFT_SHIFT) || pulsada(GLFW_KEY_RIGHT_SHIFT));
        float velocidadAnterior = automovilJugador.getVelocidad();
        float aceleracion = acelerador * 9.0f * (turbo && acelerador > 0.0f ? 1.8f : 1.0f);
        float velocidad = velocidadAnterior + aceleracion * deltaTime;
        float resistencia = pulsada(GLFW_KEY_SPACE) ? 7.0f : 0.7f;
        velocidad *= (float) Math.exp(-resistencia * deltaTime);
        float velocidadMaxima = turbo ? 24.0f : Math.max(16.0f, velocidadAnterior);
        velocidad = Math.max(-6.0f, Math.min(velocidadMaxima, velocidad));

        float direccion = 0.0f;
        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) direccion += 1.0f;
        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) direccion -= 1.0f;

        float angulo = automovilJugador.getAngulo()
                + direccion * velocidad * 0.11f * deltaTime;
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        float siguienteX = automovilJugador.getX() + frenteX * velocidad * deltaTime;
        float siguienteZ = automovilJugador.getZ() + frenteZ * velocidad * deltaTime;

        if (puedeCircular(siguienteX, siguienteZ, angulo)) {
            automovilJugador.setAngulo(angulo);
            automovilJugador.setPosicion(siguienteX, siguienteZ);
            automovilJugador.setVelocidad(velocidad);
        } else {
            automovilJugador.setVelocidad(0.0f);
        }
    }

    /** El auto conserva su impulso al bajar y pierde velocidad por rozamiento. */
    private void actualizarAutoSinConductor(float deltaTime) {
        float velocidad = automovilJugador.getVelocidad();
        if (Math.abs(velocidad) < 0.03f) {
            automovilJugador.setVelocidad(0.0f);
            return;
        }

        velocidad *= (float) Math.exp(-0.9f * deltaTime);
        float frenteX = -(float) Math.sin(automovilJugador.getAngulo());
        float frenteZ = -(float) Math.cos(automovilJugador.getAngulo());
        float siguienteX = automovilJugador.getX() + frenteX * velocidad * deltaTime;
        float siguienteZ = automovilJugador.getZ() + frenteZ * velocidad * deltaTime;
        if (puedeCircular(siguienteX, siguienteZ, automovilJugador.getAngulo())) {
            automovilJugador.setPosicion(siguienteX, siguienteZ);
            automovilJugador.setVelocidad(velocidad);
        } else {
            automovilJugador.setVelocidad(0.0f);
        }
    }

    /** Valida el terreno donde puede caminar el jugador y evita autos cercanos. */
    private boolean puedeCaminar(float x, float z) {
        if (Math.abs(x) > LIMITE - 0.6f || Math.abs(z) > LIMITE - 0.6f) {
            return false;
        }

        int columna = (int) ((x + LIMITE) / CELDA);
        int fila = (int) ((z + LIMITE) / CELDA);
        if (fila < 0 || fila >= MAPA.length || columna < 0
                || columna >= MAPA[fila].length) {
            return false;
        }

        int tipoCelda = MAPA[fila][columna];
        if (tipoCelda == 1) {
            // Las aceras rodean el edificio dentro de cada manzana; su huella ocupa 7x7.
            float centroX = centro(columna);
            float centroZ = centro(fila);
            boolean dentroDelEdificio = Math.abs(x - centroX) < 3.8f
                    && Math.abs(z - centroZ) < 3.8f;
            if (dentroDelEdificio) return false;
        }

        // Calles, parques y el borde exterior de cada manzana son transitables a pie.
        // Evita autos NPC y el auto estacionado del jugador.
        float radioColision = 1.5f;
        for (Automovil auto : gestorTrafico.getAutomoviles()) {
            float dx = x - auto.getXCarril();
            float dz = z - auto.getZCarril();
            if (dx * dx + dz * dz < radioColision * radioColision) return false;
        }
        float dxAutoJugador = x - automovilJugador.getX();
        float dzAutoJugador = z - automovilJugador.getZ();
        if (dxAutoJugador * dxAutoJugador + dzAutoJugador * dzAutoJugador
                < radioColision * radioColision) return false;

        for (Persona peaton : gestorPersonas.getPeatones()) {
            float dx = x - peaton.getX();
            float dz = z - peaton.getZ();
            if (dx * dx + dz * dz < 0.85f * 0.85f) return false;
        }
        return true;
    }

    /** Comprueba si un auto ocupa físicamente el paso peatonal. */
    @Override
    protected void dibujarInterfaz() {
        panelHUD.dibujar(
                ancho,
                alto,
                controlJugador.estaConduciendo()
                        ? automovilJugador.getVelocidad() : personaJugador.getVelocidad(),
                farosEncendidos,
                modoNoche,
                mostrarAyuda,
                controlJugador.estaConduciendo(),
                motivoBloqueo,
                indicacionEntrega(),
                nombreZonaActual()
        );
    }

    /** Identifica el sector urbano más cercano para contextualizar la ubicación. */
    private String nombreZonaActual() {
        float x = controlJugador.estaConduciendo()
                ? automovilJugador.getX() : personaJugador.getX();
        float z = controlJugador.estaConduciendo()
                ? automovilJugador.getZ() : personaJugador.getZ();
        double distanciaMinima = Double.POSITIVE_INFINITY;
        int filaCercana = -1;
        int columnaCercana = -1;
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == 0) continue;
                float dx = x - centro(columna);
                float dz = z - centro(fila);
                double distancia = dx * dx + dz * dz;
                if (distancia < distanciaMinima) {
                    distanciaMinima = distancia;
                    filaCercana = fila;
                    columnaCercana = columna;
                }
            }
        }
        if (filaCercana < 0) return "CIUDAD";
        if (MAPA[filaCercana][columnaCercana] == 2) {
            return filaCercana == 9 && columnaCercana == 11
                    ? "PARQUE CENTRAL" : "AREA VERDE";
        }
        return "ZONA RESIDENCIAL";
    }

    protected void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(automovilJugador.getVelocidad()) * 3.6f);
        String titulo = getClass().getSimpleName();
        titulo += controlJugador.estaConduciendo()
                ? " | CONDUCIENDO | E: bajar | WASD/Flechas: Conducir"
                : " | A PIE | E: subir al auto | WASD/Flechas: Caminar";
        if (!controlJugador.estaConduciendo() && puedeEntregarAPie()) {
            titulo += " | E: ENTREGAR";
        }
        titulo += " | Espacio: Freno";
        titulo += " | C: Camara | F: Faros | R: Reiniciar | ";
        titulo += kilometrosPorHora + " km/h";
        if (!motivoBloqueo.isEmpty()) {
            titulo += " | BLOQUEO: " + motivoBloqueo;
        }
        titulo += estadosExtras();
        glfwSetWindowTitle(ventana, titulo);
    }
    //agregue 1.2
    protected String estadosExtras() {
        String ambiente = modoNoche ? " | Noche" : " | Dia";  //punto 2.1


        String faros = farosEncendidos ? " | Faros: ON" : " | Faros: OFF"; //4.1
        String camara = cameraArea ? " | Camara: Area" : " | Camara: Cerca"; //4.1
        String mapa = vistaMapa ? " | Minimapa: ON" : " | Minimapa: OFF"; //4.1


        if (destinoActual >= destinos.length) {
            return " | Entregas completadas: " + entregasCompletadas + ambiente
                    + faros + camara + mapa;  //aumente ambiente para 2.1 y 4.1
        }
        return " | Entrega " + (destinoActual + 1) + "/" + destinos.length + ambiente
                    + faros + camara + mapa; //aumente ambiente para 2.1 y 4.1
    }

    private void verificarEntrega() {
        if (destinoActual >= destinos.length) {
            return;
        }

        float dx = automovilJugador.getX() - destinos[destinoActual][0];
        float dz = automovilJugador.getZ() - destinos[destinoActual][1];
        if (dx * dx + dz * dz <= RADIO_ENTREGA * RADIO_ENTREGA) {
            completarEntrega();
        }
    }

    /** A pie hay que acercarse al marcador y pulsar E para completar el pedido. */
    private boolean puedeEntregarAPie() {
        if (controlJugador.estaConduciendo() || destinoActual >= destinos.length) return false;
        float dx = personaJugador.getX() - destinos[destinoActual][0];
        float dz = personaJugador.getZ() - destinos[destinoActual][1];
        return dx * dx + dz * dz <= RADIO_ENTREGA_A_PIE * RADIO_ENTREGA_A_PIE;
    }

    private String indicacionEntrega() {
        if (puedeEntregarAPie()) return "E: entregar pedido";
        if (controlJugador.estaConduciendo() || destinoActual >= destinos.length) return "";
        float dx = personaJugador.getX() - automovilJugador.getX();
        float dz = personaJugador.getZ() - automovilJugador.getZ();
        return dx * dx + dz * dz <= RADIO_AUTO_CERCA * RADIO_AUTO_CERCA
                ? "E: subir al auto" : "";
    }

    private void completarEntrega() {
        if (destinoActual >= destinos.length) return;
        entregasCompletadas++;
        destinoActual++;
    }
    //agregue 1.2
    //--------------------- Colisiones con la ciudad ---------------------
    private boolean puedeCircular(float x, float z, float anguloPropuesto) {
        motivoBloqueo = "";
        float limitePermitido = LIMITE - RADIO_AUTO;

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) {
            motivoBloqueo = "limite de la ciudad";
            return false;
        }

        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == 0) {
                    continue;
                }

                float centroX = centro(columna);
                float centroZ = centro(fila);
                if (colisionaConCelda(x, z, anguloPropuesto, centroX, centroZ)) {
                    motivoBloqueo = MAPA[fila][columna] == 1
                            ? "acera junto al edificio" : "parque";
                    return false;
                }
            }
        }

        // Si ya hay solapamiento, permite únicamente el movimiento que lo reduce;
        // así el jugador puede salir de un contacto producido por el tráfico denso.
        for (Automovil auto : gestorTrafico.getAutomoviles()) {
            float nuevoSolapamiento = solapamientoAutos(x, z, anguloPropuesto,
                    auto.getXCarril(), auto.getZCarril(), auto.getAngulo());
            float solapamientoActual = solapamientoAutos(automovilJugador.getX(),
                    automovilJugador.getZ(), automovilJugador.getAngulo(),
                    auto.getXCarril(), auto.getZCarril(), auto.getAngulo());
            if (nuevoSolapamiento >= 0.0f
                    && (solapamientoActual < 0.0f
                    || nuevoSolapamiento >= solapamientoActual - 0.002f)) {
                motivoBloqueo = "auto " + identificarColorAuto(auto);
                return false;
            }
        }
        for (Persona peaton : gestorPersonas.getPeatones()) {
            if (colisionaPeaton(x, z, anguloPropuesto, peaton)) {
                motivoBloqueo = "peaton " + ladoDelPeaton(x, z,
                        automovilJugador.getAngulo(), peaton);
                return false;
            }
        }
        return true;
    }

    /** Devuelve la penetración mínima entre dos huellas orientadas, o -1 si se separan. */
    private float solapamientoAutos(float x1, float z1, float angulo1,
            float x2, float z2, float angulo2) {
        float[][] ejes = {
            {(float) Math.cos(angulo1), -(float) Math.sin(angulo1)},
            {-(float) Math.sin(angulo1), -(float) Math.cos(angulo1)},
            {(float) Math.cos(angulo2), -(float) Math.sin(angulo2)},
            {-(float) Math.sin(angulo2), -(float) Math.cos(angulo2)}
        };
        float dx = x2 - x1;
        float dz = z2 - z1;

        float penetracionMinima = Float.POSITIVE_INFINITY;
        for (int i = 0; i < ejes.length; i++) {
            float ejeX = ejes[i][0];
            float ejeZ = ejes[i][1];
            float distancia = Math.abs(dx * ejeX + dz * ejeZ);
            float radio1 = radioAutoEnEje(angulo1, ejeX, ejeZ, true);
            float radio2 = radioAutoEnEje(angulo2, ejeX, ejeZ, false);
            float penetracion = radio1 + radio2 + 0.02f - distancia;
            if (penetracion < 0.0f) return -1.0f;
            penetracionMinima = Math.min(penetracionMinima, penetracion);
        }
        return penetracionMinima;
    }

    /** Comprueba el auto contra la huella cuadrada de una manzana o un parque. */
    private boolean colisionaConCelda(float x, float z, float angulo,
            float centroX, float centroZ) {
        float[][] ejes = {
            {(float) Math.cos(angulo), -(float) Math.sin(angulo)},
            {-(float) Math.sin(angulo), -(float) Math.cos(angulo)},
            {1.0f, 0.0f},
            {0.0f, 1.0f}
        };
        float dx = centroX - x;
        float dz = centroZ - z;

        for (float[] eje : ejes) {
            float distancia = Math.abs(dx * eje[0] + dz * eje[1]);
            float radioAuto = radioAutoEnEje(angulo, eje[0], eje[1], true);
            float radioCelda = 5.0f * (Math.abs(eje[0]) + Math.abs(eje[1]));
            if (distancia > radioAuto + radioCelda) return false;
        }
        return true;
    }

    /** Usa el tamano del cuerpo dibujado para que un peaton lejano no frene el auto. */
    private boolean colisionaPeaton(float x, float z, float angulo, Persona peaton) {
        float[][] ejes = {
            {(float) Math.cos(angulo), -(float) Math.sin(angulo)},
            {-(float) Math.sin(angulo), -(float) Math.cos(angulo)},
            {1.0f, 0.0f},
            {0.0f, 1.0f}
        };
        float dx = peaton.getX() - x;
        float dz = peaton.getZ() - z;

        for (float[] eje : ejes) {
            float distancia = Math.abs(dx * eje[0] + dz * eje[1]);
            float radioAuto = radioAutoEnEje(angulo, eje[0], eje[1], true);
            float radioPeaton = 0.22f * (Math.abs(eje[0]) + Math.abs(eje[1]));
            if (distancia > radioAuto + radioPeaton) return false;
        }
        return true;
    }

    private String ladoDelPeaton(float x, float z, float angulo, Persona peaton) {
        float dx = peaton.getX() - x;
        float dz = peaton.getZ() - z;
        float frente = dx * -(float) Math.sin(angulo) + dz * -(float) Math.cos(angulo);
        float lateral = dx * (float) Math.cos(angulo) - dz * (float) Math.sin(angulo);
        if (Math.abs(frente) >= Math.abs(lateral)) {
            return frente >= 0.0f ? "al frente" : "detras";
        }
        return lateral >= 0.0f ? "a la derecha" : "a la izquierda";
    }

    private String identificarColorAuto(Automovil auto) {
        if (auto.getR() > 0.7f && auto.getG() > 0.55f && auto.getB() < 0.35f) return "amarillo";
        if (auto.getR() > 0.6f && auto.getB() > 0.6f) return "morado";
        if (auto.getG() > 0.65f && auto.getR() < 0.4f) return "verde";
        if (auto.getG() > 0.6f && auto.getB() > 0.6f) return "celeste";
        if (auto.getR() > 0.7f && auto.getG() < 0.5f) return "rojo";
        return "de trafico";
    }

    private float radioAutoEnEje(float angulo, float ejeX, float ejeZ, boolean autoJugador) {
        float lateralX = (float) Math.cos(angulo);
        float lateralZ = -(float) Math.sin(angulo);
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        float proyeccionLateral = Math.abs(ejeX * lateralX + ejeZ * lateralZ);
        float proyeccionFrente = Math.abs(ejeX * frenteX + ejeZ * frenteZ);
        float semiancho = autoJugador ? 1.05f : 0.92f;
        float semilargo = autoJugador ? 1.35f : 1.20f;
        return semiancho * proyeccionLateral + semilargo * proyeccionFrente;
    }

    /** Detecta si el punto del peatón entra en la huella visible de algún auto. */
    private boolean hayVehiculoCercaDelPeaton(float x, float z) {
        for (Automovil auto : gestorTrafico.getAutomoviles()) {
            if (puntoDentroDeAuto(x, z, auto.getXCarril(), auto.getZCarril(),
                    auto.getAngulo(), false)) return true;
        }
        return puntoDentroDeAuto(x, z, automovilJugador.getX(), automovilJugador.getZ(),
                automovilJugador.getAngulo(), true);
    }

    private double distanciaVehiculoMasCercano(float x, float z) {
        double distanciaMinima = Double.POSITIVE_INFINITY;
        for (Automovil auto : gestorTrafico.getAutomoviles()) {
            double dx = x - auto.getXCarril();
            double dz = z - auto.getZCarril();
            distanciaMinima = Math.min(distanciaMinima, Math.hypot(dx, dz));
        }
        double dx = x - automovilJugador.getX();
        double dz = z - automovilJugador.getZ();
        return Math.min(distanciaMinima, Math.hypot(dx, dz));
    }

    private boolean puntoDentroDeAuto(float x, float z, float autoX, float autoZ,
            float angulo, boolean autoJugador) {
        float dx = x - autoX;
        float dz = z - autoZ;
        float lateralX = (float) Math.cos(angulo);
        float lateralZ = -(float) Math.sin(angulo);
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        float lateral = Math.abs(dx * lateralX + dz * lateralZ);
        float frente = Math.abs(dx * frenteX + dz * frenteZ);
        // Margen de 0.32 m para el cuerpo del peatón alrededor del vehículo.
        float semiancho = autoJugador ? 1.05f : 0.73f;
        float semilargo = autoJugador ? 1.35f : 1.18f;
        return lateral <= semiancho + 0.32f && frente <= semilargo + 0.32f;
    }

    @Override //2.3
    protected void prepararLuces() { //2.3
        super.prepararLuces(); //2.3

        // Minimapa local: el automóvil permanece centrado. //3.3
        //vector("uCentroMinimapa", autoX, 0.0f, autoZ); //3.3 auto minimapa
        //decimal("uRadioMinimapa", 35.0f); //3.3 auto minimapa

        float frenteX = -(float) Math.sin(automovilJugador.getAngulo()); //2.3
        float frenteZ = -(float) Math.cos(automovilJugador.getAngulo()); //2.3
        float lateralX = (float) Math.cos(automovilJugador.getAngulo()); //2.3
        float lateralZ = -(float) Math.sin(automovilJugador.getAngulo()); //2.3

        // Posiciones de los faros delanteros del automóvil. //2.3
        float faroIzqX = automovilJugador.getX() + frenteX * 1.30f - lateralX * 0.55f; //2.3
        float faroIzqZ = automovilJugador.getZ() + frenteZ * 1.30f - lateralZ * 0.55f; //2.3
        float faroDerX = automovilJugador.getX() + frenteX * 1.30f + lateralX * 0.55f; //2.3
        float faroDerZ = automovilJugador.getZ() + frenteZ * 1.30f + lateralZ * 0.55f; //2.3

        entero("uFarosEncendidos", farosEncendidos ? 1 : 0); //2.3
        vector("uPosFaroIzquierdo", faroIzqX, 0.70f, faroIzqZ); //2.3
        vector("uPosFaroDerecho", faroDerX, 0.70f, faroDerZ); //2.3
        vector("uDireccionFaros", frenteX, 0.0f, frenteZ); //2.3

        configurarFarosTrafico(); //5.1

    } //2.3


    private void configurarFarosTrafico() { //5.1
        if (!modoNoche) { //5.1
            entero("uCantidadFarosTrafico", 0); //5.1
            return; //5.1
        } //5.1

        int indice = 0; //5.1

        for (Automovil auto : gestorTrafico.getAutomoviles()) { //5.1
            float xCarril = auto.getXCarril(); //5.1
            float zCarril = auto.getZCarril(); //5.1

            enviarFaroTrafico(auto, xCarril, zCarril, -0.48f, -1.22f, indice++); //5.1
            enviarFaroTrafico(auto, xCarril, zCarril, 0.48f, -1.22f, indice++); //5.1
        } //5.1

        entero("uCantidadFarosTrafico", indice); //5.1
    } //5.1

    private void enviarFaroTrafico(Automovil auto, float baseX, float baseZ, //5.1
            float localX, float localZ, int indice) { //5.1

        float coseno = (float) Math.cos(auto.getAngulo()); //5.1
        float seno = (float) Math.sin(auto.getAngulo()); //5.1

        float mundoX = baseX + coseno * localX + seno * localZ; //5.1
        float mundoZ = baseZ - seno * localX + coseno * localZ; //5.1

        vector("uPosFarosTrafico[" + indice + "]", mundoX, 0.68f, mundoZ); //5.1
    } //5.1


    @Override
    protected void configurarCamara() {
        float objetivoX = controlJugador.estaConduciendo() ? automovilJugador.getX() : personaJugador.getX();
        float objetivoZ = controlJugador.estaConduciendo() ? automovilJugador.getZ() : personaJugador.getZ();
        boolean aPie = !controlJugador.estaConduciendo();
        float objetivoAngulo = controlJugador.estaConduciendo()
                ? automovilJugador.getAngulo() : anguloCamaraPeaton;
        if (cameraArea) {
            //super.configurarCamara();
            // Cámara de órbita lejana que AHORA sigue al auto
        float camaraX = objetivoX + (float) Math.sin(objetivoAngulo) * 180; // camara de area sigue al jugador
        float camaraZ = objetivoZ + (float) Math.cos(objetivoAngulo) * 180;
        
        vector("uOjo", camaraX, 150, camaraZ);
        vector("uObjetivo", objetivoX, 0f, objetivoZ); // Enfoca al objeto controlado
        decimal("uAspecto", (float) ancho / alto);
            return;
        }
        float camaraObjetivoX = objetivoX + (float) Math.sin(objetivoAngulo) * 12;
        float camaraObjetivoZ = objetivoZ + (float) Math.cos(objetivoAngulo) * 12;
        if (aPie) {
            if (!camaraPeatonInicializada) {
                iniciarSeguimientoPeaton();
            }
            float suavizado = 1.0f - (float) Math.exp(-6.0f * deltaCamara);
            camaraPeatonX += (camaraObjetivoX - camaraPeatonX) * suavizado;
            camaraPeatonZ += (camaraObjetivoZ - camaraPeatonZ) * suavizado;
            camaraObjetivoX = camaraPeatonX;
            camaraObjetivoZ = camaraPeatonZ;
        }

        vector("uOjo", camaraObjetivoX,
                aPie ? alturaCamaraPeaton : 9.0f, camaraObjetivoZ);
        vector("uObjetivo", objetivoX, controlJugador.estaConduciendo() ? 0.8f : 1.0f, objetivoZ);
        decimal("uAspecto", (float) ancho / alto);
    }    

    /** Coloca la camara detras del peaton y prepara su seguimiento suave. */
    private void iniciarSeguimientoPeaton() {
        camaraPeatonX = personaJugador.getX() + (float) Math.sin(anguloCamaraPeaton) * 12.0f;
        camaraPeatonZ = personaJugador.getZ() + (float) Math.cos(anguloCamaraPeaton) * 12.0f;
        camaraPeatonInicializada = true;
    }
    //--------------------- Dibujo del auto ---------------------
    @Override 
    protected void escena() {
        super.escena();

        dibujarTrafico(); //5.1
        dibujarPeatones();

        dibujarDestinoActual();
        dibujarAuto();
        if (controlJugador.estaConduciendo()) {
            dibujarConductor(automovilJugador, automovilJugador.getX(), automovilJugador.getZ());
        } else {
            dibujarPersona(personaJugador);
        }
    }

    private void dibujarDestinoActual() {
        if (destinoActual >= destinos.length) {
            return;
        }

        float x = destinos[destinoActual][0];
        float z = destinos[destinoActual][1];
        // Poste y techo amarillo: marcador visible, sin colision para el vehiculo.
        cajaGirada(x, 1.25f, z, 0.7f, 2.5f, 0.7f, 1.0f, 0.82f, 0.10f, 0);
        cajaGirada(x, 2.6f, z, 1.5f, 0.25f, 1.5f, 1.0f, 0.45f, 0.05f, 0);
    }

    protected void dibujarAuto() {
        pieza(0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.95f, 0.24f, 0.12f);  // Carroceria
        pieza(0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.22f, 0.65f, 0.78f);  // Cabina
        float[] ladosRueda = { -0.88f, 0.88f };
        float[] ejesRueda = { -0.82f, 0.82f };

        for (float x : ladosRueda) {
            for (float z : ejesRueda) {
                pieza(x, 0.38f, z, 0.24f, 0.58f, 0.6f, 0.055f, 0.65f, 0.08f);
            }
        }
        float[] ladosFaros = { -0.55f, 0.55f };
        for (float x : ladosFaros) {
            pieza(x, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1f, 0.95f, 0.65f);
            pieza(x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f);
            // Faro delantero con forma redondeada aproximada. //2.3
            //pieza(x, 0.61f, -1.32f, 0.18f, 0.07f, 0.08f, 1f, 0.95f, 0.65f); //2.3
            //pieza(x, 0.68f, -1.32f, 0.34f, 0.11f, 0.08f, 1f, 0.95f, 0.65f); //2.3
            //pieza(x, 0.75f, -1.32f, 0.18f, 0.07f, 0.08f, 1f, 0.95f, 0.65f); //2.3

            // Luz trasera. //2.3
            //pieza(x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f); //2.3
        }
    }

  
    private void dibujarTrafico() { //5.1
        for (Automovil auto : gestorTrafico.getAutomoviles()) { //5.1
            dibujarAutoTrafico(auto); //5.1
        } //5.1
    } //5.1

    
    private void dibujarAutoTrafico(Automovil a) { //5.1
        // El vehículo se dibuja en un carril, no sobre la línea central. //5.1
        float xCarril = a.getXCarril(); //5.1
        float zCarril = a.getZCarril(); //5.1

        piezaTrafico(a, xCarril, zCarril, 0.0f, 0.62f, 0.0f, 1.45f, 0.50f, 2.35f, a.getR(), a.getG(), a.getB()); //5.1
        piezaTrafico(a, xCarril, zCarril, 0.0f, 1.02f, 0.10f, 1.10f, 0.45f, 1.10f, 0.18f, 0.35f, 0.52f); //5.1

        for (float lado : new float[]{-0.76f, 0.76f}) { //5.1
            for (float eje : new float[]{-0.72f, 0.72f}) { //5.1
                piezaTrafico(a, xCarril, zCarril, lado, 0.30f, eje, 0.28f, 0.35f, 0.42f, 0.04f, 0.04f, 0.04f); //5.1
            }
        }

        dibujarConductor(a, xCarril, zCarril); //5.1

        dibujarLucesTrafico(a, xCarril, zCarril);

    } //5.1

    /** Dibuja una figura sentada con las mismas cajas OpenGL del auto. */
    private void dibujarConductor(Automovil automovil, float baseX, float baseZ) { //5.1
        if (automovil.getConductor() == null) {
            return;
        }

        // El torso y la cabeza quedan sobre el borde de la cabina para ser visibles.
        piezaTrafico(automovil, baseX, baseZ,
                -0.28f, 1.28f, -0.12f, 0.34f, 0.36f, 0.30f,
                0.18f, 0.30f, 0.72f);
        piezaTrafico(automovil, baseX, baseZ,
                -0.28f, 1.55f, -0.16f, 0.24f, 0.24f, 0.24f,
                automovil.getConductor().getR(),
                automovil.getConductor().getG(),
                automovil.getConductor().getB());
        piezaTrafico(automovil, baseX, baseZ,
                -0.48f, 1.20f, -0.18f, 0.12f, 0.32f, 0.12f,
                0.90f, 0.72f, 0.52f);
        piezaTrafico(automovil, baseX, baseZ,
                -0.08f, 1.20f, -0.18f, 0.12f, 0.32f, 0.12f,
                0.90f, 0.72f, 0.52f);
    } //5.1

    /** Dibuja al jugador a pie con cajas OpenGL y animacion sencilla. */
    private void dibujarPersona(Persona persona) {
        boolean nino = persona.getTipo() == TipoPersona.NINO;
        boolean mujer = persona.getTipo() == TipoPersona.MUJER;
        float escala = nino ? 0.68f : 1.0f;
        float paso = (float) Math.sin(persona.getAnimacion()) * 0.30f * escala;
        float torsoY = 0.88f * escala;
        float cabezaY = 1.28f * escala;
        float piernaY = 0.38f * escala;
        float brazoY = 0.90f * escala;
        float pielR = 0.90f, pielG = 0.72f, pielB = 0.52f;
        float ropaR = persona.esJugador() ? 0.18f : persona.getR();
        float ropaG = persona.esJugador() ? 0.30f : persona.getG();
        float ropaB = persona.esJugador() ? 0.72f : persona.getB();

        if (persona.getEstado() == EstadoPersona.SENTADA) {
            // La cadera queda sobre la tabla; el tronco y la cabeza se elevan desde el asiento.
            piezaPersona(persona, 0.0f, 1.16f * escala, 0.0f,
                    0.34f * escala, 0.48f * escala, 0.22f * escala,
                    ropaR, ropaG, ropaB, 0.0f);
            piezaPersona(persona, 0.0f, 1.59f * escala, 0.0f,
                    0.27f * escala, 0.27f * escala, 0.27f * escala,
                    pielR, pielG, pielB, 0.0f);
            piezaPersona(persona, -0.25f * escala, 1.00f * escala, 0.0f,
                    0.12f * escala, 0.36f * escala, 0.12f * escala,
                    pielR, pielG, pielB, 0.0f);
            piezaPersona(persona, 0.25f * escala, 1.00f * escala, 0.0f,
                    0.12f * escala, 0.36f * escala, 0.12f * escala,
                    pielR, pielG, pielB, 0.0f);
            // Muslos apoyados sobre el asiento y piernas inferiores colgando al suelo.
            piezaPersona(persona, -0.11f * escala, 0.78f * escala, -0.24f * escala,
                    0.15f * escala, 0.18f * escala, 0.54f * escala,
                    0.12f, 0.14f, 0.20f, 0.0f);
            piezaPersona(persona, 0.11f * escala, 0.78f * escala, -0.24f * escala,
                    0.15f * escala, 0.18f * escala, 0.54f * escala,
                    0.12f, 0.14f, 0.20f, 0.0f);
            piezaPersona(persona, -0.11f * escala, 0.41f * escala, -0.53f * escala,
                    0.14f * escala, 0.64f * escala, 0.15f * escala,
                    0.12f, 0.14f, 0.20f, 0.0f);
            piezaPersona(persona, 0.11f * escala, 0.41f * escala, -0.53f * escala,
                    0.14f * escala, 0.64f * escala, 0.15f * escala,
                    0.12f, 0.14f, 0.20f, 0.0f);
            if (mujer) {
                piezaPersona(persona, 0.0f, 0.91f, 0.0f, 0.40f, 0.24f, 0.28f,
                        ropaR, ropaG, ropaB, 0.0f);
                piezaPersona(persona, -0.15f, 1.56f, 0.02f, 0.10f, 0.30f, 0.28f,
                        0.16f, 0.09f, 0.04f, 0.0f);
                piezaPersona(persona, 0.15f, 1.56f, 0.02f, 0.10f, 0.30f, 0.28f,
                        0.16f, 0.09f, 0.04f, 0.0f);
            }
            return;
        }

        piezaPersona(persona, 0.0f, torsoY, 0.0f,
                0.34f * escala, 0.50f * escala, 0.22f * escala,
                ropaR, ropaG, ropaB, 0.0f);
        piezaPersona(persona, 0.0f, cabezaY, 0.0f,
                0.27f * escala, 0.27f * escala, 0.27f * escala,
                pielR, pielG, pielB, 0.0f);
        piezaPersona(persona, -0.25f * escala, brazoY, 0.0f,
                0.12f * escala, 0.42f * escala, 0.12f * escala,
                pielR, pielG, pielB, paso);
        piezaPersona(persona, 0.25f * escala, brazoY, 0.0f,
                0.12f * escala, 0.42f * escala, 0.12f * escala,
                pielR, pielG, pielB, -paso);
        piezaPersona(persona, -0.11f * escala, piernaY, 0.0f,
                0.14f * escala, 0.48f * escala, 0.15f * escala,
                0.12f, 0.14f, 0.20f, -paso);
        piezaPersona(persona, 0.11f * escala, piernaY, 0.0f,
                0.14f * escala, 0.48f * escala, 0.15f * escala,
                0.12f, 0.14f, 0.20f, paso);

        if (mujer) {
            // Falda y cabello largo diferencian visualmente a las mujeres adultas.
            piezaPersona(persona, 0.0f, 0.59f, 0.0f, 0.44f, 0.28f, 0.30f,
                    ropaR, ropaG, ropaB, 0.0f);
            piezaPersona(persona, -0.15f, 1.22f, 0.02f, 0.10f, 0.31f, 0.28f,
                    0.16f, 0.09f, 0.04f, 0.0f);
            piezaPersona(persona, 0.15f, 1.22f, 0.02f, 0.10f, 0.31f, 0.28f,
                    0.16f, 0.09f, 0.04f, 0.0f);
        }
    }

    private void dibujarPeatones() {
        for (Persona persona : gestorPersonas.getPeatones()) {
            dibujarPersona(persona);
        }
    }

    private void piezaPersona(Persona persona, float localX, float y, float localZ,
            float sx, float sy, float sz, float r, float g, float b, float rotacion) {
        float angulo = persona.getAngulo() + rotacion;
        float coseno = (float) Math.cos(persona.getAngulo());
        float seno = (float) Math.sin(persona.getAngulo());
        float x = persona.getX() + coseno * localX + seno * localZ;
        float z = persona.getZ() - seno * localX + coseno * localZ;
        float elevacion = 0.0f;
        if (persona.getEstado() != EstadoPersona.SENTADA) {
            int columna = (int) Math.floor((persona.getX() + LIMITE) / CELDA);
            int fila = (int) Math.floor((persona.getZ() + LIMITE) / CELDA);
            if (fila >= 0 && fila < MAPA.length && columna >= 0
                    && columna < MAPA[fila].length) {
                int tipoSuelo = MAPA[fila][columna];
                float alturaSuelo = tipoSuelo == 1 ? 0.30f : tipoSuelo == 2 ? 0.37f : 0.0f;
                float alturaPies = persona.getTipo() == TipoPersona.NINO ? 0.095f : 0.14f;
                elevacion = alturaSuelo - alturaPies;
            } else {
                elevacion = persona.getTipo() == TipoPersona.NINO ? -0.095f : -0.14f;
            }
        }
        cajaGirada(x, y + elevacion, z, sx, sy, sz, r, g, b, angulo);
    }


    private void piezaTrafico(Automovil a, float baseX, float baseZ,
            float x, float y, float z,
            float sx, float sy, float sz,
            float r, float g, float b) { //5.1

        float coseno = (float) Math.cos(a.getAngulo()); //5.1
        float seno = (float) Math.sin(a.getAngulo()); //5.1

        float mundoX = baseX + coseno * x + seno * z; //5.1
        float mundoZ = baseZ - seno * x + coseno * z; //5.1

        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, a.getAngulo()); //5.1
    } //5.1

    /** Dibuja los faros delanteros y las luces traseras del auto autonomo. */
    private void dibujarLucesTrafico(Automovil a, float baseX, float baseZ) {
        for (float lado : new float[]{-0.48f, 0.48f}) {
            // Lentes visibles de dia; en modo noche tambien emiten luz.
            piezaTrafico(a, baseX, baseZ, lado, 0.70f, -1.20f,
                    0.30f, 0.18f, 0.10f, 0.20f, 0.20f, 0.18f);
            piezaTrafico(a, baseX, baseZ, lado, 0.70f, -1.255f,
                    0.22f, 0.12f, 0.055f, 1.0f, 0.88f, 0.48f);
            piezaTrafico(a, baseX, baseZ, lado, 0.70f, 1.20f,
                    0.30f, 0.18f, 0.10f, 0.20f, 0.035f, 0.025f);
            piezaTrafico(a, baseX, baseZ, lado, 0.70f, 1.255f,
                    0.22f, 0.12f, 0.055f, 0.95f, 0.035f, 0.025f);

            if (modoNoche) {
                dibujarLuzTraficoEmisiva(a, baseX, baseZ, lado, 0.70f, -1.29f,
                        0.22f, 0.12f, 0.055f, 1.0f, 0.90f, 0.62f,
                        1.5f, 1.35f, 0.90f);
                dibujarLuzTraficoEmisiva(a, baseX, baseZ, lado, 0.70f, 1.29f,
                        0.22f, 0.12f, 0.055f, 0.95f, 0.025f, 0.015f,
                        0.55f, 0.015f, 0.008f);
            }
        }
    }

    private void dibujarLuzTraficoEmisiva(Automovil a, float baseX, float baseZ,
            float localX, float y, float localZ,
            float sx, float sy, float sz, float r, float g, float b,
            float emisionR, float emisionG, float emisionB) {
        float coseno = (float) Math.cos(a.getAngulo());
        float seno = (float) Math.sin(a.getAngulo());
        float mundoX = baseX + coseno * localX + seno * localZ;
        float mundoZ = baseZ - seno * localX + coseno * localZ;
        cajaGiradaEmisiva(mundoX, y, mundoZ, sx, sy, sz, r, g, b,
                emisionR, emisionG, emisionB, a.getAngulo());
    }


    private void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float angulo = automovilJugador.getAngulo();
        float coseno = (float) Math.cos(angulo);
        float seno = (float) Math.sin(angulo);
        float mundoX = automovilJugador.getX() + coseno * x + seno * z;
        float mundoZ = automovilJugador.getZ() - seno * x + coseno * z;
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo);
    }

    public static void main(String[] args) {
      SimuladorCiudad aplicacion = new SimuladorCiudad();  
      aplicacion.run();
    }
        
}
