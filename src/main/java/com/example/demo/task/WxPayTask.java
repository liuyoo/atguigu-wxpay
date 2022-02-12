package com.example.demo.task;

import com.example.demo.entity.OrderInfo;
import com.example.demo.entity.RefundInfo;
import com.example.demo.service.OrderInfoService;
import com.example.demo.service.RefundInfoService;
import com.example.demo.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class WxPayTask {

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private WxPayService wxPayService;

    @Resource
    private RefundInfoService refundInfoService;

    /**
     * Linux中的，spring引入了。
     *
     * 秒 分  时  日 月  周
     * ？：不指定
     * 日和周不能同时制定
     */
//    @Scheduled(cron = "* * * * * ?")
//    public void task1(){
//        log.info("task1 被执行……");
//    }

    /**
     * 从第0秒开始每隔30秒执行一次，查询创建超过5分钟，并且未支付的订单
     */
//    @Scheduled(cron = "0/30 * * * * ?")
//    public void orderConfirm() throws Exception {
//        log.info("orderConfirm 被执行……");
//
//        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderByDuration(5);
//        for(OrderInfo orderInfo:orderInfoList){
//            String orderNo = orderInfo.getOrderNo();
//            log.warn("超时订单 === {}",orderNo);
//
//            //核实订单状态：调用微信支付查单接口
//            wxPayService.checkOrderStatus(orderNo);
//
//        }
//    }
//
//    @Scheduled(cron = "0/30 * * * * ?")
//    public void refundConfirm() throws Exception {
//        log.info("refundConfirm被执行……");
//        List<RefundInfo> refundInfoList = refundInfoService.queryRenfundByDuration(5);
//
//        for(RefundInfo refundInfo:refundInfoList){
//            String refundNo = refundInfo.getRefundNo();
//            log.info("超时未退款的退款单号===》{}",refundNo);
//            wxPayService.checkRefundStatus(refundNo);
//        }
//
//    }


}
