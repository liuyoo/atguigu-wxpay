package com.example.demo.controller;

import com.example.demo.entity.OrderInfo;
import com.example.demo.service.OrderInfoService;
import com.example.demo.vo.R;
import com.example.demo.enums.OrderStatus;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/order-info")
@Api(tags = "订单信息")
@Slf4j
public class OrderInfoController {

    @Resource
    OrderInfoService orderInfoService;

    @GetMapping("/list")
    public R orderList(){
        List<OrderInfo> list = orderInfoService.ListOrderByCreateTimeDesc();
        return R.ok().data("list",list);
    }
    /**
     * 查询
     */
    @GetMapping("query-order-status/{orderNo}")
    public R queryOrderStatus(@PathVariable String orderNo){
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        if(OrderStatus.SUCCESS.getType().equals(orderStatus)){
            return R.ok().setMessage("支付成功");
        }
        return R.ok().setCode(101).setMessage("支付中…………");
    }
}
