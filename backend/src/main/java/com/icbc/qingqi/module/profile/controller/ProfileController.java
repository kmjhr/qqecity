package com.icbc.qingqi.module.profile.controller;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.profile.dto.LinkageApplyVO;
import com.icbc.qingqi.module.profile.dto.ProfileVO;
import com.icbc.qingqi.module.profile.service.ProfileService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 青年成长信用画像（步骤 7·智能中台 缺口 #21）
 * <p>
 * 路径：/api/v1/profile/**
 * 三场景聚合（安居/创业/消费）→ 三维评分 + 成长轨迹 + 联动演示
 * 全程"模拟"口径，数据实时从各模块表读取，不写死
 */
@Tag(name = "青年成长信用画像")
@RestController
@RequestMapping("/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "查询青年成长信用画像",
            description = "三场景数据聚合（安居/创业/消费）→ 稳定性/经营力/资金健康度三维评分（30/40/30 加权）"
                    + "+ 成长轨迹 + 联动演示（安居稳+经营好→授信提额）")
    @GetMapping
    public Result<ProfileVO> getProfile() {
        return Result.success(profileService.getProfile(UserContext.getUserId()));
    }

    @Operation(summary = "应用联动提额（演示级）",
            description = "若稳定性≥60 且 经营力≥60，对 A 类授信额度执行提额 20%（上限 8 万）"
                    + "+ 利率优惠 0.30pct（下限 3.00%）；不达标则返回提示")
    @PostMapping("/linkage/apply")
    public Result<LinkageApplyVO> applyLinkage() {
        return Result.success(profileService.applyLinkage(UserContext.getUserId()));
    }
}
