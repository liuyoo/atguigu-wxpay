package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_order_info") //表名和实体类名的映射（多了t_） 使之对应
//mybatis-plus中属性名字和列名默认进行驼峰和下划线转换
public class OrderInfo  extends BaseEntity{

    private String title;//订单标题 title

    private String orderNo;//商户订单编号   order_no

    private Long userId;//用户id   user_id

    private Long productId;//支付产品id

    private Integer totalFee;//订单金额(分)

    private String codeUrl;//订单二维码连接

    private String orderStatus;//订单状态
}
