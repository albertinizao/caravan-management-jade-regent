package com.gestioncaravana.adapter.out.persistence;

import com.gestioncaravana.application.port.out.CaravanCalendarEventRepositoryPort;
import com.gestioncaravana.domain.CustomCalendarEvent;
import com.gestioncaravana.domain.GolarionDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CaravanCalendarEventRepositoryAdapter implements CaravanCalendarEventRepositoryPort {

  private final SpringDataCaravanCalendarEventRepository repository;

  public CaravanCalendarEventRepositoryAdapter(SpringDataCaravanCalendarEventRepository repository) {
    this.repository = repository;
  }

  @Override
  public CustomCalendarEvent save(CustomCalendarEvent event) {
    return toDomain(repository.save(toEntity(event)));
  }

  @Override
  public List<CustomCalendarEvent> findByCaravanIdAndDate(UUID caravanId, GolarionDate date) {
    return repository.findByCaravanIdAndDateKeyOrderByCreatedAtAsc(caravanId.toString(), formatDateKey(date)).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<CustomCalendarEvent> findByCaravanIdAndDateBetween(
      UUID caravanId, GolarionDate startDate, GolarionDate endDate) {
    return repository
        .findByCaravanIdAndDateKeyBetweenOrderByDateKeyAscCreatedAtAsc(
            caravanId.toString(), formatDateKey(startDate), formatDateKey(endDate))
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public Optional<CustomCalendarEvent> findByCaravanIdAndId(UUID caravanId, Long eventId) {
    return repository.findByCaravanIdAndId(caravanId.toString(), eventId).map(this::toDomain);
  }

  @Override
  public void deleteByCaravanIdAndId(UUID caravanId, Long eventId) {
    repository.deleteByCaravanIdAndId(caravanId.toString(), eventId);
  }

  @Override
  public void deleteByCaravanId(UUID caravanId) {
    repository.deleteByCaravanId(caravanId.toString());
  }

  private CaravanCalendarEventJpaEntity toEntity(CustomCalendarEvent event) {
    var entity = new CaravanCalendarEventJpaEntity();
    entity.setId(event.id());
    entity.setCaravanId(event.caravanId().toString());
    entity.setDateKey(formatDateKey(event.date()));
    entity.setName(event.name());
    entity.setDescription(event.description());
    entity.setSecret(event.secret());
    entity.setCategory("CUSTOM");
    entity.setCreatedAt(event.createdAt());
    return entity;
  }

  private CustomCalendarEvent toDomain(CaravanCalendarEventJpaEntity entity) {
    return new CustomCalendarEvent(
        entity.getId(),
        UUID.fromString(entity.getCaravanId()),
        parseDateKey(entity.getDateKey()),
        entity.getName(),
        entity.getDescription(),
        entity.isSecret(),
        entity.getCreatedAt());
  }

  private String formatDateKey(GolarionDate date) {
    return "%04d-%02d-%02d".formatted(date.year(), date.month(), date.day());
  }

  private GolarionDate parseDateKey(String dateKey) {
    var parts = dateKey.split("-");
    return new GolarionDate(
        Integer.parseInt(parts[0]),
        Integer.parseInt(parts[1]),
        Integer.parseInt(parts[2]));
  }
}
