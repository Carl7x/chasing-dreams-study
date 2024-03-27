package com.tianji.learning.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.tianji.learning.constant.RedisConstants.POINTS_BOARD_KEY_PREFIX;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author kyle
 * @since 2024-03-23
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;

    @Override
    public PointsBoardVO queryPointsBoardList(PointsBoardQuery pointsBoardQuery) {
        Long userId = UserContext.getUser();
        boolean isCurrent = pointsBoardQuery.getSeason() == null || pointsBoardQuery.getSeason() == 0;
        LocalDate date = LocalDate.now();
        String format = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = POINTS_BOARD_KEY_PREFIX + format;
        PointsBoard pointsBoard = new PointsBoard();
        if (isCurrent) {
            pointsBoard = queryMyCurrentRank(key);
        } else {
            pointsBoard = queryMyHistoryRank(pointsBoardQuery.getSeason());
        }

        List<PointsBoard> list = null;
        if (isCurrent) {
            list = queryCurrentBoard(key, pointsBoardQuery.getPageNo(), pointsBoardQuery.getPageSize());
        } else {
            list = queryHistoryBoard(pointsBoardQuery);
        }


        //Stream流处理集合数据
        Set<Long> uIds = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> userDTOS = userClient.queryUserByIds(uIds);
        Map<Long, String> userMap = userDTOS.stream()
                .collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));


        List<PointsBoardItemVO> boardItemVOList = new ArrayList<>();
        for (PointsBoard board : list) {
            PointsBoardItemVO itemVO = new PointsBoardItemVO();
            itemVO.setRank(board.getRank());
            String name = userMap.get(board.getUserId());
            itemVO.setName(name);
            itemVO.setPoints(board.getPoints());
            boardItemVOList.add(itemVO);
        }
        PointsBoardVO vo = new PointsBoardVO();
        vo.setRank(pointsBoard.getRank());
        vo.setPoints(pointsBoard.getPoints());
        vo.setBoardList(boardItemVOList);
        return null;
    }

    public List<PointsBoard> queryCurrentBoard(String key, Integer pageNo, Integer pageSize) {
        long start = (long) (pageNo - 1) * pageSize;
        long end = start + pageSize - 1;
        Set<ZSetOperations.TypedTuple<String>> currentBoard = redisTemplate
                .opsForZSet().reverseRangeWithScores(key, start, end);
        if (CollUtils.isEmpty(currentBoard)) {
            return CollUtils.emptyList();
        }
        List<PointsBoard> list = new ArrayList<>();
        int rank = (int) (start + 1);
        for (ZSetOperations.TypedTuple<String> stringTypedTuple : currentBoard) {
            PointsBoard pointsBoard = new PointsBoard();
            Long userId = Long.getLong(stringTypedTuple.getValue());
            Integer score = stringTypedTuple.getScore().intValue();
            pointsBoard.setUserId(userId);
            pointsBoard.setPoints(score);
            pointsBoard.setRank(rank++);
            list.add(pointsBoard);
        }

        return list;
    }

    private PointsBoard queryMyCurrentRank(String key) {
        Long userId = UserContext.getUser();
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId.toString());

        PointsBoard pointsBoard = new PointsBoard();
        pointsBoard.setPoints(score == null ? 0 : score.intValue());
        pointsBoard.setRank(rank == null ? 0 : rank.intValue());
        return pointsBoard;
    }

    private List<PointsBoard> queryHistoryBoard(PointsBoardQuery pointsBoardQuery) {
        //todo
        return null;
    }

    private PointsBoard queryMyHistoryRank(Long season) {
        //todo
        return null;
    }
}
