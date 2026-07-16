# Configuración de Firebase (obligatorio antes de compilar)

La app usa **Firebase Firestore** como base de datos compartida: el residente
crea la visita desde su celular, y quien escanea el QR en el portón (otro
celular, o el mismo) consulta esa misma base de datos. Por eso Firestore es
necesario — una base de datos local (Room) no serviría porque cada
dispositivo tendría su propia copia.

## Pasos

1. Ve a [https://console.firebase.google.com](https://console.firebase.google.com)
   y crea un proyecto nuevo (gratis, plan Spark es suficiente).
2. Dentro del proyecto, click en el ícono de Android para "Agregar app".
   - **Nombre del paquete:** `com.example.doors` (debe ser exactamente este).
   - No hace falta SHA-1 para lo que estamos usando.
3. Firebase te dará un archivo `google-services.json`. Descárgalo y
   cópialo dentro de la carpeta `app/` del proyecto (al mismo nivel que
   `app/build.gradle.kts`).
4. En el menú lateral de la consola, ve a **Firestore Database** → **Crear
   base de datos** → elige **modo de prueba** para empezar (reglas abiertas
   temporalmente, ideal para desarrollo).
5. Sincroniza el proyecto en Android Studio ("Sync Now") y compila.

## Reglas de Firestore recomendadas (antes de publicar la app)

El modo de prueba deja la base de datos abierta a cualquiera. Antes de
publicar, cambia las reglas (pestaña **Rules** en Firestore) a algo como:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /visitas/{visitaId} {
      allow read, write: if request.auth != null;
    }
    match /accesos/{accesoId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

Esto exige que el usuario esté autenticado (una vez que el login esté
integrado con Firebase Auth) para poder leer o escribir. Con las reglas de
"modo de prueba" cualquiera con el nombre del proyecto podría leer o borrar
las visitas, así que no lo dejes así en producción.

## Colecciones que la app crea automáticamente

- **`visitas`**: un documento por visita registrada. El ID del documento es
  el mismo valor que se codifica en el QR.
- **`accesos`**: un documento por cada escaneo (válido o no), para el
  historial.

No necesitas crearlas a mano — se generan solas la primera vez que se
registra una visita o se escanea un QR.
