<div align="center">

# 🌎 EcoSismo

### Red Móvil Circular de Alerta Temprana

**Una segunda vida para los celulares en desuso: estaciones sísmicas de bajo costo al servicio de la comunidad.**

Solución propuesta para el **Rally Latinoamericano de Innovación 2026**

![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Cloud-Firebase%20Firestore-FFCA28?logo=firebase&logoColor=black)
![Kotlin DSL](https://img.shields.io/badge/Build-Gradle%20Kotlin%20DSL-02303A?logo=gradle&logoColor=white)
![Estado](https://img.shields.io/badge/Estado-Prototipo%20funcional-orange)

<img width="1672" height="941" alt="Rally Latinoamericano EcoSismo Background" src="https://github.com/user-attachments/assets/b14052f3-b1cb-4811-a835-64b3c0ee9c6b" />

</div>

---

## 📋 Tabla de contenido

- [Descripción de la problemática](#-descripción-de-la-problemática)
- [Antecedentes](#-antecedentes)
- [La propuesta: EcoSismo](#-la-propuesta-ecosismo)
- [Funcionalidades](#-funcionalidades)
- [Novedades de la versión final](#-novedades-de-la-versión-final)
- [¿Cómo funciona?](#️-cómo-funciona)
- [Modos de conexión](#-modos-de-conexión)
- [Fuentes de datos](#-fuentes-de-datos)
- [Arquitectura y tecnologías](#️-arquitectura-y-tecnologías)
- [Integración con Firebase](#-integración-con-firebase)
- [Necesidades para su implementación](#-necesidades-para-su-implementación)
- [¿Por qué es una buena solución?](#-por-qué-es-una-buena-solución)
- [Diferenciador](#-diferenciador)
- [Instalación](#-instalación)
- [Estructura del repositorio](#-estructura-del-repositorio)

---

## 🧩 Descripción de la problemática

En cajones y armarios de todo el país hay **millones de celulares viejos que aún funcionan**. Aunque ya no sirven como teléfonos, conservan sensores de movimiento y orientación (**acelerómetro, giroscopio y magnetómetro**), además de **GPS y conectividad**.

Esa capacidad contrasta con una carencia frecuente en las comunidades: fenómenos como los **temblores**, las **vibraciones de una estructura** o el **desgaste de una vía** casi nunca se miden, porque los instrumentos especializados son caros y de difícil acceso.

Este reto propone aprovechar esos equipos para resolver un problema local, definiendo:

- **Qué** mide el dispositivo.
- **Dónde** se instala.
- **Cómo** se alimenta y se comunica.
- **Quién** usa sus datos para tomar decisiones.

---

## 📚 Antecedentes

El uso del celular como instrumento de medición ya cuenta con respaldo:

| Área | Trabajo | Aporte |
|---|---|---|
| Sismología | **MyShake** (Kong et al., 2016) | Demostró que los acelerómetros de teléfonos pueden conformar una red colaborativa para detectar sismos. |
| Estructuras | Figueiredo et al., 2022 | Uso del celular para medir vibraciones de puentes y reconocer posibles daños. |
| Sensórica abierta | **phyphox** y **GeoPhyphox** (RWTH Aachen University) | Registro de datos de movimiento y de campo magnético. |
| Monitoreo de vías | Mednis et al., 2011; Allouch et al., 2017; Zareei et al., 2025 | Detección de baches y estimación de la rugosidad del pavimento con acelerómetro, giroscopio y GPS. |
| Antecedente colombiano | Casas-Avellaneda & López-Parra, 2016 | Experiencia local en el uso de sensores de teléfonos. |

Adicionalmente, existen múltiples repositorios y bases de datos de libre acceso, y otros usos explorados como el seguimiento de deslizamientos, la inclinación de taludes y la vibración de maquinaria.

### 🔎 La brecha

La mayoría de estos estudios depende de **celulares que viajan dentro de vehículos de voluntarios**. Son escasos los que instalan **teléfonos en desuso como estaciones fijas**, con energía y comunicación propias.

**Ahí radica la oportunidad de esta propuesta:** darles una segunda vida a estos equipos como herramientas de medición de bajo costo al servicio de la comunidad.

---

## 💡 La propuesta: EcoSismo

**EcoSismo** es una aplicación Android que da una segunda vida a celulares en desuso para **acercar la información sísmica a la comunidad**. Funciona **con internet y también sin él**, y permite sincronizar estaciones y registros en la nube cuando hay conexión.

La app se organiza en tres secciones principales:

| Pestaña | Qué ofrece |
|---|---|
| ♻️ **Puntos de Recolección** | Mapa de lugares donde donar celulares, con ruta "Cómo llegar" y registro de donaciones. |
| 🌐 **Historial Sísmico** | Eventos sísmicos recientes de Colombia (magnitud, profundidad, ubicación) y acceso al visor oficial del SGC. |
| 📡 **Estaciones** | Red de estaciones de monitoreo, forma de onda en tiempo real, detección STA/LTA y telemetría del dispositivo. |

---

## ✨ Funcionalidades

- 🗺️ **Mapa interactivo de Colombia** con puntos de recolección y estaciones en Armenia, Pereira, Cali, Medellín, Ibagué, Manizales, Bogotá, Bucaramanga, Quibdó y Neiva.
- 📍 **"Cómo llegar"** a cada punto mediante integración con Google Maps.
- 📊 **Historial sísmico de Colombia** con datos de catálogos internacionales (USGS y EMSC).
- 🏛️ **Visor oficial del SGC** (Servicio Geológico Colombiano) integrado en la app.
- 📡 **Modo estación**: el celular mide vibraciones con su **acelerómetro y giroscopio** mediante un servicio en primer plano que sigue activo con la app en segundo plano.
- 📈 **Forma de onda sísmica en vivo** y cálculo de **PGA** (aceleración máxima del suelo, en m/s²).
- 🚨 **Detección de eventos con algoritmo STA/LTA** (promedio de corto plazo / largo plazo) con sensibilidad ajustable y notificación de alerta.
- 🔋 **Telemetría real del dispositivo**: nivel y estado de la batería, fuente de carga (AC, USB, inalámbrica), temperatura, voltaje, salud de la batería y ubicación.
- ☁️ **Sincronización en la nube con Firebase** e inicio de sesión con Google.
- 📴 **Funcionamiento offline** con base de datos local y sincronización automática de datos pendientes al recuperar la conexión.
- 🌗 **Tema claro y oscuro** según la configuración del sistema (con colores dinámicos en Android 12+).
- 📱 **Interfaz adaptable**: en pantallas anchas (tablets o modo horizontal) el mapa y el panel de la estación se muestran lado a lado.

---

## 🆕 Novedades de la versión final

- ☁️ **Integración con Firebase**: autenticación con Google (Credential Manager) y almacenamiento en **Cloud Firestore** de usuarios, estaciones y registros sísmicos.
- 🔐 **Reglas de seguridad de Firestore**: solo usuarios autenticados pueden leer, y cada quien solo puede modificar sus propias
