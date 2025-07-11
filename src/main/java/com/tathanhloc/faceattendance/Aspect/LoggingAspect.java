package com.tathanhloc.faceattendance.Aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tathanhloc.faceattendance.Service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingAspect {

    private final SystemLogService logService;
    private final ObjectMapper objectMapper;

    // Custom annotation for method logging
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LogActivity {
        String module() default "";
        String action() default "";
        String description() default "";
        boolean logParameters() default false;
        boolean logResult() default false;
        boolean logPerformance() default true;
    }

    // Log all controller methods
    @Around("execution(* com.tathanhloc.faceattendance.Controller.*.*(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Log successful API calls
            logService.logPerformance(
                    "API",
                    String.format("%s.%s", className, methodName),
                    String.format("API call completed successfully"),
                    duration
            );

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Log failed API calls
            logService.logError(
                    "API",
                    String.format("%s.%s", className, methodName),
                    String.format("API call failed: %s", e.getMessage()),
                    getStackTrace(e)
            );

            throw e;
        }
    }

    // Log service layer operations with custom annotation
    @Around("@annotation(logActivity)")
    public Object logAnnotatedMethods(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        String module = logActivity.module().isEmpty() ? className : logActivity.module();
        String action = logActivity.action().isEmpty() ? methodName : logActivity.action();
        String description = logActivity.description().isEmpty() ?
                String.format("Executed %s.%s", className, methodName) : logActivity.description();

        // Get current user info
        String[] userInfo = getCurrentUserInfo();
        String userId = userInfo[0];
        String userName = userInfo[1];

        try {
            // Log method parameters if requested
            if (logActivity.logParameters() && joinPoint.getArgs().length > 0) {
                String params = serializeParameters(joinPoint.getArgs());
                description += " with parameters: " + params;
            }

            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Log result if requested
            if (logActivity.logResult() && result != null) {
                String resultStr = serializeObject(result);
                description += " -> Result: " + resultStr;
            }

            // Log performance if requested
            if (logActivity.logPerformance()) {
                logService.logPerformance(module, action, description, duration);
            } else {
                logService.logUserAction(module, action, description, userId, userName);
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            logService.logError(
                    module,
                    action,
                    String.format("%s failed: %s", description, e.getMessage()),
                    getStackTrace(e)
            );

            throw e;
        }
    }

    // Log authentication events
    @AfterReturning(pointcut = "execution(* org.springframework.security.authentication.AuthenticationManager.authenticate(..))", returning = "authentication")
    public void logSuccessfulAuthentication(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            logService.logAuthentication(
                    "LOGIN_SUCCESS",
                    authentication.getName(),
                    getUserDisplayName(authentication),
                    true,
                    "User logged in successfully"
            );
        }
    }

    @AfterThrowing(pointcut = "execution(* org.springframework.security.authentication.AuthenticationManager.authenticate(..))", throwing = "ex")
    public void logFailedAuthentication(JoinPoint joinPoint, Exception ex) {
        Object[] args = joinPoint.getArgs();
        String username = args.length > 0 && args[0] instanceof Authentication ?
                ((Authentication) args[0]).getName() : "unknown";

        logService.logAuthentication(
                "LOGIN_FAILED",
                username,
                username,
                false,
                "Authentication failed: " + ex.getMessage()
        );
    }

    // Log data modification operations
    @AfterReturning(pointcut = "execution(* com.tathanhloc.faceattendance.Service.*.create(..))")
    public void logCreateOperations(JoinPoint joinPoint) {
        logDataOperation(joinPoint, "CREATE");
    }

    @AfterReturning(pointcut = "execution(* com.tathanhloc.faceattendance.Service.*.update(..))")
    public void logUpdateOperations(JoinPoint joinPoint) {
        logDataOperation(joinPoint, "UPDATE");
    }

    @AfterReturning(pointcut = "execution(* com.tathanhloc.faceattendance.Service.*.delete(..))")
    public void logDeleteOperations(JoinPoint joinPoint) {
        logDataOperation(joinPoint, "DELETE");
    }

    private void logDataOperation(JoinPoint joinPoint, String operation) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String entityType = className.replace("Service", "");

        String[] userInfo = getCurrentUserInfo();
        String userId = userInfo[0];
        String userName = userInfo[1];

        // Get entity ID from method parameters
        String entityId = getEntityIdFromArgs(joinPoint.getArgs());

        logService.logUserAction(
                entityType.toUpperCase(),
                operation,
                String.format("%s %s [%s]", operation, entityType, entityId),
                userId,
                userName
        );
    }

    // Helper methods
    private String[] getCurrentUserInfo() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String userId = auth.getName();
                String userName = getUserDisplayName(auth);
                return new String[]{userId, userName};
            }
        } catch (Exception e) {
            log.debug("Could not get current user info: {}", e.getMessage());
        }
        return new String[]{null, null};
    }

    private String getUserDisplayName(Authentication auth) {
        try {
            // Try to get display name from user details
            if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                // You might need to cast to your custom UserDetails implementation
                return auth.getName(); // or extract from custom user details
            }
        } catch (Exception e) {
            log.debug("Could not get user display name: {}", e.getMessage());
        }
        return auth.getName();
    }

    private String getEntityIdFromArgs(Object[] args) {
        if (args.length > 0) {
            Object firstArg = args[0];
            if (firstArg instanceof String || firstArg instanceof Number) {
                return firstArg.toString();
            }
            // Try to get ID from DTO objects
            try {
                if (firstArg.getClass().getMethod("getId") != null) {
                    Object id = firstArg.getClass().getMethod("getId").invoke(firstArg);
                    return id != null ? id.toString() : "unknown";
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return "unknown";
    }

    private String serializeParameters(Object[] args) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(serializeObject(args[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Could not serialize parameters";
        }
    }

    private String serializeObject(Object obj) {
        try {
            if (obj == null) return "null";
            if (obj instanceof String) return "\"" + obj + "\"";
            if (obj instanceof Number || obj instanceof Boolean) return obj.toString();

            // For complex objects, serialize to JSON (truncated)
            String json = objectMapper.writeValueAsString(obj);
            return json.length() > 200 ? json.substring(0, 200) + "..." : json;
        } catch (Exception e) {
            return obj.getClass().getSimpleName() + "@" + obj.hashCode();
        }
    }

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage()).append("\n");

        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 2000) { // Limit stack trace length
                sb.append("... (truncated)");
                break;
            }
        }

        return sb.toString();
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not get client IP: {}", e.getMessage());
        }
        return "unknown";
    }
}