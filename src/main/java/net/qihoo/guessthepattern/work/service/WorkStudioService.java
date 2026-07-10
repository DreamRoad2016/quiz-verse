package net.qihoo.guessthepattern.work.service;

import net.qihoo.guessthepattern.exception.BizException;
import net.qihoo.guessthepattern.work.domain.WorkCharacterRow;
import net.qihoo.guessthepattern.work.domain.WorkRow;
import net.qihoo.guessthepattern.work.dto.CharacterBriefDTO;
import net.qihoo.guessthepattern.work.dto.CreateWorkRequest;
import net.qihoo.guessthepattern.work.dto.SaveCharacterRequest;
import net.qihoo.guessthepattern.work.dto.WorkListItemDTO;
import net.qihoo.guessthepattern.work.repo.WorkJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class WorkStudioService {

    private static final Pattern WORK_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,62}$");

    @Resource
    private WorkJdbcRepository workJdbcRepository;
    @Resource
    private WorkConfigService workConfigService;

    public List<WorkListItemDTO> listWorks() {
        List<WorkListItemDTO> out = new ArrayList<>();
        for (WorkRow w : workJdbcRepository.listWorks()) {
            out.add(WorkListItemDTO.builder()
                    .id(w.getId())
                    .titleCn(w.getTitleCn())
                    .configDir(w.getConfigDir())
                    .enabled(w.getEnabled())
                    .characterCount(workJdbcRepository.countCharacters(w.getId()))
                    .activeCharacterCount(workJdbcRepository.countActiveCharacters(w.getId()))
                    .hasFieldConfig(workConfigService.hasFieldConfig(w))
                    .build());
        }
        return out;
    }

    public WorkRow createWork(CreateWorkRequest req) {
        if (req == null || !StringUtils.hasText(req.getId()) || !StringUtils.hasText(req.getTitleCn())) {
            throw new BizException("请填写作品 id 与中文名");
        }
        String id = req.getId().trim();
        if (!WORK_ID_PATTERN.matcher(id).matches()) {
            throw new BizException("作品 id 须为小写字母开头，仅含小写字母、数字、下划线");
        }
        if (workJdbcRepository.workExists(id)) {
            throw new BizException("作品已存在：" + id);
        }
        String template = StringUtils.hasText(req.getTemplateWorkId())
                ? req.getTemplateWorkId().trim() : "zhenhuan_2011";
        if (!workJdbcRepository.workExists(template)) {
            throw new BizException("模板作品不存在，请先执行种子脚本：" + template);
        }
        String configDir = "config/works/" + id;
        workConfigService.copyConfigFromTemplate(template, id, req.getTitleCn().trim());
        workJdbcRepository.insertWork(id, req.getTitleCn().trim(), configDir);
        workJdbcRepository.copyWorkColumns(template, id);
        return workJdbcRepository.findWork(id).orElseThrow(() -> new BizException("创建失败"));
    }

    public WorkRow requireWork(String workId) {
        return workJdbcRepository.findWork(workId)
                .orElseThrow(() -> new BizException("作品不存在：" + workId));
    }

    public Map<String, Object> loadFieldConfig(String workId) {
        WorkRow work = requireWork(workId);
        return workConfigService.loadStudioConfig(work);
    }

    public List<CharacterBriefDTO> listCharacterBriefs(String workId) {
        requireWork(workId);
        return workJdbcRepository.listCharacters(workId).stream()
                .map(this::toBrief)
                .collect(Collectors.toList());
    }

    public WorkCharacterRow getCharacter(String workId, long id) {
        requireWork(workId);
        return workJdbcRepository.findCharacter(workId, id)
                .orElseThrow(() -> new BizException("角色不存在"));
    }

    public WorkCharacterRow saveCharacter(String workId, Long id, SaveCharacterRequest req) {
        requireWork(workId);
        if (req == null || !StringUtils.hasText(req.getDisplayName())) {
            throw new BizException("请填写姓名");
        }
        String status = StringUtils.hasText(req.getStatus()) ? req.getStatus() : "draft";
        if (!"draft".equals(status) && !"ready".equals(status)) {
            throw new BizException("status 只能是 draft 或 ready");
        }
        boolean active = req.getIsActive() != null && req.getIsActive();
        if (active && !"ready".equals(status)) {
            throw new BizException("上架猜局须先将状态设为 ready");
        }
        Map<String, Object> attrs = req.getAttrs() == null ? Collections.emptyMap() : req.getAttrs();
        List<String> callNames = req.getCallNames() == null ? Collections.emptyList() : req.getCallNames();
        int sortOrder = req.getSortOrder() == null ? 0 : req.getSortOrder();

        if (id == null) {
            long newId = workJdbcRepository.insertCharacter(
                    workId, req.getDisplayName().trim(), callNames, attrs, status, active, sortOrder);
            return getCharacter(workId, newId);
        }
        workJdbcRepository.updateCharacter(
                id, workId, req.getDisplayName().trim(), callNames, attrs, status, active, sortOrder);
        return getCharacter(workId, id);
    }

    public void deleteCharacter(String workId, long id) {
        requireWork(workId);
        workJdbcRepository.deleteCharacter(workId, id);
    }

    private CharacterBriefDTO toBrief(WorkCharacterRow row) {
        return CharacterBriefDTO.builder()
                .id(row.getId())
                .displayName(row.getDisplayName())
                .callNames(row.getCallNames())
                .status(row.getStatus())
                .isActive(row.getIsActive())
                .build();
    }
}
