package com.gestioncaravana.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CaravanWeatherControllerTest {

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
            .content(objectMapper.writeValueAsString(Map.of("name", "Weather"))))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    caravanId = objectMapper.readTree(response).path("id").asText();
  }

  @Test
  void getsAndUpdatesWeatherProfile() throws Exception {
    mockMvc.perform(get("/api/caravans/{caravanId}/weather/profile", caravanId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.climateBaseline").value("TEMPERATE"))
        .andExpect(jsonPath("$.elevation").value("SEA_LEVEL"))
        .andExpect(jsonPath("$.crownOfWorld").value(false));

    mockMvc.perform(put("/api/caravans/{caravanId}/weather/profile", caravanId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "climateBaseline", "COLD",
                "elevation", "PEAK",
                "crownOfWorld", true,
                "effectiveFromYear", 4712,
                "effectiveFromMonth", 1,
                "effectiveFromDay", 10))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.climateBaseline").value("COLD"))
        .andExpect(jsonPath("$.elevation").value("PEAK"))
        .andExpect(jsonPath("$.crownOfWorld").value(true));
  }
}
