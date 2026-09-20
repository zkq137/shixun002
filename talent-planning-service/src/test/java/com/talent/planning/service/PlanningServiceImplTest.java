package com.talent.planning.service;

import com.talent.common.exception.BusinessException;
import com.talent.planning.dto.RiskHandleDTO;
import com.talent.planning.entity.Position;
import com.talent.planning.entity.ResignWarningRecord;
import com.talent.planning.mapper.PlanningQueryMapper;
import com.talent.planning.mapper.PositionMapper;
import com.talent.planning.mapper.PositionRiskMapper;
import com.talent.planning.mapper.ResignWarningMapper;
import com.talent.planning.mapper.SuccessionCandidateMapper;
import com.talent.planning.mapper.TalentPoolMapper;
import com.talent.planning.service.impl.PlanningServiceImpl;
import com.talent.planning.vo.CandidateSourceVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningServiceImplTest {
    @Mock private PositionMapper positionMapper;
    @Mock private SuccessionCandidateMapper candidateMapper;
    @Mock private TalentPoolMapper talentPoolMapper;
    @Mock private PositionRiskMapper positionRiskMapper;
    @Mock private ResignWarningMapper resignWarningMapper;
    @Mock private PlanningQueryMapper queryMapper;
    private PlanningServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PlanningServiceImpl(positionMapper, candidateMapper, talentPoolMapper,
                positionRiskMapper, resignWarningMapper, queryMapper, new SuccessionScoreCalculator());
    }

    @Test
    void rejectsInvalidCandidateLimitBeforeQueryingCandidates() {
        when(positionMapper.selectById(1L)).thenReturn(position());

        BusinessException error = assertThrows(BusinessException.class, () -> service.succession(1L, 101));

        assertEquals(400, error.getCode());
        verify(queryMapper, never()).candidatesByPositionName(any());
    }

    @Test
    void returnsNotFoundForMissingPosition() {
        when(positionMapper.selectById(99L)).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class, () -> service.positionDetail(99L));

        assertEquals(404, error.getCode());
    }

    @Test
    void refreshReplacesSnapshotAndStoresScoreBreakdown() {
        when(positionMapper.selectById(1L)).thenReturn(position());
        when(queryMapper.columnExists("emp_employee", "position_id")).thenReturn(0);
        CandidateSourceVO source = new CandidateSourceVO();
        source.setEmployeeId(7L);
        source.setEmployeeName("候选人");
        source.setRequiredSkillCount(1);
        source.setMatchedSkillCount(1);
        source.setPerformanceScore(new BigDecimal("80"));
        source.setPotentialScore(new BigDecimal("80"));
        source.setWorkYears(5);
        when(queryMapper.candidatesByPositionName(1L)).thenReturn(List.of(source));
        when(queryMapper.missingSkills(1L, 7L)).thenReturn(List.of());

        var result = service.refreshSuccession(1L);

        assertEquals(1, result.size());
        verify(candidateMapper).delete(any());
        ArgumentCaptor<com.talent.planning.entity.SuccessionCandidate> captor =
                ArgumentCaptor.forClass(com.talent.planning.entity.SuccessionCandidate.class);
        verify(candidateMapper).insert(captor.capture());
        assertEquals(new BigDecimal("87.00"), captor.getValue().getMatchScore());
        assertEquals("READY_NOW", captor.getValue().getReadiness());
        assertNotNull(captor.getValue().getCalculatedAt());
    }

    @Test
    void recordsWarningHandlerAndTime() {
        ResignWarningRecord record = new ResignWarningRecord();
        record.setId(3L);
        when(resignWarningMapper.selectById(3L)).thenReturn(record);
        RiskHandleDTO dto = new RiskHandleDTO();
        dto.setHandleStatus("已处理");
        dto.setHandler(" 主管 ");
        dto.setRemark(" 已沟通 ");

        service.handleEmployeeRisk(3L, dto);

        assertEquals("已处理", record.getHandleStatus());
        assertEquals("主管", record.getHandler());
        assertEquals("已沟通", record.getHandleRemark());
        assertNotNull(record.getHandledAt());
        verify(resignWarningMapper).updateById(record);
    }

    private Position position() {
        Position position = new Position();
        position.setId(1L);
        position.setPositionName("技术经理");
        position.setIsKey(1);
        return position;
    }
}
