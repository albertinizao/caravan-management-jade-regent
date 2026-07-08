package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanBeastView;
import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import java.util.List;
import java.util.UUID;

public interface UpdateCaravanBeastAssignmentUseCase {

  CaravanBeastView execute(UUID caravanId, UUID beastId, UpdateCaravanBeastAssignmentCommand command);

  record UpdateCaravanBeastAssignmentCommand(
      CaravanBeastAssignmentType assignmentType,
      UUID wagonId,
      List<String> availableRoleCodes,
      String activeRoleCode) {
    public UpdateCaravanBeastAssignmentCommand(
        CaravanBeastAssignmentType assignmentType,
        UUID wagonId) {
      this(assignmentType, wagonId, null, null);
    }
  }
}
