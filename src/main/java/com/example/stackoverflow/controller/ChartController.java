package com.example.stackoverflow.controller;

import com.example.stackoverflow.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/Chart")
public class ChartController {
    private final DataService dataService;

    //搜索keyword相关的问题
    @GetMapping("/search")
    public int search(@RequestParam String keyword) {
        return dataService.search(keyword);
    }

    //基本数据信息，界面1
    @GetMapping("/getDataSize")
    public Map<String, Long> getDataSize() {
        return dataService.getDataSize();
    }//数据大小，用横向的柱状图表示
    @GetMapping("/getUsersDistribution")
    public Map<String, Integer> getUsersDistribution() {
        return dataService.getUsersDistribution();
    }//用户得分的区间，用竖向的柱状图表示
    @GetMapping("/getQuestionsDistribution")
    public Map<String, Integer> getQuestionsDistribution() {
        return dataService.getQuestionsDistribution();
    }//问题得分的区间，用竖向的柱状图表示，和getAnswersDistribution()在同一个图标中展示
    @GetMapping("/getAnswersDistribution")
    public Map<String, Integer> getAnswersDistribution() {
        return dataService.getAnswersDistribution();
    }//回答得分的区间，用竖向的柱状图表示，和getQuestionsDistribution()在同一个图标中展示

    //一下几个方法的返回值均为Map<String, Integer>，话题（字符串），对应的数量（整型），界面2
    @GetMapping("/getTopics")
    public Map<String, Integer> getTopics(@RequestParam int n) {
        return dataService.getTopics(n);
    }
    //用户常讨论的话题
    @GetMapping("/getProTopics")
    public Map<String, Integer> getProTopics(@RequestParam int n) {
        return dataService.getProTopics(n);
    }
    //专业用户常讨论的话题，和上一个方法画在同一个柱状图中
    @GetMapping("/getErrors")
    public Map<String, Integer> getErrors(@RequestParam int n) {
        return dataService.getErrors(n);
    }
    //分析出的错误的类型，用饼图表示

    //界面三，分析答案的得分情况
    @GetMapping("/getAverageUserScores")
    public Map<String, Double> getAverageUserScores() {
        return dataService.getAverageUserScores();
    }
    /*返回值如下：
    {
        "avgUserScore": 487.06207809332625,
        "avgUserScoreAbove10": 1621.3896759485426
    }表示所有回答的用户的平均得分和回答的答案评分大于10的用户的平均得分，用柱状图表示
    * */
    @GetMapping("/calculateResponseTimeMetrics")
    public Map<String, Map<String, Double>> calculateResponseTimeMetrics() {
        return dataService.calculateResponseTimeMetrics();
    }
    /*
    {
      "3 days": {
        "simpleAvg": 3.106913758615857,
        "proAvg": 2.323471263590021
      },
      "1 week": {
        "simpleAvg": 5.555086339408389,
        "proAvg": 4.096564254488161
      },
      "1 month": {
        "simpleAvg": 14.119789118013388,
        "proAvg": 11.931148755668382
      },
      "3 months": {
        "simpleAvg": 35.08568999389759,
        "proAvg": 41.456193950593686
      },
      "6 months": {
        "simpleAvg": 73.9094748865927,
        "proAvg": 115.83549670213428
      },
      "1 year": {
        "simpleAvg": 180.18190056547826,
        "proAvg": 315.86408203399355
      }
    }表示回复答案与问题发布的时间间隔，用折线图表示
    * */
    @GetMapping("/getAverageScoresByLengthRange")
    public Map<String, Double> getAverageScoresByLengthRange() {
        return dataService.getAverageScoresByLengthRange();
    }
    /*
    {
      "0-1999": 1.9001939609981129,
      "2000-3999": 3.0210865274748104,
      "4000-5999": 3.738537794299876,
      "6000-7999": 4.290513833992095,
      "8000-9999": 4.908108108108108,
      "10000-11999": 5.75,
      "12000-13999": 5.2407407407407405,
      "14000-15999": 2.125
    }表示答案长度与答案评分的关系，用柱状图融合折线图表示
    * */

    @GetMapping("/answerByUser")
    public Map<String, Double> getAnswerByUser() {
        return dataService.getAvgAnswerScoresByUserScoreRange();
    }

    @GetMapping("/answerByTime")
    public Map<String, Double> getAnswerByTime() {
        return dataService.getAvgAnswerScoresByTimeRange();
    }

    @GetMapping("/answerByLength")
    public Map<String, Double> getAnswerByLength() {
        return dataService.getAvgAnswerScoresByLengthRange();
    }
}
