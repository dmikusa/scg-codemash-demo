package com.mikusa.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class RewriteDogJsonGatewayFilterFactory extends AbstractGatewayFilterFactory<RewriteDogJsonGatewayFilterFactory.Config> {
    private static final Logger log = LoggerFactory.getLogger(RewriteDogJsonGatewayFilterFactory.class);

    private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory;
    private final ObjectMapper mapper;

    public RewriteDogJsonGatewayFilterFactory(ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory, ObjectMapper objectMapper) {
        super(RewriteDogJsonGatewayFilterFactory.Config.class);
        this.modifyResponseBodyGatewayFilterFactory = modifyResponseBodyGatewayFilterFactory;
        this.mapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        ModifyResponseBodyGatewayFilterFactory.Config modifyResponseBodyConfig = new ModifyResponseBodyGatewayFilterFactory.Config();
        modifyResponseBodyConfig.setInClass(String.class);
        modifyResponseBodyConfig.setOutClass(String.class);
        modifyResponseBodyConfig.setRewriteFunction(getRewriteFunction(config));

        return modifyResponseBodyGatewayFilterFactory.apply(modifyResponseBodyConfig);
    }

    public RewriteFunction<String, String> getRewriteFunction(Config config) {
        return (exchange, body) -> {
            if (!MediaType.APPLICATION_JSON.isCompatibleWith(exchange.getResponse().getHeaders().getContentType())) {
                return Mono.just(body);
            }

            try {
                JsonNode jsonNode = mapper.readTree(body);
                URI dogUri = URI.create(jsonNode.get("message").asText());

                String path = dogUri.getPath();
                String[] pathParts = path.split("/");
                String breed = pathParts[pathParts.length - 2];
                String image = pathParts[pathParts.length - 1];
                String id = image.substring(0, image.lastIndexOf('.'));
                String imageType = image.substring(image.lastIndexOf('.') + 1);

                return Mono.just(mapper.writeValueAsString(new DogResponse(
                        dogUri.toString(),
                        id,
                        imageType,
                        breed)));
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON", e);
            }
        };
    }

    public static class Config {
    }

    private record DogResponse(String url, String id, String imageType, String breed) {
    }
}
