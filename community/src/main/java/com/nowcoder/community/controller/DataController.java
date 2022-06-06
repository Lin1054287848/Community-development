package com.nowcoder.community.controller;

import com.nowcoder.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataController {

    @Autowired
    private DataService dataService;

    //统计页面
    @RequestMapping(path = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage(){

        return "/site/admin/data";
    }

    //统计网站UV
    @RequestMapping(path="/data/uv", method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model){ //@DateTimeFormat(pattern = "yyyy-MM-dd")将输入日期的格式进行转换
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);
        model.addAttribute("uvStartData", start);//统计结束后，将之前输入的开始时间和结束时间又传入模板，用于开始和结束的默认显示
        model.addAttribute("uvEndData", end);
        return "forward:/data";
    }

    //统计活跃用户
    @RequestMapping(path="/data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model){ //@DateTimeFormat(pattern = "yyyy-MM-dd")将输入日期的格式进行转换
        long dau = dataService.calculateDAU(start, end);
        model.addAttribute("dauResult", dau);
        model.addAttribute("dauStartData", start);//统计结束后，将之前输入的开始时间和结束时间又传入模板，用于开始和结束的默认显示
        model.addAttribute("dauEndData", end);
        return "forward:/data";
    }
}
