package com.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL33.*;

public class PanelHUD implements AutoCloseable {

    private static final int ANCHO = 440;

    private final int programa;
    private final int vao;
    private final int textura;

    private String ultimoEstado = "";
    private int altoPanel = 140;

    public PanelHUD() {
        String vertexShader = """
                #version 330 core

                uniform vec2 uVentana;
                uniform vec2 uTamano;

                out vec2 uv;

                void main() {
                    vec2 vertices[4] = vec2[](
                        vec2(0.0, 0.0),
                        vec2(0.0, 1.0),
                        vec2(1.0, 0.0),
                        vec2(1.0, 1.0)
                    );

                    uv = vertices[gl_VertexID];

                    // Panel situado a 16 pixeles de la esquina superior izquierda.
                    vec2 pixel = vec2(16.0) + uv * uTamano;

                    gl_Position = vec4(
                        pixel.x / uVentana.x * 2.0 - 1.0,
                        1.0 - pixel.y / uVentana.y * 2.0,
                        0.0,
                        1.0
                    );
                }
                """;

        String fragmentShader = """
                #version 330 core

                in vec2 uv;
                uniform sampler2D uTextura;

                out vec4 color;

                void main() {
                    color = texture(uTextura, uv);
                }
                """;

        int vertex = compilar(GL_VERTEX_SHADER, vertexShader);
        int fragment = compilar(GL_FRAGMENT_SHADER, fragmentShader);

        programa = glCreateProgram();
        glAttachShader(programa, vertex);
        glAttachShader(programa, fragment);
        glLinkProgram(programa);

        glDeleteShader(vertex);
        glDeleteShader(fragment);

        if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException(glGetProgramInfoLog(programa));
        }

        vao = glGenVertexArrays();
        textura = glGenTextures();

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textura);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void dibujar(int ancho, int alto, float velocidad,
                        boolean faros, boolean noche, boolean ayuda) {

        int kmh = Math.round(Math.abs(velocidad) * 3.6f);
        String estado = kmh + "|" + faros + "|" + noche + "|" + ayuda;

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textura);

        // Solo genera otra imagen cuando cambia la informacion.
        if (!estado.equals(ultimoEstado)) {
            actualizarTextura(kmh, faros, noche, ayuda);
            ultimoEstado = estado;
        }

        float escala = Math.min(1.0f, Math.min(
                Math.max(1, ancho - 32) / (float) ANCHO,
                Math.max(1, alto - 32) / (float) altoPanel
        ));

        // El panel debe aparecer por encima de la ciudad.
        boolean profundidad = glIsEnabled(GL_DEPTH_TEST);
        boolean mezcla = glIsEnabled(GL_BLEND);

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);

        glUseProgram(programa);
        glBindVertexArray(vao);

        glUniform2f(glGetUniformLocation(programa, "uVentana"), ancho, alto);
        glUniform2f(glGetUniformLocation(programa, "uTamano"),
                ANCHO * escala, altoPanel * escala);
        glUniform1i(glGetUniformLocation(programa, "uTextura"), 0);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
        glUseProgram(0);

        if (profundidad) glEnable(GL_DEPTH_TEST);
        if (mezcla) glEnable(GL_BLEND);
    }

    private void actualizarTextura(int kmh, boolean faros,
                                   boolean noche, boolean ayuda) {

        altoPanel = ayuda ? 370 : 140;

        BufferedImage imagen = new BufferedImage(
                ANCHO, altoPanel, BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(18, 25, 36));
        g.fillRect(0, 0, ANCHO, altoPanel);

        g.setColor(new Color(80, 190, 220));
        g.drawRect(0, 0, ANCHO - 1, altoPanel - 1);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("VELOCIDAD: " + kmh + " km/h", 18, 32);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        g.setColor(faros ? new Color(110, 240, 140) : Color.LIGHT_GRAY);
        g.drawString("FAROS: " + (faros ? "ENCENDIDOS" : "APAGADOS"),
                18, 60);

        g.setColor(Color.WHITE);
        g.drawString("AMBIENTE: " + (noche ? "NOCHE" : "DIA"), 18, 88);

        g.setColor(new Color(100, 205, 240));
        g.drawString("H: " + (ayuda ? "Ocultar controles" : "Ver controles"),
                18, 116);

        if (ayuda) {
            g.setColor(Color.WHITE);
            g.drawString("CONTROLES", 18, 152);

            String[] controles = {
                "W / S o arriba / abajo: acelerar / retroceder",
                "A / D o izquierda / derecha: girar",
                "Espacio: frenar",
                "C: cambiar camara",
                "F: encender / apagar faros",
                "N: cambiar dia / noche",
                "M: mostrar / ocultar minimapa",
                "R: reiniciar",
                "Esc: salir"
            };

            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

            for (int i = 0; i < controles.length; i++) {
                g.drawString(controles[i], 18, 180 + i * 20);
            }
        }

        g.dispose();

        ByteBuffer pixeles = BufferUtils.createByteBuffer(
                ANCHO * altoPanel * 4
        );

        for (int y = 0; y < altoPanel; y++) {
            for (int x = 0; x < ANCHO; x++) {
                int color = imagen.getRGB(x, y);
                pixeles.put((byte) ((color >> 16) & 255));
                pixeles.put((byte) ((color >> 8) & 255));
                pixeles.put((byte) (color & 255));
                pixeles.put((byte) 255);
            }
        }

        pixeles.flip();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8,
                ANCHO, altoPanel, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, pixeles);
    }

    private static int compilar(int tipo, String fuente) {
        int shader = glCreateShader(tipo);
        glShaderSource(shader, fuente);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException(error);
        }

        return shader;
    }

    @Override
    public void close() {
        glDeleteTextures(textura);
        glDeleteVertexArrays(vao);
        glDeleteProgram(programa);
    }
}