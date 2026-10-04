# Informe de proceso

Fundamentos de Programación Funcional y Concurrente — Taller 1: cifrados clásicos.
Este informe muestra cómo se ejecuta paso a paso cada función en
`app/src/main/scala/taller/CifradosClasicos.scala` y cuál es el estado de la pila
de llamados en cada punto.

El modelo de datos común es:

```scala
type Mensaje = String
type Clave = String
type Frecuencias = List[(Char, Int)]
val letras = 26
val primera = 'a'.toInt
def esMinuscula(c: Char): Boolean = c >= 'a' && c <= 'z'
```

Cifrar una letra de posición $p$ con desplazamiento $k$ es
$p' = (((p + k) \bmod 26) + 26) \bmod 26$, y toda letra que no esté entre `a` y `z`
se copia sin cambio.

## 1. Punto 1: `cesar` con recursión lineal

```scala
def cesar(m: Mensaje, k: Int): Mensaje = {
  if (m.isEmpty) ""
  else {
    val cifrado = m.head
    if (esMinuscula(cifrado))
      ((((cifrado - primera + k) % 26) + 26) % 26 + primera).toChar + cesar(m.tail, k)
    else cifrado + cesar(m.tail, k)
  }
}
```

El caso base es el mensaje vacío. En el caso recursivo se cifra la cabeza y se
concatena con el resultado de cifrar la cola: la suma `+` queda pendiente hasta
que la llamada recursiva retorna. La recursión es lineal: se acumula una operación
pendiente por cada letra.

### Traza de `cesar("casa", 3)`

| Paso | Llamada | Cabeza | Operación pendiente |
| ---- | ------- | ------ | ------------------- |
| 1 | `cesar("casa", 3)` | `c` | `'f' + _` |
| 2 | `cesar("asa", 3)` | `a` | `'d' + _` |
| 3 | `cesar("sa", 3)` | `s` | `'v' + _` |
| 4 | `cesar("a", 3)` | `a` | `'d' + _` |
| 5 | `cesar("", 3)` | — | caso base: `""` |

La pila crece hasta 5 marcos (uno por llamada) y luego se desarma resolviendo las
concatenaciones de abajo hacia arriba:

$$\text{cesar}("a",3) \rightarrow \text{'d'} + "" \rightarrow \text{"d"}$$
$$\text{cesar}("sa",3) \rightarrow \text{'v'} + \text{"d"} \rightarrow \text{"vd"}$$
$$\text{cesar}("asa",3) \rightarrow \text{'d'} + \text{"vd"} \rightarrow \text{"dvd"}$$
$$\text{cesar}("casa",3) \rightarrow \text{'f'} + \text{"dvd"} \rightarrow \text{"fdvd"}$$

```mermaid
sequenceDiagram
    participant C1 as cesar("casa", 3)
    participant C2 as cesar("asa", 3)
    participant C3 as cesar("sa", 3)
    participant C4 as cesar("a", 3)
    participant C5 as cesar("", 3)

    C1->>C2: 'f' + cesar("asa", 3)
    C2->>C3: 'd' + cesar("sa", 3)
    C3->>C4: 'v' + cesar("a", 3)
    C4->>C5: 'd' + cesar("", 3)
    C5-->>C4: ""
    C4-->>C3: "d"
    C3-->>C2: "vd"
    C2-->>C1: "dvd"
    C1-->>C1: "fdvd"
```

Con un mensaje de $n$ letras la pila alcanza $n$ marcos: para entradas muy largas el
proceso desborda la pila.

## 2. Punto 2: `cesarCola` con recursión de cola

```scala
@tailrec
final def cesarCola(m: Mensaje, k: Int, acc: Mensaje = ""): Mensaje = {
  if (m.isEmpty) acc
  else {
    val primero = m.head
    val caracterCesar = {
      if (esMinuscula(primero)) ((primero.toInt - primera + k) % letras + letras) % letras + primera
      else primero.toInt
    }.toChar
    cesarCola(m.tail, k, acc + caracterCesar)
  }
}
```

Aquí la letra ya cifrada se acumula en `acc` y la llamada recursiva es la última
instrucción, por lo que `@tailrec` la convierte en un ciclo.

### Traza de `cesarCola("casa", 3)`

| Paso | `m` | `k` | `acc` entrante | `acc` resultante |
| ---- | --- | --- | -------------- | ---------------- |
| 1 | `"casa"` | 3 | `""` | `"f"` |
| 2 | `"asa"` | 3 | `"f"` | `"fd"` |
| 3 | `"sa"` | 3 | `"fd"` | `"fdv"` |
| 4 | `"a"` | 3 | `"fdv"` | `"fdvd"` |
| 5 | `""` | 3 | `"fdvd"` | caso base: `"fdvd"` |

```mermaid
sequenceDiagram
    participant M as cesarCola("casa", 3)
    participant L1 as cesarCola("casa", 3, "")
    participant L2 as cesarCola("asa", 3, "f")
    participant L3 as cesarCola("sa", 3, "fd")
    participant L4 as cesarCola("a", 3, "fdv")
    participant L5 as cesarCola("", 3, "fdvd")

    M->>L1: llamada inicial
    L1->>L2: tail call, acc = "f"
    L2->>L3: tail call, acc = "fd"
    L3->>L4: tail call, acc = "fdv"
    L4->>L5: tail call, acc = "fdvd"
    L5-->>M: return "fdvd"
```

### Por qué una pila crece y la otra no

En `cesar`, cada marco espera el resultado de la llamada siguiente para poder
completar su `+`; el trabajo pendiente es proporcional a $n$. En `cesarCola` cada llamada entrega el mensaje más corto
y el acumulador más completo, y no queda ninguna operación por resolver. El compilador reutiliza el
mismo marco entonces el espacio es constante, $O(1)$ y el proceso equivale a un
ciclo. Ambas versiones devuelven lo mismo para toda entrada.

```mermaid
flowchart TD
    A[cesar/cesarCola: cada letra] --> B{¿letra a-z?}
    B -- sí --> C[sumar k, módulo 26]
    B -- no --> D[copiar sin cambio]
    C --> E{¿queda cola?}
    D --> E
    E -- sí, recursión --> A
    E -- no --> F[caso base: devolver acc o cadena vacía]
    F --> G[acumular con +, de regreso hacia arriba]
```

## 3. Punto 3: `frecuencias` con recursión de cola

```scala
def frecuencias(m: Mensaje): Frecuencias = {
  @tailrec
  def contar(i: Int, acc: Map[Char, Int]): Map[Char, Int] =
    if (i >= m.length) acc
    else {
      val c = m(i)
      if (c >= 'a' && c <= 'z') contar(i + 1, acc + (c -> (acc.getOrElse(c, 0) + 1)))
      else contar(i + 1, acc)
    }
  contar(0, Map.empty[Char, Int]).toList
    .sortBy { case (letra, frecuencia) => (-frecuencia, letra) }
}
```

`contar` recorre el mensaje con un índice y acumula un mapa de conteos; es de cola
porque la llamada recursiva es lo último. Cuando termina el mapa se vuelve lista y se
ordena por frecuencia descendente y, en empate, por letra ascendente.

### Traza de `frecuencias("casa")`

| Paso | `i` | `m(i)` | ¿Letra? | `acc` resultante |
| ---- | --- | ------ | ------- | ---------------- |
| 1 | 0 | `c` | sí | `Map('c' -> 1)` |
| 2 | 1 | `a` | sí | `Map('c' -> 1, 'a' -> 1)` |
| 3 | 2 | `s` | sí | `Map('c' -> 1, 'a' -> 1, 's' -> 1)` |
| 4 | 3 | `a` | sí | `Map('c' -> 1, 'a' -> 2, 's' -> 1)` |
| 5 | 4 | — | — | final: `Map('c' -> 1, 'a' -> 2, 's' -> 1)` |

El ordenamiento produce `List(('a', 2), ('c', 1), ('s', 1))`: la `a` va primero por
tener mayor frecuencia y, entre `c` y `s` empatan y gana la menor alfabéticamente.
Los caracteres que no son letras (como el espacio de `"hola mundo"`) se saltan sin
tocar el acumulador, y por eso `frecuencias("123 !?")` es `List()`.

```mermaid
sequenceDiagram
    participant F as frecuencias("casa")
    participant P1 as contar(0, {})
    participant P2 as contar(1, {c:1})
    participant P3 as contar(2, {c:1, a:1})
    participant P4 as contar(3, {c:1, a:1, s:1})
    participant P5 as contar(4, {c:1, a:2, s:1})

    F->>P1: llamada inicial
    P1->>P2: tail call, lee 'c'
    P2->>P3: tail call, lee 'a'
    P3->>P4: tail call, lee 's'
    P4->>P5: tail call, lee 'a'
    P5-->>F: return {c:1, a:2, s:1}
    F-->>F: ordenar -> List(('a',2), ('c',1), ('s',1))
```

## 4. Punto 4: `desplazamientoProbable` y `romperCesar`

```scala
def desplazamientoProbable(m: Mensaje): Int = {
  val conteo: Map[Char, Int] = m.toLowerCase
    .filter(c => c >= 'a' && c <= 'z')
    .groupBy(identity)
    .map { case (letra, ocurrencias) => letra -> ocurrencias.length }

  if (conteo.isEmpty) 0
  else {
    val maxFrecuencia = conteo.values.max
    val candidatas = conteo.collect { case (letra, f) if f == maxFrecuencia => letra }
    (((candidatas.min - 'e') % 26) + 26) % 26
  }
}

def romperCesar(m: Mensaje): Mensaje = {
  val k = desplazamientoProbable(m)
  cesar(m, -k)
}
```

Se normaliza el mensaje, se agrupan las letras y, si hay conteos, se toma la de
mayor frecuencia y, en empate, la menor alfabéticamente (`candidatas.min`). El
desplazamiento es la distancia entre esa letra y la `e`, y `romperCesar` vuelve a
cifrar con el desplazamiento negado.

### Traza de `desplazamientoProbable("hhhaaa")`

| Paso | Operación | Resultado |
| ---- | --------- | --------- |
| 1 | filtrar minúsculas | `"hhhaaa"` |
| 2 | agrupar y contar | `Map('h' -> 3, 'a' -> 3)` |
| 3 | `maxFrecuencia` | `3` |
| 4 | candidatas (empate) | `['h', 'a']` → `min` = `'a'` |
| 5 | `(('a' - 'e') % 26 + 26) % 26` | `(((0 - 4) % 26) + 26) % 26 = 22` |

Sin letras, como en `"123"`, el mapa queda vacío y el resultado es `0`. Con `"h"`,
la única candidata es `'h'` y el desplazamiento es `3`.

### Traza de `romperCesar(cesar("el mensaje secreto", 7))`

| Paso | Expresión | Valor |
| ---- | --------- | ----- |
| 1 | `cesar("el mensaje secreto", 7)` | `"ls tlyzwhz zljylavy"` |
| 2 | `desplazamientoProbable(...)` | `7` (la letra más frecuente es la `l`) |
| 3 | `cesar("ls tlyzwhz zljylavy", -7)` | `"el mensaje secreto"` |

La estimación acierta porque la letra dominante del cifrado corresponde a la `e`
del original. Cuando el mensaje es muy corto o su letra más frecuente no es la `e`,
el desplazamiento estimado es otro y `romperCesar` no recupera el mensaje.

```mermaid
sequenceDiagram
    participant R as romperCesar
    participant D as desplazamientoProbable
    participant C as cesar(m, -k)

    R->>D: analizar frecuencias
    D-->>R: k = 7
    R->>C: descifrar con -7
    C-->>R: "el mensaje secreto"
```
