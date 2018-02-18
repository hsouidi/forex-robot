package com.trading.forex.model;

import com.trading.forex.common.model.Way;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by hsouidi on 11/28/2017.
 */
@Data
@NoArgsConstructor
public class PivotPointResult implements Serializable {

    private Double r1;
    private Double r2;
    private Double r3;
    private Double s1;
    private Double s2;
    private Double s3;
    private Double pivot ;
    private Double pr1;
    private Double pr2;
    private Double pr3;
    private Double ps1;
    private Double ps2;
    private Double ps3;
    private Map<Double,String> barriers=new LinkedHashMap<>();

    public PivotPointResult(Double r1, Double r2, Double r3, Double s1, Double s2, Double s3, Double pivot, Double pr1, Double pr2, Double pr3, Double ps1, Double ps2, Double ps3) {
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.s1 = s1;
        this.s2 = s2;
        this.s3 = s3;
        this.pivot = pivot;
        barriers.put(s3,BarrierName.S3.name());
        barriers.put(s2,BarrierName.S2.name());
        barriers.put(s1,BarrierName.S1.name());
        barriers.put(pivot,BarrierName.PIVOT.name());
        barriers.put(r1,BarrierName.R1.name());
        barriers.put(r2,BarrierName.R2.name());
        barriers.put(r3,BarrierName.R3.name());

        this.pr1 = pr1;
        this.pr2 = pr2;
        this.pr3 = pr3;
        this.ps1 = ps1;
        this.ps2 = ps2;
        this.ps3 = ps3;
    }

    public Barrier barrier(final Way way, final double price){

        return barrier(way,price,barriers);
    }

    public Barrier barrier(final Way way, final double price,Map<Double,String> barriers){

        final List<Entry<Double,String>> values=new ArrayList(barriers.entrySet());
        if(way==Way.SELL){
            for(int i=0;i<values.size()-2;i++){
                final Entry<Double,String> entry=values.get(i);
                final Entry<Double,String> entryPrevious=values.get(i+1);
                if(price-entryPrevious.getKey()<0){
                    return new Barrier(getKey(entryPrevious),getKey(entry),getValue(entryPrevious),getValue(entry));
                }
            }
        }
        else if(way==Way.BUY){
            for(int i=values.size()-2;i>=0;i--){
                final Entry<Double,String> entry=values.get(i);
                final Entry<Double,String> entryPrevious=values.get(i+1);
                if(price-entry.getKey()>0){
                    return new Barrier(getKey(entry),getKey(entryPrevious),getValue(entry),getValue(entryPrevious));
                }
            }
        }

        return null;
    }

    private static String getValue(Entry<Double,String> entry){
        return entry.getValue()!=null?entry.getValue():null;
    }

    private static Double getKey(Entry<Double,String> entry){
        return entry.getKey()!=null?entry.getKey():null;
    }

    @AllArgsConstructor
    @Getter
    public static class Barrier implements Serializable{
        private Double support;
        private Double resistance;
        private String supportName;
        private String resistanceName;

    }

    public enum BarrierName{
        R1,R2,R3,PIVOT,S3,S2,S1
    }
}
