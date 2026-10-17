# OpenGL Java Class

Proyecto Java con Maven, LWJGL y OpenGL.

## Requisitos

- JDK 17 o superior. El proyecto compila bytecode Java 17; LWJGL 3.4.3 permite ejecutarlo también con JDK 25.
- Maven
- Windows con soporte para OpenGL

## Compilar

Ejecuta los comandos desde la carpeta raiz del proyecto, donde se encuentra `pom.xml`:

```powershell
mvn clean compile
```

## Compilar las pruebas

```powershell
mvn test-compile
```

## Ejecutar las pruebas

```powershell
mvn test
```

## Ejecutar la aplicacion

```powershell
mvn compile exec:java
```

Este comando ejecuta `com.graphics.SimuladorCiudad`.

Para ejecutar la aplicacion con movimiento del teclado:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.graphics.AppMovimientoTeclado"

mvn compile exec:java "-Dexec.mainClass=com.graphics.SimuladorCiudad"
```

La aplicacion crea una ventana OpenGL con un triangulo.

## Nota

Usa `exec:java`, no `exec:exec`, porque el `pom.xml` configura `mainClass` para el objetivo `java` del plugin `exec-maven-plugin`.
