-- 可部署人才模型的版本与参数存储
CREATE TABLE IF NOT EXISTS `model_artifact` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `model_code` VARCHAR(64) NOT NULL COMMENT '稳定模型标识',
  `model_name` VARCHAR(128) NOT NULL COMMENT '模型名称',
  `model_type` VARCHAR(32) NOT NULL COMMENT '分类模型/排序模型',
  `version` VARCHAR(32) NOT NULL COMMENT '训练版本',
  `feature_names` TEXT NOT NULL COMMENT '特征名 JSON',
  `means` TEXT NOT NULL COMMENT '标准化均值 JSON',
  `stddevs` TEXT NOT NULL COMMENT '标准化标准差 JSON',
  `coefficients` TEXT NOT NULL COMMENT '逻辑回归系数 JSON',
  `intercept` DOUBLE NOT NULL COMMENT '截距',
  `threshold` DOUBLE NOT NULL COMMENT '分类阈值',
  `metrics` TEXT NOT NULL COMMENT '测试集评估指标 JSON',
  `sample_count` INT NOT NULL COMMENT '训练样本量',
  `positive_count` INT NOT NULL COMMENT '正样本量',
  `dataset_version` VARCHAR(64) DEFAULT NULL COMMENT '训练数据版本',
  `train_time` DATETIME NOT NULL COMMENT '训练时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_version` (`model_code`, `version`),
  KEY `idx_model_time` (`model_code`, `train_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可部署模型参数';

-- 人才流入、流出、晋升趋势预测批次
CREATE TABLE IF NOT EXISTS `flow_forecast_run` (
  `run_code` VARCHAR(40) NOT NULL,
  `horizon_months` INT NOT NULL,
  `model_version` VARCHAR(32) NOT NULL,
  `generated_at` DATETIME NOT NULL,
  `metrics` TEXT NOT NULL,
  `notes` TEXT NOT NULL,
  `status` VARCHAR(16) NOT NULL,
  `source` VARCHAR(16) NOT NULL DEFAULT 'generated',
  PRIMARY KEY (`run_code`),
  KEY `idx_forecast_time` (`generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人才流动预测批次';

CREATE TABLE IF NOT EXISTS `flow_forecast_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `run_code` VARCHAR(40) NOT NULL,
  `movement_type` VARCHAR(16) NOT NULL,
  `month` CHAR(7) NOT NULL,
  `predicted_value` DECIMAL(10,2) NOT NULL,
  `lower_bound` DECIMAL(10,2) NOT NULL,
  `upper_bound` DECIMAL(10,2) NOT NULL,
  `method` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forecast_point` (`run_code`, `movement_type`, `month`),
  KEY `idx_forecast_run` (`run_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人才流动预测明细';
