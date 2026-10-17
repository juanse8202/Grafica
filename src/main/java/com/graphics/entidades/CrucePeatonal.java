package com.graphics.entidades;

/** Describe un paso peatonal y el semaforo vehicular que lo controla. */
public class CrucePeatonal {
    private final float inicioX;
    private final float inicioZ;
    private final float finX;
    private final float finZ;
    private final float desfaseSemaforo;
    private final float direccionVehicularX;
    private final float direccionVehicularZ;

    public CrucePeatonal(float inicioX, float inicioZ, float finX, float finZ,
            float desfaseSemaforo, float direccionVehicularX, float direccionVehicularZ) {
        this.inicioX = inicioX;
        this.inicioZ = inicioZ;
        this.finX = finX;
        this.finZ = finZ;
        this.desfaseSemaforo = desfaseSemaforo;
        this.direccionVehicularX = direccionVehicularX;
        this.direccionVehicularZ = direccionVehicularZ;
    }

    public float getInicioX() { return inicioX; }
    public float getInicioZ() { return inicioZ; }
    public float getFinX() { return finX; }
    public float getFinZ() { return finZ; }
    public float getDesfaseSemaforo() { return desfaseSemaforo; }
    public float getDireccionVehicularX() { return direccionVehicularX; }
    public float getDireccionVehicularZ() { return direccionVehicularZ; }
}
