package com.ustccb.mall.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Web 请求日志切面
 * <p>统一打印：traceId / method / uri / ip / cost(ms) / status</p>
 */
@Slf4j
@Aspect
@Component
public class WebLogAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        HttpServletRequest req = attrs == null ? null : attrs.getRequest();
        String uri = req == null ? "N/A" : req.getRequestURI();
        String method = req == null ? "N/A" : req.getMethod();
        String ip = req == null ? "N/A" : req.getRemoteAddr();

        log.info("[REQ] traceId={} {} {} ip={}", traceId, method, uri, ip);
        try {
            Object result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[RESP] traceId={} {} {} cost={}ms", traceId, method, uri, cost);
            return result;
        } catch (Throwable t) {
            long cost = System.currentTimeMillis() - start;
            log.warn("[ERR ] traceId={} {} {} cost={}ms ex={}", traceId, method, uri, cost, t.getMessage());
            throw t;
        }
    }
}
