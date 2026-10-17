package com.graphics.mundo;

/** Una caja coloreada que forma parte del terreno exterior de la ciudad. */
public record ElementoPaisaje(float x, float y, float z,
        float ancho, float alto, float profundidad,
        float rojo, float verde, float azul, float giro) {
}
