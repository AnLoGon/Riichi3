# Riichi3: Calculadora de Puntuaciones de Riichi Mahjong para 3 Jugadores (Sanma)

**Riichi3** es una calculadora de puntuaciones y combinaciones premium para la variante de 3 jugadores del juego de mesa **Riichi Mahjong (Sanma)**, desarrollada de forma nativa en **Kotlin** para Android. Combina una interfaz visual en modo oscuro premium de alta fidelidad (Material Design 3) con un motor matemático extremadamente rápido y riguroso de cálculo y partición de manos.

---

## 🚀 Características Principales

### 1. Tablero Blanco Interactivo de Doble Fila
- **Mano Cerrada (Fila Superior):** Muestra las fichas de tu mano de forma automática y ordenada de acuerdo con el orden reglamentario tradicional (`Man -> Pin -> Sou -> Vientos -> Dragones`). Al hacer clic en cualquier ficha del tablero, se elimina de forma fluida.
- **Melds Declarados (Fila Inferior):** Muestra de forma agrupada físicamente las secuencias o tripletes abiertos/ocultos. Al tocar un grupo de meld declarado, se elimina por completo del tablero de una sola vez.

### 2. Panel Avanzado de Declaración de Melds
- **Declaración Interactiva de Melds (`Chi`, `Pon`, `Kan`, `Kan oculto`):** 
  - **Pon, Kan y Ankan:** Al presionar el botón y hacer clic en una ficha del grid, se forman e integran inmediatamente las 3 o 4 copias idénticas.
  - **Chi:** Permite seleccionar 3 fichas en el grid; valida que pertenezcan a la misma categoría (Círculos o Bambúes) y formen una secuencia perfecta antes de integrarse.
- **Estilo Físico de Kan Oculto (Ankan):** Las fichas se dibujan en horizontal con los extremos visibles y los dos centros vacíos con un fondo **verde jade**, respetando el reglamento oficial.
- **Restricción de 4 Copias:** Valida en tiempo real que no se excedan más de 4 copias idénticas de una ficha de forma global (cerrada + melds).

### 3. Rastreo y Validación de Ficha Ganadora (Regla de Esperas para Pinfu)
- **Rastreo de Ficha Ganadora:** Registra cronológicamente la última ficha añadida a la mano cerrada (la del descarte o muro) y la destaca con un elegante borde grueso **anaranjado** en el tablero y en la pantalla de resultados.
- **Validación Estricta de Pinfu (Ryanzan Machi):** Comprueba que la ficha ganadora complete una secuencia en el Chow con una **espera doble**. Las esperas intermedias (Kanchan), esperas al borde (Penchan, ej. 3 esperando con 1-2, o 7 esperando con 8-9) y esperas de pareja única (Tanki) invalidan automáticamente Pinfu de acuerdo con el reglamento oficial.

### 4. Motor de Partición y Evaluación de Yakus (Sanma)
- **Algoritmo de Backtracking recursivo:** Evalúa y separa las 14 fichas en particiones válidas (Chow, Pong y Pareja), 7 Parejas (`Chiitoitsu`) o 13 Huérfanos (`Kokushi Musou`).
- **Validación Estricta de Yakus:** Comprueba todas las condiciones reglamentarias (ej. Yaku mínimo para ganar, reducción de Han por mano abierta/Kuichigi en Itsu, Chanta, Junchan, Honitsu y Chinitsu, y desactivación de Pinfu, Iipeiko y Ryanpeiko en manos abiertas).
- **Rinshan Kaihou Contextual:** Habilitado únicamente al declarar victoria por Tsumo tras completar al menos un Kan (abierto o cerrado).
- **Yakumans completos:** Reconoce Daisangen, Suuankou, Shousuushii, Daisuushii, Tsuuiisou, Chinroutou, Ryuuiisou, Chuuren Poutou y Kokushi Musou.

### 5. Pantalla de Resultados Premium y Desglose de Puntos
- **Cálculo Exacto de Puntos:** Implementa de forma exacta la matriz de puntos de Sanma distinguiendo si el ganador es Este (Dealer/Este) o no, y si la victoria es por Tsumo (se divide el pago entre los otros 2) o Ron (paga todo el descartador).
- **Honba exacto:** Aplica e incrementa con precisión los puntos por Honba (+200 total a Ron; cada oponente paga +100 adicionales en Tsumo).
- **Lista Dinámica de Yakus:** Genera de forma programática las filas con cada combinación obtenida y sus respectivos Han.
- **Nombre tradicional del Score:** Muestra de forma destacada el nombre según los Han obtenidos (Mangan, Haneman, Baiman, Sanbaiman, Yakuman).

### 6. Controles y Optimización del Flujo
- **Selección Requerida en Toggles:** Evita el estado desmarcado en las opciones críticas (Tsumo/Ron, Viento de Ronda y Viento de Jugador), garantizando que al menos una opción esté siempre seleccionada.
- **Habilitación Contextual:** Desactiva y desmarca automáticamente Riichi, Double Riichi e Ippatsu si la mano se abre (melds declarados abiertos).
- **Reinicio Automático:** Limpia todo el tablero, Dora, Honba y toggles al retornar desde la pantalla de resultados al pulsar `"NUEVA COMBINACIÓN"`. Si se regresa con la tecla de sistema "Atrás", conserva la mano para permitir correcciones rápidas.
- **Bloqueo Estricto de Pantalla:** Orientación bloqueada exclusivamente a modo vertical (portrait).

---

## 🛠️ Stack Tecnológico
- **Lenguaje:** Kotlin
- **Diseño de Interfaz:** XML Layouts, Material Design 3 Components (Modo Oscuro Premium en Pizarra Azul `#0F172A`).
- **Arquitectura de Navegación:** Moderno Activity Result API (`registerForActivityResult`).
- **SDK Mínimo:** Android 24 (Android 7.0 Nougat).
- **Herramienta de Construcción:** Gradle (Kotlin DSL).

---

## 📂 Arquitectura del Código Fuente
El código del proyecto está estructurado de manera modular y limpia en el paquete `com.example.riichi3`:

* **`Tile.kt`:** Define las categorías de fichas (`TileCategory`), tipos de fichas (`TileType`), el enum `MeldSource` y la clase de datos `DeclaredMeld` para estructurar la mano.
* **`MahjongEngine.kt`:** Contiene los algoritmos de partición recursiva, la lógica de Yakus (estándar y Yakumans), reducción de Han, reglas de esperas de Pinfu, y la matriz de cálculo de puntos y pagos.
* **`MainActivity.kt`:** Controla la interfaz principal del tablero, los botones interactivos de melds, selectores de viento, toggles de condiciones especiales, contadores de Dora/Honba y la transición de actividades.
* **`ScoreActivity.kt`:** Renderiza la mano ganadora final (destacando los melds y la ficha ganadora con borde naranja), calcula el desglose de pagos y genera dinámicamente la lista de combinaciones válidas obtenidas.
* **`ExampleUnitTest.kt` (Suite de Pruebas):** Suite completa de pruebas locales que validan el motor de partición, los Yakus de Tanyao, Pinfu, Chiitoitsu, Kokushi Musou, Suuankou cerrado con Ankan, reducciones por mano abierta (Kuichigi), y todas las esperas de Pinfu (Ryanzan, Penchan, Kanchan y Tanki).

---
