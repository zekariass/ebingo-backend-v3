package com.ebingo.backend.common.telegram;


import com.ebingo.backend.system.exceptions.TelegramAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TelegramUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final TelegramAuthService telegramAuthService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedTelegramUser.class)
                && TelegramUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter,
                                        BindingContext bindingContext,
                                        ServerWebExchange exchange) {
        String initData = exchange.getRequest().getHeaders().getFirst("x-init-data");
        if (initData == null || initData.isBlank()) {
            return Mono.error(new TelegramAuthException(HttpStatus.UNAUTHORIZED, "Missing x-init-data header"));
        }

        return telegramAuthService.verifyAndExtractUser(initData)
                .onErrorMap(ex -> new TelegramAuthException(HttpStatus.UNAUTHORIZED, "Invalid Telegram init data"))
                .cast(Object.class);
    }
}

