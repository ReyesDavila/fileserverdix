# FileServerDix 📱💻

**FileServerDix** es un servidor HTTP local minimalista diseñado para Android. Su objetivo principal es permitir la transferencia de archivos a alta velocidad entre múltiples dispositivos dentro de la misma red local a través del navegador web, eliminando la necesidad de cables o aplicaciones de terceros.

El proyecto destaca por su enfoque híbrido de alto rendimiento y accesibilidad universal: un motor nativo potente en el backend y una interfaz web ultraligera diseñada bajo estándares heredados para garantizar que incluso los dispositivos más antiguos del mercado puedan conectarse.

---

## ✨ Características Principales

* **Interfaz Android Ultra-Minimalista:** Control total con un único botón para encender/apagar el servidor local y un campo de entrada flotante para personalizar el puerto de red.
* **Compatibilidad Universal Absoluta:** La aplicación web servida está programada estrictamente en **ECMAScript 3** y **CSS 2.1**. Esto garantiza compatibilidad nativa hacia atrás con navegadores históricos como **Internet Explorer 8**, manteniendo al mismo tiempo un renderizado e interoperabilidad perfectos en navegadores modernos.
* **Cero Dependencias Externas:** No utiliza frameworks pesados ni librerías externas en el frontend (Vanilla JS puro), garantizando tiempos de carga instantáneos.
* **Privacidad y Velocidad:** Toda la transferencia de datos ocurre de forma local dentro de tu red Wi-Fi; tus archivos nunca pasan por internet.
* **Próximamente 🚀:** Integración de un generador de códigos QR nativo en la pantalla principal para escanear la IP y el puerto automáticamente desde cualquier móvil o tablet.

---

## 🛠️ Arquitectura Técnica

El proyecto se divide limpiamente en dos capas integradas:

### 1. Backend (Android Nativo)
* **Lenguaje:** Java (Android API 14+).
* **Motor:** Servidor HTTP integrado personalizado (`MonoServer.java`) que gestiona los sockets de red, mapea los recursos estáticos y expone los endpoints de la API interna.

### 2. Frontend (Aplicación Web Servida)
* **Estructura:** HTML5 básico estructurado para máxima compatibilidad.
* **Estilos:** CSS 2.1 optimizado para evitar problemas de maquetación en motores de renderizado antiguos (Trident, Gecko antiguo).
* **Lógica:** ECMAScript 3 nativo para el manejo de transferencias asíncronas de datos sin requerir polyfills modernos.

---

## 📂 Estructura del Repositorio

Para mantener el proyecto limpio y apto para los estándares de auditoría de plataformas como **F-Droid**, el código se organiza de la siguiente manera:

* `app/src/main/java/`: Código fuente nativo de Android (Manejo de UI y lógica del Servidor HTTP).
* `app/src/main/assets/public_html/`: Entorno de la aplicación web (HTML, CSS y JS que se sirven al navegador cliente).
* `.gitignore`: Configuración estricta de exclusión para evitar la subida de archivos basura del IDE (`.idea/`, cachés o binarios compilados).

---

## 🚀 Compilación e Instalación

1. Clona este repositorio en tu máquina local.
2. Abre el proyecto en **Android Studio**.
3. Asegúrate de tener configurado el SDK en la API 29 y las Build Tools en la versión 30.0.3 (definidas para evitar conflictos de empaquetado).
4. Sincroniza el proyecto mediante **"Sync Now"**.
5. Compila el APK a través de `Build > Build APK(s)` o ejecútalo directamente en tu dispositivo físico mediante depuración USB.

---

## 📄 Licencia

Este proyecto es de código abierto y está disponible bajo la licencia descrita en el archivo `LICENCE`. ¡Las contribuciones y reportes de optimización para navegadores antiguos son siempre bienvenidos!