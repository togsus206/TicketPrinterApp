
# Ticket Printer App

Una aplicación Android diseñada para simplificar la gestión de productos y la generación de tickets de venta personalizables. Permite a los usuarios añadir productos, calcular el total de la venta, configurar detalles del ticket (logo, encabezado, pie de página, tamaño del papel) y compartir previsualizaciones del ticket con códigos QR dinámicos o predefinidos.

## Características

  * **Gestión de Productos:**
      * Añade productos con nombre, cantidad y precio.
      * Edita y elimina productos de la lista.
      * Cálculo automático del total de la venta.
  * **Personalización de Tickets:**
      * Configura un logo personalizado para el ticket.
      * Define un encabezado y un pie de página para el ticket.
      * Opción para incluir la fecha y hora de impresión.
      * Selección del ancho del papel (58mm o 80mm).
  * **Códigos QR en Tickets:**
      * Genera un código QR dinámico que muestra el total de la venta.
      * Opción de usar un código QR predefinido desde una imagen seleccionada por el usuario.
  * **Previsualización y Compartir:**
      * Previsualiza el ticket antes de compartir.
      * Comparte la imagen del ticket generado con otras aplicaciones (WhatsApp, correo, etc.).
  * **Conectividad Bluetooth (En desarrollo/Futura implementación):**
      * Búsqueda y listado de dispositivos Bluetooth cercanos (para futuras conexiones con impresoras).
      * Manejo de permisos de Bluetooth para Android 12+.

## Permisos Necesarios

La aplicación requiere los siguientes permisos para su correcto funcionamiento:

  * `android.permission.READ_EXTERNAL_STORAGE` (hasta API 32): Para seleccionar imágenes de logo y QR predefinidos.
  * `android.permission.WRITE_EXTERNAL_STORAGE` (hasta API 29): Para guardar la previsualización del ticket antes de compartirlo.
  * `android.permission.BLUETOOTH`: Permisos básicos de Bluetooth.
  * `android.permission.BLUETOOTH_ADMIN`: Permisos para administrar Bluetooth.
  * `android.permission.BLUETOOTH_SCAN` (Android 12+): Para escanear dispositivos Bluetooth cercanos.
  * `android.permission.BLUETOOTH_CONNECT` (Android 12+): Para conectarse a dispositivos Bluetooth.
  * `android.permission.ACCESS_FINE_LOCATION` / `android.permission.ACCESS_COARSE_LOCATION`: Para la búsqueda de dispositivos Bluetooth en versiones anteriores a Android 12.

Estos permisos son solicitados en tiempo de ejecución cuando son necesarios.

## Estructura del Proyecto

El proyecto está organizado en las siguientes actividades y clases principales:

  * **`MainActivity.kt`**: La pantalla principal de la aplicación donde se gestionan los productos, se calcula el total y se accede a la previsualización/compartir y a los ajustes.
  * **`SettingsActivity.kt`**: Permite al usuario configurar las preferencias del ticket, incluyendo el logo, encabezado, pie de página, tamaño del papel y el tipo de contenido del QR (total o predefinido).
  * **`Product.kt`**: Una clase de datos simple para representar un producto con nombre, cantidad y precio.
  * **`ProductAdapter.kt`**: Adaptador para el `RecyclerView` que muestra la lista de productos añadidos, permitiendo editar y eliminar ítems.
  * **`activity_main.xml`**: Layout XML para la `MainActivity`.
  * **`activity_settings.xml`**: Layout XML para la `SettingsActivity`.
  * **`ticket_preview.xml`**: Layout XML utilizado para generar la imagen del ticket de previsualización.
  * **`ticket_item_layout.xml`**: Layout XML para cada ítem de producto dentro del ticket de previsualización.

## Cómo Compilar y Ejecutar

1.  **Clonar el repositorio:**
    ```bash
    git clone https://github.com/togsus206/TicketPrinterApp
    ```
2.  **Abrir en Android Studio:**
      * Abre Android Studio y selecciona `Open an existing Android Studio project`.
      * Navega hasta la carpeta donde clonaste el repositorio y selecciónala.
3.  **Sincronizar Gradle:**
      * Android Studio debería sincronizar automáticamente los archivos Gradle. Si no lo hace, haz clic en `Sync Project with Gradle Files` (el icono del elefante con la flecha verde).
4.  **Generar Íconos Adaptativos (Recomendado):**
      * Haz clic derecho en la carpeta `app/src/main/res`.
      * Selecciona `New` \> `Image Asset`.
      * Configura las capas de Foreground y Background para generar un ícono de aplicación optimizado. Usa una imagen PNG transparente de tu logo para el Foreground.
5.  **Ejecutar en un Dispositivo/Emulador:**
      * Conecta un dispositivo Android a tu computadora (asegúrate de tener la depuración USB habilitada) o inicia un emulador.
      * Haz clic en el botón `Run` (el triángulo verde) en la barra de herramientas de Android Studio.

## Uso de la Aplicación

1.  **Añadir Productos:** En la pantalla principal, introduce el nombre, cantidad y precio de un producto y pulsa "Agregar Producto".
2.  **Editar/Eliminar Productos:** En la lista de productos, puedes hacer clic en los botones de editar o eliminar junto a cada ítem.
3.  **Ajustes del Ticket:** Pulsa el botón "Ajustes" para acceder a la pantalla de configuración donde puedes:
      * Seleccionar un logo para el ticket.
      * Introducir un encabezado y pie de página.
      * Activar/desactivar la impresión de fecha y hora.
      * Elegir el ancho del papel del ticket (58mm o 80mm).
      * Seleccionar un código QR predefinido o elegir que se genere con el total de la venta.
      * ¡No olvides pulsar "Guardar Configuración"\!
4.  **Previsualizar y Compartir:** Una vez que tengas productos en tu lista y los ajustes configurados, pulsa el botón "Previsualizar/Compartir" para ver el diseño del ticket y luego compartirlo a través de las opciones de tu dispositivo (WhatsApp, correo, etc.).
5.  **Búsqueda Bluetooth:** Utiliza el botón "Escanear" en la pantalla principal para buscar dispositivos Bluetooth. (La funcionalidad de conexión e impresión directa aún está en desarrollo).
