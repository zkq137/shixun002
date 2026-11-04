package com.talent.training.service.impl;
import com.talent.training.entity.TrainingAlgorithm;
import java.math.*;
import java.util.*;

/** 可解释的规则评分；三项特征范围 0–1，权重总和为 100。 */
public final class TrainingMatchScorer {
    private TrainingMatchScorer() {}
    public static List<String> tokens(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.toLowerCase(Locale.ROOT).split("[,，、;；\\s]+"))
                .map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
    }
    public static double coverage(String targets, List<String> evidence) {
        List<String> skills = tokens(targets);
        if (skills.isEmpty() || evidence == null || evidence.isEmpty()) return 0;
        return skills.stream().filter(skill -> evidence.stream().filter(Objects::nonNull).filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT)).anyMatch(s -> s.contains(skill) || skill.contains(s)))
                .count() / (double) skills.size();
    }
    public static BigDecimal score(TrainingAlgorithm config, double skill, double position, double goal) {
        return config.getSkillWeight().multiply(BigDecimal.valueOf(skill))
                .add(config.getPositionWeight().multiply(BigDecimal.valueOf(position)))
                .add(config.getGoalWeight().multiply(BigDecimal.valueOf(goal))).setScale(2, RoundingMode.HALF_UP);
    }
}
