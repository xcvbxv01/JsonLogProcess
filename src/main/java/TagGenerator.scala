package temp

import com.test.taggen.ReviewTags
import org.apache.spark.{SparkConf, SparkContext}

object TagGenerator {
    def main(args: Array[String])= {

        val conf = new SparkConf().setMaster("local[*]").setAppName("tagGen");
        val sc = new SparkContext(conf);

        val poi_tags = sc.textFile("file:///D:\\share\\project\\data\\temptags.txt")

        val poi_taglist = poi_tags.map(e=>e.split("\t")).filter(e=>e.length == 2).           //按照/t切割，过滤不等于2的行

                map(e => e(0) -> ReviewTags.extractTags(e(1))).                                       // -> 映射形成tuple(id,values) ==> // 77287793 -> 音响效果好,干净卫生,服务热情
                filter(e=> e._2.length > 0).                                                        //过滤掉评论串为空的
                map(e=> e._1 -> e._2.split(",")).                                           //切割，映射成tiple ==> // 77287793 -> [音响效果好,干净卫生,服务热情]
                flatMapValues(e=>e).                                                                //压扁  ==> // 77287793 -> 音响效果好 , 77287793 -> 干净卫生, 77287793 -> 服务热情
                map(e=> (e._1,e._2)->1).                                                            //映射 ==> // (77287793,音响效果好)->1,(77287793,干净卫生)->1,(77287793,服务热情)->1
                reduceByKey(_+_).                                                                   //按key聚合 ==> // (77287793,音响效果好)->340 , (77287793,干净卫生)->125 ,(77287793,服务热情)-> 20
                // 使用列表的意义在于：将key相桶的value可以直接聚合到一个列表中
                map(e=> e._1._1 -> List((e._1._2,e._2))).                                           //映射 ==> // 77287793->List(音响效果好,340), 77287793 -> List(干净卫生,125) ...
                // ::: 的意义就是集合的累加聚合
                reduceByKey(_:::_).                                                                 //聚合 ==> //77287793->List((音响效果好,340),(干净卫生,125),(..))
                map(
                    e => e._1 -> e._2.sortBy(_._2).                     //这些只是对value的操作==> 排序，倒序，取出前10，做映射，输出
                            reverse.take(10).
                            map(a=> a._1 + ":" + a._2.toString).        //将List<Tuple(Strint,String) 变成 List<String> ==>  List(音响效果好:340, 干净卫生:125, ..)
                            mkString(",")                               //将List<String>,以‘,’进行连接，作为字符串输出 ==> 音响效果好:340, 干净卫生:125, ...
                )                                                       //最终返回元组 ==>  77287793-> 音响效果好:340, 干净卫生:125, ...

        poi_taglist.map(e=>e._1+"\t"+e._2).                             //将元组变成字符串，然后保存成文本 ==> "77287793 \t 音响效果好:340, 干净卫生:125, ..."
                saveAsTextFile("file:///D:\\share\\project\\data\\temptagsOutPut.txt")
    }
}