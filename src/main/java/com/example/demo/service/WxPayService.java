package com.example.demo.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

public interface WxPayService {
    Map<String,Object> nativePay(Long productId) throws IOException;

    void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException;

    void cancelOrder(String orderNO) throws Exception;

    String queryOrder(String orderNo) throws Exception;

    void checkOrderStatus(String orderNo) throws Exception;

    void refund(String orderNo, String reason) throws Exception;

    String queryRefund(String refundNo) throws Exception;

    void processRefund(Map<String, Object> bodyMap) throws GeneralSecurityException;

    void checkRefundStatus(String refundNo) throws Exception;
}
