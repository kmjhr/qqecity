package com.icbc.qingqi.module.safety;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 青年金融安全模块接口
 */
@RestController
@RequestMapping("/api/v1/safety")
public class SafetyController {

    private final SafetyService safetyService;

    public SafetyController(SafetyService safetyService) {
        this.safetyService = safetyService;
    }

    @Data
    public static class VerifyRequest {
        @NotBlank(message = "话术内容不能为空")
        private String text;
    }

    @Data
    public static class LearnRequest {
        @NotBlank(message = "学习内容不能为空")
        private String contentId;
        private String learnType;
        private Integer score;
    }

    /** 反诈教学内容列表 */
    @GetMapping("/fraud-content")
    public Result<List<Map<String, String>>> fraudContent() {
        return Result.ok(safetyService.fraudContent());
    }

    /** 骗局甄别：输入话术文本，返回命中风险 */
    @PostMapping("/verify-text")
    public Result<List<Map<String, String>>> verify(@Valid @RequestBody VerifyRequest req) {
        return Result.ok(safetyService.verifyText(req.getText()));
    }

    /** 记录反诈学习完成 */
    @PostMapping("/learn")
    public Result<Long> learn(@Valid @RequestBody LearnRequest req) {
        String type = req.getLearnType() == null ? "情景模拟" : req.getLearnType();
        return Result.ok(safetyService.recordLearn(
                UserContext.requireUserId(), type, req.getContentId(), req.getScore()));
    }
}
