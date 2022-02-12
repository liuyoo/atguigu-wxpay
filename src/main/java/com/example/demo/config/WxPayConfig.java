package com.example.demo.config;

import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.ScheduledUpdateCertificatesVerifier;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.Data;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

/**
 * @ConfigurationProperties
 *      与@bean结合为属性赋值
 *      与@PropertySource（只能用于properties文件）结合读取指定文件
 */

@Configuration//在应用程序加载的时候 配置文件就会被创建出来
@PropertySource("classpath:wxpay.properties") //读取配置文件
@ConfigurationProperties(prefix="wxpay") //指定去读取wxpay节点下的所有信息
@Data //使用set方法将wxpay节点中的值填充到当前类的属性中
public class WxPayConfig {

    // 商户号
    private String mchId;

    // 商户API证书序列号
    private String mchSerialNo;

    // 商户私钥文件
    private String privateKeyPath;

    // APIv3密钥
    private String apiV3Key;

    // APPID
    private String appid;

    // 微信服务器地址
    private String domain;

    // 接收结果通知地址
    private String notifyDomain;

    /**
     * 获取商户的私钥文件
     * @param filename
     * @return
     */
    public PrivateKey getPraivateKey(String filename){
        try {
            return PemUtil.loadPrivateKey(
                    new FileInputStream(filename));
        } catch (FileNotFoundException e) {
//            e.printStackTrace();
            throw new RuntimeException("私钥文件不存在", e);//把错误往上抛
        }
    }

    /**
     * 获取签名验证器
     * @return
     */
    @Bean
    public ScheduledUpdateCertificatesVerifier getVerifier(){
        //获取商户私钥
        PrivateKey praivateKey = getPraivateKey(privateKeyPath);
        //私钥签名对象
        PrivateKeySigner privateKeySigner = new PrivateKeySigner(mchSerialNo, praivateKey);
        //身份认证对象
        WechatPay2Credentials wechatPay2Credentials = new WechatPay2Credentials(mchId, privateKeySigner);


        // 使用定时更新的签名验证器，不需要传入证书
        ScheduledUpdateCertificatesVerifier verifier = new ScheduledUpdateCertificatesVerifier(
                wechatPay2Credentials,
                apiV3Key.getBytes(StandardCharsets.UTF_8));
        return verifier;
    }

    /**
     * 获取http请求对象
     * @return
     */
    @Bean(name = "wxPayClient")//可以直接把方法注入     在WxPayServiceImpl中33、118行使用
    public CloseableHttpClient getWePayClient(ScheduledUpdateCertificatesVerifier verifier){

        //获取商户私钥
        PrivateKey praivateKey = getPraivateKey(privateKeyPath);

        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                .withMerchant(mchId, mchSerialNo, praivateKey)
                .withValidator(new WechatPay2Validator(verifier));

    // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        CloseableHttpClient httpClient = builder.build();

        return httpClient;
    }
}
