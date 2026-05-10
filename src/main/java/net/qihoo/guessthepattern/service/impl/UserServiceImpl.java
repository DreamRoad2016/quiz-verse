package net.qihoo.guessthepattern.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.qihoo.guessthepattern.dto.GameRecord;
import net.qihoo.guessthepattern.dto.LoginResult;
import net.qihoo.guessthepattern.dto.RankRecord;
import net.qihoo.guessthepattern.dto.UserLoginDTO;
import net.qihoo.guessthepattern.exception.BizException;
import net.qihoo.guessthepattern.model.UserEntity;
import net.qihoo.guessthepattern.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import org.springframework.data.redis.core.ZSetOperations;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl implements IUserService {
    
    private static final String USER_KEY_PREFIX = "user:";
    private static final String TOKEN_KEY_PREFIX = "token:";
    private static final String RECORD_KEY_PREFIX = "record:";
    private static final String LEADERBOARD_KEY = "leaderboard";
    private static final String LEADERBOARD_DATA_KEY = "leaderboard_data:";
    
    // Token 有效期24小时
    private static final long TOKEN_EXPIRE_HOURS = 24;
    
    // 排行榜最大人数
    private static final int LEADERBOARD_MAX_SIZE = 20;
    
    // 每个用户最多保留的战绩记录数
    private static final int MAX_GAME_RECORDS = 100;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Override
    public LoginResult register(UserLoginDTO dto) {
        String userKey = USER_KEY_PREFIX + dto.getUsername();
        
        // 检查用户是否已存在
        if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
            throw new BizException("用户名已存在");
        }
        
        // 创建用户实体
        UserEntity user = new UserEntity();
        user.setUsername(dto.getUsername());
        user.setPassword(encryptPassword(dto.getPassword()));
        user.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 保存到 Redis（永久存储）
        redisTemplate.opsForValue().set(userKey, JSONObject.toJSONString(user));
        
        // 生成 token 并返回
        String token = generateToken(dto.getUsername());
        return LoginResult.of(token, dto.getUsername());
    }
    
    @Override
    public LoginResult login(UserLoginDTO dto) {
        String userKey = USER_KEY_PREFIX + dto.getUsername();
        
        // 检查用户是否存在
        String userJson = redisTemplate.opsForValue().get(userKey);
        if (!StringUtils.hasText(userJson)) {
            throw new BizException("用户不存在");
        }
        
        // 校验密码
        UserEntity user = JSON.parseObject(userJson, UserEntity.class);
        if (!user.getPassword().equals(encryptPassword(dto.getPassword()))) {
            throw new BizException("密码错误");
        }
        
        // 生成 token 并返回
        String token = generateToken(dto.getUsername());
        return LoginResult.of(token, dto.getUsername());
    }
    
    @Override
    public String validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String tokenKey = TOKEN_KEY_PREFIX + token;
        return redisTemplate.opsForValue().get(tokenKey);
    }
    
    @Override
    public void saveGameRecord(String username, GameRecord record) {
        String recordKey = RECORD_KEY_PREFIX + username;
        // 使用 List 存储战绩记录（新记录在前）
        redisTemplate.opsForList().leftPush(recordKey, JSONObject.toJSONString(record));
        // 只保留最新的100条记录，超出部分自动删除
        redisTemplate.opsForList().trim(recordKey, 0, MAX_GAME_RECORDS - 1);
    }
    
    @Override
    public List<GameRecord> getGameRecords(String username) {
        String recordKey = RECORD_KEY_PREFIX + username;
        List<String> records = redisTemplate.opsForList().range(recordKey, 0, -1);
        List<GameRecord> result = new ArrayList<>();
        if (records != null) {
            for (String record : records) {
                result.add(JSON.parseObject(record, GameRecord.class));
            }
        }
        return result;
    }
    
    /**
     * 生成 token
     */
    private String generateToken(String username) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenKey = TOKEN_KEY_PREFIX + token;
        redisTemplate.opsForValue().set(tokenKey, username, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        return token;
    }
    
    /**
     * 密码加密（MD5）
     */
    private String encryptPassword(String password) {
        // 加盐
        String salted = "guess_game_" + password + "_salt";
        return DigestUtils.md5DigestAsHex(salted.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 尝试更新排行榜
     * @return 排名规则：
     *   正数 1-20：首次上榜或刷新了记录
     *   负数 -1 到 -20：已在榜但本次没刷新记录（绝对值是当前排名）
     *   0：未上榜
     */
    @Override
    public int tryUpdateLeaderboard(String username, int steps, long duration) {
        // 计算分数：步数 * 10000000 + 耗时（这样步数小的优先，步数相同时耗时短的优先）
        double score = steps * 10000000.0 + duration;
        
        // 获取当前排行榜大小
        Long size = redisTemplate.opsForZSet().size(LEADERBOARD_KEY);
        if (size == null) {
            size = 0L;
        }
        
        // 检查是否已有该用户的记录
        Double existingScore = redisTemplate.opsForZSet().score(LEADERBOARD_KEY, username);
        
        if (existingScore != null) {
            // 用户已在榜上
            Long rank = redisTemplate.opsForZSet().rank(LEADERBOARD_KEY, username);
            int currentRank = rank != null ? rank.intValue() + 1 : 1;
            
            if (score < existingScore) {
                // 新成绩更好，更新记录
                redisTemplate.opsForZSet().add(LEADERBOARD_KEY, username, score);
                saveLeaderboardData(username, steps, duration);
                // 重新获取排名（可能变化了）
                Long newRank = redisTemplate.opsForZSet().rank(LEADERBOARD_KEY, username);
                return newRank != null ? newRank.intValue() + 1 : currentRank;
            } else {
                // 新成绩不如旧成绩，返回负数表示没刷新
                return -currentRank;
            }
        } else if (size < LEADERBOARD_MAX_SIZE) {
            // 榜未满，直接加入
            redisTemplate.opsForZSet().add(LEADERBOARD_KEY, username, score);
            saveLeaderboardData(username, steps, duration);
            Long rank = redisTemplate.opsForZSet().rank(LEADERBOARD_KEY, username);
            return rank != null ? rank.intValue() + 1 : 1;
        } else {
            // 榜已满，检查是否能挤掉最后一名
            Set<ZSetOperations.TypedTuple<String>> lastOne = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(LEADERBOARD_KEY, 0, 0);
            if (lastOne != null && !lastOne.isEmpty()) {
                ZSetOperations.TypedTuple<String> last = lastOne.iterator().next();
                if (last.getScore() != null && score < last.getScore()) {
                    // 移除最后一名
                    String lastUsername = last.getValue();
                    redisTemplate.opsForZSet().remove(LEADERBOARD_KEY, lastUsername);
                    redisTemplate.delete(LEADERBOARD_DATA_KEY + lastUsername);
                    // 加入新记录
                    redisTemplate.opsForZSet().add(LEADERBOARD_KEY, username, score);
                    saveLeaderboardData(username, steps, duration);
                    Long rank = redisTemplate.opsForZSet().rank(LEADERBOARD_KEY, username);
                    return rank != null ? rank.intValue() + 1 : LEADERBOARD_MAX_SIZE;
                }
            }
        }
        
        // 未能上榜
        return 0;
    }
    
    /**
     * 保存排行榜详细数据
     */
    private void saveLeaderboardData(String username, int steps, long duration) {
        RankRecord record = new RankRecord();
        record.setUsername(username);
        record.setSteps(steps);
        record.setDuration(duration);
        record.setGameTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        redisTemplate.opsForValue().set(LEADERBOARD_DATA_KEY + username, JSON.toJSONString(record));
    }
    
    @Override
    public List<RankRecord> getLeaderboard() {
        List<RankRecord> result = new ArrayList<>();
        
        // 获取排行榜（按分数从小到大，同时获取分数）
        Set<ZSetOperations.TypedTuple<String>> rangeWithScores = redisTemplate.opsForZSet()
                .rangeWithScores(LEADERBOARD_KEY, 0, LEADERBOARD_MAX_SIZE - 1);
        if (rangeWithScores == null || rangeWithScores.isEmpty()) {
            return result;
        }
        
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : rangeWithScores) {
            String username = tuple.getValue();
            Double score = tuple.getScore();
            
            // 先尝试从详细数据获取
            String dataJson = redisTemplate.opsForValue().get(LEADERBOARD_DATA_KEY + username);
            RankRecord record;
            
            if (StringUtils.hasText(dataJson)) {
                record = JSON.parseObject(dataJson, RankRecord.class);
            } else if (score != null) {
                // 如果详细数据丢失，从 score 反推
                record = new RankRecord();
                record.setUsername(username);
                record.setSteps((int) (score / 10000000));
                record.setDuration((long) (score % 10000000));
                record.setGameTime("未知");
            } else {
                continue;
            }
            
            record.setRank(rank++);
            result.add(record);
        }
        
        return result;
    }
}

