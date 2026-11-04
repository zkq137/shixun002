package com.talent.training.service;

import com.talent.common.result.PageResult;
import com.talent.training.dto.CourseSaveDTO;
import com.talent.training.dto.CourseCategorySaveDTO;
import com.talent.training.dto.LearningPathSaveDTO;
import com.talent.training.dto.TrainingRecordSaveDTO;
import com.talent.training.vo.CourseItemVO;
import com.talent.training.vo.CourseCategoryVO;
import com.talent.training.vo.CourseImportResultVO;
import com.talent.training.vo.LearningPathVO;
import com.talent.training.vo.TrainingEffectVO;
import com.talent.training.vo.TrainingPlanVO;

import java.util.List;

public interface TrainingService {
    TrainingPlanVO plan(Long employeeId);
    TrainingPlanVO plan(Long employeeId, String developmentGoal);
    int importPaths(List<LearningPathSaveDTO> templates);
    List<CourseItemVO> completed(Long employeeId);
    PageResult<CourseItemVO> coursePage(long pageNum, long pageSize, String keyword, String courseType, Long categoryId);
    default PageResult<CourseItemVO> coursePage(long pageNum, long pageSize, String keyword, String courseType) {
        return coursePage(pageNum, pageSize, keyword, courseType, null);
    }
    List<CourseCategoryVO> categories();
    Long createCategory(CourseCategorySaveDTO dto);
    void updateCategory(Long id, CourseCategorySaveDTO dto);
    void deleteCategory(Long id);
    CourseImportResultVO importCourses(String csv);
    String exportCourses(String keyword, String courseType, Long categoryId);
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
