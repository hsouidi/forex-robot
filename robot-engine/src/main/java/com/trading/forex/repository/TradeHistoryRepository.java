package com.trading.forex.repository;

import com.trading.forex.entity.TradeHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

/**
 * Created by hsouidi on 05/09/2017.
 */
public interface TradeHistoryRepository extends JpaRepository<TradeHistoryEntity, String> {

    List<TradeHistoryEntity>  findByResultIsNull();
    List<TradeHistoryEntity>  findByTradeDateBetween (Date start, Date end);
    List<TradeHistoryEntity>  findByResultIsNotNullAndTradeDateAfterOrderByTradeDateDesc(Date start);

}
