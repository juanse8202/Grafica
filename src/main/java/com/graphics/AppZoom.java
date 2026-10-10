package com.graphics;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Este programa hace 3 cosas:
 * 1. Abrir una ventana usando la biblioteca de GLFW
 * 2. Definir un triangulo y usos shgaders
 * 3. En un bucle, dibujar el triangulo en la ventana una y otra vez
 *
 * CONCEPTOS CLAVES:
 *  - OpenGL: conjunto de funciones para dibujar en pantalla usando el GPU
 *  - GLFW: biblioteca que crea la ventana y maneja el teclado/raton. OpenGL solo dice como dibujar no crea ventanas
 *  - GPU: tarjeta grafica
 *  - Shaders: codigo que dice como transformar vertice y que color dar a cada pixel. Se escribe en lenguaje GLSL
 *  - VAO/VBO: forma de guardar en la GPU los datos de los vetices
 */
public class AppZoom 
{
    //referencia a nuestra ventana
    private long window;

    //Nuestro programa de shaders: es el vertex shader + fragment shader unidos
    // La GPU usa este programa para dibujar cada frame
    private int program;

    // VAO = Vertex Array Object: es como un dosificador que queda toda la configuracion  
    private int vao;

    // VBO = Vertex Buffer Object: es un bloque de memoria en la GPU donde guardamos las posiciones de los vertices
    private int vbo;

    private static final int ANCHO = 800;
    private static final int ALTO = 600;

    // Diferencia de zoom
    private int uZoomLocation; 
    // Zoom actual
    private float zoom = 1.0f;
    private static final float ZOOM_MIN = 0.25f;
    private static final float ZOOM_MAX = 3.00f;
    private static final float VELOCIDAD_ZOOM = 1.25f;

    /**
     * Punto de entrada: inicializamos todo, ejecutamos el bucle del dibujo y al salir limpiamos
     */
    
    public void run() {
        init();
        loop();
        cleanup();
    }

    // PASO 1: Inicializar GLFW y crear la ventana
    // GLFW (Graphics Library Framework) es una biblioteca que:
    // -Crea y gestiona la ventana del sistema
    // -Recibe igual eventos, teclado, raton, ventana
    public void init() {
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
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_ANY_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        // MacOS hay que reactivar para usar el core profile
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        // Creamos la ventana: ancho, alto, titulo, monitor (0=ventana), ventana compartida
        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "OpenGL - Triangulo", 0, 0);

        // Cerrar la ventana al presionar ESC
        GLFW.glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS){
                GLFW.glfwSetWindowShouldClose(w, true);
            }
        });

        GLFW.glfwSetScrollCallback(window, (w, xoffser, yoffset) ->{
            zoom += (float) yoffset * 0.10f;
            zoom = clamp(zoom, ZOOM_MIN, ZOOM_MAX);
        });


        // Decir que todas las ordenes de OPENGL que ejecutamos a partir de ahora se aplican a esta ventana
        GLFW.glfwMakeContextCurrent(window);

        // Sincronizar el dibujo con el monitor (1 = esperar al siguiente refresco)
        // Evitamos el "tearing" (imagen partida)
        GLFW.glfwSwapInterval(1);
        // Hacer visible la ventana
        GLFW.glfwShowWindow(window);

        // Carga en java las funciones de OpenGl
        //LWJGL necesita este paso para enlazar las llamadas de Java con la GPU
        GL.createCapabilities();

        crearShaders();
        crearTriangulo();
    }


    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }


    //================================
    //paso 2: CREAR LOS SHADERS (programas que corren en la GPU)
    //================================
    //¿pro que existen dos shaders?
    // En OpenGL la GPU dibuja en dos etapas:
    // 1. VERTEX SHADER: se ejecuta por cada VERTICE (en nuestro triangulo, 3 veces)
    //    Su trabajo es decir donde va ese vertice en pantalla (posicion 2D/3D)
    // 2. FRAGMENT SHADER: se ejecuta por cada PIXEL que ocupa el triangulo (miles y miles)
    //    Su trabajo es decir de que color se pinta ese píxel.
    // Sin el vertes shader, la GPU no sabria donde colocar los puntos, Sin el fragment shader no 
    // sabria que color dar a cada pixel. Por eso se requiere los dos.
    //
    // GLSL (OpenGL Shading Language) Es un lenguaje parecido a C, por que se usa para 
    // programar la GPU
    //================================
    private void crearShaders() 
    
    {
        //vertexSRC: codigo GLSL del vertex shader
        // Recibe la poscion de cada vertice (aPos) y la asigna a gl_Position (salida estandar)
        //"layaout(location = 0)" in vec3 aPos = entrada en el canal 0, 3 floats (x,y,z) 
        String vertexSrc = """
            #version 330 core
            layout(location = 0) in vec3 aPos;
            uniform float uZoom;
            void main() {
                vec3 pos = vec3(aPos.xy * uZoom, aPos.z);
                gl_Position = vec4(pos, 1.0);
            }
            """;
        
        // franmentSrc: codigo GLSL del fragment shader
        // fragColor = color de salida del pixel. vec4(R,G,B,A) = rojo, verde, azul, alpha
        // Se ejecuta por cada pixel del triangulo. la salida es el color del pixel
        // vec4(0.2, 0.8 , 0.4, 1.0) = color verde
        String fragmentSrc = """
            #version 330 core
            out vec4 fragColor;
            void main(){ 
                fragColor = vec4(0.2, 0.8, 0.4, 1.0); 
            }
            """;

        // Crear el objeto en la GPU y compilar el codigo
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "VERTEX");

        // Crear el objeto en la GPU y compilar el codigo
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        comprobarShader(fragmentShader, "Fragment"); 

        // Crear el programa (vertex + fragment unidos)
        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);

        // Comprobar que el enlace fue correcto
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar programa de shaders: " + GL20.glGetProgramInfoLog(program));
        }

        uZoomLocation = GL20.glGetUniformLocation(program, "uZoom");
        if (uZoomLocation == -1) {
            throw new RuntimeException("Error al encontrar uniform uZoom");
        }

        // Los shaders ya estan copiados dentro del programa, podemos borrarlos para liberar memoria
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private void comprobarShader(int shader, String tipo){
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " shader " + GL20.glGetShaderInfoLog(shader));
        }
    } 

    // ================================
    // PASO 3: CREAR EL TRIANGULO
    // ================================
    // Un triangulo son 3 puntos (vertices). Cada verticer tiene una posicion (x,y,z).
    // En OpenGL las coordenadas suelen ir de -1 a 1: (-1, -1) = esquina inferior izquierda, 
    // (1, 1) = esquina superior derecha. (0,0) 
    private void crearTriangulo() {

        // Definimos los 3 vertices del triangulo (x,y,z)
        float[] vertices = {
             0.0f,  0.5f, 0.0f,  // vertice 1
            -0.5f, -0.5f, 0.0f, // vertice 2
             0.5f, -0.5f, 0.0f, // vertice 3
            
        };

        // VAO: objeto que guarda la configuracion de los buffers y atributos
        vao = GL30.glGenVertexArrays(); // Crear un VAO
        GL30.glBindVertexArray(vao); // Activar el VAO

        //VBO: buffer en la GPU donde guardamos las coordenadas
        vbo = GL15.glGenBuffers(); // Crear un VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo); // Activar el VBO

        // OpenGL espera los datos en FloatBuffer, no en un float[] de Java
        // copiamos el array al buffer y flip()  dejamos lsitos para la lectura
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip(); // Copiar los vertices al buffer
        // Enviar los datos al VBO. GL_STATIC_DRAW = "no vamos a cambiar estos datos"
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW); // Copiar el buffer a la GPU

        // Decir a OpenGL como voy a leer este buffer
        // atributo location 0 (como layout (location =0) en el vertex shader)
        // 3 floats por vertice (x,y,z)
        // tipo GL_FLOAT, sin normalizar
        // stride = 12 bytes (3 floats x 4 byte), offset = 0
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0); // Decirle a OpenGL como interpretar los datos del VBO
        // Activar el atributo 0 (en el shader en in vec3 aPos en location 0)
        GL20.glEnableVertexAttribArray(0);

        // Desenlazar el VBO y VAO. Porque ya no hace falta dejarlos activos
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0); // Desactivar el VBO
        GL30.glBindVertexArray(0); // Desactivar el VAO

    }

    private void procesarInput(float deltaTime) {
        float paso = VELOCIDAD_ZOOM * deltaTime;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_ADD) == GLFW.GLFW_PRESS 
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_EQUAL) == GLFW.GLFW_PRESS) {
            zoom += paso;
        }

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_SUBTRACT) == GLFW.GLFW_PRESS 
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_MINUS) == GLFW.GLFW_PRESS) {
            zoom -= paso;
        }

        zoom = clamp(zoom, ZOOM_MIN, ZOOM_MAX);
    }
    // BUCLE PRINCIPAL: en cada iteracion dibujamos un frame
    // Minetras la ventana no se cierre:
    // 1. Limpiamos la ventana
    // 2. Decimos usar este programa de shaders Y este VAO para dibujar
    // 3. Ordenamos dibujar los 3 vertices
    // 4. Intercambiamos los buffer 
    // 5. Procesamos los eventos
    private void loop() {
        // Tiempo ultimo
        float ultimoTiempo = (float) GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            // Movimiento desacoplado de FPS usando deltaTime
            float tiempoActual = (float) GLFW.glfwGetTime();
            float deltaTime = tiempoActual - ultimoTiempo;
            ultimoTiempo = tiempoActual;

            // Actualizar el offsetX/offsetY segun el teclado
            procesarInput(deltaTime);
            
            
            // Color con el que vamos a limpiar la pantalla (gris oscuro: RGBA)
            GL11.glClearColor(0.08f, 0.08f, 0.12f, 1.0f);
            //limpiar el buffer de color (rellenar con el color anterior)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            // Activar nuestro programa de shaders 
            GL20.glUseProgram(program);

            // Enviamos el uZoom actualizado al shader antes de dibujar
            GL20.glUniform1f(uZoomLocation, zoom);
            // Activar el VAO que contiene el triangulo
            GL30.glBindVertexArray(vao);

            // Orden del dibujo: modo, emepzar con el vertice 0 y dubujar 3 vertices
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

            // Doble buffer: hemos dibujado en un buffer oculto y ahora lo mostramos en pantalla e intercambiamos
            GLFW.glfwSwapBuffers(window);

            // Procesamos eventos del sistema (si el usuario cerro ventana, si hizo click, etc)
            GLFW.glfwPollEvents();
        }
    }

    private void cleanup() {
        // Liberar recursos de OpenGL
        GL20.glDeleteProgram(program);
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);

        // Liberar recursos de GLFW
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main( String[] args )
    {
        new AppZoom().run();
    }
}
