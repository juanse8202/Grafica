package com.graphics.entidades;

/** Datos de una persona; SimuladorCiudad realiza su dibujo con OpenGL. */
public class Persona {
    private float x;
    private float y;
    private float z;
    private float angulo;
    private float velocidad;
    private final float r;
    private final float g;
    private final float b;
    private final boolean jugador;
    private final TipoPersona tipo;
    private EstadoPersona estado;
    private float animacion;

    public Persona(float x, float y, float z, float velocidad,
            float r, float g, float b, boolean jugador, EstadoPersona estado) {
        this(x, y, z, velocidad, r, g, b, jugador, estado, TipoPersona.HOMBRE);
    }

    public Persona(float x, float y, float z, float velocidad,
            float r, float g, float b, boolean jugador, EstadoPersona estado,
            TipoPersona tipo) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.velocidad = velocidad;
        this.r = r;
        this.g = g;
        this.b = b;
        this.jugador = jugador;
        this.estado = estado;
        this.tipo = tipo;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getAngulo() { return angulo; }
    public float getVelocidad() { return velocidad; }
    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }
    public boolean esJugador() { return jugador; }
    public TipoPersona getTipo() { return tipo; }
    public EstadoPersona getEstado() { return estado; }
    public float getAnimacion() { return animacion; }

    public void setPosicion(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setAngulo(float angulo) { this.angulo = angulo; }
    public void setVelocidad(float velocidad) { this.velocidad = velocidad; }
    public void setEstado(EstadoPersona estado) { this.estado = estado; }

    public void actualizarAnimacion(float deltaTime, boolean caminando) {
        if (caminando) {
            animacion += deltaTime * 8.0f;
        } else {
            animacion = 0.0f;
        }
    }
}
