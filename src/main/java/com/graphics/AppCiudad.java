package com.graphics;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glViewport;

import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST; //3.4
import static org.lwjgl.opengl.GL11.glDisable; //3.4
import static org.lwjgl.opengl.GL11.glScissor; //3.4

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL33.*;

public class AppCiudad {
    protected long ventana;
    protected int programa;
    protected int vao;
    protected int vbo;
    protected int ancho = 1100;
    protected int alto = 760;

    protected PanelHUD panelHUD; //punto 5.2

    // Angulo inicial de la camara de la ciudad
    protected float orbita = 0.6f;

    // Cambiar el angulo o la vista del mapa
    protected boolean vistaMapa = false;

    protected boolean modoNoche = false; //punto 2.1

    //agregue 1.2

    // Distancia del origen a cada vorde
    protected static final float LIMITE = 105;  //modifique aqui se coloca 21*10/2

    // Ancho y profundida de cada celda del mapa
    protected static final float CELDA = 10;

    private static final int CANTIDAD_FAROLAS = 100; //2.2

    private static final int CANTIDAD_SEMAFOROS = 324; //3.2

    protected static final int SEMAFORO_ROJO = 0; //3.2
    private static final int SEMAFORO_VERDE = 1; //3.2
    private static final int SEMAFORO_AMARILLO = 2; //3.2
    
    // Intersecciones de calle repartidas por toda la ciudad ampliada. //2.2
    private final float[][] farolas = crearFarolas(); //2.2

    private float[][] crearFarolas() { //2.2
        float[][] posiciones = new float[CANTIDAD_FAROLAS][2]; //2.2
        int indice = 0; //2.2

        // Recorre las intersecciones de calles en orden: fila, luego columna. //2.2
        for (int fila = 2; fila < MAPA.length; fila += 2) { //2.2
            for (int columna = 2; columna < MAPA[fila].length; columna += 2) { //2.2
                posiciones[indice][0] = centro(columna) - 4.8f; // X: columna. //2.2;
                posiciones[indice][1] = centro(fila) - 4.8f; // Z: fila. //2.2
                indice++; //2.2  
            }
        }
        return posiciones;
    }


    // X, Z y desfase de tiempo para que no todos cambien al mismo tiempo. //3.2
    protected final float[][] semaforos = crearSemaforos(); //3.2

    private float[][] crearSemaforos() { //3.2
        float[][] posiciones = new float[CANTIDAD_SEMAFOROS][5]; //3.2
        int indice = 0; //3.2

        for (int fila = 2; fila < MAPA.length - 2; fila += 2) { //3.2
            for (int columna = 2; columna < MAPA[fila].length - 2; columna += 2) { //3.2
                float xCruce = centro(columna); //3.2
                float zCruce = centro(fila); //3.2

                float faseVertical = (fila + columna) % 10.0f; //3.2
                float faseHorizontal = (faseVertical + 5.0f) % 10.0f; //3.2

                // [x, z, desfase, direccionX, direccionZ] //3.2

                // Auto que viaja hacia el norte (+Z). //3.2
                posiciones[indice++] = new float[]{xCruce - 4.5f, zCruce - 4.0f, faseVertical, 0.0f, 1.0f}; //3.2

                // Auto que viaja hacia el sur (-Z). //3.2
                posiciones[indice++] = new float[]{xCruce + 4.5f, zCruce + 4.0f, faseVertical, 0.0f, -1.0f}; //3.2

                // Auto que viaja hacia el este (+X). //3.2
                posiciones[indice++] = new float[]{xCruce - 4.0f, zCruce + 4.5f, faseHorizontal, 1.0f, 0.0f}; //3.2

                // Auto que viaja hacia el oeste (-X). //3.2
                posiciones[indice++] = new float[]{xCruce + 4.0f, zCruce - 4.5f, faseHorizontal, -1.0f, 0.0f}; //3.2
            } //3.2
        } //3.2

        return posiciones; //3.2
    } //3.2

    private final Map<String, Integer> uniforms = new HashMap<>();

    protected static final int[][] MAPA = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0, 1, 0, 2, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0, 1, 0, 2, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    // Organizar las tres fases de la aplicacion y limpiar en caso ocurra un error
    public void run() {
        // Preparamos mensajes de error en la consola
        GLFWErrorCallback errores = GLFWErrorCallback.createPrint(System.err);
        // Instamos el manejador de errores en GLFW
        errores.set();
        try {
            iniciar();
            loop();
        } finally {
            limpiar();
            glfwSetErrorCallback(null);
            errores.free();
        }
    }
    // configurar GLFW y OpenGL igual que las clases App
    private void iniciar() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        // Restablece todas las opciones por defecto
        GLFW.glfwDefaultWindowHints();

        // Ocultar la ventana al principio; mostrar recien cuando OpenGL este listo
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        // Permitir que el ususario pueda redimensionar la pantalla
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        // Queremos que use la version de OpenGL 3.3
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3); 

        //Solo funciones modernas de OpenGL (evitar compatibilidad con codigo muy antiguo)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        // MacOS hay que reactivar para usar el core profile
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        // Creamos la ventana: ancho, alto, titulo, monitor (0=ventana), ventana compartida
        ventana = GLFW.glfwCreateWindow(ancho, alto, "Ciudad | Flechas: orbita | Esc: Salir", 0, 0);
        if (ventana == 0) {
            throw new IllegalStateException("No se pudo crear la ventana OpenGL");
        }

        // Decir que todas las ordenes de OPENGL que ejecutamos a partir de ahora se aplican a esta ventana
        GLFW.glfwMakeContextCurrent(ventana);

        // Sincronizar el dibujo con el monitor (1 = esperar al siguiente refresco)
        // Evitamos el "tearing" (imagen partida)
        GLFW.glfwSwapInterval(1);
        // Hacer visible la ventana
        GLFW.glfwShowWindow(ventana);

        // Carga en java las funciones de OpenGl
        //LWJGL necesita este paso para enlazar las llamadas de Java con la GPU
        GL.createCapabilities();

        // Hace que las superficie cercanas oculten a las lejanas
        glEnable(GL_DEPTH_TEST);

        glfwSetKeyCallback(ventana, (ventanaEvento, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                tecla(key);
            }
        });

        crearPrograma();
        crearCubo();

        panelHUD = new PanelHUD(); //punto 5.2
    }

    private void loop() {
        // Guardamos el instante inicial para calcular el tiempo entre cuadros
        double tiempoAnterior = glfwGetTime();
        // Cuenta los cuadros dibujados para la comprobacion
        int cuadros = 0;
        // Cero permite jugar; otro valor limita el arranque de prueba
        int maxCuadros = Integer.getInteger("demo.frames", 0);
        // Reserva espacio para que GLFW escriba el ancho en pixeles
        int[] anchoReal = new int[1];
        // Reserva espacio para que GLFW escriba el alto en pixeles
        int[] altoReal = new int[1];

        while (!glfwWindowShouldClose(ventana)) {
            // Procesa el teclado, redimensionamiento y boton de cierre
            glfwPollEvents();
            // Consulta el tiempo actual en segundos
            double tiempoActual = glfwGetTime();
            float deltaTime = (float) Math.min(tiempoActual - tiempoAnterior, 0.05);
            tiempoAnterior = tiempoActual;
            actualizar(deltaTime);
            GLFW.glfwGetFramebufferSize(ventana, anchoReal, altoReal);
            ancho = anchoReal[0];
            alto = altoReal[0];
            if (ancho > 0 && alto > 0) {
                dibujarFrame();
            }
            glfwSwapBuffers(ventana);
            cuadros++;
            if (maxCuadros > 0 && cuadros >= maxCuadros) {
                glfwSetWindowShouldClose(ventana, true);
            }
        }
    }

    private void limpiar() {

        if (panelHUD != null) {
            panelHUD.close(); //punto 5.2
        }

        if (programa != 0) {  // Comprueba si existe un programa en la GPU.
            glDeleteProgram(programa);  // Libera el programa de shaders.
        }
        if (vbo != 0) {  // Comprueba si se reservo el buffer de vertices.
            glDeleteBuffers(vbo);  // Libera los datos de geometria de la GPU
        }
        if (vao != 0) {  // Comprueba si se creo la descripcion de atributos.
            glDeleteVertexArrays(vao); // Libera la configuracion del cubo.
        }
        if (ventana != 0) {  // Evita destruir una ventana que no pudo crearse. 
            Callbacks.glfwFreeCallbacks(ventana);  // Libera los callbacks de teclado registrados en la ventana.
            glfwDestroyWindow(ventana);  // Destruye la ventana y su contexto OpenGL.
        }
        glfwTerminate(); // Finaliza GLFW ysus recursos globales.
    }
    // ============================ Teclas y camaras =========================
    /** Atiende las acciones de una sola pulsacion */
    protected void tecla(int key) {
        if (key == GLFW_KEY_ESCAPE) {
            glfwSetWindowShouldClose(ventana, true);
        }
//1.3
        if (key == GLFW_KEY_M) {
            vistaMapa = !vistaMapa;
        }
//2.1 modo noche
        if (key == GLFW_KEY_N) {
            modoNoche = !modoNoche;
        }

    }

    protected boolean pulsada(int key) {
        return glfwGetKey(ventana, key) == GLFW_PRESS;
    }

    /** Mueve la camara alrededor de la cuidad con las flechas */
    protected void actualizar(float deltaTime) {
        if (pulsada(GLFW_KEY_LEFT)) {
            orbita -= deltaTime;    // Reducimos el angulo
        }

        if (pulsada(GLFW_KEY_RIGHT)) {
            orbita += deltaTime;    // Aumentamos el angulo
        }
    }

    protected void configurarCamara() {
        float camaraX = (float) Math.sin(orbita) * 180;
        float camaraZ = (float) Math.cos(orbita) * 180;

        vector("uOjo", camaraX, 150, camaraZ);
        vector("uObjetivo", 0, 0, 0);
        decimal("uAspecto", (float) ancho / alto);
    }

    protected void prepararLuces() {
        // Enviar las luces al GPU punto 2.1
        if (modoNoche) {
            vector("uLuzAmbiente", 0.03f, 0.04f, 0.08f);
            vector("uLuzDireccional", 0.08f, 0.10f, 0.18f);
            vector("uDireccionLuz", 0.35f, -1.0f, -0.25f);
        } else {
            vector("uLuzAmbiente", 0.55f, 0.55f, 0.60f);
            vector("uLuzDireccional", 0.75f, 0.70f, 0.60f);
            vector("uDireccionLuz", -0.35f, -1.0f, -0.30f);
        }

        // Las farolas iluminan el entorno solo durante la noche. //2.2
        entero("uCantidadFarolas", modoNoche ? CANTIDAD_FAROLAS : 0); //2.2
        vector("uColorFarola", 1.0f, 0.95f, 0.85f); //2.2
        for (int i = 0; i < CANTIDAD_FAROLAS; i++) { //2.2
            vector("uPosFarolas[" + i + "]", farolas[i][0], 4.2f, farolas[i][1]); //2.2
        } //2.2

        entero("uFarosEncendidos", 0); //2.3


        // Vista completa: se usa si no se ejecuta Vehiculo. //3.3 minimapa auto
        //vector("uCentroMinimapa", 0.0f, 0.0f, 0.0f); //3.3
        //decimal("uRadioMinimapa", 115.0f); //3.3



    }

    // ============================ Ciudad a partir de la matriz =========================
    protected float centro(int indice) {
        return -LIMITE + CELDA * (indice + 0.5f);
    }

    private void dibujarFrame() {
        glViewport(0, 0, ancho, alto); //3.4
       // glClearColor(0.12f, 0.20f, 0.30f, 1);
// modo noche 2.1
        if (modoNoche) { //punto 2.1
            glClearColor(0.01f, 0.02f, 0.08f, 1);
        } else {
            glClearColor(0.12f, 0.20f, 0.30f, 1);
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(programa);
        glBindVertexArray(vao);
        entero("uMapa", 0);   // punto 1.3
        configurarCamara();
        prepararLuces();
        escena();

        // Vista superior simultanea en la esquina superior derecha de la ventana// punto 3.3
        if (vistaMapa) { // punto 3.4
            dibujarMinimapa(); // punto 3.3
        }

        dibujarInterfaz(); // punto 5.2

        if (glGetError() != GL_NO_ERROR){
            throw new IllegalStateException("Error OpenGL al dibujar");
        }
    }


    private void dibujarMinimapa() { //3.3
        //int margen = 18; //3.3
        //int tamano = Math.min(250, Math.min(ancho, alto) / 3); //3.3

        int margen = Math.max(10, Math.min(ancho, alto) / 60); //3.4
        int tamano = Math.min(180, Math.min(ancho, alto) / 4); //3.4

        // OpenGL usa el origen abajo a la izquierda. //3.3
        int xMinimapa = ancho - tamano - margen; //3.3
        int yMinimapa = alto - tamano - margen; //3.3

        glEnable(GL_SCISSOR_TEST); //3.4
        glScissor(xMinimapa, yMinimapa, tamano, tamano); //3.4
        glClearColor(0.03f, 0.04f, 0.07f, 1.0f); //3.4
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); //3.4
        glDisable(GL_SCISSOR_TEST); //3.4

        glViewport(xMinimapa, yMinimapa, tamano, tamano); //3.3

        

        // El shader usa la proyección superior cuando uMapa vale 1. //3.3
        entero("uMapa", 1); //3.3
        escena(); //3.3

        // Restaura el estado para no afectar la vista principal del del sgte frame. //3.4
        entero("uMapa", 0); //3.4

        // Restaura el viewport normal para el siguiente frame. //3.3
        glViewport(0, 0, ancho, alto); //3.3
    } //3.3

    protected void dibujarInterfaz() { //5.2
        // Vehiculo implementa el dibujo del panel.
    }

    private void dibujarPapelera(float x, float z) {
        // Cuerpo verde.
        caja(x, 0.85f, z,
                0.75f, 1.10f, 0.75f,
                0.08f, 0.38f, 0.22f);

        // Tapa.
        caja(x, 1.45f, z,
                0.85f, 0.12f, 0.85f,
                0.05f, 0.18f, 0.10f);

        // Abertura negra en el frente.
        caja(x, 1.12f, z - 0.385f,
                0.45f, 0.22f, 0.04f,
                0.02f, 0.02f, 0.02f);

        // Base.
        caja(x, 0.38f, z,
                0.85f, 0.16f, 0.85f,
                0.12f, 0.14f, 0.16f);
    }

    protected void escena() {
        caja(0, -0.5f, 0, 210, 0.5f, 210, 0.16f, 0.19f, 0.23f);  // modifique 210
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                float x = centro(columna);
                float z = centro(fila);
                int tipo = MAPA[fila][columna];
                if (tipo == 0) {
                    dibujarMarcasCalles(fila, columna, x, z);
                } else {
                    caja(x, 0.15f, z, 10, 0.3f, 10, 0.60f, 0.64f, 0.66f);
                    if (tipo == 1) {
                        float altura = 5 +(fila * 3 + columna * 7) % 9;
                        float rojo = 0.28f + fila * 0.025f;
                        float azul = 0.48f + fila * 0.025f;
                        caja(x, altura / 2 + 0.3f, z, 7, altura, 7, rojo, 0.05f, azul);
                        caja(x, altura + 0.45f, z, 7.3f, 0.3f, 7.3f, 0.20f, 0.24f, 0.32f);

                        dibujarVentanas(x, z, altura); //3.1

                    } else {
                        caja(x, 0.32f, z, 9, 0.1f, 9, 0.20f, 0.45f, 0.28f);
                        
                        dibujarArbolesParque(x, z); //3.1
                        dibujarBancosParque(x, z); //3.1

                        dibujarPapelera(x + 3.4f, z + 2.5f); //5.2
                        
                    }
                    dibujarPasoJuntoManzana(fila, columna, x, z); //3.1
                }
            }
        }
        dibujarFarolas(); //2.2

        dibujarSemaforos(); //3.2
    }

    private void dibujarFarolas() { //2.2
        for (float[] farola : farolas) { //2.2
            float x = farola[0]; //2.2
            float z = farola[1]; //2.2
            caja(x, 2.0f, z, 0.22f, 4.0f, 0.22f, 0.12f, 0.14f, 0.18f); //2.2
            caja(x, 4.0f, z, 0.75f, 0.16f, 0.75f, 0.16f, 0.18f, 0.22f); //2.2
            //caja(x, 4.25f, z, 0.42f, 0.38f, 0.42f, 1.0f, 0.78f, 0.30f); //2.2

            cajaEmisiva(    //2.4
                x, 4.25f, z, // Posicion //2.4
                0.42f, 0.38f, 0.42f, // Escala //2.4
                1.0f, 0.95f, 0.85f, // Color  //2.4
                1.1f, 1.0f, 0.85f // Emision //2.4
            ); //2.4 

        } //2.2
    } //2.2

    


    protected int estadoSemaforo(float desfase) { //3.2
        float tiempoCiclo = (float) ((glfwGetTime() + desfase) % 10.0); //3.2

        // Secuencia: rojo 4 s -> verde 4 s -> amarillo 2 s. //3.2
        if (tiempoCiclo < 4.0f) { //3.2
            return SEMAFORO_ROJO; //3.2
        } //3.2

        if (tiempoCiclo < 8.0f) { //3.2
            return SEMAFORO_VERDE; //3.2
        } //3.2

        return SEMAFORO_AMARILLO; //3.2
    } //3.2


    private void dibujarSemaforos() { //3.2
        for (float[] semaforo : semaforos) { //3.2
            int estado = estadoSemaforo(semaforo[2]); //3.2

            dibujarSemaforo( //3.2
                    semaforo[0], // X //3.2
                    semaforo[1], // Z //3.2
                    estado, // Estado actual //3.2
                    semaforo[3], // Dirección X //3.2
                    semaforo[4] // Dirección Z //3.2
            ); //3.2
        } //3.2
    } //3.2

    private void dibujarSemaforo(float x, float z, int estado, //3.2
            float direccionX, float direccionZ) { //3.2

        // El frente iluminado queda dirigido hacia el vehículo. //3.2
        float giroSemaforo = (float) Math.atan2(direccionX, direccionZ); //3.2
        float frenteX = -direccionX * 0.26f; //3.2
        float frenteZ = -direccionZ * 0.26f; //3.2

        // Poste. //3.2
        cajaGirada(x, 1.45f, z, 0.14f, 2.90f, 0.14f, //3.2
                0.04f, 0.04f, 0.05f, giroSemaforo); //3.2

        // Caja negra del semáforo. //3.2
        cajaGirada(x, 2.65f, z, 0.42f, 0.95f, 0.18f, //3.2
                0.03f, 0.03f, 0.03f, giroSemaforo); //3.2

        // Soporte superior. //3.2
        cajaGirada(x, 3.25f, z, 0.55f, 0.12f, 0.30f, //3.2
                0.08f, 0.08f, 0.08f, giroSemaforo); //3.2

        // Luces del semáforo. //3.2
        dibujarLuzSemaforo(x + frenteX, 3.02f, z + frenteZ, //3.2
                1.0f, 0.02f, 0.02f, estado == SEMAFORO_ROJO); //3.2

        dibujarLuzSemaforo(x + frenteX, 2.68f, z + frenteZ, //3.2
                1.0f, 0.72f, 0.02f, estado == SEMAFORO_AMARILLO); //3.2

        dibujarLuzSemaforo(x + frenteX, 2.34f, z + frenteZ, //3.2
                0.02f, 1.0f, 0.08f, estado == SEMAFORO_VERDE); //3.2
    } //3.2

    private void dibujarLuzSemaforo(float x, float y, float z, //3.2
            float r, float g, float b, boolean encendida) { //3.2

        if (encendida) { //3.2
            cajaEmisiva(x, y, z, //3.2
                    0.18f, 0.18f, 0.08f, //3.2
                    r, g, b, //3.2
                    r * 1.2f, g * 1.2f, b * 1.2f); //3.2
        } else { //3.2
            caja(x, y, z, //3.2
                    0.18f, 0.18f, 0.08f, //3.2
                    r * 0.20f, g * 0.20f, b * 0.20f); //3.2
        } //3.2
    } //3.2


    private void dibujarMarcasCalles(int fila, int columna, float x, float z) {

        //if (fila % 2 == 0 && columna % 2 == 0) {
          //  dibujarPasoPeatonal(fila, columna, x, z); //3.1
        //}

        if (fila % 2 == 0 && columna % 2 == 1) {
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento++) {
                caja(x + desplazamiento, 0.025f, z, 1.6f, 0.03f, 0.13f, 1,0.84f, 0.35f);
            }
        }

        if (columna % 2 == 0 && fila % 2 == 1) {
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) {
                caja(x, 0.025f, z + desplazamiento, 0.13f, 0.03f, 1.6f, 1, 0.84f, 0.35f);
            }
        }
    } 

    // ============================ Dibujar Paso Peatonal =========================

    private void dibujarPasoJuntoManzana(int fila, int columna, float x, float z) { //3.1
        // Calle derecha de la manzana: calle vertical. //3.1
        float xCalleDerecha = centro(columna + 1); //3.1
        float zEsquinaDerecha = z + 3.7f; //3.1

        // Calle inferior de la manzana: calle horizontal. //3.1
        float xEsquinaInferior = x + 3.7f; //3.1
        float zCalleInferior = centro(fila + 1); //3.1

        // En calle vertical, franjas verticales. //3.1
        dibujarPasoVertical(xCalleDerecha, zEsquinaDerecha); //3.1

        // En calle horizontal, franjas horizontales. //3.1
        dibujarPasoHorizontal(xEsquinaInferior, zCalleInferior); //3.1
    } //3.1

    private void dibujarPasoVertical(float x, float z) { //3.1
        for (float desplazamiento = -4.0f; desplazamiento <= 4.0f; desplazamiento += 1.0f) { //3.1
            caja( //3.1
                x + desplazamiento, 0.05f, z, //3.1
                0.45f, 0.04f, 2.5f, // Franja larga en Z. //3.1
                0.95f, 0.95f, 0.92f //3.1
            ); //3.1
        } //3.1
    } //3.1


    private void dibujarPasoHorizontal(float x, float z) { //3.1
        for (float desplazamiento = -4.0f; desplazamiento <= 4.0f; desplazamiento += 1.0f) { //3.1
            caja( //3.1
                x, 0.05f, z + desplazamiento, //3.1
                2.5f, 0.04f, 0.45f, // Franja larga en X. //3.1
                0.95f, 0.95f, 0.92f //3.1
            ); //3.1
        } //3.1
    } //3.1


    

    private void dibujarArbolesParque(float x, float z) { //3.1
        dibujarArbol(x - 2.4f, z - 2.2f); //3.1
        dibujarArbol(x + 2.3f, z - 2.1f); //3.1
    }

    private void dibujarArbol(float x, float z) { //3.1
        // Tronco del arbol. //3.1
        caja(x, 1.05f, z, 0.38f, 2.1f, 0.38f, 0.34f, 0.18f, 0.06f); //3.1

        // Copa forma por cubos verdes. //3.1
        caja(x, 2.45f, z, 2.0f, 1.6f, 2.0f, 0.08f, 0.42f, 0.12f); //3.1
        caja(x - 0.65f, 2.25f, z, 1.3f, 1.3f, 1.3f, 0.10f, 0.50f, 0.15f); //3.1
        caja(x + 0.65f, 2.25f, z, 1.3f, 1.3f, 1.3f, 0.10f, 0.50f, 0.15f); //3.1
    }

    private void dibujarBancosParque(float x, float z) { //3.1
        dibujarBanco(x - 2.0f, z + 2.5f); //3.1
        dibujarBanco(x + 2.1f, z - 2.5f); //3.1
    } //3.1

    private void dibujarBanco(float x, float z) { //3.1
        // Asiento. //3.1
        caja(x, 0.82f, z, 2.2f, 0.18f, 0.55f, 0.48f, 0.25f, 0.08f); //3.1

        // Respaldo. //3.1
        caja(x, 1.22f, z + 0.23f, 2.2f, 0.62f, 0.12f, 0.42f, 0.22f, 0.10f); //3.1

        // Patas. //3.1
        caja(x - 0.82f, 0.40f, z, 0.16f, 0.75f, 0.42f, 0.16f, 0.18f, 0.20f); //3.1
        caja(x + 0.82f, 0.40f, z, 0.16f, 0.75f, 0.42f, 0.16f, 0.18f, 0.20f); //3.1
    } //3.1


    private void dibujarVentanas(float x, float z, float altura) { //3.1
        float alturaInferior = altura * 0.38f + 0.3f; //3.1
        float alturaSuperior = altura * 0.67f + 0.3f; //3.1

        // Fachada frontal. //3.1
        dibujarVentana(x - 2.0f, alturaInferior, z - 3.56f); //3.1
        dibujarVentana(x, alturaInferior, z - 3.56f); //3.1
        dibujarVentana(x + 2.0f, alturaInferior, z - 3.56f); //3.1
        dibujarVentana(x - 2.0f, alturaSuperior, z - 3.56f); //3.1
        dibujarVentana(x, alturaSuperior, z - 3.56f); //3.1
        dibujarVentana(x + 2.0f, alturaSuperior, z - 3.56f); //3.1

        // Fachada lateral derecha. //3.1
        dibujarVentanaLateral(x + 3.56f, alturaInferior, z - 1.8f); //3.1
        dibujarVentanaLateral(x + 3.56f, alturaInferior, z + 1.8f); //3.1
        dibujarVentanaLateral(x + 3.56f, alturaSuperior, z - 1.8f); //3.1
        dibujarVentanaLateral(x + 3.56f, alturaSuperior, z + 1.8f); //3.1
    } //3.1


    private void dibujarVentana(float x, float y, float z) { //3.1
        if (modoNoche) { //3.1
            cajaEmisiva(x, y, z, 0.85f, 0.72f, 0.08f, //3.1
                    0.24f, 0.45f, 0.68f, //3.1
                    0.34f, 0.52f, 0.82f); //3.1
        } else { //3.1
            caja(x, y, z, 0.85f, 0.72f, 0.08f, 0.12f, 0.25f, 0.42f); //3.1
        } //3.1
    } //3.1


    private void dibujarVentanaLateral(float x, float y, float z) { //3.1
        if (modoNoche) { //3.1
            cajaEmisiva(x, y, z, 0.08f, 0.72f, 0.85f, //3.1
                    0.24f, 0.45f, 0.68f, //3.1
                    0.34f, 0.52f, 0.82f); //3.1
        } else { //3.1
            caja(x, y, z, 0.08f, 0.72f, 0.85f, 0.12f, 0.25f, 0.42f); //3.1
        } //3.1
    } //3.1


    // ============================ Dibujar cajas y enviar uniform =========================
    private void caja(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, 0);
    }

    protected void cajaGirada(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float angulo) {
        vector("uPos", x, y, z);
        vector("uEscala", sx, sy, sz);
        vector("uColor", r, g, b);
        decimal("uGiro", angulo);

        vector("uEmision", 0.0f, 0.0f, 0.0f); //2.4

        glDrawArrays(GL_TRIANGLES, 0, 36);
    }

    protected void cajaEmisiva(float x, float y, float z,
            float sx, float sy, float sz,
            float r, float g, float b,
            float emisionR, float emisionG, float emisionB) {
        vector("uPos", x, y, z);
        vector("uEscala", sx, sy, sz);
        vector("uColor", r, g, b);
        vector("uEmision", emisionR, emisionG, emisionB); //2.4
        decimal("uGiro", 0); //2.4
        glDrawArrays(GL_TRIANGLES, 0, 36); // 36 vertices = 12 triangulos = 6 caras // 2.4
    }

    protected int uniform(String nombre){
        if (!uniforms.containsKey(nombre)) {
            int ubicacion = glGetUniformLocation(programa, nombre);
            uniforms.put(nombre, ubicacion); 
        }
        return uniforms.get(nombre);
    }

    protected void vector(String nombre, float x, float y, float z) {
        glUniform3f(uniform(nombre), x, y, z);
    }

    protected void decimal(String nombre, float valor) {
        glUniform1f(uniform(nombre), valor);
    }

    protected void entero(String nombre, int valor) {
        glUniform1i(uniform(nombre), valor);
    }


    // ============================ Shader: Codigo que se ejecuta en la GPU =========================

    private String vertexShader() {
        return """
            #version 330 core 
             // Seleccionamos GLSL 3.30
            layout (location = 0) in vec3 aPos; // Leemos la posicion local del vertice desde el VBO
            layout (location = 1) in vec3 aNormal; // Leemos la normla de la cara desde el mismo VBO
            uniform vec3 uPos;                          // Recibe el centro de la caja de la ciudad
            uniform vec3 uEscala;                   // Recibe el tamaño de la caja eje
            uniform vec3 uOjo;                      // Recibe la posicion de la camara
            uniform vec3 uObjetivo;                 // Punto que observa la camara
            uniform float uGiro;                 // Recibe el giro del objeto alrededor de Y
            uniform float uAspecto;                 // Recibe la relacion del ancho y alto de la imagen
            uniform int uMapa;                 // Selecionamos la perspectiva (0,1)
            out vec3 vMundo;                    // La posicion mundial al shader de fragmentos
            out vec3 vNormal;                   // Enviamos la normal transformada para la iluminacion

            //uniform vec3 uCentroMinimapa; //3.3 auto
            //uniform float uRadioMinimapa; //3.3 auto

            void main() {
                float coseno = cos(uGiro);  // Calculamos el coseno del giro del objeto
                float seno = sin(uGiro);    
                mat3 giro = mat3(
                    coseno, 0.0, -seno,
                    0.0, 1.0, 0.0,
                    seno, 0.0, coseno
                );
                vMundo = giro * (aPos * uEscala) + uPos; // Escalar, tirar y trasladar el vertice al mundo
                vNormal = normalize(giro * (aNormal / uEscala)); // Corregimos la normal con la inversa transpuesta de la escala y giro

                if (uMapa == 1) { //Esta rama se usa al dibujar el minimapa
                    float pantallaX = vMundo.x / 115.0;
                    float pantallaY = -vMundo.z / 115.0;
                    float profundidad = -vMundo.y / 100.0;
                    gl_Position = vec4(pantallaX, pantallaY, profundidad, 1.0);

                    //float pantallaX = (vMundo.x - uCentroMinimapa.x) / uRadioMinimapa; //3.3 auto
                    //float pantallaY = -(vMundo.z - uCentroMinimapa.z) / uRadioMinimapa; //3.3 auto
                    //float profundidad = -vMundo.y / 100.0; //3.3 auto
                    //gl_Position = vec4(pantallaX, pantallaY, profundidad, 1.0); //3.3 auto minimapa


                } else {    // La escena principal utilizando la camara como perspectiva
                    vec3 frente = normalize(uObjetivo - uOjo); // Calcular la direccion hacia lo que mira la camara
                    vec3 derecha = normalize(cross(frente, vec3(0.0, 1.0, 0.0))); // Obtenemos el eje horizontal de la camara
                    vec3 arriba = cross(derecha, frente); // Obtenemnos el eje vertical 
                    vec3 diferencia = vMundo - uOjo; // Trasladamos el origen del mundo hasta la camara
                    float vistaX = dot(diferencia, derecha); 
                    float vistaY = dot(diferencia, arriba);
                    float vistaZ = -dot(diferencia, frente);
                    float factor = 1.0 / tan(radians(55.0) * 0.5); // Convertimos el campo visual (55 grados) en escala de perspectiva
                    float cerca = 0.1; 
                    float lejos = 500.0;
                    float clipX = vistaX * factor / uAspecto;
                    float clipY = vistaY * factor;
                    float clipZ = (lejos + cerca) / (cerca - lejos) * vistaZ;
                    clipZ += (2.0 * lejos * cerca) / (cerca - lejos); // añadir el constante de profundidad
                    gl_Position = vec4(clipX, clipY, clipZ, -vistaZ);  
                }   
            }
        
        """;
    }

    private String fragmentShader() {
        return """
                #version 330 core

                uniform vec3 uColor;
                uniform vec3 uLuzAmbiente;  //2.1
                uniform vec3 uLuzDireccional; //2.1
                uniform vec3 uDireccionLuz; //2.1
                uniform int uCantidadFarolas; //2.2
                uniform vec3 uPosFarolas[100]; //2.2
                uniform vec3 uColorFarola; //2.2

                uniform int uFarosEncendidos; //2.3
                uniform vec3 uPosFaroIzquierdo; //2.3
                uniform vec3 uPosFaroDerecho; //2.3
                uniform vec3 uDireccionFaros; //2.3

                uniform vec3 uEmision; //2.4


                uniform int uCantidadFarosTrafico; //5.1
                uniform vec3 uPosFarosTrafico[26]; //5.1
                

                in vec3 vNormal; //2.1
                in vec3 vMundo; //2.2
                out vec4 color;

                void main() {
                    vec3 normal = normalize(vNormal); //2.1
                    // Direccion desde la superficie hacia la fuente de luz.
                    vec3 haciaLuz = normalize(-uDireccionLuz); //2.1
                    // Iluminacion direccional segun la orientacion de la cara.
                    float intensidadDifusa = max(dot(normal, haciaLuz), 0.0); //2.1
                    // Se combina luz ambiental + luz direccional.
                    vec3 iluminacion = uLuzAmbiente + uLuzDireccional * intensidadDifusa; //2.1

                    // Cada farola usa atenuacion cuadratica: su luz disminuye con la distancia. //2.2
                    vec3 luzFarolas = vec3(0.0); //2.2
                    for (int i = 0; i < 100; i++) { //2.2
                        if (i >= uCantidadFarolas) { //2.2
                            break; //2.2
                        } //2.2
                        float distancia = length(uPosFarolas[i] - vMundo); //2.2
                        // Aplica atenuacion exponencial para simular la disminucion de la luz con la distancia.
                        float atenuacion = exp(-0.015 * distancia * distancia) //2.2
                                                       / (1.0 + 0.25 * distancia); //2.2
                            
                                                                               
                        luzFarolas += uColorFarola * (5.0 * atenuacion); //2.2
                        
                    } //2.2
                    iluminacion += luzFarolas; //2.2
                    // Los faros del auto solo iluminan el entorno si estan encendidos. //2.3
                    if (uFarosEncendidos == 1) { //2.3
                        vec3 luzFaros = vec3(0.0); //2.3

                        for (int i = 0; i < 2; i++) { //2.3
                            vec3 posicionFaro = i == 0 ? uPosFaroIzquierdo : uPosFaroDerecho; //2.3
                            vec3 haciaFragmento = normalize(vMundo - posicionFaro); //2.3
                            float distancia = length(vMundo - posicionFaro); //2.3

                            // Cono dirigido hacia delante del automóvil. //2.3
                            float dentroDelCono = smoothstep( //2.3
                                cos(radians(28.0)), cos(radians(12.0)), //2.3
                                dot(haciaFragmento, normalize(uDireccionFaros)) //2.3
                            ); //2.3

                            // Alcance máximo suave de 22 unidades. //2.3
                            float alcance = 1.0 - smoothstep(14.0, 30.0, distancia); //2.3
                            float atenuacion = 1.0 / (1.0 + 0.08 * distancia //2.3
                                                        + 0.025 * distancia * distancia); //2.3

                            luzFaros += vec3(0.85, 0.92, 1.0) //2.3
                                    * (2.0 * dentroDelCono * alcance * atenuacion); //2.3
                        } //2.3

                        iluminacion += luzFaros; //2.3
                    } //2.3
                    
                    //color = vec4(uColor * iluminacion, 1.0); eso es solo hasta el 2.3

                    vec3 luzTrafico = vec3(0.0); //5.1

                    for (int i = 0; i < 26; i++) { //5.1
                        if (i >= uCantidadFarosTrafico) { //5.1
                            break; //5.1
                        } //5.1

                        vec3 haciaFaro = uPosFarosTrafico[i] - vMundo; //5.1
                        float distancia = length(haciaFaro); //5.1
                        vec3 direccionFaro = normalize(haciaFaro); //5.1

                        float difusa = max(dot(normalize(vNormal), direccionFaro), 0.0); //5.1
                        float atenuacion = 1.0 / (1.0 + 0.45 * distancia + 0.05 * distancia * distancia); //5.1

                        luzTrafico += vec3(1.0, 0.98, 0.85) * difusa * atenuacion * 3.5; //5.1
                    } //5.1

                    iluminacion += luzTrafico; //5.1

                    vec3 colorFinal = uColor * iluminacion + uEmision; //2.4
                    color = vec4(colorFinal, 1.0); //2.4
                }
            """;
    }
    
    /**
     * Compilar un shader y comunizca el mensaje del controlador si hay un error GLSL.
     */

    private int compilar(int tipo, String fuente) {
        int shader = glCreateShader(tipo); // Reserva un shader del tipo vertice o fragmento.
        glShaderSource(shader, fuente); // Entrega a openGL el texto GLSL.
        glCompileShader(shader); // Solicita la compilacion para esta GPU.
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(shader); // Recupera el detalle del error antes de liberar el shader.
            glDeleteShader(shader); // Libera el shader que no pudo compilarse.
            throw new IllegalStateException(error); // Detiene la aplicacion con el mensaje de error GLSL.
        }
        return shader; // Devuelve el identificador del shader compilado.
    }

    /**
     * Une los shaders en un programa ejecutable y libera los objetos intermedios.
     */

    private void crearPrograma() {
        int shaderVertice = compilar(GL_VERTEX_SHADER, vertexShader()); // Compila las transformaciones de vertices
        int shaderFragmento = 0; // Permite saber si el segundo shader llego a crearse.
        try { // Garantiza que ambos objetos intermedios se liberan incluso ante un fallo.
            shaderFragmento = compilar(GL_FRAGMENT_SHADER, fragmentShader()); // Compila el color de los fragmentos
            programa = glCreateProgram(); // Crea el contenedor que enlazara ambos shaders.
            glAttachShader(programa, shaderVertice); // Adjunta el shader que calcula posiciones de vertices.
            glAttachShader(programa, shaderFragmento); // Adjunta el shader que calcula color de fragmentos.
            glLinkProgram(programa); // Enlaza las entradas y salidas de ambos shader.
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) {  // Comprueba que el enlace fue exitoso
                throw new IllegalStateException(glGetProgramInfoLog(programa)); // Muestra incompleto el error de enlace si fallo.
            }
        } finally {  // Ya no hace falta objetos shader separados despues del enlace.
            glDeleteShader(shaderVertice); // Libera el objeto del shader de vertices.
            if (shaderFragmento != 0) {  // Comprueba que el segundo objeto exista.
                glDeleteShader(shaderFragmento); // Libera el objeto del shader de fragmentos.
            }
        }
            
    }
        

    /// ============================ 7. GEOMETRIA DEL CUBO: VAO Y VBO =========================
    
    /**
     * Crea un cubo explicito, con posicion XYZ y normal XYZ para vertice, como en
     * los ejemplos
     */

    private void crearCubo() {
        float[] vertices = {  // Cada linea describe un vertice: X, Y, Z, normalX, normalY, normalZ
            -0.5f, -0.5f, 0.5f,  0, 0, 1,   // Cara frontal: esquina inferior izquierda, normal apuntando hacia el frente
            0.5f, -0.5f, 0.5f,  0, 0, 1,    // Cara frontal: esquina inferior derecha.
            0.5f, 0.5f, 0.5f,  0, 0, 1,   // Cara frontal: esquina superior derecha; termina el primer triangulo de la cara frontal
            0.5f, 0.5f, 0.5f,  0, 0, 1,     // Cara frontal: repite la esquina para el segundo triangulo de la cara frontal
            -0.5f, 0.5f, 0.5f,  0, 0, 1,    // Cara frontal: esquina superior izquierda.
            -0.5f, -0.5f, 0.5f,  0, 0, 1,   // Cara frontal: Cierra el segundo triangulo.
            
            -0.5f, -0.5f, -0.5f,  0, 0, -1, // Cara trasera: primer vertice, normal hacia -Z.
            -0.5f, 0.5f, -0.5f,  0, 0, -1,  // Cara trasera: segundo vertice.
            0.5f, 0.5f, -0.5f,  0, 0, -1,   // Cara trasera: termina el primer triangulo.
            0.5f, 0.5f, -0.5f,  0, 0, -1,   // Cara trasera: inicia el segundo triangulo.
            0.5f, -0.5f, -0.5f,  0, 0, -1,  // Cara trasera: esquina inferior derecha.
            -0.5f, -0.5f, -0.5f,  0, 0, -1, // Cara trasera: cierra la cara.

            -0.5f, 0.5f, 0.5f,  -1, 0, 0,   // Cara izquierda: primer vertice, normal hacia -X.
            -0.5f, 0.5f, -0.5f,  -1, 0, 0,  // Cara izquierda: segundo vertice.
            -0.5f, -0.5f, -0.5f,  -1, 0, 0, // Cara izquierda: termina el primer triangulo.
            -0.5f, -0.5f, -0.5f,  -1, 0, 0, // Cara izquierda: inicia el segundo triangulo.
            -0.5f, -0.5f, 0.5f,  -1, 0, 0,  // Cara izquierda: esquina inferior frontal.
            -0.5f, 0.5f, 0.5f,  -1, 0, 0,   // Cara izquierda: cierra la cara.

            0.5f, 0.5f, 0.5f,  1, 0, 0,     // Cara derecha: primer vertice, normal hacia +X.
            0.5f, -0.5f, 0.5f,  1, 0, 0,    // Cara derecha: segundo vertice.
            0.5f, -0.5f, -0.5f,  1, 0, 0,   // Cara derecha: termina el primer triangulo.
            0.5f, -0.5f, -0.5f,  1, 0, 0,   // Cara derecha: inicia el segundo triangulo.
            0.5f, 0.5f, -0.5f,  1, 0, 0,    // Cara derecha: esquina superior trasera.
            0.5f, 0.5f, 0.5f,  1, 0, 0,     // Cara derecha: cierra la cara.

            -0.5f, 0.5f, -0.5f,  0, 1, 0,   // Cara superior: primer vertice, normal hacia +Y.
            -0.5f, 0.5f, 0.5f,  0, 1, 0,    // Cara superior: segundo vertice.
            0.5f, 0.5f, 0.5f,  0, 1, 0,     // Cara superior: termina el primer triangulo.
            0.5f, 0.5f, 0.5f,  0, 1, 0,     // Cara superior: inicia el segundo triangulo.
            0.5f, 0.5f, -0.5f,  0, 1, 0,    // Cara superior: esquina superior derecha.
            -0.5f, 0.5f, -0.5f,  0, 1, 0,   // Cara superior: cierra la cara.

            -0.5f, -0.5f, -0.5f,  0, -1, 0, // Cara inferior: primer vertice, normal hacia -Y.
            0.5f, -0.5f, -0.5f,  0, -1, 0,  // Cara inferior: segundo vertice.
            0.5f, -0.5f, 0.5f,  0, -1, 0,   // Cara inferior: termina el primer triangulo.
            0.5f, -0.5f, 0.5f,  0, -1, 0,   // Cara inferior: inicia el segundo triangulo.
            -0.5f, -0.5f, 0.5f,  0, -1, 0,  // Cara inferior: esquina inferior izquierda.
            -0.5f, -0.5f, -0.5f,  0, -1, 0  // Cara inferior: cierra los 36 vertices del cubo.
        };
        vao = glGenVertexArrays(); // Reserva el objeto que recordara la disposicion de los atributos de los vertices en el VBO.
        glBindVertexArray(vao); // Activa ese VAO para configurarlo.
        vbo = glGenBuffers(); // Reserva el buffer que almacenara los vertices.
        glBindBuffer(GL_ARRAY_BUFFER, vbo); // Selecciona el BVO como destino de datos de vertices.
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW); // Copia los datos de la GPU; el VBO queda lleno y listo para dibujar.
        int bytesPorVertice = 6 * Float.BYTES; // Cada vertice contiene seis float de cuadro bytes cada uno.
        glVertexAttribPointer(0, 3, GL_FLOAT, false, bytesPorVertice, 0L); // Describe XYZ desde el inicio del vertice (offset=0) para el atributo 0 del shader.

        glEnableVertexAttribArray(0); // Habilita la posicion que recibe aPos.
        glVertexAttribPointer(1, 3, GL_FLOAT, false, bytesPorVertice, 3 * Float.BYTES); // Describe la normal XYZ desde el cuarto float (offset=3) para el atributo 1 del shader.

        glEnableVertexAttribArray(1); // Habilita la normal que recibe aNormal.
        glBindBuffer(GL_ARRAY_BUFFER, 0); // Deja de seleccionar el VBO una vez configurado.
        glBindVertexArray(0); // Finaliza la configuracion del VAO.
    }
    
    /** Punto de entrada para ejecutar la primera leccion. */
    public static void main(String[] args) {
        AppCiudad aplicacion = new AppCiudad(); // Crea la aplicacion que muestra unicamente la ciudad.
        aplicacion.run(); // Inicia la ventana, el ciclo de dibujo y la limpieza final.
    }

}    
