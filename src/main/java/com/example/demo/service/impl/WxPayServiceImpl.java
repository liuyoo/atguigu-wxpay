package com.example.demo.service.impl;

import com.example.demo.config.WxPayConfig;
import com.example.demo.entity.OrderInfo;
import com.example.demo.entity.PaymentInfo;
import com.example.demo.entity.RefundInfo;
import com.example.demo.enums.OrderStatus;
import com.example.demo.enums.wxpay.WxApiType;
import com.example.demo.enums.wxpay.WxNotifyType;
import com.example.demo.enums.wxpay.WxRefundStatus;
import com.example.demo.enums.wxpay.WxTradeState;
import com.example.demo.service.OrderInfoService;
import com.example.demo.service.PaymentInfoService;
import com.example.demo.service.RefundInfoService;
import com.example.demo.service.WxPayService;
import com.google.gson.Gson;
import com.mysql.cj.util.StringUtils;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private CloseableHttpClient wxPayClient;//注入WxPayConfig中的getWxPayClient方法

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Resource
    private RefundInfoService refundInfoService;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 创建订单 调用Native支付接口
     * @param productId
     * @return code_url 和 订单号
     * @throws IOException
     */
    @Override
    public Map<String, Object> nativePay(Long productId) throws IOException {
        log.info("生成订单");

        //生成订单
        OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId);
        String codeUrl = orderInfo.getCodeUrl();
        if(orderInfo!=null && !StringUtils.isNullOrEmpty(codeUrl)){
            Map<String, Object> result = new HashMap<>();
            result.put("codeUrl",codeUrl);
            result.put("orderNo",orderInfo.getOrderNo());

            return result;
        }


        log.info("调用统一下单API");
        /**
         * 调用统一下单API
         */

//        HttpPost httpPost = new HttpPost("https://api.mch.weixin.qq.com/v3/pay/transactions/native");
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType()));
        // 请求body参数
        Gson gson = new Gson();
        Map paramsMap = new HashMap();
        paramsMap.put("mchid",wxPayConfig.getMchId());
        paramsMap.put("appid",wxPayConfig.getAppid());
        paramsMap.put("description",orderInfo.getTitle());
        paramsMap.put("out_trade_no",orderInfo.getOrderNo());
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));//拼接主机和通知地址

        Map amountMap = new HashMap();
        amountMap.put("total",orderInfo.getTotalFee());
        amountMap.put("currency","CNY");

        paramsMap.put("amount",amountMap);

        String jsonParams = gson.toJson(paramsMap);
        log.info("请求参数：" + jsonParams);

        StringEntity entity = new StringEntity(jsonParams,"utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        /**
         * 完成签名并执行请求
         */
        //httpClient在WxPayConfig中获得
        CloseableHttpResponse response = wxPayClient.execute(httpPost);

        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());//响应体
            int statusCode = response.getStatusLine().getStatusCode();//响应状态码
            if (statusCode == 200) { //处理成功
                log.info("成功,返回结果= " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("成功");
            } else {
                System.out.println("Native下单失败,响应码 = " + statusCode+ ",返回结果 = " + bodyAsString);
                throw new IOException("request failed");
            }

            //响应结果
            Map<String,String> resultMap = gson.fromJson(bodyAsString, HashMap.class);
            //二维码
            codeUrl = resultMap.get("code_url");

            //保存二维码
            orderInfoService.saveCodeUrl(orderInfo.getOrderNo(), codeUrl);

            //返回二维码
            Map<String, Object> result = new HashMap<>();
            result.put("codeUrl",codeUrl);
            result.put("orderNo",orderInfo.getOrderNo());

            return result;
        } finally {
            response.close();
        }



    }

    /**
     * 对称解密
     * @param bodyMap
     */
    @Override
    public void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("处理订单");
        //解密
        String plainText = decryptFromResource(bodyMap);
        //将明文json转为map
        Gson gson = new Gson();
        HashMap plainTextMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainTextMap.get("out_trade_no");

        //“对业务数据进行状态检查和处理之前，采用数据锁进行并发控制，以避免函数重入造成的数据混乱”
        //尝试获取锁：成功获取则立即返回true，获取失败则立即返回false，不必一直等待锁的释放。synchronize是一直等待
        if(lock.isLocked()){
            try {
                //处理重复的通知
                //接口调用的幂等性：无论接口调用多少次，产生的结果是一致的
                String orderStatus = orderInfoService.getOrderStatus(orderNo);
                if(!OrderStatus.NOTPAY.getType().equals(orderStatus)){
                    return;
                }

                //模拟通知并发
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //更新数据库订单状态
                orderInfoService.updateStatusByOrderNo(orderNo,OrderStatus.SUCCESS);

                //记录支付日志
                paymentInfoService.createPaymentInfo(plainText);
            } finally {
                lock.unlock();
            }
        }


    }

    private String decryptFromResource(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("密文解密");
        //通知数据  可参考https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_5.shtml
        Map<String, String> resourceMap = (Map)bodyMap.get("resource");
        String ciphertext = resourceMap.get("ciphertext");
        String nonce = resourceMap.get("nonce");
        String associatedData = resourceMap.get("associated_data");

        log.info("密文==》{}", ciphertext);
        AesUtil aesUtil = new AesUtil(wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));//对称加密的密钥
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),//需要参数是byte[]类型
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);
        log.info("明文==》{}",plainText);
        return plainText;
    }

    /**
     * 用户取消订单
     * @param orderNO
     */
    @Override
    public void cancelOrder(String orderNO) throws Exception {
        //调用微信支付的关单接口
        this.closeOrder(orderNO);
        //更新商户端的订单状态
        orderInfoService.updateStatusByOrderNo(orderNO,OrderStatus.CANCEL);
    }

    /**
     * 查单接口的调用（向微信发送请求）
     * @param orderNo
     */
    @Override
    public String queryOrder(String orderNo) throws Exception {
        log.info("查单接口的调用===》{}",orderNo);
        String url = String.format(WxApiType.ORDER_QUERY_BY_NO.getType(),orderNo);
        url = wxPayConfig.getDomain().concat(url).concat("?mchid=").concat(wxPayConfig.getMchId());//商户号要作为查询字符串，连接在url后面

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept","application/json");

        CloseableHttpResponse response = wxPayClient.execute(httpGet);
        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());//响应体
            int statusCode = response.getStatusLine().getStatusCode();//响应状态码
            if (statusCode == 200) { //处理成功
                log.info("成功,返回结果= " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("成功");
            } else {
                System.out.println("查询订单失败,响应码 = " + statusCode+ ",返回结果 = " + bodyAsString);
                throw new IOException("request failed");
            }

            return bodyAsString;

        } finally {
            response.close();
        }

    }

    /**
     * 根据订单号查询微信支付查单接口，核实订单状态
     *      如果订单已支付，则更新商户端订单状态
     *      如果订单未支付，则调用关单接口，并更新商户端订单状态
     * @param orderNo
     */
    @Override
    public void checkOrderStatus(String orderNo) throws Exception {
        log.warn("根据订单号核实订单状态 === {}", orderNo);

        //调用微信支付查单接口
        String result = this.queryOrder(orderNo);

        Gson gson = new Gson();
        HashMap resultMap = gson.fromJson(result,HashMap.class);

        //获取微信支付端的订单状态
        Object tradeState = resultMap.get("trade_state");

        if(WxTradeState.SUCCESS.getType().equals(tradeState)){
            log.warn("核实订单已支付 ===》 {}",orderNo);
            orderInfoService.updateStatusByOrderNo(orderNo,OrderStatus.SUCCESS);
            paymentInfoService.createPaymentInfo(result);//result与之前解密出来的格式是一样的
        }

        if(WxTradeState.NOTPAY.getType().equals(tradeState)){
            log.warn("核实订单未支付 ===》 {}", orderNo);
            this.closeOrder(orderNo);
            orderInfoService.updateStatusByOrderNo(orderNo,OrderStatus.CLOSED);
        }
    }

    /**
     * 退款
     * @param orderNo
     * @param reason
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void refund(String orderNo, String reason) throws Exception {
        log.info("创建退款单记录");
        //根据订单编号创建退款单
        RefundInfo refundInfo = refundInfoService.createRefundByOrderNo(orderNo,reason);

        log.info("调用退款API");
        //调用API
        String url = wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType());
        HttpPost httpPost = new HttpPost(url);

        //请求body参数
        Gson gson = new Gson();
        Map paramMap = new HashMap();
        paramMap.put("out_trade_no",orderNo);//订单编号
        paramMap.put("out_refund_no",refundInfo.getRefundNo());
        paramMap.put("reason",reason);
        paramMap.put("notify_url",wxPayConfig.getNotifyDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));

        Map amountMap = new HashMap();
        amountMap.put("refund",refundInfo.getRefund());//退款金额
        amountMap.put("total",refundInfo.getTotalFee());//原订单金额
        amountMap.put("currency","CNY");
        paramMap.put("amount",amountMap);

        //将参数转换成json字符串
        String jsonParams = gson.toJson(paramMap);
        log.info("请求参数 ==》 {}", jsonParams);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");//设置请求报文格式
        httpPost.setEntity(entity);//将请求报文放入请求对象
        httpPost.setHeader("Accept","application/json");//设置响应报文格式

        //完成签名并执行请求，并完成验签
        CloseableHttpResponse response = wxPayClient.execute(httpPost);

        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode == 200){
                log.info("退款成功,返回结果 = "+ bodyAsString);
            }else if(statusCode == 204){
                log.info("成功");
            }else{
                throw new RuntimeException("退款异常，响应码 = " + statusCode + "，退款返回结果 = " + bodyAsString);
            }
            //更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo,OrderStatus.REFUND_PROCESSING);//本地状态

            //更新退款单
            //创建的退款单只有初始化信息，需要把响应中的一些信息填进去
            refundInfoService.updateRefund(bodyAsString);
        } finally {
            response.close();

        }
    }

    /**
     * 关单接口的调用
     * @param orderNO
     */
    private void closeOrder(String orderNO) throws Exception {
        log.info("关单接口的调用，订单号 ===》", orderNO);

        //创建远程请求对象
        String url = String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(),orderNO);//占位符传参
        url = wxPayConfig.getDomain().concat(url);
        HttpPost httpPost = new HttpPost(url);

        //组装json请求体
        Gson gson = new Gson();
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("mchid",wxPayConfig.getMchId());
        String jsonParams = gson.toJson(paramMap);
        log.info("请求参数===》",jsonParams);

        //将请求参数设置到请求对象中
        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept","application/json");//想要接收的返回数据的格式

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpPost);

        try {
            int statusCode = response.getStatusLine().getStatusCode();//响应状态码
            if (statusCode == 200) { //处理成功
                log.info("成功200" );
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("成功204");
            } else {
                System.out.println("关闭订单失败,响应码 = " + statusCode);
                throw new IOException("request failed");
            }

        } finally {
            response.close();
        }

    }


    @Override
    public String queryRefund(String refundNo) throws Exception {
        log.info("查询退款接口调用===》{}",refundNo);

        String url = String.format(wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS_QUERY.getType()),refundNo);
        //创建远程Get请求对象
        HttpGet httpGet = new HttpGet();
        httpGet.setHeader("Accept","application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpGet);

        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode == 200){
                log.info("成功，查询退款返回结果=={}",bodyAsString);
            }else if(statusCode == 204){
                log.info("成功");
            }else {
                throw new RuntimeException("查询退款异常，响应码="+statusCode+",查询退款返回结果："+bodyAsString);
            }
            return bodyAsString;
        }  finally {
            response.close();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void checkRefundStatus(String refundNo) throws Exception {
        log.warn("根据退款单号核实退款单状态 ===> {}", refundNo);
        String result = this.queryRefund(refundNo);
        Gson gson = new Gson();
        Map<String,String> resultMap = gson.fromJson(result, HashMap.class);
        String status = resultMap.get("status");
        String orderNo = resultMap.get("out_trade_no");

        if(WxRefundStatus.SUCCESS.getType().equals(status)){
            log.warn("核实订单已退款成功 ===> {}", refundNo);
            //如果确认退款成功，则更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);
            //更新退款单
            refundInfoService.updateRefund(result);
        }

        if (WxRefundStatus.ABNORMAL.getType().equals(status)) {
            log.warn("核实订单退款异常 ===> {}", refundNo);
            //如果确认退款成功，则更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_ABNORMAL);
            //更新退款单
            refundInfoService.updateRefund(result);
        }


    }

    @Override
    public void processRefund(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("退款单");
        //解密
        String plainText = decryptFromResource(bodyMap);
        //将明文json转为map
        Gson gson = new Gson();
        HashMap plainTextMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainTextMap.get("out_trade_no");

        //“对业务数据进行状态检查和处理之前，采用数据锁进行并发控制，以避免函数重入造成的数据混乱”
        //尝试获取锁：成功获取则立即返回true，获取失败则立即返回false，不必一直等待锁的释放。synchronize是一直等待
        if(lock.isLocked()){
            try {
                //处理重复的通知
                //接口调用的幂等性：无论接口调用多少次，产生的结果是一致的
                String orderStatus = orderInfoService.getOrderStatus(orderNo);
                if(!OrderStatus.REFUND_PROCESSING.getType().equals(orderStatus)){
                    return;
                }

                //更新数据库订单状态
                orderInfoService.updateStatusByOrderNo(orderNo,OrderStatus.REFUND_SUCCESS);

                //记录支付日志
                refundInfoService.updateRefund(plainText);

            } finally {
                lock.unlock();
            }
        }
    }




}
