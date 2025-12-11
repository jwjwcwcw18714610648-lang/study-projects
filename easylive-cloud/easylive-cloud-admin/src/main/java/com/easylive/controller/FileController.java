package com.easylive.controller;

import com.easylive.api.consumer.ResourceClient;
import com.easylive.entity.vo.ResponseVO;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Validated
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController extends ABaseController {

    @Resource
    private ResourceClient resourceClient;

    @RequestMapping("/uploadImage")
    public ResponseVO uploadImage(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) {
        return getSuccessResponseVO(resourceClient.uploadCover(file, createThumbnail));
    }


    @RequestMapping("/getResource")
    public void getResource(HttpServletResponse servletResponse, @NotEmpty String sourceName) {
        Response response = resourceClient.getResource(sourceName);
        convertFileReponse2Stream(servletResponse, response);
    }

    @RequestMapping("/videoResource/{fileId}")
    public void getVideoResource(HttpServletResponse response, @PathVariable @NotEmpty String fileId) {
        convertFileReponse2Stream(response, resourceClient.videoResource(fileId));
    }

    @RequestMapping("/videoResource/{fileId}/{ts}")
    public void getVideoResourceTs(HttpServletResponse response, @PathVariable @NotEmpty String fileId, @PathVariable @NotNull String ts) {
        convertFileReponse2Stream(response, resourceClient.getVideoResourceTs(fileId, ts));
    }
}
