package com.tianji.learning.task;

import com.tianji.common.utils.CollUtils;
import com.tianji.learning.constant.LearningConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.tianji.learning.constant.LearningConstants.POINTS_BOARD_TABLE_PREFIX;
import static com.tianji.learning.constant.RedisConstants.POINTS_BOARD_KEY_PREFIX;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.learning.task
 * @Project：tianji
 * @Date：2024/3/26 9:24
 * @Filename：PointBoardPersistentHandler
 */
@Component
@RequiredArgsConstructor
public class PointBoardPersistentHandler {

    private final IPointsBoardService pointsBoardService;
    private final IPointsBoardSeasonService pointsBoardSeasonService;

    //@Scheduled(cron = "0 0 3 1 * ?")
    @XxlJob("createTableJob")
    public void createLastSeasonBoard(){
        LocalDate time = LocalDate.now().minusMonths(1);
        PointsBoardSeason one = pointsBoardSeasonService
                .lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .one();
        if(one == null){
            return;
        }
        pointsBoardSeasonService.createPointBoard(one.getId());
    }

    @XxlJob("savePointsBoard2DB")
    public void savePointsBoard2DB(){
        LocalDate time = LocalDate.now().minusMonths(1);
        PointsBoardSeason one = pointsBoardSeasonService
                .lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .one();
        if(one == null){
            return;
        }
        String tableName = POINTS_BOARD_TABLE_PREFIX + one.getId();
        TableInfoContext.setInfo(tableName);
        String format = time.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = POINTS_BOARD_KEY_PREFIX + format;
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        int pageNo = index + 1;
        int pageSize = 1000;
        //分页查询
        while(true) {
            //查询redis
            List<PointsBoard> list = pointsBoardService.queryCurrentBoard(key, pageNo, pageSize);
            if(CollUtils.isEmpty(list)){
                break;
            }
            pageNo+=total;
            list.forEach(pointsBoard -> {
                pointsBoard.setId(Long.valueOf(pointsBoard.getRank()));
                pointsBoard.setRank(null);
                pointsBoard.setSeason(null);
            });
            //存储到db
            pointsBoardService.saveBatch(list);
        }
        TableInfoContext.remove();
    }
}
