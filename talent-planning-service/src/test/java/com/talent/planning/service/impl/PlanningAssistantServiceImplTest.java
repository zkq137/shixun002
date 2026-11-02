package com.talent.planning.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.planning.service.PlanningService;
import com.talent.planning.vo.PlanningDashboardVO;
import com.talent.planning.vo.PositionVO;
import com.talent.planning.vo.SuccessionCandidateVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanningAssistantServiceImplTest {

    @Test
    void buildsTargetedContextWithoutEmployeeIdentity() {
        PlanningService planningService = mock(PlanningService.class);
        PositionVO position = new PositionVO();
        position.setId(12L);
        position.setPositionName("产品经理");
        position.setPositionLevel("中层");
        position.setKey(true);

        SuccessionCandidateVO candidate = new SuccessionCandidateVO();
        candidate.setEmployeeId(99L);
        candidate.setEmpNo("EMP0099");
        candidate.setEmployeeName("测试姓名");
        candidate.setDepartment("产品部");
        candidate.setCurrentPosition("产品专员");
        candidate.setMatchScore(new BigDecimal("82.50"));
        candidate.setReadiness("READY_NOW");

        when(planningService.positions(null, null)).thenReturn(List.of(position));
        when(planningService.dashboard()).thenReturn(new PlanningDashboardVO());
        when(planningService.talentPools()).thenReturn(List.of());
        when(planningService.positionRisks()).thenReturn(List.of());
        when(planningService.positionDetail(12L)).thenReturn(position);
        when(planningService.skillCoverage(12L)).thenReturn(List.of());
        when(planningService.succession(12L, 20)).thenReturn(List.of(candidate));

        PlanningAssistantServiceImpl service = new PlanningAssistantServiceImpl(
                planningService, new ObjectMapper().findAndRegisterModules());
        String context = service.buildContext("分析产品经理岗位的继任情况");

        assertThat(context)
                .contains("产品经理", "候选人1", "82.50", "READY_NOW")
                .doesNotContain("测试姓名", "EMP0099", "employeeId");
    }
}
