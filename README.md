# DOOR'S 

**Sistema inteligente de gestión de accesos para condominios**
*(Portón de acceso a condominio)*

![Estado](https://img.shields.io/badge/estado-en%20desarrollo-yellow)
![Plataforma](https://img.shields.io/badge/plataforma-Android-green)
![Lenguaje](https://img.shields.io/badge/lenguaje-Kotlin-blueviolet)
![Metodología](https://img.shields.io/badge/metodolog%C3%ADa-Scrum-blue)

---

### 📌 ¿Qué es DOOR'S?

DOOR'S es una aplicación móvil Android que digitaliza y moderniza el control de acceso en condominios y comunidades residenciales, reemplazando los controles remotos (llaveros RF) y los registros manuales en papel por un sistema inteligente basado en **códigos QR**, con trazabilidad total y gestión en la nube.

### El problema que resuelve

Hoy en día, la mayoría de los condominios operan con métodos análogos y reactivos que generan tres brechas críticas:

- **Inseguridad operativa**: los controles remotos se pierden, se clonan o quedan en manos de ex-residentes.
- **Invisibilidad de datos**: el registro manual en conserjería es propenso a errores y no permite auditorías rápidas.
- **Fricción en la experiencia**: autorizar visitas o delivery requiere llamadas o gestiones presenciales innecesarias.

### La solución

- Generación de **códigos QR temporales** para autorizar visitas de forma autónoma.
- **Registro automático** de cada entrada y salida, con datos almacenados en la nube.
- **Roles diferenciados** para residentes, conserjes y administración.
- **Historial y reportes** de accesos para mejorar la seguridad y la toma de decisiones.

---

### 👥 Actores del sistema

| Actor | Rol principal |
|---|---|
| **Residente** | Genera QR para visitas, autoriza ingresos, controla el portón y consulta su historial. |
| **Conserje** | Valida códigos QR, registra accesos y consulta el historial general. |
| **Administrador** | Gestiona usuarios, roles y reportes de seguridad. |
| **Visita** | Ingresa al condominio presentando el código QR recibido. |

---

### 🧩 Alcance del proyecto

### Incluye
- Gestión de usuarios (registro, roles)
- Gestión de accesos (app residentes, códigos QR, gestión de visitas, integración con portones eléctricos)
- Gestión de visitas (invitaciones digitales, notificaciones en tiempo real)
- Seguridad (reportes, control de vigilancia, registro automático de entradas/salidas, autenticación)

### No incluye (fuera de alcance)
- Cámaras de reconocimiento facial u otras tecnologías biométricas (huella digital, reconocimiento facial)
- Integración física real con hardware de portones (queda planteada de forma teórica/simulada)

---

### ⚙️ Requisitos funcionales (resumen)

| ID | Funcionalidad | Prioridad |
|---|---|---|
| RF-01 | Inicio de sesión seguro | Alta |
| RF-02 | Gestión de residentes (CRUD) | Alta |
| RF-03 | Generación de código QR temporal | Alta |
| RF-04 | Control remoto del portón | Alta |
| RF-05 | Registro de accesos | Alta |
| RF-06 | Autorización remota de visitas | Alta |
| RF-07 | Consulta de historial de accesos | Alta |

### 🔒 Requisitos no funcionales (resumen)

| ID | Característica | Criterio |
|---|---|---|
| RNF-01 | Seguridad | Cifrado AES-256 y HTTPS |
| RNF-02 | Tiempo de respuesta | < 3 segundos |
| RNF-03 | Usabilidad | Flujo principal completable en < 1 min |
| RNF-04 | Multiplataforma | Compatible con Android e iOS |
| RNF-05 | Disponibilidad | SLA 99.9% anual |

---

### 🏗️ Arquitectura y modelado

El proyecto cuenta con documentación de diseño que incluye:

- **Diagrama de casos de uso** (Residente, Conserje, Administración)
- **Diagrama de clases** (Usuario, Residente, Conserje, Administración, Visitas, AccesoQR, Portón, Registro Acceso, Notificación)
- **Diagrama de secuencia** (flujo de generación y uso de código QR)

> 📄 Estos diagramas y el detalle completo del levantamiento (contexto, EDT, historias de usuario, matriz de riesgos, stakeholders, etc.) se encuentran en la carpeta [`/docs`](./docs).

---

### 🛠️ Stack tecnológico

- **Lenguaje:** Kotlin
- **Plataforma:** Android (Android Studio)
- **Base de datos:** *(completar según definición final: Firebase / SQLite / Room / backend propio)*
- **Generación/lectura de QR:** *(completar librería utilizada, ej. ZXing)*
- **Control de versiones:** Git + GitHub

---

### 📋 Metodología de trabajo

El proyecto se desarrolla bajo **Scrum**, organizando el trabajo en sprints que permiten entrega progresiva de funcionalidades, validación temprana con usuarios (residentes y conserjes) y adaptación a cambios durante el desarrollo.

El seguimiento de tareas se gestiona mediante un tablero **Kanban** con las columnas:
`Project Backlog` → `Sprint Backlog` → `En proceso` → `Realizado`

---

### 🚀 Estado actual del proyecto

El proyecto se encuentra en **fase de desarrollo controlado**, con la etapa de análisis, diseño y planificación ya consolidada.

---

### ⚠️ Riesgos identificados

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Validación incorrecta de datos | Alto | Revisión de datos / bloqueo de usuario |
| Falla en apertura del portón | Alto | Pruebas de sistema / apertura manual |
| Alcance demasiado amplio | Alto | Definir un MVP claro |
| Complejidad en integración de patentes/hardware | Media | Dejar como función opcional |
| Baja adopción por parte de usuarios | Media | Interfaz simple + capacitación |

---

### 📄 Supuestos y restricciones

**Supuestos:**
- El condominio cuenta con acceso controlado (portón, reja o conserjería).
- Los residentes tienen acceso a internet y a un dispositivo móvil.
- La administración y el conserje están dispuestos a usar el sistema.

**Restricciones:**
- Proyecto desarrollado dentro del tiempo limitado del semestre académico.
- Algunas funciones (integración con portones automáticos, lectores mediante cámaras) pueden quedar solo simuladas o teóricas.
- El enfoque está en el control de acceso y gestión de visitas, no en la administración integral del condominio.

---

## 👨‍💻 Equipo

| Integrante | Rol principal |
|---|---|
| Alonso C. | Definición de alcance y requisitos |
| Seba M. | Diseño, ciclo de vida y Carta Gantt |
| Paz O. | Metodología, calidad y riesgos |
| Seba B. | Stakeholders, roles del equipo y supuestos/restricciones |

---

*Proyecto desarrollado como parte de la asignatura de Ingeniería de Software.*

---