package com.parca.yedekparca.config;

import java.util.Arrays;

public final class DemoOfferDistribution {
    private DemoOfferDistribution() { }
    public static int[] counts() {
        int[] result=new int[300];
        int[][] groups={{80,1},{60,2},{60,3},{40,4},{40,5},{20,8}};
        int index=0;
        for(int[] group:groups)for(int i=0;i<group[0];i++)result[index++]=group[1];
        return result;
    }
    public static int total(){return Arrays.stream(counts()).sum();}
}
