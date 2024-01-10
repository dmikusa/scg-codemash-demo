package com.mikusa.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

@Component
public class CaptureRequestParameterGatewayFilterFactory extends AbstractGatewayFilterFactory<CaptureRequestParameterGatewayFilterFactory.Config> {
    private static final Logger log = LoggerFactory.getLogger(CaptureRequestParameterGatewayFilterFactory.class);

    public CaptureRequestParameterGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                Optional<List<String>> paramValues = Optional
                        .ofNullable(exchange
                                .getRequest()
                                .getQueryParams()
                                .get(config.getName()));

                if (paramValues.isEmpty() && config.getDefaultValue() == null) {
                    throw new IllegalStateException("Parameter [" + config.getName() + "] not set on request and no default value set on the filter");
                }

                List<String> values = paramValues.orElse(config.getDefaultValueAsList());
                if (values.size() > 1) {
                    log.warn("Multiple values for request parameter [{}] are present, but only the first is being used", config.getName());
                }

                values.stream()
                        .findFirst()
                        .ifPresent(val -> {
                            log.debug("Captured request parameter [{}] with value [{}]", config.getName(), val);
                            ServerWebExchangeUtils.putUriTemplateVariables(exchange, Map.of(config.getName(), val));
                        });

                return chain.filter(exchange);
            }

            @Override
            public String toString() {
                return filterToStringCreator(CaptureRequestParameterGatewayFilterFactory.this)
                        .append("name", config.getName())
                        .append("defaultValue", config.getDefaultValueAsList())
                        .toString();
            }
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("name", "defaultValue");
    }

    public static class Config {
        private String name;
        private String defaultValue;

        public Config() {
        }

        public Config(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public List<String> getDefaultValueAsList() {
            return (defaultValue == null) ? List.of() : List.of(defaultValue);
        }
    }
}
