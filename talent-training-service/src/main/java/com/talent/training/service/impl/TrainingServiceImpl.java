package com.talent.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.PageResult;
import com.talent.common.result.ResultCode;
import com.talent.training.dto.CourseSaveDTO;
import com.talent.training.dto.LearningPathSaveDTO;
import com.talent.training.dto.TrainingRecordSaveDTO;
import com.talent.training.entity.Course;
import com.talent.training.entity.LearningPath;
import com.talent.training.entity.PathTask;
import com.talent.training.entity.TrainingRecord;
import com.talent.training.mapper.CourseMapper;
import com.talent.training.mapper.LearningPathMapper;
import com.talent.training.mapper.PathTaskMapper;
import com.talent.training.mapper.TrainingQueryMapper;
import com.talent.training.mapper.TrainingRecordMapper;
import com.talent.training.service.TrainingService;
import com.talent.training.vo.CourseItemVO;
import com.talent.training.vo.EmployeeTrainingProfileVO;
import com.talent.training.vo.LearningPathVO;
import com.talent.training.vo.TrainingEffectVO;
import com.talent.training.vo.TrainingPlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {
    private final CourseMapper courseMapper;
    private final LearningPathMapper learningPathMapper;
    private final PathTaskMapper pathTaskMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final TrainingQueryMapper trainingQueryMapper;

    @Override
    public TrainingPlanVO plan(Long employeeId) {
        EmployeeTrainingProfileVO profile = requireEmployee(employeeId);
        List<CourseItemVO> completed = trainingQueryMapper.completedCourses(employeeId);
        List<CourseItemVO> recommended = trainingQueryMapper.requiredButIncompleteCourses(employeeId);
        List<String> gaps = trainingQueryMapper.skillGaps(employeeId);

        TrainingPlanVO plan = new TrainingPlanVO();
        plan.setEmployeeId(employeeId);
        plan.setEmployeeName(profile.getEmployeeName());
        plan.setPositionName(profile.getPositionName());
        plan.setCompletedCourses(completed);
        plan.setRecommendedCourses(recommended);
        plan.setSkillGaps(gaps);
        plan.setRecommendationReason("基于岗位必修课程、已完成培训和岗位技能缺口生成");
        return plan;
    }

    @Override
    public List<CourseItemVO> completed(Long employeeId) {
        requireEmployee(employeeId);
        return trainingQueryMapper.completedCourses(employeeId);
    }

    @Override
    public PageResult<CourseItemVO> coursePage(long pageNum, long pageSize, String keyword, String courseType) {
        validatePage(pageNum, pageSize);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .like(StringUtils.hasText(keyword), Course::getCourseName, keyword)
                .eq(StringUtils.hasText(courseType), Course::getCourseType, courseType)
                .orderByDesc(Course::getId);
        List<Course> allCourses = courseMapper.selectList(wrapper);
        int fromIndex = (int) Math.min((pageNum - 1) * pageSize, allCourses.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, allCourses.size());
        List<CourseItemVO> records = allCourses.subList(fromIndex, toIndex).stream().map(this::toCourseItem).toList();
        return new PageResult<>(allCourses.size(), pageNum, pageSize, records);
    }

    @Override
    public Long createCourse(CourseSaveDTO dto) {
        Course course = toCourse(dto);
        courseMapper.insert(course);
        return course.getId();
    }

    @Override
    public void updateCourse(Long id, CourseSaveDTO dto) {
        Course course = requireCourse(id);
        copyCourse(dto, course);
        courseMapper.updateById(course);
    }

    @Override
    public void deleteCourse(Long id) {
        requireCourse(id);
        if (trainingQueryMapper.courseReferenceCount(id) > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "课程已被培训记录、学习路径或岗位要求使用，不能删除");
        }
        courseMapper.deleteById(id);
    }

    @Override
    public List<LearningPathVO> learningPaths() {
        return learningPathMapper.selectList(new LambdaQueryWrapper<LearningPath>()
                        .orderByDesc(LearningPath::getId))
                .stream().map(path -> toLearningPath(path, false)).toList();
    }

    @Override
    public LearningPathVO learningPathDetail(Long id) {
        return toLearningPath(requirePath(id), true);
    }

    @Override
    @Transactional
    public Long createLearningPath(LearningPathSaveDTO dto) {
        validatePath(dto);
        LearningPath path = new LearningPath();
        path.setPathName(dto.getPathName().trim());
        path.setTargetSkill(trimToNull(dto.getTargetSkill()));
        learningPathMapper.insert(path);
        saveTasks(path.getId(), dto.getTasks());
        return path.getId();
    }

    @Override
    @Transactional
    public void updateLearningPath(Long id, LearningPathSaveDTO dto) {
        validatePath(dto);
        LearningPath path = requirePath(id);
        path.setPathName(dto.getPathName().trim());
        path.setTargetSkill(trimToNull(dto.getTargetSkill()));
        learningPathMapper.updateById(path);
        pathTaskMapper.delete(new LambdaQueryWrapper<PathTask>().eq(PathTask::getPathId, id));
        saveTasks(id, dto.getTasks());
    }

    @Override
    @Transactional
    public void deleteLearningPath(Long id) {
        requirePath(id);
        pathTaskMapper.delete(new LambdaQueryWrapper<PathTask>().eq(PathTask::getPathId, id));
        learningPathMapper.deleteById(id);
    }

    @Override
    public Long createRecord(TrainingRecordSaveDTO dto) {
        if (dto == null || dto.getEmployeeId() == null || dto.getCourseId() == null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "员工和课程不能为空");
        }
        requireEmployee(dto.getEmployeeId());
        requireCourse(dto.getCourseId());
        validateRate(dto.getAttendRate(), "培训参与率");
        validateRate(dto.getPassRate(), "考核通过率");
        TrainingRecord record = new TrainingRecord();
        record.setEmployeeId(dto.getEmployeeId());
        record.setCourseId(dto.getCourseId());
        record.setAttendRate(dto.getAttendRate());
        record.setPassRate(dto.getPassRate());
        record.setImproveScore(dto.getImproveScore());
        trainingRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public void deleteRecord(Long id) {
        if (trainingRecordMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        trainingRecordMapper.deleteById(id);
    }

    @Override
    public List<TrainingEffectVO> effects() {
        return trainingQueryMapper.trainingEffects();
    }

    private Course toCourse(CourseSaveDTO dto) {
        Course course = new Course();
        copyCourse(dto, course);
        return course;
    }

    private void copyCourse(CourseSaveDTO dto, Course course) {
        if (dto == null || !StringUtils.hasText(dto.getCourseName())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "课程名称不能为空");
        }
        if (dto.getDuration() != null && dto.getDuration() < 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "培训时长不能小于 0");
        }
        course.setCourseName(dto.getCourseName().trim());
        course.setCourseType(trimToNull(dto.getCourseType()));
        course.setForPosition(trimToNull(dto.getForPosition()));
        course.setDifficulty(trimToNull(dto.getDifficulty()));
        course.setDuration(dto.getDuration() == null ? 0 : dto.getDuration());
    }

    private void validatePath(LearningPathSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getPathName())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "学习路径名称不能为空");
        }
        if (dto.getTasks() == null || dto.getTasks().isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "学习路径至少需要一个阶段任务");
        }
        for (LearningPathSaveDTO.PathTaskDTO task : dto.getTasks()) {
            if (task == null || !StringUtils.hasText(task.getTaskName()) || task.getStage() == null || task.getStage() < 1) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "任务名称不能为空，阶段必须从 1 开始");
            }
            if (task.getCourseId() != null) {
                requireCourse(task.getCourseId());
            }
        }
    }

    private void saveTasks(Long pathId, List<LearningPathSaveDTO.PathTaskDTO> tasks) {
        for (LearningPathSaveDTO.PathTaskDTO dto : tasks) {
            PathTask task = new PathTask();
            task.setPathId(pathId);
            task.setTaskName(dto.getTaskName().trim());
            task.setStage(dto.getStage());
            task.setCourseId(dto.getCourseId());
            pathTaskMapper.insert(task);
        }
    }

    private LearningPathVO toLearningPath(LearningPath path, boolean includeTasks) {
        LearningPathVO vo = new LearningPathVO();
        vo.setId(path.getId());
        vo.setPathName(path.getPathName());
        vo.setTargetSkill(path.getTargetSkill());
        vo.setCreatedAt(path.getCreatedAt());
        if (includeTasks) {
            List<LearningPathVO.PathTaskVO> tasks = new ArrayList<>();
            for (PathTask task : pathTaskMapper.selectList(new LambdaQueryWrapper<PathTask>()
                    .eq(PathTask::getPathId, path.getId()).orderByAsc(PathTask::getStage).orderByAsc(PathTask::getId))) {
                LearningPathVO.PathTaskVO taskVO = new LearningPathVO.PathTaskVO();
                taskVO.setId(task.getId());
                taskVO.setTaskName(task.getTaskName());
                taskVO.setStage(task.getStage());
                taskVO.setCourseId(task.getCourseId());
                if (task.getCourseId() != null) {
                    Course course = courseMapper.selectById(task.getCourseId());
                    taskVO.setCourseName(course == null ? null : course.getCourseName());
                }
                tasks.add(taskVO);
            }
            vo.setTasks(tasks);
        }
        return vo;
    }

    private CourseItemVO toCourseItem(Course course) {
        CourseItemVO vo = new CourseItemVO();
        vo.setId(course.getId());
        vo.setCourseName(course.getCourseName());
        vo.setCourseType(course.getCourseType());
        vo.setForPosition(course.getForPosition());
        vo.setDifficulty(course.getDifficulty());
        vo.setDuration(course.getDuration());
        vo.setCompleted(false);
        return vo;
    }

    private Course requireCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "课程不存在");
        }
        return course;
    }

    private LearningPath requirePath(Long id) {
        LearningPath path = learningPathMapper.selectById(id);
        if (path == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "学习路径不存在");
        }
        return path;
    }

    private EmployeeTrainingProfileVO requireEmployee(Long employeeId) {
        EmployeeTrainingProfileVO profile = trainingQueryMapper.employeeProfile(employeeId);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在");
        }
        return profile;
    }

    private void validatePage(long pageNum, long pageSize) {
        if (pageNum < 1 || pageNum > 1_000_000 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "分页参数不合法，pageNum 在 1 到 1000000 之间且 pageSize 在 1 到 100 之间");
        }
    }

    private void validateRate(BigDecimal value, String fieldName) {
        if (value != null && (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0)) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), fieldName + "必须在 0 到 100 之间");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
