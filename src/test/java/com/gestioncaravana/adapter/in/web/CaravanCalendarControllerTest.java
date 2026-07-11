package com.gestioncaravana.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CaravanCalendarControllerTest {

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private WebApplicationContext webApplicationContext;

  private String caravanId;

  @BeforeEach
  void createCaravan() throws Exception {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    var response = mockMvc.perform(post("/api/caravans")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("name", "Calendario"))))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    caravanId = objectMapper.readTree(response).path("id").asText();
  }

  @Test
  void returnsCalendarMonthAndDayDetails() throws Exception {
    mockMvc.perform(get("/api/caravans/{caravanId}/calendar", caravanId)
            .param("year", "4712")
            .param("month", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentDate.year").value(4712))
        .andExpect(jsonPath("$.currentDate.month").value(1))
        .andExpect(jsonPath("$.days.length()").value(42));

    mockMvc.perform(get("/api/caravans/{caravanId}/calendar/day", caravanId)
            .param("year", "4712")
            .param("month", "1")
            .param("day", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date.year").value(4712))
        .andExpect(jsonPath("$.date.month").value(1))
        .andExpect(jsonPath("$.date.day").value(1));
  }

  @Test
  void hidesFutureWeatherUnlessSecretsAreVisible() throws Exception {
    mockMvc.perform(get("/api/caravans/{caravanId}/calendar/day", caravanId)
            .param("year", "4712")
            .param("month", "1")
            .param("day", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weather").value(org.hamcrest.Matchers.nullValue()));

    mockMvc.perform(get("/api/caravans/{caravanId}/calendar/day", caravanId)
            .param("year", "4712")
            .param("month", "1")
            .param("day", "2")
            .param("showSecrets", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weather").exists())
        .andExpect(jsonPath("$.weather.midnightToDawn").exists());
  }

  @Test
  void exposesPolarLightConditionWhenCrownOfWorldIsActive() throws Exception {
    mockMvc.perform(put("/api/caravans/{caravanId}/weather/profile", caravanId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "climateBaseline", "COLD",
                "elevation", "HIGHLAND",
                "crownOfWorld", true,
                "effectiveFromYear", 4712,
                "effectiveFromMonth", 1,
                "effectiveFromDay", 1))))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/caravans/{caravanId}/calendar/day", caravanId)
            .param("year", "4712")
            .param("month", "1")
            .param("day", "12")
            .param("showSecrets", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weather.crownLightCondition").value("POLAR_TWILIGHT"));
  }

  @Test
  void updatesCurrentDateAndAdvancesDays() throws Exception {
    mockMvc.perform(put("/api/caravans/{caravanId}/calendar/current-date", caravanId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("year", 4712, "month", 3, "day", 20))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date.year").value(4712))
        .andExpect(jsonPath("$.date.month").value(3))
        .andExpect(jsonPath("$.date.day").value(20));

    mockMvc.perform(post("/api/caravans/{caravanId}/calendar/advance", caravanId)
            .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("days", 5))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date.year").value(4712))
        .andExpect(jsonPath("$.date.month").value(3))
        .andExpect(jsonPath("$.date.day").value(25));
  }

  @Test
  void createsCustomCalendarEvents() throws Exception {
    var response = mockMvc.perform(post("/api/caravans/{caravanId}/calendar/events", caravanId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "year", 4712,
                "month", 3,
                "day", 20,
                "name", "Consejo reservado",
                "description", "Solo para miembros",
                "secret", true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customEvents.length()").value(1))
        .andExpect(jsonPath("$.customEvents[0].name").value("Consejo reservado"))
        .andExpect(jsonPath("$.customEvents[0].secret").value(true))
        .andExpect(jsonPath("$.customEvents[0].id").isNumber())
        .andReturn();

    var createdId = objectMapper.readTree(response.getResponse().getContentAsString()).path("customEvents").get(0).path("id").asLong();

    mockMvc.perform(delete("/api/caravans/{caravanId}/calendar/events/{eventId}", caravanId, createdId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customEvents.length()").value(0));
  }

  @Test
  void rejectsDatesOutsideSupportedRange() throws Exception {
    mockMvc.perform(put("/api/caravans/{caravanId}/calendar/current-date", caravanId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("year", 4723, "month", 1, "day", 1))))
        .andExpect(status().isBadRequest());
  }
}
