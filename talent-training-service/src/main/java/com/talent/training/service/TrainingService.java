package com.talent.training.service;

import com.talent.common.result.PageResult;
import com.talent.training.dto.CourseSaveDTO;
import com.talent.training.dto.LearningPathSaveDTO;
import com.talent.training.dto.TrainingRecordSaveDTO;
import com.talent.training.vo.CourseItemVO;
import com.talent.training.vo.LearningPathVO;
import com.talent.training.vo.TrainingEffectVO;
import com.talent.training.vo.TrainingPlanVO;

import java.util.List;

public interface TrainingService {
    TrainingPlanVO plan(Long employeeId);
    List<CourseItemVO> completed(Long employeeId);
    PageResult<CourseItemVO> coursePage(long pageNum, long pageSize, String keyword, String courseType);
    Long createCourse(CourseSaveDTO dto);
    void updateCourse(Long id, CourseSaveDTO dto);
    void deleteCourse(Long id);
    List<LearningPathVO> learningPaths();
    LearningPathVO learningPathDetail(Long id);
    Long createLearningPath(LearningPathSaveDTO dto);
    void updateLearningPath(Long id, LearningPathSaveDTO dto);
    void deleteLearningPath(Long id);
    Long createRecord(TrainingRecordSaveDTO dto);
    void deleteRecord(Long id);
    List<TrainingEffectVO> effects();
}
