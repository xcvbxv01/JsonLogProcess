package com.test.taggen;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;
import java.util.*;

public class TagGenJava {

    public static void main(String [] args)
    {
        SparkConf conf = new SparkConf().setMaster("local[*]").setAppName("tagGen");
        JavaSparkContext sc = new JavaSparkContext(conf);
        JavaRDD<String> rdd = sc.textFile("file:///D:\\share\\project\\data\\temptags.txt");


        JavaRDD<String[]> rdd1 = rdd.map( s -> s.split("\t"));

        JavaRDD<String[]> rdd2 = rdd1.filter(s -> s.length == 2);

        JavaPairRDD<String, String> rdd3 = rdd2.mapToPair(s -> new Tuple2<>(s[0], ReviewTags.extractTags(s[1])));

        JavaPairRDD<String, String> rdd4 = rdd3.filter(s -> s._2.length() > 0);

        JavaPairRDD<String, String[]> rdd5 = rdd4.mapToPair(s -> new Tuple2<>(s._1, s._2.split(",")));

        JavaPairRDD<String, String> rdd6 = rdd5.flatMapValues(s -> Arrays.asList(s));

        JavaPairRDD<Tuple2<String, String>, Integer> rdd7 = rdd6.mapToPair(s -> new Tuple2<>(new Tuple2<>(s._1, s._2), 1));

        JavaPairRDD<Tuple2<String, String>, Integer> rdd8 = rdd7.reduceByKey((s1, s2) -> s1 + s2);

        JavaPairRDD<String, List<Tuple2<String, Integer>>> rdd9 = rdd8.mapToPair(s -> new Tuple2<>(s._1._1, Arrays.asList(new Tuple2<>(s._1._2, s._2))));

        JavaPairRDD<String, List<Tuple2<String, Integer>>> rdd10 = rdd9.reduceByKey((s1, s2) -> Arrays.asList(s1.get(0), s2.get(0)));

        JavaPairRDD<String, List<Tuple2<String, Integer>>> rdd11 = rdd10.mapValues(s -> {
            //排序

            s.sort(new Comparator<Tuple2<String, Integer>>() {
                @Override
                public int compare(Tuple2<String, Integer> o1, Tuple2<String, Integer> o2) {
                    return o1._2 > o2._2 ? -1 : 1;
                }
            });
            //取出前10
            int len = 10;
            if(s.size() < 10)
            {
              len = s.size();
            }
            Tuple2<String, Integer> [] arr = new Tuple2[len];
            for (int i = 0; i <len ; i++) {
                arr[i] = s.get(i);
            }
            return Arrays.asList(arr);
        });

        JavaPairRDD<String, String> rdd12 = rdd11.mapValues(s -> {
            String str = "";
            Tuple2<String, Integer>[] array = (Tuple2<String, Integer>[]) s.toArray();
            for (Tuple2<String, Integer> t : array) {
                str += t._1 + ":" + t._2 + ",";
            }
            str = str.substring(0,str.length()-1);
            return str;
        });

        JavaRDD<String> rdd13 = rdd12.map(e -> e._1 + "\t" + e._2);

        rdd13.collect().forEach(s -> System.out.println(s));

        rdd13.saveAsTextFile("file:///D:\\share\\project\\data\\temptagsOutPut");
    }
}
