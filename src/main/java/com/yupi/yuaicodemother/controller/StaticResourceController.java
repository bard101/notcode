package com.yupi.yuaicodemother.controller;

import com.yupi.yuaicodemother.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源访问
 * - 预览生成代码：/api/static/{codeGenType}_{appId}/
 * - 访问已部署代码：/api/deploy/{deployKey}/
 */
@RestController
public class StaticResourceController {

    // 应用生成根目录（用于预览）
    private static final String PREVIEW_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    // 应用部署根目录（用于访问已部署站点）
    private static final String DEPLOY_ROOT_DIR = AppConstant.CODE_DEPLOY_ROOT_DIR;

    @GetMapping("/static/{previewKey}/**")
    public ResponseEntity<Resource> servePreviewResource(
            @PathVariable String previewKey,
            HttpServletRequest request) {
        return serveResourceFromRoot(PREVIEW_ROOT_DIR, "static", previewKey, request);
    }

    @GetMapping("/deploy/{deployKey}/**")
    public ResponseEntity<Resource> serveDeployedResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        return serveResourceFromRoot(DEPLOY_ROOT_DIR, "deploy", deployKey, request);
    }

    /**
     * 提供静态资源访问，支持目录重定向，并防止路径穿越。
     */
    private ResponseEntity<Resource> serveResourceFromRoot(
            String rootDir,
            String routeBase,
            String key,
            HttpServletRequest request) {
        try {
            // 获取资源路径（形如 /static/{key}/xxx 或 /deploy/{key}/xxx）
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/" + routeBase + "/" + key).length());

            // 如果是目录访问（不带斜杠），重定向到带斜杠的 URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }

            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }

            String relativePath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

            // 以 rootDir/key 作为根目录，防止 ../ 路径穿越
            Path rootPath = Paths.get(rootDir, key).toAbsolutePath().normalize();
            Path resolvedPath = rootPath.resolve(relativePath).normalize();
            if (!resolvedPath.startsWith(rootPath)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            File file = resolvedPath.toFile();
            if (!file.exists() || file.isDirectory()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(file.getName()))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String fileName) {
        if (fileName.endsWith(".html")) return "text/html; charset=UTF-8";
        if (fileName.endsWith(".css")) return "text/css; charset=UTF-8";
        if (fileName.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        if (fileName.endsWith(".json")) return "application/json; charset=UTF-8";
        return "application/octet-stream";
    }
}
