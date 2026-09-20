package com.talent.planning.controller;

import com.talent.common.exception.BusinessException;
import com.talent.common.exception.GlobalExceptionHandler;
import com.talent.planning.feign.EmployeeClient;
import com.talent.planning.service.PlanningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanningController.class)
@Import(GlobalExceptionHandler.class)
class PlanningControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PlanningService planningService;
    @MockBean private EmployeeClient employeeClient;

    @Test
    void mapsBusinessValidationErrorToUnifiedResponse() throws Exception {
        when(planningService.succession(1L, 101)).thenThrow(new BusinessException(400, "limit 参数不合法"));

        mockMvc.perform(get("/api/planning/succession").param("positionId", "1").param("limit", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("limit 参数不合法"));
    }
}
