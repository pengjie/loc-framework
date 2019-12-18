package com.loc.framework.miniapp.sample.controller;

import com.loc.framework.autoconfigure.common.BaseResultCode;
import com.loc.framework.autoconfigure.utils.ProblemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

/**
 * Created on 2018/6/25.
 */
@Slf4j
@RestController
public class HelloWorldController {

  @GetMapping(value = "/helloWorld")
  public Problem helloWorld() {
    return ProblemUtil.createProblem(BaseResultCode.UNKNOWN_THROWABLE_EXCEPTION_CODE.getMsg(),
        BaseResultCode.UNKNOWN_THROWABLE_EXCEPTION_CODE.getCode(), Status.INTERNAL_SERVER_ERROR);
//    return ProblemUtil.createProblem(BaseResultCode.SUCCESS_RESPONSE_CODE.getCode(),"helloWorld");
  }
}
