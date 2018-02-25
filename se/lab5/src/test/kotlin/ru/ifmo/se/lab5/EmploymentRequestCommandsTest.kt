package ru.ifmo.se.lab5

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

import ru.ifmo.se.lab5.EmploymentRequestCommands.*

class EmploymentRequestCommandsTest {
  private val jsonDeserializer = Deserializer(EmploymentRequest::class.java)

  @Test
  fun `"clear" removes all elements`() {
    val queue = queue(EmploymentRequest("name", LocalDateTime.now()))
    assertTrue(queue.isNotEmpty())

    ClearCommand().run("", queue)
    assertTrue(queue.isEmpty())
  }

  @Test
  fun `"add" inserts an element`() {
    val date = LocalDateTime.now()

    val queue = queue()
    AddCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
      "\"date\": \"$date\"}", queue)

    assertEquals(EmploymentRequest("joe", date), queue.peek())
  }

  @Test
  fun `"add_if_max" inserts an element if it has the highest priority in the queue`() {
    val date = LocalDateTime.now()

    val queue = queue(
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("mary", date))

    AddIfMaxCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
      "\"date\": \"$date\"}", queue)
    assertEquals(2, queue.size)

    AddIfMaxCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
      "\"date\": \"${date.minusHours(2)}\"}", queue)
    assertEquals(3, queue.size)
  }

  @Test
  fun `"add_if_min" inserts an element if it has the lowest priority in the queue`() {
    val date = LocalDateTime.now()

    val queue = queue(
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("mary", date.minusSeconds(1)))

    AddIfMinCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
      "\"date\": \"${date.minusMinutes(1)}\"}", queue)
    assertEquals(2, queue.size)

    AddIfMinCommand(jsonDeserializer).run("{\"applicant\": \"joe\"," +
      "\"date\": \"$date\"}", queue)
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

    RemoveLowerCommand(jsonDeserializer).run(
      "{\"applicant\": \"-\", \"date\": \"$date\"}", queue)

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
      EmploymentRequest("jane", date)
    )

    RemoveGreaterCommand(jsonDeserializer).run(
      "{\"applicant\": \"-\", \"date\": \"$date\"}", queue)

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

    RemoveFirstCommand().run("", queue)

    assertQueueContentsEqual(queue,
      EmploymentRequest("joe", date.minusHours(1)),
      EmploymentRequest("jane", date)
    )
  }

  @Test
  fun `"remove_last" removes the tail element`() {
    val date = LocalDateTime.now()

    var queue = queue(
      EmploymentRequest("mary", date.minusHours(2)),
      EmploymentRequest("jane", date),
      EmploymentRequest("joe", date.minusHours(1))
    )

    RemoveLastCommand().run("", queue)

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

    RemoveLastCommand().run("", queue)

    assertQueueContentsEqual(queue,
      EmploymentRequest("bob", date.plusHours(1),
        status = EmploymentRequest.Status.INTERVIEW_SCHEDULED),
      EmploymentRequest("jane", date)
    )

    RemoveLastCommand().run("", queue)

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

    RemoveCommand(jsonDeserializer).run("{\"applicant\": \"jane\"," +
      "\"date\": \"$date\"}", queue)
    assertQueueContentsEqual(queue, element)

    RemoveCommand(jsonDeserializer).run("{\"applicant\": \"jane\"," +
      "\"date\": \"$date\", \"status\": \"Rejected\"}", queue)
    assertTrue(queue.isEmpty())
  }

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
