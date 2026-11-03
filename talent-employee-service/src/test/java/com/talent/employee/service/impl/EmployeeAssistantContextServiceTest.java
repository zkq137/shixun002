package com.talent.employee.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.common.result.PageResult;
import com.talent.employee.dto.EmployeeQuery;
import com.talent.employee.service.EmployeeService;
import com.talent.employee.service.PerformanceService;
import com.talent.employee.service.PotentialService;
import com.talent.employee.service.SkillService;
import com.talent.employee.vo.EmployeeDetailVO;
import com.talent.employee.vo.EmployeeReferenceVO;
import com.talent.employee.vo.EmployeeVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeAssistantContextServiceTest {

    @Test
    void buildsEmployeeProfileContextWithoutLocalHttpCall() {
        EmployeeService employeeService = mock(EmployeeService.class);
        PerformanceService performanceService = mock(PerformanceService.class);
        PotentialService potentialService = mock(PotentialService.class);
        SkillService skillService = mock(SkillService.class);

        EmployeeReferenceVO reference = new EmployeeReferenceVO();
        reference.setId(1L);
        reference.setEmpNo("E70014679");
        reference.setName("龙秀英");
        reference.setDepartment("行政中心");

        EmployeeDetailVO detail = new EmployeeDetailVO();
        detail.setId(1L);
        detail.setEmpNo("E70014679");
        detail.setName("龙秀英");
        detail.setDepartment("行政中心");

        EmployeeVO row = new EmployeeVO();
        row.setId(1L);
        row.setName("龙秀英");

        when(employeeService.referencedEmployees(any())).thenReturn(List.of(reference));
        when(employeeService.detail(1L)).thenReturn(detail);
        when(employeeService.departments()).thenReturn(List.of("行政中心"));
        when(employeeService.positionOptions()).thenReturn(List.of());
        when(employeeService.pageQuery(any(EmployeeQuery.class)))
                .thenReturn(new PageResult<>(1, 1, 20, List.of(row)));
        when(employeeService.count()).thenReturn(1L);
        when(performanceService.history(1L)).thenReturn(List.of());
        when(performanceService.trend(1L)).thenReturn(null);
        when(potentialService.get(1L)).thenReturn(null);

        EmployeeAssistantContextService service = new EmployeeAssistantContextService(
                employeeService, performanceService, potentialService, skillService, new ObjectMapper());
        String context = service.buildContext("查询龙秀英的绩效趋势");

        assertTrue(context.contains("\"employeeProfiles\""));
        assertTrue(context.contains("\"empNo\":\"E70014679\""));
        assertTrue(context.contains("\"readOnly\":true"));
    }
}