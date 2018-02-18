package com.trading.forex.economic.repository;

import com.trading.forex.economic.entity.EconomicCalendarEntity;
import com.trading.forex.common.model.Importance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.Date;
import java.util.List;

/**
 * Created by hsouidi on 11/21/2017.
 */
public interface EconomicCalendarRepository extends JpaRepository<EconomicCalendarEntity, EconomicCalendarEntity.EconomicCalendarID>
{

    @Query("select u from EconomicCalendarEntity u  where economicCalendarID.eventDate between ?1 and ?2  and importance in ?3 ")
    List<EconomicCalendarEntity> findAllByEventDateAndImportance(Date begin, Date end, List<Importance> importance);
}
