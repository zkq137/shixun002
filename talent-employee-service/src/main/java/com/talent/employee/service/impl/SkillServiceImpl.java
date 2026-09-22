package com.talent.employee.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import com.talent.employee.dto.SkillSaveDTO;
import com.talent.employee.entity.Skill;
import com.talent.employee.mapper.SkillMapper;
import com.talent.employee.service.SkillService;
import com.talent.employee.vo.SkillTreeVO;
import com.talent.employee.vo.SkillVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能树业务实现。
 */
@Service
public class SkillServiceImpl extends ServiceImpl<SkillMapper, Skill> implements SkillService {

    private static final long ROOT = 0L;

    @Override
    public List<SkillTreeVO> tree() {
        List<SkillVO> all = baseMapper.selectAllWithRefCount();

        Map<Long, SkillTreeVO> nodes = new LinkedHashMap<>();
        List<SkillTreeVO> roots = new ArrayList<>();
        for (SkillVO item : all) {
            SkillTreeVO node = toTreeVO(item);
            nodes.put(node.getId(), node);
            if (item.getParentId() == null || item.getParentId() == ROOT) {
                roots.add(node);
            }
        }
        for (SkillVO item : all) {
            Long parentId = item.getParentId();
            if (parentId == null || parentId == ROOT) {
                continue;
            }
            SkillTreeVO parent = nodes.get(parentId);
            if (parent == null) {
                // 父节点被删了的脏数据，挂到根上，别让它凭空消失
                roots.add(nodes.get(item.getId()));
            } else {
                parent.getChildren().add(nodes.get(item.getId()));
            }
        }
        return roots;
    }

    @Override
    public List<SkillVO> categories() {
        return baseMapper.selectCategories();
    }

    @Override
    public List<SkillVO> listByParent(Long parentId) {
        return baseMapper.selectByParent(parentId == null ? ROOT : parentId);
    }

    @Override
    public List<SkillVO> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return baseMapper.search(keyword.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SkillSaveDTO dto) {
        String name = dto.getSkillName().trim();
        if (baseMapper.countByName(name, null) > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "技能名称已存在：" + name);
        }

        long parentId = dto.getParentId() == null ? ROOT : dto.getParentId();
        String category = "未分类";
        int level = 1;
        if (parentId != ROOT) {
            Skill parent = getById(parentId);
            if (parent == null) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "父分类不存在：" + parentId);
            }
            if (parent.getParentId() != null && parent.getParentId() != ROOT) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "只支持两层：分类下面挂技能，技能下面不能再挂");
            }
            category = parent.getSkillName();
            level = 2;
        }

        Skill entity = new Skill();
        entity.setSkillName(name);
        entity.setParentId(parentId);
        entity.setLevel(level);
        entity.setSkillCategory(category);
        entity.setDescription(dto.getDescription());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "启用");
        entity.setSortOrder(dto.getSortOrder() == null ? nextSortOrder(parentId) : dto.getSortOrder());
        save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SkillSaveDTO dto) {
        Skill exists = getById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "技能不存在：" + id);
        }
        String name = dto.getSkillName().trim();
        if (baseMapper.countByName(name, id) > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "技能名称已存在：" + name);
        }

        boolean isCategory = exists.getParentId() == null || exists.getParentId() == ROOT;
        boolean renameCategory = isCategory && !name.equals(exists.getSkillName());

        Skill entity = new Skill();
        entity.setId(id);
        entity.setSkillName(name);
        entity.setDescription(dto.getDescription());
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        // 支持把技能挪到别的分类
        if (!isCategory && dto.getParentId() != null && !dto.getParentId().equals(exists.getParentId())) {
            Skill parent = getById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "父分类不存在：" + dto.getParentId());
            }
            entity.setParentId(parent.getId());
            entity.setSkillCategory(parent.getSkillName());
        }
        updateById(entity);

        // 分类改名后，把子技能上的冗余分类名一起改掉，避免两边对不上
        if (renameCategory) {
            Skill patch = new Skill();
            patch.setSkillCategory(name);
            lambdaUpdate().eq(Skill::getParentId, id).update(patch);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Skill exists = getById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "技能不存在：" + id);
        }

        int children = baseMapper.countChildren(id);
        if (children > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "这个分类下还有 " + children + " 个技能，先把它们删掉或挪走再删分类");
        }

        int employeeRef = baseMapper.countEmployeeRef(id);
        int positionRef = baseMapper.countPositionRef(id);
        if (employeeRef > 0 || positionRef > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "已有 " + employeeRef + " 名员工、 " + positionRef + " 个岗位在用这个技能，不能直接删；可以改成「停用」");
        }
        removeById(id);
    }

    private int nextSortOrder(long parentId) {
        long count = lambdaQuery().eq(Skill::getParentId, parentId).count();
        return (int) count + 1;
    }

    private SkillTreeVO toTreeVO(SkillVO item) {
        SkillTreeVO node = new SkillTreeVO();
        node.setId(item.getId());
        node.setSkillName(item.getSkillName());
        node.setParentId(item.getParentId());
        node.setLevel(item.getLevel());
        node.setSortOrder(item.getSortOrder());
        node.setStatus(item.getStatus());
        node.setDescription(item.getDescription());
        node.setSkillCategory(item.getSkillCategory());
        node.setEmployeeCount(item.getEmployeeCount());
        node.setPositionCount(item.getPositionCount());
        return node;
    }
}
