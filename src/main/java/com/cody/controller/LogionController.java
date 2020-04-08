package com.cody.controller;

import com.cody.util.IPUtils;
import com.github.hui.quick.plugin.base.DomUtil;
import com.github.hui.quick.plugin.base.constants.MediaType;
import com.github.hui.quick.plugin.qrcode.wrapper.QrCodeGenWrapper;
import com.github.hui.quick.plugin.qrcode.wrapper.QrCodeOptions;
import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.sound.sampled.Port;
import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * ClassName: LogionController
 *
 * @author WQL
 * @Description:
 * @date: 2020/4/8 21:22
 * @since JDK 1.8
 */
@CrossOrigin
@Controller
public class LogionController {

    @Value("${server.port}")
    private int port;

    /**
     * redirect: 跳转 url（app 授权之后，跳转的页面）
     * subscribe: 订阅url（用户访问会这个url，开启长连接，接收服务端推送的扫码，登陆事件）
     * qrcode: base64格式的二维码图片
     *
     * @param data
     * @return
     * @throws IOException
     * @throws WriterException
     */
    @GetMapping("login")
    public String qr(Map<String, Object> data) throws IOException, WriterException {
        String id = UUID.randomUUID().toString();
        // 本机测试时不能用127.0.0.1
        String ip = IPUtils.getLocalIP();

        String pref = "http://" + ip + ":" + port + "/";
        data.put("redirect", pref + "home");
        data.put("subscribe", pref + "subscribe?id=" + id);

        String qrUrl = pref + "scan?id=" + id;

        String qrCode = QrCodeGenWrapper.of(qrUrl).setW(200).setDrawPreColor(Color.RED)
                .setDrawStyle(QrCodeOptions.DrawStyle.CIRCLE).asString();

        data.put("qrcode", DomUtil.toDomSrc(qrCode, MediaType.ImageJpg));

        return "login";
    }

    private Map<String, SseEmitter> cache = new ConcurrentHashMap<>();

    @GetMapping(value = "subscribe", produces = {org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter scan(String id) {
        // 设置五分钟的超时时间
        SseEmitter sseEmitter = new SseEmitter(5 * 60 * 1000L);
        cache.put(id, sseEmitter);
        sseEmitter.onTimeout(() -> cache.remove(id));
        sseEmitter.onError((e) -> cache.remove(id));
        return sseEmitter;
    }

    @GetMapping("scan")
    public String scan(Model model, HttpServletRequest request) throws IOException {
        String id = request.getParameter("id");
        SseEmitter sseEmitter = cache.get(request.getParameter("id"));
        if (sseEmitter != null) {
            // 告诉pc端，已经扫码了
            sseEmitter.send("scan");
        }

        // 授权同意的url
        String url = "http://" + IPUtils.getLocalIP() + ":" + port + "/accept?id=" + id;
        model.addAttribute("url", url);

        return "scan";
    }

    @ResponseBody
    @GetMapping(path = "accept")
    public String accept(String id, String token) throws IOException {
        SseEmitter sseEmitter = cache.get(id);
        if (sseEmitter != null) {
            // 发送登录成功事件，并携带上用户的token，我们这里用cookie来保存token
            sseEmitter.send("login#qrlogin=" + token);
            sseEmitter.complete();
            cache.remove(id);
        }
        return "登陆成功：" + token;
    }

    @GetMapping(path = {"home", ""})
    @ResponseBody
    public String home(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return "未登录!";
        }

        Optional<Cookie> cookie = Stream.of(cookies).filter(s -> s.getName().equalsIgnoreCase("qrlogin")).findFirst();
        return cookie.map(cookie1 -> "欢迎进入首页：" + cookie1.getValue()).orElse("未登录!");
    }
}
