package net.qihoo.guessthepattern.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.qihoo.guessthepattern.dto.GameRecord;
import net.qihoo.guessthepattern.dto.LoginResult;
import net.qihoo.guessthepattern.dto.RankRecord;
import net.qihoo.guessthepattern.dto.UserLoginDTO;
import net.qihoo.guessthepattern.exception.BizException;
import net.qihoo.guessthepattern.result.ResultResponse;
import net.qihoo.guessthepattern.service.IUserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 用户接口
 *
 * @author zhouqingji
 */
@RestController
@Api(tags = "用户管理")
@RequestMapping("/user")
public class UserController {
    
    @Resource
    private IUserService userService;
    
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public ResultResponse<LoginResult> register(@Valid @RequestBody UserLoginDTO dto) {
        LoginResult result = userService.register(dto);
        return ResultResponse.success(result);
    }
    
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public ResultResponse<LoginResult> login(@Valid @RequestBody UserLoginDTO dto) {
        LoginResult result = userService.login(dto);
        return ResultResponse.success(result);
    }
    
    @ApiOperation("获取用户战绩")
    @GetMapping("/records")
    public ResultResponse<List<GameRecord>> getRecords(@RequestParam(value = "token") String token) {
        String username = userService.validateToken(token);
        if (!StringUtils.hasText(username)) {
            throw new BizException("登录已过期，请重新登录");
        }
        List<GameRecord> records = userService.getGameRecords(username);
        return ResultResponse.success(records);
    }
    
    @ApiOperation("验证登录状态")
    @GetMapping("/check")
    public ResultResponse<String> checkToken(@RequestParam(value = "token") String token) {
        String username = userService.validateToken(token);
        if (!StringUtils.hasText(username)) {
            throw new BizException("登录已过期，请重新登录");
        }
        return ResultResponse.success(username);
    }
    
    @ApiOperation("获取排行榜（前20名）")
    @GetMapping("/leaderboard")
    public ResultResponse<List<RankRecord>> getLeaderboard() {
        List<RankRecord> leaderboard = userService.getLeaderboard();
        return ResultResponse.success(leaderboard);
    }
}

