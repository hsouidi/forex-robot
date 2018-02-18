package com.trading.forex.oanda.service;

import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.order.*;
import com.oanda.v20.transaction.MarketOrderTransaction;
import com.oanda.v20.transaction.OrderFillTransaction;
import com.oanda.v20.transaction.StopLossDetails;
import com.oanda.v20.transaction.TakeProfitDetails;
import com.trading.forex.common.model.Symbol;
import com.trading.forex.common.model.Way;
import com.trading.forex.connector.exceptions.ConnectorTechnicalException;
import com.trading.forex.connector.model.OrderCreateResponse;
import com.trading.forex.connector.model.OrderStatus;
import com.trading.forex.connector.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.trading.forex.common.utils.AlgoUtils.normalize;


@Service
@Slf4j
public class OandaOrderServiceImpl implements OrderService {


    @Autowired
    private Context context;

    @Autowired
    private AccountID accountID;

    @Override
    public OrderCreateResponse requestOrder(Symbol symbol, Long unit, Double stopLoss, Double takeProfit){
        OrderCreateRequest orderCreateRequest = new OrderCreateRequest(accountID);
        MarketOrderRequest marketorderrequest = new MarketOrderRequest();
        if (stopLoss != null) {
            final StopLossDetails stopLossDetails = new StopLossDetails();
            final double normalizeStoppLoss = normalize(stopLoss, symbol);
            stopLossDetails.setPrice(normalizeStoppLoss);
            marketorderrequest.setStopLossOnFill(stopLossDetails);
        }
        if (takeProfit != null) {
            final TakeProfitDetails takeProfitDetails = new TakeProfitDetails();
            double normalizeTakeProfit = normalize(takeProfit, symbol);
            takeProfitDetails.setPrice(normalizeTakeProfit);
            marketorderrequest.setTakeProfitOnFill(takeProfitDetails);
        }
        // Populate the body parameter fields
        marketorderrequest.setInstrument(symbol.getBrokerValue());
        marketorderrequest.setUnits(unit);


        // Attach the body parameter to the request
        orderCreateRequest.setOrder(marketorderrequest);
        // Execute the request and obtain the response object
        log.info(" Order  symbol {} unit {} , stop loss {} , takeProfit {}", symbol, unit, marketorderrequest.getStopLossOnFill(), marketorderrequest.getTakeProfitOnFill());
        try {
            return toOrderCreateResponse(context.order.create(orderCreateRequest),symbol,unit,stopLoss,takeProfit);
        } catch (RequestException| ExecuteException  e ) {
            throw new ConnectorTechnicalException(e);
        }
    }

    private OrderCreateResponse toOrderCreateResponse(com.oanda.v20.order.OrderCreateResponse response,Symbol symbol, Long unit, Double stopLoss, Double takeProfit ) {
        final OrderFillTransaction orderFillTransaction = response.getOrderFillTransaction();
        final MarketOrderTransaction marketOrderTransaction = (MarketOrderTransaction) response.getOrderCreateTransaction();
        if (orderFillTransaction == null) {
            return OrderCreateResponse.builder()
                    .orderStatus(OrderStatus.KO)
                    .comment(response.getOrderCancelTransaction().getReason().name())
                    .symbol(symbol)
                    .stopLoss(stopLoss)
                    .takeProfit(takeProfit)
                    .way(unit > 0 ? Way.BUY : unit < 0 ? Way.SELL : null)
                    .build();
        }else {
            return OrderCreateResponse.builder()
                    .tradeID(orderFillTransaction.getTradeOpened().getTradeID().toString())
                    .symbol(Symbol.fromBrokerValue(orderFillTransaction.getInstrument().toString()))
                    .price(orderFillTransaction.getPrice().doubleValue())
                    .takeProfit(marketOrderTransaction.getTakeProfitOnFill()!=null?marketOrderTransaction.getTakeProfitOnFill().getPrice().doubleValue():null)
                    .stopLoss(marketOrderTransaction.getStopLossOnFill()!=null?marketOrderTransaction.getStopLossOnFill().getPrice().doubleValue():null)
                    .way(orderFillTransaction.getUnits().doubleValue() > 0 ? Way.BUY : orderFillTransaction.getUnits().doubleValue() < 0 ? Way.SELL : null)
                    .orderStatus(OrderStatus.OK)
                    .build();
        }
    }
}
