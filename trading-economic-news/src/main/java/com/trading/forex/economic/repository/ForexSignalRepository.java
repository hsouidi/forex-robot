package com.trading.forex.economic.repository;

import com.trading.forex.economic.entity.ForexSignal;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by hsouidi on 11/21/2017.
 */
public interface ForexSignalRepository
        extends JpaRepository<ForexSignal, ForexSignal.ForexSignalID>
{

}
