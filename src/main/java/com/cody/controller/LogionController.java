package com.cody.controller;

import com.cody.util.IPUtils;
import com.github.hui.quick.plugin.base.DomUtil;
import com.github.hui.quick.plugin.base.constants.MediaType;
import com.github.hui.quick.plugin.qrcode.wrapper.QrCodeGenWrapper;
import com.github.hui.quick.plugin.qrcode.wrapper.QrCodeOptions;
import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

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
    private int prot;

    @GetMapping("login")
    public String qr(Map<String, Object> data) throws IOException, WriterException {
        String id = UUID.randomUUID().toString();
        String ip = IPUtils.getLocalIP();

        String pref = "http://" + ip + ":" + prot + "/";
        data.put("redirect", pref + "home");
        data.put("subscribe", pref + "subscribe?id=" + id);

        String qrUrl = pref + "scan?id=" + id;

        String qrCode = QrCodeGenWrapper.of(qrUrl).setW(200).setDrawPreColor(Color.RED)
                .setDrawStyle(QrCodeOptions.DrawStyle.CIRCLE).asString();

        data.put("qrcode", DomUtil.toDomSrc(qrCode, MediaType.ImageJpg));

        return "login";
    }
}
