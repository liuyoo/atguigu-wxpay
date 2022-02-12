package com.example.demo.service.impl;

import com.example.demo.entity.PaymentInfo;
import com.example.demo.enums.PayType;
import com.example.demo.mapper.PaymentInfoMapper;
import com.example.demo.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Override
    public void createPaymentInfo(String plainText) {
        log.info("记录支付日志");
        Gson gson = new Gson();
        HashMap plainTextMap = gson.fromJson(plainText, HashMap.class);

        String orderNo = (String) plainTextMap.get("out_trade_no");//商户订单号
        String transactionId = (String) plainTextMap.get("transaction_id");//微信支付订单号
        String tradeType = (String) plainTextMap.get("trade_type");//支付类型
        String tradeState = (String) plainTextMap.get("trade_state");//交易状态
        Map<String,Object> amount = (Map) plainTextMap.get("amount");//用户实际支付金额
        Integer payerTotal =  (int)amount.get("payer_total");

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType(tradeType);//小程序支付、扫码支付等多种前端
        paymentInfo.setTradeState(tradeState);
        paymentInfo.setPayerTotal(payerTotal);
        paymentInfo.setContent(plainText);

        baseMapper.insert(paymentInfo);
    }
}
