package com.easylive.api.consumer;

import com.easylive.entity.constants.Constants;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@FeignClient(name = Constants.SERVER_NAME_RESOURCE)
public interface ResourceClient {


    @RequestMapping(value = Constants.INNER_API_PREFIX + "/file/uploadImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadCover(@RequestPart MultipartFile file, @RequestParam Boolean createThumbnail);


    @RequestMapping(value = Constants.INNER_API_PREFIX + "/file/getResource")
    Response getResource(@RequestParam String sourceName);


    @RequestMapping(Constants.INNER_API_PREFIX + "/file/videoResource/{fileId}")
    Response videoResource(@PathVariable @NotEmpty String fileId);


    @RequestMapping(Constants.INNER_API_PREFIX + "/file/videoResource/{fileId}/{ts}")
    Response getVideoResourceTs(@PathVariable @NotEmpty String fileId, @PathVariable @NotNull String ts);
}
