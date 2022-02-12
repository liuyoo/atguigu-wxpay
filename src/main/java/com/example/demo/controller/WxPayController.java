package com.example.demo.controller;

import com.example.demo.entity.RefundInfo;
import com.example.demo.service.WxPayService;
import com.example.demo.util.HttpUtils;
import com.example.demo.util.WechatPay2ValidatorForRequest;
import com.example.demo.vo.R;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.wechat.pay.contrib.apache.httpclient.auth.ScheduledUpdateCertificatesVerifier;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@CrossOrigin
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "微信支付")
@Slf4j
public class WxPayController {
    @Resource
    private WxPayService wxPayService;
    @Resource
    private Verifier verifier;//是WxPayConfig中的ScheduledUpdateCertificatesVerifier  该类实现了接口Verifier 所以直接注入接口就行

    @ApiOperation("调用统一下单API，生成支付二维码")
    @PostMapping("native/{productId}")
    public R nativePay(@PathVariable Long productId) throws IOException {

        log.info("发起支付请求");

        //返回支付二维码连接和订单号
        Map<String,Object> map = wxPayService.nativePay(productId);

        return R.ok().setData(map);
    }

    /**
     * 支付通知
     * 微信支付通过支付通知接口将用户支付成功消息通知给商户
     */
    @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest request,HttpServletResponse response){

        Gson gson = new Gson();
        Map<String,String> map = new HashMap<>();//应答对象  是商户系统发送给微信服务器的

        try {
            String body = HttpUtils.readData(request);
            Map<String,Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的Id ===> {}",requestId);  //{}是占位符
            log.info("支付通知的完整数据 ===> {}",body);

            //签名的验证
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest = new WechatPay2ValidatorForRequest(verifier, body, requestId);
            if(!wechatPay2ValidatorForRequest.validate(request)){
                log.error("签名验证失败");

                response.setStatus(500);
                map.put("code","ERROR");
                map.put("message","失败");
                return gson.toJson(map);
            }
            log.info("通知验签成功");

            //处理订单  bodaMap中有密文数据
            wxPayService.processOrder(bodyMap);

            //应答超时  模拟接收重复通知
            //TimeUnit.SECONDS.sleep(5);

            //成功应答
            response.setStatus(200);
            map.put("code","SUCCESS");
            map.put("message","成功");

            return gson.toJson(map);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            map.put("code","ERROR");
            map.put("message","失败");

            return gson.toJson(map);
        }
    }

    @PostMapping("/cancel")
    public R cancel(@PathVariable String orderNO) throws Exception {
        log.info("取消订单");
        wxPayService.cancelOrder(orderNO);
        return R.ok().setMessage("订单已取消");
    }

    //时序图中商户后台没有接到异步通知的时候，主动去查询
    @GetMapping("/query/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) throws Exception {
        log.info("查询订单");
        String result = wxPayService.queryOrder(orderNo);
        return R.ok().setMessage("查询订单").data("result",result);
    }

    @ApiOperation("申请退款")
    @PostMapping("/refunds/{orderNo}/{reason}")
    public R refunds(@PathVariable String orderNo, @PathVariable String reason) throws Exception {
        log.info("申请退款");
        wxPayService.refund(orderNo,reason);
        return R.ok();
    }

    @ApiOperation("查询退款：测试用")
    @GetMapping("/query-refund/{refundNo}")
    public R queryRefund(@PathVariable String refundNo) throws Exception {
        log.info("查询退款");
        String result = wxPayService.queryRefund(refundNo);
        return R.ok().setMessage("查询成功").data("result",result);
    }

    @PostMapping("/refunds/notify")
    public String refundsNotify(HttpServletRequest request,HttpServletResponse response){
        log.info("退款通知执行");

        Gson gson = new Gson();
        Map<String,String> map = new HashMap<>();//应答对象  是商户系统发送给微信服务器的

        try {
            String body = HttpUtils.readData(request);
            Map<String,Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的Id ===> {}",requestId);  //{}是占位符
            log.info("支付通知的完整数据 ===> {}",body);

            //签名的验证
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest = new WechatPay2ValidatorForRequest(verifier, body, requestId);
            if(!wechatPay2ValidatorForRequest.validate(request)){
                log.error("签名验证失败");

                response.setStatus(500);
                map.put("code","ERROR");
                map.put("message","失败");
                return gson.toJson(map);
            }
            log.info("通知验签成功");

            //处理退款单  bodaMap中有密文数据
            wxPayService.processRefund(bodyMap);

            //应答超时  模拟接收重复通知
//            TimeUnit.SECONDS.sleep(5);

            //成功应答
            response.setStatus(200);
            map.put("code","SUCCESS");
            map.put("message","成功");

            return gson.toJson(map);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            map.put("code","ERROR");
            map.put("message","失败");

            return gson.toJson(map);
        }
    }

}
