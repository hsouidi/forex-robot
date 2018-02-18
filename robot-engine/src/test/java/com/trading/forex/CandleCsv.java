package com.trading.forex;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandleCsv {

    @CsvBindByPosition(position = 0)
    private String date;
    @CsvBindByPosition(position = 1)
    private String time;
    @CsvBindByPosition(position = 2)
    private Double open;
    @CsvBindByPosition(position = 3)
    private Double high;
    @CsvBindByPosition(position = 4)
    private Double low;
    @CsvBindByPosition(position = 5)
    private Double close;
    @CsvBindByPosition(position = 6)
    private Double volume;

    public long epouch(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        try {
            return simpleDateFormat.parse(date+" "+time).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }



}


