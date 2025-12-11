package com.easylive.api.provider;

import com.easylive.annotation.GlobalInterceptor;
import com.easylive.controller.FileController;
import com.easylive.entity.constants.Constants;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;

@RestController
@Validated
@RequestMapping(Constants.INNER_API_PREFIX + "/file")
public class ResourceApi {

    @Resource
    private FileController fileController;

    @RequestMapping("/uploadImage")
    public String uploadCover(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) throws IOException {
        return fileController.uploadCoverInner(file, createThumbnail);
    }

    @RequestMapping(value = "/getResource")
    public void getResource(HttpServletResponse response, @NotEmpty String sourceName) {
        fileController.getResource(response, sourceName);
    }

    @RequestMapping("/videoResource/{fileId}")
    public void videoResource(HttpServletResponse response, @PathVariable @NotEmpty String fileId) {
        fileController.getVideoResource(response, fileId);
    }

    @RequestMapping("/videoResource/{fileId}/{ts}")
    @GlobalInterceptor
    public void getVideoResourceTs(HttpServletResponse response, @PathVariable @NotEmpty String fileId, @PathVariable @NotNull String ts) {
        fileController.getVideoResourceTs(response, fileId, ts);
    }
}
