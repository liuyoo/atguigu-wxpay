package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.OrderInfo;
import com.example.demo.entity.Product;
import com.example.demo.enums.OrderStatus;
import com.example.demo.mapper.OrderInfoMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.util.OrderNoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private OrderInfoMapper orderInfoMapper;

    /**
     * 创建订单
     * @param productId
     * @return
     */
    @Override
    public OrderInfo createOrderByProductId(Long productId) {

        //查找已存在但未支付的订单
        OrderInfo orderInfo = this.getNoPayOrderByProductId(productId);
        if(orderInfo != null){//对象不为空
            return orderInfo;
        }

        Product product = productMapper.selectById(productId);//可以调用productService吗

        //生成订单
        orderInfo = new OrderInfo();//同一个变量 再次赋值
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(product.getPrice());//单位是 分
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());

        orderInfoMapper.insert(orderInfo);
        //baseMapper.insert(orderInfo); 效果相同  不用注入对象 更简单

        return orderInfo;
    }

    /**
     * 保存二维码
     * @param orderNo
     * @param codeUrl
     * @return
     */
    @Override
    public OrderInfo saveCodeUrl(String orderNo, String codeUrl) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no",orderNo);

        OrderInfo orderInfo =new OrderInfo();//新的对象？？
        orderInfo.setCodeUrl(codeUrl);

        orderInfoMapper.update(orderInfo,queryWrapper);

        return orderInfo;
    }

    /**
     * 获得所有订单 倒序
     * @return
     */
    @Override
    public List<OrderInfo> ListOrderByCreateTimeDesc() {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        List list = baseMapper.selectList(queryWrapper);
        return list;
    }

    /**
     * 根据订单号更新订单状态
     * @param orderNo
     * @param orderStatus
     */
    @Override
    public void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus) {
        log.info("更新订单状态==》{}",orderStatus.getType());
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no",orderNo);//查询条件

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(orderStatus.getType());
        baseMapper.update(orderInfo,queryWrapper);

    }

    /**
     * 根据订单号得到订单状态
     * @param orderNo
     * @return
     */
    @Override
    public String getOrderStatus(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no",orderNo);
        OrderInfo orderInfo = baseMapper.selectOne(queryWrapper);
        if(orderInfo==null){
            return null;
        }
        return orderInfo.getOrderStatus();
    }

    /**
     * 查询创建超过minutes分钟并且未支付的订单
     * @param minutes
     * @return
     */
    @Override
    public List<OrderInfo> getNoPayOrderByDuration(int minutes) {

        Instant instant = Instant.now().minus(Duration.ofMinutes(minutes));//当前的时间减去5

        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_status",OrderStatus.NOTPAY.getType());
        queryWrapper.le("create_time",instant);

        List<OrderInfo> orderInfoList = baseMapper.selectList(queryWrapper);

        return orderInfoList;
    }

    /**
     * 根据订单号查询订单
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo getOrderByOrderNo(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no",orderNo);
        OrderInfo orderInfo = baseMapper.selectOne(queryWrapper);
        return orderInfo;
    }

    /**
     * 获取未支付订单
     * @param productId
     * @return
     */
    private OrderInfo getNoPayOrderByProductId(Long productId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id",productId);//组装where条件的语句
        queryWrapper.eq("order_status",OrderStatus.NOTPAY.getType());
//        queryWrapper.eq("user_id",userId); //如果有权限认证的话
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        return orderInfo;
    }
}
