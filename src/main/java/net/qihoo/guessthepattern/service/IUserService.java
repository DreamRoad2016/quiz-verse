package net.qihoo.guessthepattern.service;

import net.qihoo.guessthepattern.dto.GameRecord;
import net.qihoo.guessthepattern.dto.LoginResult;
import net.qihoo.guessthepattern.dto.RankRecord;
import net.qihoo.guessthepattern.dto.UserLoginDTO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface IUserService {
    
    /**
     * 用户注册
     */
    LoginResult register(UserLoginDTO dto);
    
    /**
     * 用户登录
     */
    LoginResult login(UserLoginDTO dto);
    
    /**
     * 验证 token，返回用户名
     */
    String validateToken(String token);
    
    /**
     * 记录游戏战绩
     */
    void saveGameRecord(String username, GameRecord record);
    
    /**
     * 获取用户战绩列表
     */
    List<GameRecord> getGameRecords(String username);
    
    /**
     * 尝试更新排行榜（如果成绩够好）
     * @return 排名规则：
     *   正数 1-20：首次上榜或刷新了记录
     *   负数 -1 到 -20：已在榜但本次没刷新记录（绝对值是当前排名）
     *   0：未上榜
     */
    int tryUpdateLeaderboard(String username, int steps, long duration);
    
    /**
     * 获取排行榜（前20名）
     */
    List<RankRecord> getLeaderboard();
}

