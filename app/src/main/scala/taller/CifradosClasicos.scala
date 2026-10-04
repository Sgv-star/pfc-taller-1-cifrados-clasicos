package taller

import scala.annotation.tailrec

/**
 * Taller 1 — cifrados clásicos con recursión.
 *
 * Solo se cifran las 26 letras minúsculas del alfabeto inglés; cualquier otro
 * carácter se copia sin cambio.
 */
class CifradosClasicos {

  type Mensaje = String
  type Clave = String

  // Una frecuencia asocia cada letra con las veces que aparece.
  type Frecuencias = List[(Char, Int)]

  val letras = 26
  val primera = 'a'.toInt

  def esMinuscula(c: Char): Boolean = c >= 'a' && c <= 'z'

  // Punto 1 -------------------------------------------------------------------

  /** César con recursión lineal: una operación pendiente por letra. */
  def cesar(m: Mensaje, k: Int): Mensaje = {
    if (m.isEmpty) ""
    else{
      val cifrado = m.head
      if(esMinuscula(cifrado))
        ((((cifrado - primera + k) % 26) + 26) % 26 + primera).toChar + cesar(m.tail, k)
      else cifrado + cesar(m.tail, k)
    }
  }



  // Punto 2 -------------------------------------------------------------------

  /**
   * El mismo César como proceso iterativo: espacio constante.
   * Cuando la función esté escrita, anótela con @tailrec: el compilador
   * comprueba que la llamada recursiva sea lo último que hace.
   */
  @tailrec
  final def cesarCola(m: Mensaje, k: Int, acc: Mensaje = ""): Mensaje = {
      if (m.isEmpty) acc
      else {
        val primero = m.head
        val caracterCesar = {
          if (esMinuscula(primero)) {
            ((primero.toInt - primera + k) % letras + letras) % letras + primera
          }
          else {
            primero.toInt
          }
        }.toChar
        cesarCola(m.tail, k, acc + caracterCesar)
      }
  }

  // Punto 3 -------------------------------------------------------------------

  /**
   * Cuenta las letras minúsculas del mensaje, de mayor a menor frecuencia y,
   * en empate, en orden alfabético. El recorrido es recursivo de cola.
   */
  def frecuencias(m: Mensaje): Frecuencias = {
     
    @tailrec
    def contar(i: Int, acc: Map[Char, Int]): Map[Char, Int] =
      if (i >= m.length) acc
      else {
        val c = m(i)
        if (c >= 'a' && c <= 'z')
          contar(i + 1, acc + (c -> (acc.getOrElse(c, 0) + 1)))
        else
          contar(i + 1, acc)
      }

    contar(0, Map.empty[Char, Int]).toList
      .sortBy { case (letra, frecuencia) => (-frecuencia, letra) }
  }

  // Punto 4 -------------------------------------------------------------------

  /**
   * Supone que la letra más frecuente del mensaje cifrado es la 'e' del
   * original y devuelve la distancia entre las dos. Sin letras, cero.
   */
  def desplazamientoProbable(m: Mensaje): Int = {
        val conteo: Map[Char, Int] =
      m.toLowerCase
        .filter(c => c >= 'a' && c <= 'z')
        .groupBy(identity)
        .map { case (letra, ocurrencias) => letra -> ocurrencias.length }
 
    if (conteo.isEmpty) {
      0
    } else {
      val maxFrecuencia = conteo.values.max
      val candidatas = conteo.collect { case (letra, f) if f == maxFrecuencia => letra }
      val letraMasFrecuente = candidatas.min
 
      (((letraMasFrecuente - 'e') % 26) + 26) % 26
    }
  }

  def romperCesar(m: Mensaje): Mensaje = {
        val k = desplazamientoProbable(m)
        cesar(m, -k)
  }

  // Punto 5 -------------------------------------------------------------------

  /**
   * Cuántos mensajes de longitud n se forman con a letras sin dos iguales
   * seguidas.
   */
  def combinaciones(n: Int, a: Int): BigInt = {
    @tailrec
    def aux(restantes: Int, acc: BigInt): BigInt =
      if (restantes == 0) acc
      else aux(restantes - 1, acc * (a - 1))
    if (n <= 0) BigInt(1)
    else if (a <= 0) BigInt(0)
    else aux(n - 1, BigInt(a))
  }
  /**
   * Vigenère: cada letra se corre según la letra de la clave que le toca. Lo
   * que no es letra minúscula se copia y no consume clave.
   */
  def vigenere(m: Mensaje, clave: Clave): Mensaje ={
    @tailrec
    def aux(i: Int, j: Int, acc: Mensaje): Mensaje =
      if (i >= m.length) acc
      else {
        val c = m(i)
        if (esMinuscula(c)) {
          val k = (((clave(j) - 'a') % letras) + letras) % letras
          val cifrada = ((c - 'a' + k) % letras + 'a').toChar
          aux(i + 1, (j + 1) % clave.length, acc + cifrada)
        } else
          aux(i + 1, j, acc + c)
      }
    if (clave.isEmpty) m else aux(0, 0, "")
  }
}
