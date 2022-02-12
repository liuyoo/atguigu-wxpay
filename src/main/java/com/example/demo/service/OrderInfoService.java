package com.example.demo.service;

import com.example.demo.entity.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.enums.OrderStatus;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {
    OrderInfo createOrderByProductId(Long productId);
    OrderInfo saveCodeUrl(String orderNo, String codeUrl);

    List<OrderInfo> ListOrderByCreateTimeDesc();

    void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus);

    String getOrderStatus(String orderNo);

    List<OrderInfo> getNoPayOrderByDuration(int minutes);

    OrderInfo getOrderByOrderNo(String orderNo);
}
