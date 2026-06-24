package com.ustccb.mall.exception;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String,Object>> handleBiz(BizException e) {
        return ResponseEntity.status(400).body(Map.of("code", e.getCode(), "msg", e.getMessage()));
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String,Object>> handleRuntime(RuntimeException e) {
        return ResponseEntity.status(500).body(Map.of("code", 500, "msg", e.getMessage()));
    }
}
