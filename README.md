# Oasis Club — Plataforma Digital para la Gestión de un Club Deportivo


Este proyecto consiste en el diseño y desarrollo de una aplicación web completa orientada a la administración y reserva de instalaciones en un club deportivo y a la gestión del entrenamiento personalizado de sus socios. Implementando una arquitectura moderna desacoplada en la que el frontend y el backend se comunican mediante una API REST protegida por JWT.

---

## Índice

1. [Arquitectura General](#arquitectura-general)
2. [Stack Tecnológico Detallado](#stack-tecnológico-detallado)
   - [Backend (Servidor API)](#backend-servidor-api)
   - [Frontend (Aplicación Cliente)](#frontend-aplicación-cliente)
3. [Base de Datos e Integraciones](#base-de-datos-e-integraciones)
4. [Estructura del Repositorio](#estructura-del-repositorio)
5. [Funcionalidades y Control de Roles](#funcionalidades-y-control-de-roles)
6. [Entornos y Despliegue en Producción](#entornos-y-despliegue-en-producción)
7. [Guía de Configuración y Ejecución Local](#guía-de-configuración-y-ejecución-local)
8. [Compilación y Batería de Pruebas](#compilación-y-batería-de-pruebas)

---

## Arquitectura General

El sistema se basa en un modelo desacoplado y sin estado (Stateless):

```text
  [ Navegador Web ] <--- HTTPS / JWT ---> [ API Gateway / Backend ] <---> [ PostgreSQL ]
  (Angular Client)                         (Spring Boot REST API)          (Persistencia)
         |                                           |
         v                                           v
[ Vercel (Hosting) ]                       [ Render (Hosting) ] <---> [ Stripe API (Pagos) ]
```

* **Comunicación:** El cliente interactúa con la API REST enviando datos en formato JSON.
* **Seguridad:** Las peticiones a rutas protegidas se autorizan mediante el esquema OAuth2 Bearer Token (JWT).
* **Despliegue Continuo (CI/CD):** Integración con Vercel (Frontend) y Render (Backend) para despliegues automáticos a partir de ramas de Git.

---

## Stack Tecnológico Detallado

### Backend (Servidor API)

Desarrollado bajo una arquitectura limpia en capas (Controlador, Servicio, Repositorio, Entidad) utilizando Java 21 y el ecosistema de Spring Boot.

* **Java 21 (LTS):** Aprovechamiento de características modernas del lenguaje (como la mejora de rendimiento general de la JVM, Pattern Matching y el soporte nativo para características avanzadas).
* **Spring Boot 4.0.3:** Acelerador de desarrollo que nos provee de autoconfiguración y dependencias integradas.
* **Spring Security:** Framework encargado de la seguridad perimetral de la API. Se ha configurado de manera stateless para no almacenar sesiones HTTP en el servidor, utilizando un filtro personalizado (`JwtFilter`) que intercepta y autentica cada petición.
* **JSON Web Tokens (JWT):** Utilizado como mecanismo seguro para delegar la autenticación al cliente. Los tokens viajan cifrados, tienen una validez de 24 horas (`expiration-ms: 86400000`) y permiten al servidor verificar la identidad del cliente mediante su firma matemática sin realizar accesos redundantes a la base de datos.
* **Spring Data JPA (Hibernate):** Capa de abstracción de datos para interactuar con la base de datos relacional sin necesidad de escribir SQL nativo complejo, asegurando la portabilidad del código.
* **Flyway Migrations:** Sistema de control de versiones para el esquema de la base de datos. Garantiza la consistencia estructural del esquema SQL mediante scripts versionados ejecutados durante el arranque del backend.
* **Lombok:** Librería para la reducción de código repetitivo (boilerplate) en los DTOs y Entidades de Java.

---

### Frontend (Aplicación Cliente)

Construido utilizando Angular como framework corporativo para interfaces de usuario SPA (Single Page Application).

* **Angular 21 & TypeScript:** Tipado estático y desarrollo estructurado basado en componentes reutilizables y servicios inyectables.
* **Angular Signals:** Utilización del nuevo motor reactivo nativo de Angular para la gestión de estados compartidos en tiempo real (por ejemplo, en la sesión del usuario a través de `AuthStore`). Esto optimiza la detección de cambios y los renders del árbol de componentes.
* **RxJS:** Programación reactiva mediante flujos de datos asíncronos para el manejo eficiente de peticiones HTTP, eventos del DOM y observables de navegación.
* **Angular Guards (Guardas de Ruta):** Control perimetral a nivel de cliente (`AdminOnlyGuard`, `GuestOnlyGuard`). Impide la carga de rutas y la visualización de vistas si el usuario no cuenta con el token adecuado almacenado en el navegador.
* **Local Storage de Sesión:** Persistencia del token de sesión (`oasisclub.auth.session`) en el cliente para mantener al usuario conectado tras refrescar el navegador.

---

## Base de Datos e Integraciones

* **PostgreSQL:** Motor relacional robusto que asegura la integridad de los datos mediante restricciones de clave ajena, eliminaciones en cascada y claves únicas indexadas.
* **Pasarela de Pago Stripe:** Integración con la pasarela de pagos líder del mercado mediante SDK de backend y Webhooks para asegurar que una reserva se confirme únicamente tras un cobro válido y seguro.
* **Servicio SMTP (Gmail):** Envío automático de notificaciones de confirmación de reserva y correos seguros para la recuperación de contraseñas mediante tokens de un solo uso.

---

## Estructura del Repositorio

El monorrepocitorio se organiza de la siguiente manera:

```text
Oasis Club/
├── backend/                # Proyecto Spring Boot (Java 21)
│   ├── src/main/java       # Código fuente estructurado por módulos funcionales
│   ├── src/main/resources  # Configuración y migraciones de Flyway (db/migration)
│   └── pom.xml             # Gestor de dependencias Maven
├── frontend/               # Aplicación cliente Angular
│   ├── src/app/            # Componentes, servicios, guardas y modelos
│   └── package.json        # Gestor de dependencias npm
├── docs/                   # Documentación técnica adicional (memorias, diagramas)
├── docker-compose.yml      # Definición de contenedor local para PostgreSQL
├── .env.example            # Plantilla para variables de entorno locales
└── README.md               # Este documento explicativo
```

---

## Funcionalidades y Control de Roles

La aplicación divide su comportamiento según dos roles principales bien definidos:

| Funcionalidad                                                 | Rol `MEMBER` (Socio) | Rol `ADMIN` (Administrador) |
| :------------------------------------------------------------ | :--------------------: | :---------------------------: |
| Registrar cuenta y recuperar clave por email                  |          Sí          |              Sí              |
| Ver perfil, historial de reservas y actividad                 |          Sí          |              Sí              |
| Consultar disponibilidad de pistas en tiempo real             |          Sí          |              Sí              |
| Realizar reservas de pistas mediante pago con**Stripe** |          Sí          |               -               |
| Ver rutinas de gimnasio personalizadas (Días y Ejercicios)   |          Sí          |               -               |
| Bloquear pistas por tareas de mantenimiento                   |           -           |              Sí              |
| Crear, modificar o dar de baja pistas e instalaciones         |           -           |              Sí              |
| Gestión completa de clientes y sus roles                     |           -           |              Sí              |
| Crear y publicar Eventos/Torneos para el club                 |           -           |              Sí              |

---

## Entornos y Despliegue en Producción

Los entornos se encuentran totalmente operativos para su consulta en los siguientes enlaces:

* **Frontend (Web App):** [https://tfg-mocha.vercel.app](https://tfg-mocha.vercel.app)
* **Backend (Base API URL):** [https://oasisclub-backend.onrender.com](https://oasisclub-backend.onrender.com)
* **Documentación de la API (Swagger UI):** [https://oasisclub-backend.onrender.com/swagger-ui.html](https://oasisclub-backend.onrender.com/swagger-ui.html)

---

## Guía de Configuración y Ejecución Local

### Prerrequisitos

* **Java 21 LTS** y **Maven** instalados.
* **Node.js (v18+)** y **npm**.
* **Docker Desktop** (para levantar la base de datos).

### Paso 1: Configurar Variables de Entorno

1. Duplique el archivo `.env.example` en la raíz del proyecto y renombre la copia como `.env`.
2. Edite el archivo `.env` configurando las variables solicitadas (Credenciales de base de datos local, Claves de Stripe en modo pruebas, cuenta Gmail para SMTP y la semilla `JWT_SECRET` de mínimo 32 caracteres).

### Paso 2: Base de Datos Local

Levante el contenedor de PostgreSQL en segundo plano:

```bash
docker compose up -d postgres
```

### Paso 3: Lanzar el Backend

Diríjase a la carpeta del backend y arranque la aplicación con el perfil de desarrollo (`dev`):

```bash
cd backend
# En Windows (PowerShell/CMD):
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

# En Linux/macOS:
chmod +x mvnw
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

*El servidor arrancará en `http://localhost:8080` y Flyway creará automáticamente las tablas e insertará los datos iniciales.*

### Paso 4: Lanzar el Frontend

En otra terminal, diríjase a la carpeta del frontend, instale dependencias y levante el servidor de desarrollo:

```bash
cd frontend
npm install
npm run dev
```

*La aplicación estará accesible en `http://localhost:4200` y usará un proxy interno para comunicarse con el backend en el puerto 8080.*

> **Nota:** Si prefiere probar su frontend local contra el servidor en la nube ya desplegado en Render, puede ejecutar:
>
> ```bash
> npm run dev:render
> ```

---

## Compilación y Batería de Pruebas

Para garantizar la calidad del código e integración del proyecto, puede ejecutar la batería de pruebas integradas de la siguiente manera:

* **Pruebas unitarias y de integración del Backend (JUnit / Spring Boot Test):**

  ```bash
  cd backend
  ./mvnw test
  ```
* **Construcción de producción y Pruebas del Frontend (Karma / Jasmine):**

  ```bash
  cd frontend
  npm run build
  npm run test -- --watch=false
  ```

---

*Desarrollado como proyecto académico de Trabajo Fin de Grado (TFG).*
