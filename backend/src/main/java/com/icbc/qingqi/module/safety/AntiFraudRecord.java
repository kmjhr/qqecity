package com.icbc.qingqi.module.safety;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反诈学习记录（anti_fraud_record）
 */
@Data
@TableName("anti_fraud_record")
public class AntiFraudRecord {

    @TableId(type = IdType.AUTO)
    private Long recordId;
    private Long userId;
    /** 学习类型：情景模拟/财商课程 */
    private String learnType;
    /** 学习内容ID */
    private String contentId;
    /** 完成状态：0未完成/1已完成 */
    private Integer finishStatus;
    private Integer score;
    private LocalDateTime learnTime;
}
