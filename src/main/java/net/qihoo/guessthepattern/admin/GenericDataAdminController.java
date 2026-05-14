package net.qihoo.guessthepattern.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.qihoo.guessthepattern.config.AdminApiAuthInterceptor;
import net.qihoo.guessthepattern.result.ResultResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 通用表白名单管理（需请求头 {@value AdminApiAuthInterceptor#HEADER_ADMIN_KEY}）。
 * <p>在 {@code application.yml} 的 {@code quiz.admin.tables} 增加表名即可，无需为每张表写专用前端。</p>
 * <p>静态页入口：{@code /db-admin.html}（不挂在首页）。</p>
 */
@RestController
@Api(tags = "管理-通用表（需密钥）")
@RequestMapping("/api/admin")
public class GenericDataAdminController {

    @Resource
    private GenericTableAdminService genericTableAdminService;
    @Resource
    private ObjectMapper objectMapper;

    @ApiOperation("白名单表名列表")
    @GetMapping("/meta/tables")
    public ResultResponse<List<String>> listTables() {
        return ResultResponse.success(genericTableAdminService.listTables());
    }

    @ApiOperation("表结构（列、主键），用于前端动态渲染")
    @GetMapping("/meta/tables/{table}/schema")
    public ResultResponse<GenericTableAdminService.AdminSchemaView> schema(@PathVariable String table) {
        return ResultResponse.success(genericTableAdminService.schema(table));
    }

    @ApiOperation("分页行数据；q 在可搜索列上做 ILIKE")
    @GetMapping("/tables/{table}/rows")
    public ResultResponse<GenericTableAdminService.GenericPageView> page(
            @PathVariable String table,
            @ApiParam("页码从 0 开始") @RequestParam(defaultValue = "0") int page,
            @ApiParam("每页条数，最大 200") @RequestParam(defaultValue = "20") int size,
            @ApiParam("模糊搜索") @RequestParam(required = false) String q) {
        return ResultResponse.success(genericTableAdminService.page(table, q, page, size));
    }

    @ApiOperation("按主键取一行（JSON 对象为 snake_case 列名）")
    @GetMapping("/tables/{table}/rows/{pk}")
    public ResultResponse<Map<String, Object>> one(@PathVariable String table, @PathVariable String pk) {
        return ResultResponse.success(genericTableAdminService.getRow(table, pk));
    }

    @ApiOperation("新建行")
    @PostMapping("/tables/{table}/rows")
    public ResultResponse<Map<String, Object>> create(
            @PathVariable String table, @RequestBody Map<String, Object> body) {
        return ResultResponse.success(genericTableAdminService.createRow(table, body));
    }

    @ApiOperation("按主键部分更新（body 只含要改的列）")
    @PutMapping("/tables/{table}/rows/{pk}")
    public ResultResponse<Map<String, Object>> update(
            @PathVariable String table, @PathVariable String pk, @RequestBody Map<String, Object> body) {
        return ResultResponse.success(genericTableAdminService.updateRow(table, pk, body));
    }

    @ApiOperation("按主键删除")
    @DeleteMapping("/tables/{table}/rows/{pk}")
    public ResultResponse<Void> delete(@PathVariable String table, @PathVariable String pk) {
        genericTableAdminService.deleteRow(table, pk);
        return ResultResponse.success();
    }

    @ApiOperation("导出整张表为 JSON 数组")
    @GetMapping("/tables/{table}/-/export")
    public void export(@PathVariable String table, HttpServletResponse response) throws Exception {
        List<Map<String, Object>> data = genericTableAdminService.exportAll(table);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String fn = table + "-export.json";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fn + "\"");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(response.getOutputStream(), data);
    }

    @ApiOperation("批量导入：JSON 数组，按主键 upsert（UUID 主键缺省则自动生成）")
    @PostMapping("/tables/{table}/-/import")
    public ResultResponse<GenericTableAdminService.ImportResultView> importJson(
            @PathVariable String table, @RequestBody List<Map<String, Object>> rows) {
        return ResultResponse.success(genericTableAdminService.importRows(table, rows));
    }
}
