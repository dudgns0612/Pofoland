/*
 * @(#) BoardFileController.java
 *
 * v1.0.0 / 2017. 7. 27. 
 *
 * Copyright ((c) 2017 by HST, Inc. All Rights Reserved.
 */
package com.hst.pofoland.biz.file.ctrl;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.hst.pofoland.common.utils.LoggerManager;

/**
 * 
 * 시스템명 : 
 * $com.hst.pofoland.biz.file.BoardFileController.java
 * 클래스 설명 : 
 *
 * @author 이현규
 * @since 2017. 7. 27.
 * @version 1.0.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 * 수정일          수정자         수정내용
 * -------------------------------------------------
 * 2017. 7. 27.   이현규  최초생성
 * </pre>
 */
@Controller
public class BoardFileController {

    @RequestMapping(value="/file/tempImageUpload", method = RequestMethod.POST)
    @ResponseBody
    public String tempImageUpload(HttpServletRequest request) {
        LoggerManager.info(getClass(), "{}", request);
        
        MultipartHttpServletRequest mul = (MultipartHttpServletRequest) request;
        
        String uploadedUrl = "";
        
        return uploadedUrl;
    }

}
