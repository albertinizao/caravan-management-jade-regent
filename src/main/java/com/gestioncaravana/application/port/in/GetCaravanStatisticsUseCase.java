package com.gestioncaravana.application.port.in;

import com.gestioncaravana.application.model.CaravanStatisticsView;
import java.util.UUID;

public interface GetCaravanStatisticsUseCase {

  CaravanStatisticsView getById(UUID caravanId);
}
