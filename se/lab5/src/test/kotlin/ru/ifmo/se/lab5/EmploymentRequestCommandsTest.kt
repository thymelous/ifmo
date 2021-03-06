package ru.ifmo.se.lab5

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

import ru.ifmo.se.lab5.EmploymentRequestCommands.*
import ru.ifmo.se.lab5.CommandRunner.Command.CommandStatus
import ru.ifmo.se.lab5.CommandRunner.Command.CommandStatus.*
import ru.ifmo.se.lab5.EmploymentRequestCommands.Companion.STATUS_CLEARED
import ru.ifmo.se.lab5.EmploymentRequestCommands.Companion.STATUS_ELEMENT_ADDED
import ru.ifmo.se.lab5.EmploymentRequestCommands.Companion.STATUS_IMPORTED
import ru.ifmo.se.lab5.EmploymentRequestCommands.Companion.STATUS_LOADED
import ru.ifmo.se.lab5.EmploymentRequestCommands.Companion.STATUS_MANY_REMOVED
import ru.ifmo.se.lab5.EmploymentRequestCommands.Companion.STATUS_ONE_REMOVED
import ru.ifmo.se.lab5.EmploymentRequestCommands.Companion.STATUS_SAVED
import ru.ifmo.se.lab5.EmploymentRequestCommands.Companion.STATUS_UNCHANGED
import java.io.File

class EmploymentRequestCommandsTest {
  private val jsonDeserializer = Deserializer(EmploymentRequest::class.java)

  @Test
  fun `"clear" removes all elements`() {
    val queue = queue(EmploymentRequest("joe"), EmploymentRequest("amy"))
    assertTrue(queue.isNotEmpty())

    assertSuccess(STATUS_CLEARED) { ClearCommand().run("", queue) }
    assertTrue(queue.isEmpty())
  }

  @Test
  fun `"add" inserts an element`() {
    val date = LocalDateTime.now()
    val queue = queue()

    assertSuccess("$STATUS_ELEMENT_ADDED: ${EmploymentRequest("joe", date)}") {
      AddCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
        "\"date\": \"$date\"}", queue)
    }
    assertEquals(EmploymentRequest("joe", date), queue.peek())
  }

  @Test
  fun `"add_if_max" inserts an element if it has the highest priority in the queue`() {
    val date = LocalDateTime.now()
    val queue = queue(
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("mary", date))

    assertNeutral(STATUS_UNCHANGED) {
      AddIfMaxCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
        "\"date\": \"$date\"}", queue)
    }
    assertEquals(2, queue.size)

    assertSuccess("$STATUS_ELEMENT_ADDED: ${EmploymentRequest(
      "joe", date.minusHours(2))}") {
      AddIfMaxCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
        "\"date\": \"${date.minusHours(2)}\"}", queue)
    }
    assertEquals(3, queue.size)
  }

  @Test
  fun `"add_if_min" inserts an element if it has the lowest priority in the queue`() {
    val date = LocalDateTime.now()
    val queue = queue(
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("mary", date.minusSeconds(1)))

    assertNeutral(STATUS_UNCHANGED) {
      AddIfMinCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
        "\"date\": \"${date.minusMinutes(1)}\"}", queue)
    }
    assertEquals(2, queue.size)

    assertSuccess("$STATUS_ELEMENT_ADDED: ${EmploymentRequest("joe", date)}") {
      AddIfMinCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
        "\"date\": \"$date\"}", queue)
    }
    assertEquals(3, queue.size)
  }

  @Test
  fun `"remove_lower" removes all lower-priority elements`() {
    val date = LocalDateTime.now()
    val queue = queue(
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("mary", date.minusHours(2)),
      EmploymentRequest("amy", date.plusHours(1),
        status = EmploymentRequest.Status.INTERVIEW_SCHEDULED),
      EmploymentRequest("steve", status = EmploymentRequest.Status.REJECTED),
      EmploymentRequest("jane", date)
    )

    assertSuccess(STATUS_ONE_REMOVED) {
      RemoveLowerCommand(jsonDeserializer).run(
        "{\"applicant\": \"-\", \"date\": \"$date\"}", queue)
    }
    assertQueueContentsEqual(queue,
      EmploymentRequest("amy", date.plusHours(1),
        status = EmploymentRequest.Status.INTERVIEW_SCHEDULED),
      EmploymentRequest("mary", date.minusHours(2)),
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("jane", date))
  }

  @Test
  fun `"remove_greater" removes all higher-priority elements`() {
    val date = LocalDateTime.now()
    val queue = queue(
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("amy", date.plusHours(1)),
      EmploymentRequest("bob", date.plusHours(1),
        status = EmploymentRequest.Status.INTERVIEW_SCHEDULED),
      EmploymentRequest("jane", date)
    )

    assertSuccess("2 $STATUS_MANY_REMOVED") {
      RemoveGreaterCommand(jsonDeserializer).run(
        "{\"applicant\": \"-\", \"date\": \"$date\"}", queue)
    }
    assertQueueContentsEqual(queue,
      EmploymentRequest("jane", date),
      EmploymentRequest("amy", date.plusHours(1)))
  }

  @Test
  fun `"remove_first" removes the head element`() {
    val date = LocalDateTime.now()
    val queue = queue(
      EmploymentRequest("mary", date.minusHours(2)),
      EmploymentRequest("jane", date),
      EmploymentRequest("joe", date.minusHours(1))
    )

    assertSuccess(STATUS_ONE_REMOVED) { RemoveFirstCommand().run("", queue) }
    assertQueueContentsEqual(queue,
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("jane", date)
    )

    assertNeutral(STATUS_UNCHANGED) { RemoveFirstCommand().run("", queue()) }
  }

  @Test
  fun `"remove_last" removes the tail element`() {
    val date = LocalDateTime.now()
    var queue = queue(
      EmploymentRequest("mary", date.minusHours(2)),
      EmploymentRequest("jane", date),
      EmploymentRequest("joe", date.minusHours(1))
    )

    assertSuccess(STATUS_ONE_REMOVED) { RemoveLastCommand().run("", queue) }
    assertQueueContentsEqual(queue,
      EmploymentRequest("mary", date.minusHours(2)),
      EmploymentRequest("joe", date.minusHours(1))
    )

    /* Now including status... */

    queue = queue(
      EmploymentRequest("bob", date.plusHours(1),
        status = EmploymentRequest.Status.INTERVIEW_SCHEDULED),
      EmploymentRequest("joe", date.minusHours(1),
        status = EmploymentRequest.Status.REJECTED),
      EmploymentRequest("jane", date)
    )

    assertSuccess(STATUS_ONE_REMOVED) { RemoveLastCommand().run("", queue) }
    assertQueueContentsEqual(queue,
      EmploymentRequest("bob", date.plusHours(1),
        status = EmploymentRequest.Status.INTERVIEW_SCHEDULED),
      EmploymentRequest("jane", date)
    )

    assertSuccess(STATUS_ONE_REMOVED) { RemoveLastCommand().run("", queue) }
    assertQueueContentsEqual(queue,
      EmploymentRequest("bob", date.plusHours(1),
        status = EmploymentRequest.Status.INTERVIEW_SCHEDULED)
    )
  }

  @Test
  fun `"remove" removes an element`() {
    val date = LocalDateTime.now()
    val element = EmploymentRequest("jane", date,
      status = EmploymentRequest.Status.REJECTED)
    val queue = queue(element)

    assertNeutral(STATUS_UNCHANGED) {
      RemoveCommand(jsonDeserializer).run("{\"applicant\": \"jane\"," +
        "\"date\": \"$date\"}", queue)
    }
    assertQueueContentsEqual(queue, element)

    assertSuccess(STATUS_ONE_REMOVED) {
      RemoveCommand(jsonDeserializer).run("{\"applicant\": \"jane\"," +
        "\"date\": \"$date\", \"status\": \"Rejected\"}", queue)
    }
    assertTrue(queue.isEmpty())
  }

  @Test
  fun `"remove_all" removes all equivalent elements`() {
    val date = LocalDateTime.now()
    val queue = queue(
      EmploymentRequest("joe", date),
      EmploymentRequest("joe", date),
      EmploymentRequest("jane", date))

    assertSuccess("2 $STATUS_MANY_REMOVED") {
      RemoveAllCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
        "\"date\": \"$date\"}", queue)
    }
    assertQueueContentsEqual(queue,
      EmploymentRequest("jane", date))
  }

  @Test
  fun `"info" prints basic collection info`() {
    val date = LocalDateTime.now()
    val queue = queue(
      EmploymentRequest("mary", date.minusHours(2)),
      EmploymentRequest("jane", date),
      EmploymentRequest("joe", date.minusHours(1))
    )

    assertNeutral(
      "=== Queue information\n" +
      "Type:\n" +
      "  java.util.PriorityQueue\n" +
      "Instantiated on:\n" +
      "  $date\n" +
      "Elements:\n" +
      "  1. ${EmploymentRequest("mary", date.minusHours(2))}\n" +
      "  2. ${EmploymentRequest("joe", date.minusHours(1))}\n" +
      "  3. ${EmploymentRequest("jane", date)}\n" +
      "===") {
      InfoCommand(date).run("", queue)
    }
    assertEquals(3, queue.size)
  }

  @Test
  fun `"load" reloads the collection from a file`() {
    val date = LocalDateTime.now()
    val storage =  PriorityQueueStorage(
      EmploymentRequest::class.java,
      File.createTempFile("commandstest", "txt"),
      QUEUE_COMPARATOR)
    val queue = queue()
    val stored = arrayOf(
      EmploymentRequest("mary", date.minusHours(2)),
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("jane", date)
    )
    storage.write(PriorityQueue(listOf(*stored)))

    assertSuccess(STATUS_LOADED) {
      LoadCommand(storage).run("", queue)
    }
    assertQueueContentsEqual(queue, *stored)
  }

  @Test
  fun `"save" stores the collection in a file`() {
    val date = LocalDateTime.now()
    val storage =  PriorityQueueStorage(
      EmploymentRequest::class.java,
      File.createTempFile("commandstest", "txt"),
      QUEUE_COMPARATOR)
    val queue = queue(
      EmploymentRequest("mary", date.minusHours(2)),
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("jane", date)
    )

    assertSuccess(STATUS_SAVED) {
      SaveCommand(storage).run("", queue)
    }
    assertArrayEquals(queue.toArray(), storage.read().toArray())
  }

  @Test
  fun `"import" appends file contents to the queue`() {
    val date = LocalDateTime.now()
    val queue = queue(
      EmploymentRequest("mary", date.minusHours(2))
    )
    val temp = File.createTempFile("commandstest", "txt")
    PriorityQueueStorage(
      EmploymentRequest::class.java, temp, QUEUE_COMPARATOR).write(queue(
      EmploymentRequest("amy", date)))

    assertSuccess(STATUS_IMPORTED) {
      ImportCommand().run(temp.absolutePath, queue)
    }
    assertQueueContentsEqual(queue,
      EmploymentRequest("mary", date.minusHours(2)),
      EmploymentRequest("amy", date))
  }

  private inline fun assertSuccess(message: String, command: () -> CommandStatus) =
    assertEquals(SuccessStatus(message), command())

  private inline fun assertNeutral(message: String, command: () -> CommandStatus) =
    assertEquals(NeutralStatus(message), command())

  private fun queue(vararg elements: EmploymentRequest) =
    PriorityQueue<EmploymentRequest>(QUEUE_COMPARATOR).apply { addAll(listOf(*elements)) }

  private inline fun<reified E> assertQueueContentsEqual(queue: PriorityQueue<E>, vararg expected: E) {
    /* PriorityQueue#toArray() returns an _unsorted_ array, hence using #poll()
     * to retrieve elements in order. */
    val actual = mutableListOf<E>().apply {
      val source = PriorityQueue<E>(queue)
      while (source.isNotEmpty()) add(source.poll())
    }.toTypedArray()

    assertArrayEquals(expected, actual)
  }
}
