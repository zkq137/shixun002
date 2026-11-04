package com.talent.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.PageResult;
import com.talent.common.result.ResultCode;
import com.talent.training.dto.CourseSaveDTO;
import com.talent.training.dto.CourseCategorySaveDTO;
import com.talent.training.dto.LearningPathSaveDTO;
import com.talent.training.dto.TrainingRecordSaveDTO;
import com.talent.training.entity.Course;
import com.talent.training.entity.CourseCategory;
import com.talent.training.entity.LearningPath;
import com.talent.training.entity.PathTask;
import com.talent.training.entity.TrainingRecord;
import com.talent.training.mapper.CourseMapper;
import com.talent.training.mapper.CourseCategoryMapper;
import com.talent.training.mapper.LearningPathMapper;
import com.talent.training.mapper.PathTaskMapper;
import com.talent.training.mapper.TrainingQueryMapper;
import com.talent.training.mapper.TrainingRecordMapper;
import com.talent.training.service.TrainingService;
import com.talent.training.vo.CourseItemVO;
import com.talent.training.vo.CourseCategoryVO;
import com.talent.training.vo.CourseImportResultVO;
import com.talent.training.vo.EmployeeTrainingProfileVO;
import com.talent.training.vo.LearningPathVO;
import com.talent.training.vo.TrainingEffectVO;
import com.talent.training.vo.TrainingPlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {
    private final TrainingAlgorithmService algorithmService;
    private final CourseMapper courseMapper;
    private final CourseCategoryMapper courseCategoryMapper;
    private final LearningPathMapper learningPathMapper;
    private final PathTaskMapper pathTaskMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final TrainingQueryMapper trainingQueryMapper;

    @Override
    public TrainingPlanVO plan(Long employeeId) { return plan(employeeId, null); }

    @Override
    public TrainingPlanVO plan(Long employeeId, String developmentGoal) {
        if (developmentGoal != null && developmentGoal.length() > 255) throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "培养目标最多 255 字");
        EmployeeTrainingProfileVO profile = requireEmployee(employeeId);
        List<CourseItemVO> completed = trainingQueryMapper.completedCourses(employeeId);
        List<CourseItemVO> recommended = trainingQueryMapper.requiredButIncompleteCourses(employeeId);
        List<String> gaps = trainingQueryMapper.skillGaps(employeeId);

        TrainingPlanVO plan = new TrainingPlanVO();
        plan.setEmployeeId(employeeId);
        plan.setEmployeeName(profile.getEmployeeName());
        plan.setPositionName(profile.getPositionName());
        plan.setCompletedCourses(completed);
        var config = algorithmService.active();
        plan.setRecommendedCourses(rankCourses(recommended, completed, gaps, profile, developmentGoal, config));
        plan.setSkillGaps(gaps);
        plan.setRecommendedPaths(recommendPaths(gaps, profile, developmentGoal, config));
        plan.setAlgorithmName(config.getName());
        plan.setMatchThreshold(config.getMatchThreshold());
        plan.setDevelopmentGoal(developmentGoal);
        plan.setRecommendationReason("岗位必修课程始终保留；补充课程与学习路径按技能缺口、岗位适配及培养目标加权评分，达到阈值后推荐");
        plan.setGeneratedAt(LocalDateTime.now());
        return plan;
    }

    @Override
    public List<CourseItemVO> completed(Long employeeId) {
        requireEmployee(employeeId);
        return trainingQueryMapper.completedCourses(employeeId);
    }

    @Override
    public PageResult<CourseItemVO> coursePage(long pageNum, long pageSize, String keyword, String courseType, Long categoryId) {
        validatePage(pageNum, pageSize);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .like(StringUtils.hasText(keyword), Course::getCourseName, keyword)
                .eq(StringUtils.hasText(courseType), Course::getCourseType, courseType)
                .eq(categoryId != null, Course::getCategoryId, categoryId)
                .orderByDesc(Course::getId);
        Page<Course> page = courseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> categoryNames = new HashMap<>();
        courseCategoryMapper.selectList(new LambdaQueryWrapper<CourseCategory>())
                .forEach(category -> categoryNames.put(category.getId(), category.getCategoryName()));
        List<CourseItemVO> records = page.getRecords().stream().map(course -> {
            CourseItemVO vo = toCourseItem(course);
            vo.setCategoryName(categoryNames.get(course.getCategoryId()));
            return vo;
        }).toList();
        return new PageResult<>(page.getTotal(), pageNum, pageSize, records);
    }

    @Override
    public List<CourseCategoryVO> categories() {
        Map<Long, Long> counts = new HashMap<>();
        courseMapper.selectList(new LambdaQueryWrapper<Course>())
                .forEach(course -> counts.merge(course.getCategoryId(), 1L, Long::sum));
        return courseCategoryMapper.selectList(new LambdaQueryWrapper<CourseCategory>()
                        .orderByAsc(CourseCategory::getId)).stream().map(category -> {
                    CourseCategoryVO vo = new CourseCategoryVO();
                    vo.setId(category.getId());
                    vo.setCategoryName(category.getCategoryName());
                    vo.setDescription(category.getDescription());
                    vo.setCreatedAt(category.getCreatedAt());
                    vo.setCourseCount(counts.getOrDefault(category.getId(), 0L));
                    return vo;
                }).toList();
    }

    @Override
    public Long createCategory(CourseCategorySaveDTO dto) {
        CourseCategory category = toCategory(dto);
        courseCategoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategory(Long id, CourseCategorySaveDTO dto) {
        CourseCategory category = requireCategory(id);
        copyCategory(dto, category);
        courseCategoryMapper.updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        requireCategory(id);
        long count = courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "分类下还有课程，不能删除");
        }
        courseCategoryMapper.deleteById(id);
    }

    @Override
    @Transactional
    public CourseImportResultVO importCourses(String csv) {
        if (!StringUtils.hasText(csv)) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "导入文件为空");
        }
        int imported = 0;
        List<List<String>> rows = parseCsv(csv.startsWith("\uFEFF") ? csv.substring(1) : csv);
        if (rows.size() > 2001) throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "单次最多导入 2000 行课程");
        for (int i = 0; i < rows.size(); i++) {
            String[] cells = rows.get(i).toArray(String[]::new);
            if (java.util.Arrays.stream(cells).allMatch(String::isBlank)) continue;
            if (i == 0 && cells[0].equalsIgnoreCase("courseName")) continue;
            if (cells.length != 6 || !StringUtils.hasText(cells[0])) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "第 " + (i + 1) + " 条记录格式错误，需要 6 列");
            }
            CourseSaveDTO dto = new CourseSaveDTO();
            dto.setCourseName(cells[0].trim());
            dto.setCourseType(trimToNull(cells[1]));
            if (StringUtils.hasText(cells[2])) {
                CourseCategory category = courseCategoryMapper.selectOne(new LambdaQueryWrapper<CourseCategory>()
                        .eq(CourseCategory::getCategoryName, cells[2]));
                if (category == null) {
                    CourseCategorySaveDTO categoryDto = new CourseCategorySaveDTO();
                    categoryDto.setCategoryName(cells[2]);
                    createCategory(categoryDto);
                    category = courseCategoryMapper.selectOne(new LambdaQueryWrapper<CourseCategory>()
                            .eq(CourseCategory::getCategoryName, cells[2]));
                }
                dto.setCategoryId(category.getId());
            }
            dto.setForPosition(trimToNull(cells[3]));
            dto.setDifficulty(trimToNull(cells[4]));
            if (cells.length > 5 && StringUtils.hasText(cells[5])) {
                try { dto.setDuration(Integer.valueOf(cells[5].trim())); }
                catch (NumberFormatException e) { throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "第 " + (i + 1) + " 行时长不是数字"); }
            }
            createCourse(dto);
            imported++;
            if (imported > 2000) throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "单次最多导入 2000 条课程");
        }
        if (imported == 0) throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "文件中没有可导入的课程");
        return new CourseImportResultVO(imported);
    }

    @Override
    public String exportCourses(String keyword, String courseType, Long categoryId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .like(StringUtils.hasText(keyword), Course::getCourseName, keyword)
                .eq(StringUtils.hasText(courseType), Course::getCourseType, courseType)
                .eq(categoryId != null, Course::getCategoryId, categoryId)
                .orderByAsc(Course::getId);
        Map<Long, String> categoryNames = new HashMap<>();
        courseCategoryMapper.selectList(new LambdaQueryWrapper<CourseCategory>())
                .forEach(category -> categoryNames.put(category.getId(), category.getCategoryName()));
        StringBuilder output = new StringBuilder("courseName,courseType,categoryName,forPosition,difficulty,duration\r\n");
        for (Course course : courseMapper.selectList(wrapper)) {
            output.append(csvCell(course.getCourseName())).append(',')
                    .append(csvCell(course.getCourseType())).append(',')
                    .append(csvCell(categoryNames.get(course.getCategoryId()))).append(',')
                    .append(csvCell(course.getForPosition())).append(',')
                    .append(csvCell(course.getDifficulty())).append(',')
                    .append(course.getDuration() == null ? 0 : course.getDuration()).append("\r\n");
        }
        return "\uFEFF" + output;
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
                .stream().map(path -> toLearningPath(path, true)).toList();
    }

    private List<LearningPathVO> recommendPaths(List<String> gaps, EmployeeTrainingProfileVO profile,
            String goal, com.talent.training.entity.TrainingAlgorithm config) {
        List<String> goals = goalEvidence(profile, goal);
        List<LearningPathVO> matches = new ArrayList<>();
        for (LearningPath path : learningPathMapper.selectList(new LambdaQueryWrapper<LearningPath>())) {
            double skill = TrainingMatchScorer.coverage(path.getTargetSkill(), gaps);
            double position = TrainingMatchScorer.coverage(path.getForPosition(), java.util.Collections.singletonList(profile.getPositionName()));
            double target = TrainingMatchScorer.coverage(path.getDevelopmentGoal(), goals);
            BigDecimal score = TrainingMatchScorer.score(config, skill, position, target);
            if (score.signum() == 0 || score.compareTo(config.getMatchThreshold()) < 0) continue;
            LearningPathVO vo = toLearningPath(path, true);
            vo.setMatchScore(score);
            vo.setMatchReason(reason(skill, position, target));
            matches.add(vo);
        }
        return matches.stream().sorted(java.util.Comparator.comparing(LearningPathVO::getMatchScore).reversed()
                .thenComparing(LearningPathVO::getId)).toList();
    }

    private List<CourseItemVO> rankCourses(List<CourseItemVO> required, List<CourseItemVO> completed,
            List<String> gaps, EmployeeTrainingProfileVO profile, String goal,
            com.talent.training.entity.TrainingAlgorithm config) {
        java.util.Set<Long> requiredIds = required.stream().map(CourseItemVO::getId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> completedIds = completed.stream().map(CourseItemVO::getId).collect(java.util.stream.Collectors.toSet());
        Map<Long, CourseItemVO> result = new java.util.LinkedHashMap<>();
        for (Course course : courseMapper.selectList(new LambdaQueryWrapper<Course>())) {
            if (completedIds.contains(course.getId())) continue;
            String text = course.getCourseName() + " " + (course.getCourseType() == null ? "" : course.getCourseType());
            double skill = evidenceInText(gaps, text);
            double position = requiredIds.contains(course.getId()) ? 1 : TrainingMatchScorer.coverage(course.getForPosition(), java.util.Collections.singletonList(profile.getPositionName()));
            double target = evidenceInText(goalEvidence(profile, goal), text);
            BigDecimal score = TrainingMatchScorer.score(config, skill, position, target);
            if (!requiredIds.contains(course.getId()) && (score.signum() == 0 || score.compareTo(config.getMatchThreshold()) < 0)) continue;
            CourseItemVO vo = toCourseItem(course);
            vo.setMatchScore(score);
            vo.setMatchReason((requiredIds.contains(course.getId()) ? "岗位必修；" : "") + reason(skill, position, target));
            result.put(course.getId(), vo);
        }
        return result.values().stream().sorted(java.util.Comparator.<CourseItemVO, Boolean>comparing(c -> requiredIds.contains(c.getId())).reversed()
                .thenComparing(CourseItemVO::getMatchScore, java.util.Comparator.reverseOrder()).thenComparing(CourseItemVO::getId)).toList();
    }

    private static double evidenceInText(List<String> evidence, String text) {
        if (evidence.isEmpty()) return 0;
        String normalized = text.toLowerCase(java.util.Locale.ROOT);
        return evidence.stream().filter(s -> normalized.contains(s.toLowerCase(java.util.Locale.ROOT))).count() / (double) evidence.size();
    }

    private List<String> goalEvidence(EmployeeTrainingProfileVO profile, String goal) {
        String value = String.join("，", goal == null ? "" : goal,
                profile.getLevelTier() == null ? "" : profile.getLevelTier(),
                profile.getTalentTag() == null ? "" : profile.getTalentTag());
        return TrainingMatchScorer.tokens(value);
    }

    private static String reason(double skill, double position, double goal) {
        return String.format(java.util.Locale.ROOT, "技能匹配 %.0f%% / 岗位适配 %.0f%% / 培养目标 %.0f%%", skill * 100, position * 100, goal * 100);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importPaths(List<LearningPathSaveDTO> templates) {
        if (templates == null || templates.isEmpty() || templates.size() > 200)
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "每次导入 1–200 个路径模板");
        for (LearningPathSaveDTO dto : templates) validatePath(dto);
        for (LearningPathSaveDTO dto : templates) createLearningPath(dto);
        return templates.size();
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
        path.setForPosition(trimToNull(dto.getForPosition()));
        path.setDevelopmentGoal(trimToNull(dto.getDevelopmentGoal()));
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
        path.setForPosition(trimToNull(dto.getForPosition()));
        path.setDevelopmentGoal(trimToNull(dto.getDevelopmentGoal()));
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
        validateRate(dto.getImproveScore(), "能力提升分值");
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
        if (dto.getCourseName().trim().length() > 128 || dto.getCourseType() != null && dto.getCourseType().length() > 32
                || dto.getForPosition() != null && dto.getForPosition().length() > 64
                || dto.getDifficulty() != null && dto.getDifficulty().length() > 16
                || dto.getDuration() != null && dto.getDuration() > 10000)
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "课程字段长度超限或培训时长超过 10000 小时");
        course.setCourseName(dto.getCourseName().trim());
        course.setCourseType(trimToNull(dto.getCourseType()));
        if (dto.getCategoryId() != null) requireCategory(dto.getCategoryId());
        course.setCategoryId(dto.getCategoryId());
        course.setForPosition(trimToNull(dto.getForPosition()));
        course.setDifficulty(trimToNull(dto.getDifficulty()));
        course.setDuration(dto.getDuration() == null ? 0 : dto.getDuration());
    }

    private CourseCategory toCategory(CourseCategorySaveDTO dto) {
        CourseCategory category = new CourseCategory();
        copyCategory(dto, category);
        return category;
    }

    private void copyCategory(CourseCategorySaveDTO dto, CourseCategory category) {
        if (dto == null || !StringUtils.hasText(dto.getCategoryName())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "分类名称不能为空");
        }
        String name = dto.getCategoryName().trim();
        if (name.length() > 64 || dto.getDescription() != null && dto.getDescription().length() > 255)
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "分类名称最多 64 字，说明最多 255 字");
        long duplicate = courseCategoryMapper.selectCount(new LambdaQueryWrapper<CourseCategory>()
                .eq(CourseCategory::getCategoryName, name)
                .ne(category.getId() != null, CourseCategory::getId, category.getId()));
        if (duplicate > 0) throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "分类名称已存在");
        category.setCategoryName(name);
        category.setDescription(trimToNull(dto.getDescription()));
    }

    private void validatePath(LearningPathSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getPathName())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "学习路径名称不能为空");
        }
        if (dto.getPathName().trim().length() > 64 || dto.getTargetSkill() != null && dto.getTargetSkill().length() > 64
                || dto.getForPosition() != null && dto.getForPosition().length() > 128
                || dto.getDevelopmentGoal() != null && dto.getDevelopmentGoal().length() > 255)
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "路径字段超出允许长度");
        if (dto.getTasks() == null || dto.getTasks().isEmpty() || dto.getTasks().size() > 100) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "学习路径至少需要一个阶段任务");
        }
        for (LearningPathSaveDTO.PathTaskDTO task : dto.getTasks()) {
            if (task == null || !StringUtils.hasText(task.getTaskName()) || task.getStage() == null || task.getStage() < 1 || task.getStage() > 99 || task.getTaskName().length() > 128) {
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
        vo.setForPosition(path.getForPosition());
        vo.setDevelopmentGoal(path.getDevelopmentGoal());
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
        vo.setCategoryId(course.getCategoryId());
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

    private CourseCategory requireCategory(Long id) {
        CourseCategory category = courseCategoryMapper.selectById(id);
        if (category == null) throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "课程分类不存在");
        return category;
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

    static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        boolean closed = false;
        for (int i = 0; i < csv.length(); i++) {
            char ch = csv.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') { cell.append('"'); i++; }
                    else { quoted = false; closed = true; }
                } else cell.append(ch);
            } else if (ch == ',' || ch == '\n' || ch == '\r') {
                row.add(cell.toString().trim()); cell.setLength(0); closed = false;
                if (ch != ',') {
                    rows.add(row); row = new ArrayList<>();
                    if (ch == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                }
            } else if (ch == '"' && cell.toString().isBlank() && !closed) {
                cell.setLength(0); quoted = true;
            } else {
                if (ch == '"' || closed && !Character.isWhitespace(ch))
                    throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "CSV 引号格式错误");
                if (!closed) cell.append(ch);
            }
        }
        if (quoted) throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "CSV 引号未闭合");
        if (!row.isEmpty() || cell.length() > 0 || closed) { row.add(cell.toString().trim()); rows.add(row); }
        return rows;
    }

    private String csvCell(String value) {
        if (value == null) return "";
        if (value.matches("^[=+@-].*")) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
