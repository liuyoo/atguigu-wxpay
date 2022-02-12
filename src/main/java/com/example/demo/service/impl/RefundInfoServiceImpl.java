package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.OrderInfo;
import com.example.demo.entity.RefundInfo;
import com.example.demo.enums.wxpay.WxRefundStatus;
import com.example.demo.mapper.RefundInfoMapper;
import com.example.demo.service.OrderInfoService;
import com.example.demo.service.RefundInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.util.OrderNoUtils;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {

    @Resource
    private OrderInfoService orderInfoService;

    @Override
    public RefundInfo createRefundByOrderNo(String orderNo, String reason) {

        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo());
        refundInfo.setTotalFee(orderInfo.getTotalFee());
        refundInfo.setRefund(orderInfo.getTotalFee());
        refundInfo.setReason(reason);

        baseMapper.insert(refundInfo);

        return refundInfo;
    }

    /**
     * 记录退款记录
     * @param content
     */
    @Override
    public void updateRefund(String content) {
        //将json字符串转换成Map
        Gson gson = new Gson();
        Map<String,String> resultMap = gson.fromJson(content, HashMap.class);

        //根据退款单编号修改退款单
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no",resultMap.get("out_refund_no"));

        //设置要修改的字段
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundId(resultMap.get("refund_id"));//微信支付退款单号

        /**
         * 该函数在“查询退款”、“申请退款”、“退款通知”中都有用到。他们的API略有不同
         */
        //查询退款和申请退款中的返回参数
        if(resultMap.get("status" )!= null){
            refundInfo.setRefundStatus(resultMap.get("status"));
            refundInfo.setContentReturn(content);//将全部响应结果存入content字段
        }
        //退款通知 回调中的回调参数
        if(resultMap.get("refund_status")!=null){
            refundInfo.setRefundStatus(resultMap.get("refund_status"));
            refundInfo.setContentNotify(content);//将全部响应结果存入content字段
        }


        //更新退款单
        baseMapper.update(refundInfo,queryWrapper);

    }

    @Override
    public List<RefundInfo> queryRenfundByDuration(int minute) {
        Instant instant = Instant.now().minus(Duration.ofDays(minute));

        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("create_time", instant);
        queryWrapper.eq("refund_status", WxRefundStatus.PROCESSING.getType());
        List<RefundInfo> refundInfoList = baseMapper.selectList(queryWrapper);
        return refundInfoList;
    }
}
