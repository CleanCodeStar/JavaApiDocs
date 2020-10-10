package com.citrsw.controller;

import com.citrsw.core.ApiContext;
import com.citrsw.core.MarkdownHandler;
import com.citrsw.definition.Doc;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 写点注释
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-01-13 20:19
 */
@RestController
public class ApiController {

    private final ApiContext apiContext;

    public ApiController(ApiContext apiContext) {
        this.apiContext = apiContext;
    }

    @GetMapping("/citrsw/api")
    public Doc api() {
        return apiContext.getDoc();
    }

    @GetMapping("/citrsw/api/down/markdown")
    public ResponseEntity<InputStreamResource> down() throws UnsupportedEncodingException {
        String generate = new MarkdownHandler().generate(apiContext.getDoc());
        byte[] bytes = generate.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        //对文件名进行url编码处理防止出现乱码
        String newName = URLEncoder.encode(StringUtils.isBlank(apiContext.getDoc().getName())?"Api文档.md":apiContext.getDoc().getName()+".md", "utf-8")
                .replaceAll("\\+", "%20").replaceAll("%28", "\\(")
                .replaceAll("%29", "\\)").replaceAll("%3B", ";")
                .replaceAll("%40", "@").replaceAll("%23", "\\#")
                .replaceAll("%26", "\\&").replaceAll("%2C", "\\,");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", newName));
        headers.add("Expires", "0");
        headers.add("Pragma", "no-cache");
        return org.springframework.http.ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(bytes.length)
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(new InputStreamResource(byteArrayInputStream));
    }
}
