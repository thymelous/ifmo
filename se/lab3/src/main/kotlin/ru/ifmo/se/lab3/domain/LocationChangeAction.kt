package ru.ifmo.se.lab3.domain

import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@Entity
class LocationChangeAction(
  @JsonSerialize(using = Person.ToNameStringSerializer::class)
  actor: Person,

  date: LocalDateTime,
  
  val newLocation: String,
  
  @Enumerated(EnumType.STRING)
  val means: TransportationMeans) : Action(actor, date) {

  /**
   * A DTO representing a new LocationChangeAction record.
   *
   * Actor and date fields are omitted to prevent client-side manipulation;
   * they are filled by [ActionService], which converts the DTO to an entity bean.
   */
  data class Dto(
    @NotBlank
    val newLocation: String,

    @NotNull
    val date: LocalDateTime,
    
    @NotNull
    @Enumerated(EnumType.STRING)
    val means: TransportationMeans)
}
