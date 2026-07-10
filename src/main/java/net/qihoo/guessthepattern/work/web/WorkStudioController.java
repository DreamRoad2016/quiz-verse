package net.qihoo.guessthepattern.work.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.qihoo.guessthepattern.config.AdminApiAuthInterceptor;
import net.qihoo.guessthepattern.result.ResultResponse;
import net.qihoo.guessthepattern.work.domain.WorkCharacterRow;
import net.qihoo.guessthepattern.work.domain.WorkRow;
import net.qihoo.guessthepattern.work.dto.CharacterBriefDTO;
import net.qihoo.guessthepattern.work.dto.CreateWorkRequest;
import net.qihoo.guessthepattern.work.dto.SaveCharacterRequest;
import net.qihoo.guessthepattern.work.dto.WorkListItemDTO;
import net.qihoo.guessthepattern.work.service.WorkStudioService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 测试环境录入：影视剧作品 + 角色（需 {@value AdminApiAuthInterceptor#HEADER_ADMIN_KEY}）。
 */
@RestController
@Api(tags = "录入-影视剧 Studio（需密钥）")
@RequestMapping("/api/studio")
public class WorkStudioController {

    @Resource
    private WorkStudioService workStudioService;

    @ApiOperation("作品列表")
    @GetMapping("/works")
    public ResultResponse<List<WorkListItemDTO>> listWorks() {
        return ResultResponse.success(workStudioService.listWorks());
    }

    @ApiOperation("新建作品（复制模板配置目录与 work_column）")
    @PostMapping("/works")
    public ResultResponse<WorkRow> createWork(@RequestBody CreateWorkRequest req) {
        return ResultResponse.success(workStudioService.createWork(req));
    }

    @ApiOperation("作品字段配置 + 枚举选项（读 config/works/…）")
    @GetMapping("/works/{workId}/config")
    public ResultResponse<Map<String, Object>> fieldConfig(@PathVariable String workId) {
        return ResultResponse.success(workStudioService.loadFieldConfig(workId));
    }

    @ApiOperation("角色列表（简要）")
    @GetMapping("/works/{workId}/characters")
    public ResultResponse<List<CharacterBriefDTO>> listCharacters(@PathVariable String workId) {
        return ResultResponse.success(workStudioService.listCharacterBriefs(workId));
    }

    @ApiOperation("角色详情")
    @GetMapping("/works/{workId}/characters/{id}")
    public ResultResponse<WorkCharacterRow> getCharacter(@PathVariable String workId, @PathVariable long id) {
        return ResultResponse.success(workStudioService.getCharacter(workId, id));
    }

    @ApiOperation("新建角色")
    @PostMapping("/works/{workId}/characters")
    public ResultResponse<WorkCharacterRow> createCharacter(
            @PathVariable String workId, @RequestBody SaveCharacterRequest req) {
        return ResultResponse.success(workStudioService.saveCharacter(workId, null, req));
    }

    @ApiOperation("更新角色")
    @PutMapping("/works/{workId}/characters/{id}")
    public ResultResponse<WorkCharacterRow> updateCharacter(
            @PathVariable String workId, @PathVariable long id, @RequestBody SaveCharacterRequest req) {
        return ResultResponse.success(workStudioService.saveCharacter(workId, id, req));
    }

    @ApiOperation("删除角色")
    @DeleteMapping("/works/{workId}/characters/{id}")
    public ResultResponse<Void> deleteCharacter(@PathVariable String workId, @PathVariable long id) {
        workStudioService.deleteCharacter(workId, id);
        return ResultResponse.success();
    }
}
