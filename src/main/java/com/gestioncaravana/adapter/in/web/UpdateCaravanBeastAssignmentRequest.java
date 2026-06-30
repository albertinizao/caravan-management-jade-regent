package com.gestioncaravana.adapter.in.web;

import com.gestioncaravana.domain.CaravanBeastAssignmentType;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record UpdateCaravanBeastAssignmentRequest(
    @NotNull CaravanBeastAssignmentType assignmentType,
    UUID wagonId) {}
