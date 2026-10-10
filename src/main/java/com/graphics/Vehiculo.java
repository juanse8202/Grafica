package com.graphics;

import static com.graphics.AppCiudad.SEMAFORO_ROJO;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class Vehiculo extends AppCiudad {
    private float autoX = 0;
    private float autoZ = 0;
    private float angulo = 0;
    private float velocidad = 0;
    private boolean cameraArea = false;  // Camara para primera y tercera persona
    private boolean farosEncendidos = false; //2.3

    private boolean mostrarAyuda = true; //5.2

    private final AutoTrafico[] trafico = { //5.1
        new AutoTrafico(new float[][]{ //5.1
                {centro(2), centro(2)}, {centro(18), centro(2)}, //5.1
                {centro(18), centro(18)}, {centro(2), centro(18)} //5.1
        }, 5.0f, 0.95f, 0.75f, 0.10f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(4), centro(4)}, {centro(16), centro(4)}, //5.1
                {centro(16), centro(16)}, {centro(4), centro(16)} //5.1
        }, 4.2f, 0.15f, 0.75f, 0.92f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(2), centro(10)}, {centro(10), centro(10)}, //5.1
                {centro(10), centro(18)}, {centro(2), centro(18)} //5.1
        }, 5.8f, 0.90f, 0.18f, 0.25f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(6), centro(2)}, {centro(14), centro(2)}, //5.1
                {centro(14), centro(10)}, {centro(6), centro(10)} //5.1
        }, 4.6f, 0.95f, 0.85f, 0.10f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(18), centro(6)}, {centro(18), centro(14)}, //5.1
                {centro(10), centro(14)}, {centro(10), centro(6)} //5.1
        }, 5.2f, 0.65f, 0.15f, 0.95f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(4), centro(18)}, {centro(4), centro(8)}, //5.1
                {centro(12), centro(8)}, {centro(12), centro(18)} //5.1
        }, 4.8f, 0.10f, 0.90f, 0.35f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(16), centro(18)}, {centro(16), centro(8)}, //5.1
                {centro(8), centro(8)}, {centro(8), centro(18)} //5.1
        }, 5.4f, 0.95f, 0.35f, 0.15f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(2), centro(14)}, {centro(8), centro(14)}, //5.1
                {centro(8), centro(4)}, {centro(2), centro(4)} //5.1
        }, 4.4f, 0.85f, 0.15f, 0.75f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(18), centro(4)}, {centro(12), centro(4)}, //5.1
                {centro(12), centro(12)}, {centro(18), centro(12)} //5.1
        }, 5.6f, 0.15f, 0.85f, 0.85f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(6), centro(16)}, {centro(14), centro(16)}, //5.1
                {centro(14), centro(6)}, {centro(6), centro(6)} //5.1
        }, 4.7f, 0.95f, 0.45f, 0.15f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(10), centro(2)}, {centro(10), centro(18)}, //5.1
                {centro(4), centro(18)}, {centro(4), centro(2)} //5.1
        }, 5.1f, 0.45f, 0.35f, 0.95f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(14), centro(14)}, {centro(6), centro(14)}, //5.1
                {centro(6), centro(4)}, {centro(14), centro(4)} //5.1
        }, 4.5f, 0.20f, 0.75f, 0.25f), //5.1

        new AutoTrafico(new float[][]{ //5.1
                {centro(2), centro(6)}, {centro(18), centro(6)}, //5.1
                {centro(18), centro(10)}, {centro(2), centro(10)} //5.1
        }, 5.3f, 0.90f, 0.25f, 0.55f) //5.1
    }; //5.1

    private static final float RADIO_AUTO = 1.65f; // Radio del vehiculo para las colisiones
    private static final float RADIO_ENTREGA = 2.5f;    //agregue 1.2

    // Cada destino esta situado sobre una calle, junto a un sector urbanizado.
    private final float[][] destinos = {                //agregue 1.2
        {centro(2), centro(1)},         
        {centro(7), centro(4)},         
        {centro(12), centro(11)},
        {centro(17), centro(18)}
    };
    private int destinoActual = 0;
    private int entregasCompletadas = 0;        //agregue 1.2

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

    }

    private void reiniciar() {
        autoX = 0.0f;
        autoZ = 0.0f;
        angulo = 0.0f;
        velocidad = 0.0f;

        cameraArea = false;  // Camara para primera y tercera persona punto 3.5
        orbita = 0.6f;  // Camara para primera y tercera persona punto 3.5

        destinoActual = 0;  //agregue   1.2
        entregasCompletadas = 0; //agregue 1.2

        farosEncendidos = false; // punto 3.5

        for (AutoTrafico auto : trafico) {
           auto.reiniciar();
        }

    }



    //--------------------- Conduccion ---------------------
    @Override
    protected void actualizar(float deltaTime) {
        float acelerador = 0;

        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) {
            acelerador += 1;
        }

        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) {
            acelerador -= 1;
        }

        velocidad += acelerador * 9 * deltaTime; 
        float resistencia = 0.7f;

        if (pulsada(GLFW_KEY_SPACE)) {
            resistencia = 7;
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime);
        velocidad *= factorFrenado;
        velocidad = Math.max(-6, Math.min(16, velocidad));
        float direccion = 0;

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) {
            direccion += 1;
        }

        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) {
            direccion -= 1;
        }

        angulo += direccion * velocidad * 0.11f * deltaTime;
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        float siguienteX = autoX + frenteX * velocidad * deltaTime;
        float siguienteZ = autoZ + frenteZ * velocidad * deltaTime;

        if (puedeCircular(siguienteX, siguienteZ)) {
            autoX = siguienteX;
            autoZ = siguienteZ;
        } else {
            velocidad = 0;
        }

        for (AutoTrafico auto : trafico) { //5.1
            if (!debeDetenerseEnSemaforo(auto)) { //5.1
                auto.actualizar(deltaTime); //5.1
            } //5.1
            
        } //5.1

        verificarEntrega(); // agregue
        actualizarTitulo(); 
    }

    @Override
    protected void dibujarInterfaz() {
        panelHUD.dibujar(
                ancho,
                alto,
                velocidad,
                farosEncendidos,
                modoNoche,
                mostrarAyuda
        );
    }

    protected void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f);
        String titulo = getClass().getSimpleName();
        titulo += " | WASD/Flechas: Conducir | Espacio: Freno";
        titulo += " | C: Camara | F: Faros | R: Reiniciar | ";
        titulo += kilometrosPorHora + " km/h";
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

        float dx = autoX - destinos[destinoActual][0];
        float dz = autoZ - destinos[destinoActual][1];
        if (dx * dx + dz * dz <= RADIO_ENTREGA * RADIO_ENTREGA) {
            entregasCompletadas++;
            destinoActual++;
        }
    }
    //agregue 1.2
    //--------------------- Colisiones con la ciudad ---------------------
    private boolean puedeCircular(float x, float z) {
        float limitePermitido = LIMITE - RADIO_AUTO;

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) {
            return false;
        }

        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == 0) {
                    continue;
                }

                float centroX = centro(columna);
                float centroZ = centro(fila);
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5));
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5));
                float distanciaX = x - cercaX;
                float distanciaZ = z - cercaZ;
                float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ;

                if (distanciaCuadrada < RADIO_AUTO * RADIO_AUTO) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override //2.3
    protected void prepararLuces() { //2.3
        super.prepararLuces(); //2.3

        // Minimapa local: el automóvil permanece centrado. //3.3
        //vector("uCentroMinimapa", autoX, 0.0f, autoZ); //3.3 auto minimapa
        //decimal("uRadioMinimapa", 35.0f); //3.3 auto minimapa

        float frenteX = -(float) Math.sin(angulo); //2.3
        float frenteZ = -(float) Math.cos(angulo); //2.3
        float lateralX = (float) Math.cos(angulo); //2.3
        float lateralZ = -(float) Math.sin(angulo); //2.3

        // Posiciones de los faros delanteros del automóvil. //2.3
        float faroIzqX = autoX + frenteX * 1.30f - lateralX * 0.55f; //2.3
        float faroIzqZ = autoZ + frenteZ * 1.30f - lateralZ * 0.55f; //2.3
        float faroDerX = autoX + frenteX * 1.30f + lateralX * 0.55f; //2.3
        float faroDerZ = autoZ + frenteZ * 1.30f + lateralZ * 0.55f; //2.3

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

        for (AutoTrafico auto : trafico) { //5.1
            float separacionCarril = 2.0f; //5.1
            float xCarril = auto.x + (float) Math.cos(auto.angulo) * separacionCarril; //5.1
            float zCarril = auto.z - (float) Math.sin(auto.angulo) * separacionCarril; //5.1

            enviarFaroTrafico(auto, xCarril, zCarril, -0.48f, -1.22f, indice++); //5.1
            enviarFaroTrafico(auto, xCarril, zCarril, 0.48f, -1.22f, indice++); //5.1
        } //5.1

        entero("uCantidadFarosTrafico", indice); //5.1
    } //5.1

    private void enviarFaroTrafico(AutoTrafico auto, float baseX, float baseZ, //5.1
            float localX, float localZ, int indice) { //5.1

        float coseno = (float) Math.cos(auto.angulo); //5.1
        float seno = (float) Math.sin(auto.angulo); //5.1

        float mundoX = baseX + coseno * localX + seno * localZ; //5.1
        float mundoZ = baseZ - seno * localX + coseno * localZ; //5.1

        vector("uPosFarosTrafico[" + indice + "]", mundoX, 0.68f, mundoZ); //5.1
    } //5.1


    @Override
    protected void configurarCamara() {
        if (cameraArea) {
            //super.configurarCamara();
            // Cámara de órbita lejana que AHORA sigue al auto
        float camaraX = autoX + (float) Math.sin(angulo) * 180; // cambie orbita por angulo
        float camaraZ = autoZ + (float) Math.cos(angulo) * 180; // cambie orbita por angulo
        
        vector("uOjo", camaraX, 150, camaraZ);
        vector("uObjetivo", autoX, 0f, autoZ); // Enfoca exactamente en el auto
        decimal("uAspecto", (float) ancho / alto);
            return;
        }
        float camaraX = autoX + (float) Math.sin(angulo) * 12;
        float camaraZ = autoZ + (float) Math.cos(angulo) * 12;

        vector("uOjo", camaraX, 9, camaraZ);
        vector("uObjetivo", autoX, 0.8f, autoZ);
        decimal("uAspecto", (float) ancho / alto);
    }    
    //--------------------- Dibujo del auto ---------------------
    @Override 
    protected void escena() {
        super.escena();

        dibujarTrafico(); //5.1

        dibujarDestinoActual();
        dibujarAuto();
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
        for (AutoTrafico auto : trafico) { //5.1
            dibujarAutoTrafico(auto); //5.1
        } //5.1
    } //5.1

    
    private boolean debeDetenerseEnSemaforo(AutoTrafico auto) { //5.1
        float objetivoX = auto.ruta[auto.siguientePunto][0]; //5.1
        float objetivoZ = auto.ruta[auto.siguientePunto][1]; //5.1

        float frenteX = objetivoX - auto.x; //5.1
        float frenteZ = objetivoZ - auto.z; //5.1
        float longitud = (float) Math.sqrt(frenteX * frenteX + frenteZ * frenteZ); //5.1

        if (longitud < 0.01f) { //5.1
            return false; //5.1
        } //5.1

        frenteX /= longitud; //5.1
        frenteZ /= longitud; //5.1

        for (float[] semaforo : semaforos) { //5.1
            if (estadoSemaforo(semaforo[2]) != SEMAFORO_ROJO) { //5.1
                continue; //5.1
            } //5.1

            float direccionSemaforoX = semaforo[3]; //5.1
            float direccionSemaforoZ = semaforo[4]; //5.1

            // Solo acepta el semáforo dirigido al mismo sentido del auto. //5.1
            float mismoSentido = frenteX * direccionSemaforoX + frenteZ * direccionSemaforoZ; //5.1
            if (mismoSentido < 0.95f) { //5.1
                continue; //5.1
            } //5.1

            float haciaSemaforoX = semaforo[0] - auto.x; //5.1
            float haciaSemaforoZ = semaforo[1] - auto.z; //5.1

            float adelante = haciaSemaforoX * frenteX + haciaSemaforoZ * frenteZ; //5.1
            float lateral = Math.abs(haciaSemaforoX * frenteZ - haciaSemaforoZ * frenteX); //5.1

            // El auto se detiene antes de la esquina y nunca después de cruzarla. //5.1
            boolean estaDelante = adelante > 0.4f && adelante < 3.5f; //5.1
            boolean estaEnSuCarril = lateral < 5.2f; //5.1

            if (estaDelante && estaEnSuCarril) { //5.1
                return true; //5.1
            } //5.1
        } //5.1

        return false; //5.1
    } //5.1


    private void dibujarAutoTrafico(AutoTrafico a) { //5.1
        float separacionCarril = 2.0f; //5.1

        // Vector lateral respecto al sentido en el que avanza el auto. //5.1
        float desplazamientoX = (float) Math.cos(a.angulo) * separacionCarril; //5.1
        float desplazamientoZ = -(float) Math.sin(a.angulo) * separacionCarril; //5.1

        // El vehículo se dibuja en un carril, no sobre la línea central. //5.1
        float xCarril = a.x + desplazamientoX; //5.1
        float zCarril = a.z + desplazamientoZ; //5.1

        piezaTrafico(a, xCarril, zCarril, 0.0f, 0.62f, 0.0f, 1.45f, 0.50f, 2.35f, a.r, a.g, a.b); //5.1
        piezaTrafico(a, xCarril, zCarril, 0.0f, 1.02f, 0.10f, 1.10f, 0.45f, 1.10f, 0.18f, 0.35f, 0.52f); //5.1

        for (float lado : new float[]{-0.76f, 0.76f}) { //5.1
            for (float eje : new float[]{-0.72f, 0.72f}) { //5.1
                piezaTrafico(a, xCarril, zCarril, lado, 0.30f, eje, 0.28f, 0.35f, 0.42f, 0.04f, 0.04f, 0.04f); //5.1
            }
        }

            // Los faros de los autos autónomos se encienden solo de noche. //5.1
        if (modoNoche) { //5.1
            dibujarFaroTrafico(a, xCarril, zCarril, -0.48f, 0.68f, -1.22f); //5.1
            dibujarFaroTrafico(a, xCarril, zCarril, 0.48f, 0.68f, -1.22f); //5.1
        } //5.1

    } //5.1


    private void piezaTrafico(AutoTrafico a, float baseX, float baseZ,
            float x, float y, float z,
            float sx, float sy, float sz,
            float r, float g, float b) { //5.1

        float coseno = (float) Math.cos(a.angulo); //5.1
        float seno = (float) Math.sin(a.angulo); //5.1

        float mundoX = baseX + coseno * x + seno * z; //5.1
        float mundoZ = baseZ - seno * x + coseno * z; //5.1

        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, a.angulo); //5.1
    } //5.1

    private void dibujarFaroTrafico(AutoTrafico a, float baseX, float baseZ, //5.1
            float localX, float y, float localZ) { //5.1

        float coseno = (float) Math.cos(a.angulo); //5.1
        float seno = (float) Math.sin(a.angulo); //5.1

        float mundoX = baseX + coseno * localX + seno * localZ; //5.1
        float mundoZ = baseZ - seno * localX + coseno * localZ; //5.1

        // Cubo blanco emisivo: representa un faro encendido. //5.1
        cajaEmisiva(mundoX, y, mundoZ, //5.1
                0.16f, 0.12f, 0.08f, //5.1
                1.0f, 1.0f, 0.82f, //5.1
                1.6f, 1.6f, 1.3f); //5.1
    } //5.1


    private void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo);
        float seno = (float) Math.sin(angulo);
        float mundoX = autoX + coseno * x + seno * z;
        float mundoZ = autoZ - seno * x + coseno * z;
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo);
    }

    private static class AutoTrafico { //5.1
        private final float[][] ruta; //5.1
        private final float velocidad; //5.1
        private final float r; //5.1
        private final float g; //5.1
        private final float b; //5.1

        private float x; //5.1
        private float z; //5.1
        private float angulo; //5.1
        private int siguientePunto; //5.1

        private AutoTrafico( //5.1
                float[][] ruta, float velocidad, float r, float g, float b) { //5.1
            this.ruta = ruta; //5.1
            this.velocidad = velocidad; //5.1
            this.r = r; //5.1
            this.g = g; //5.1
            this.b = b; //5.1
            reiniciar(); //5.1
        } //5.1

        private void reiniciar() { //5.1
            x = ruta[0][0]; //5.1
            z = ruta[0][1]; //5.1
            siguientePunto = 1; //5.1
            actualizarOrientacion(); //5.1
        } //5.1

        private void actualizar(float deltaTime) { //5.1
            float destinoX = ruta[siguientePunto][0]; //5.1
            float destinoZ = ruta[siguientePunto][1]; //5.1

            float dx = destinoX - x; //5.1
            float dz = destinoZ - z; //5.1
            float distancia = (float) Math.sqrt(dx * dx + dz * dz); //5.1
            float avance = velocidad * deltaTime; //5.1

            if (distancia <= avance) { //5.1
                x = destinoX; //5.1
                z = destinoZ; //5.1
                siguientePunto = (siguientePunto + 1) % ruta.length; //5.1
                actualizarOrientacion(); //5.1
                return; //5.1
            } //5.1

            x += dx / distancia * avance; //5.1
            z += dz / distancia * avance; //5.1
            angulo = (float) Math.atan2(-dx, -dz); //5.1
        } //5.1

        private void actualizarOrientacion() { //5.1
            float dx = ruta[siguientePunto][0] - x; //5.1
            float dz = ruta[siguientePunto][1] - z; //5.1
            angulo = (float) Math.atan2(-dx, -dz); //5.1
        } //5.1
    } //5.1

    public static void main(String[] args) {
      Vehiculo aplicacion = new Vehiculo();  
      aplicacion.run();
    }
        
}
