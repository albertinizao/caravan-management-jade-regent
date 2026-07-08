package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBeastView;
import com.gestioncaravana.domain.CaravanBeastSourceType;
import java.math.BigDecimal;
import java.util.UUID;

public interface AddCaravanBeastUseCase {

  CaravanBeastView execute(UUID caravanId, AddCaravanBeastCommand command);

  record AddCaravanBeastCommand(
      CaravanBeastSourceType sourceType,
      String catalogBeastCode,
      String name,
      String size,
      Integer strength,
      Integer speed,
      Integer thermalAdaptation,
      Integer basePrice,
      Integer trainedPrice,
      Boolean fourLegged,
      String specialNote,
      String description,
      String customNotes,
      Integer consumption,
      BigDecimal occupiedSpace,
      Integer quantity) {
    public AddCaravanBeastCommand(
        CaravanBeastSourceType sourceType,
        String catalogBeastCode,
        String name,
        String size,
        Integer strength,
        Integer speed,
        Integer thermalAdaptation,
        Integer basePrice,
        Integer trainedPrice,
        Boolean fourLegged,
        String specialNote,
        String description,
        String customNotes) {
      this(
          sourceType,
          catalogBeastCode,
          name,
          size,
          strength,
          speed,
          thermalAdaptation,
          basePrice,
          trainedPrice,
          fourLegged,
          specialNote,
          description,
          customNotes,
          null,
          null,
          1);
    }

    public AddCaravanBeastCommand(
        CaravanBeastSourceType sourceType,
        String catalogBeastCode,
        String name,
        String size,
        Integer strength,
        Integer speed,
        Integer thermalAdaptation,
        Integer basePrice,
        Integer trainedPrice,
        Boolean fourLegged,
        String specialNote,
        String description,
        String customNotes,
        Integer consumption,
        Integer quantity) {
      this(
          sourceType,
          catalogBeastCode,
          name,
          size,
          strength,
          speed,
          thermalAdaptation,
          basePrice,
          trainedPrice,
          fourLegged,
          specialNote,
          description,
          customNotes,
          consumption,
          null,
          quantity);
    }

    public AddCaravanBeastCommand(
        CaravanBeastSourceType sourceType,
        String catalogBeastCode,
        String name,
        String size,
        Integer strength,
        Integer speed,
        Integer thermalAdaptation,
      Integer basePrice,
      Integer trainedPrice,
      Boolean fourLegged,
      String specialNote,
      String description,
      String customNotes,
      Integer consumption,
      BigDecimal occupiedSpace) {
      this(
          sourceType,
          catalogBeastCode,
          name,
          size,
          strength,
          speed,
          thermalAdaptation,
          basePrice,
          trainedPrice,
          fourLegged,
          specialNote,
          description,
          customNotes,
          consumption,
          occupiedSpace,
          1);
    }
  }
}
