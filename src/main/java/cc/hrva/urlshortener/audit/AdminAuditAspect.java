package cc.hrva.urlshortener.audit;

import cc.hrva.urlshortener.service.AuditLogService;
import cc.hrva.urlshortener.service.UserService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminAuditAspect {

    private final AuditLogService auditLogService;
    private final UserService userService;

    @Around("execution(* cc.hrva.urlshortener.controller.AdminController.clearLoginAttempts(..)) || " +
            "execution(* cc.hrva.urlshortener.controller.UserController.deleteUser(..)) || " +
            "execution(* cc.hrva.urlshortener.controller.UserController.updateUser(..)) || " +
            "execution(* cc.hrva.urlshortener.controller.UrlController.revokeUrl(..)) || " +
            "execution(* cc.hrva.urlshortener.controller.UrlController.activateUrl(..)) || " +
            "execution(* cc.hrva.urlshortener.controller.UrlController.deleteUrl(..)) || " +
            "execution(* cc.hrva.urlshortener.controller.UrlController.updateUrl(..))")
    public Object logAdminAction(final ProceedingJoinPoint joinPoint) throws Throwable {
        final var user = userService.getUserFromToken();
        final var performedBy = user != null ? user.getEmail() : "anonymous";
        final var methodName = joinPoint.getSignature().getName();
        final var args = joinPoint.getArgs();
        final var targetType = joinPoint.getTarget().getClass().getSimpleName().replace("Controller", "");

        final var details = new StringBuilder();
        for (final var arg : args) {
            if (arg != null) {
                details.append(arg.toString()).append(", ");
            }
        }

        try {
            final var result = joinPoint.proceed();
            auditLogService.log(performedBy, methodName, targetType, args.length > 0 ? String.valueOf(args[0]) : "", details.toString());
            return result;
        } catch (final Throwable e) {
            auditLogService.log(performedBy, methodName + "_FAILED", targetType, args.length > 0 ? String.valueOf(args[0]) : "", e.getMessage());
            throw e;
        }
    }

}
