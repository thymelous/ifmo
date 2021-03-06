package ru.ifmo.se.lab5

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import kotlin.Comparator

class PriorityQueueStorage<E>(
  elementClass: Class<E>,
  source: File,
  private val comparator: Comparator<in E>? = null
) {
  private val storage: ArrayStorage<E> = ArrayStorage(elementClass, source)

  fun read(): PriorityQueue<E> {
    val queue = if (comparator != null)
      PriorityQueue<E>(comparator)
    else
      PriorityQueue()

    return queue.apply { addAll(storage.read()) }
  }

  fun write(queue: PriorityQueue<E>) =
    storage.write(queue.toArray() as Array<E>)

  class ArrayStorage<E>(private val elementClass: Class<E>, private val source: File) {
    private val mapper = ObjectMapper().apply {
      registerModule(JavaTimeModule())
      disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    init {
      source.createNewFile()

      if (!(source.canRead() && source.canWrite()))
        throw IOException(
          "The specified file has to be readable and writable by the application.")
    }

    fun read(): Array<E> {
      val json = StringBuilder().apply {
        Scanner(source).let { scan ->
          while (scan.hasNext()) append(scan.nextLine())
          scan.close()
        }
        if (isEmpty()) append("[]")
      }.toString()

      return with(mapper) {
        readValue(json, typeFactory.constructArrayType(elementClass))
      }
    }

    fun write(array: Array<E>) {
      val writer = PrintWriter(source)
      mapper.writeValue(writer, array)
      writer.close()
    }
  }
}
