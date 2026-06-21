package com.willu.buyitornot.web.config;

import com.willu.buyitornot.web.ui.common.WrapResponse;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * 런타임에 {@link WrapResponse}(→ ResponseWrapperAdvice)가 응답을
 * {@code ApiResponse{success, data, message, error}} 봉투로 감싸므로,
 * 생성되는 OpenAPI 문서의 응답 스키마도 동일한 봉투 형태로 재작성한다.
 *
 * <p>래핑 여부는 ResponseWrapperAdvice.supports()와 동일 규칙(@WrapResponse 존재)으로 판정한다.
 */
@Component
public class ApiResponseWrapperCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (!isWrapped(handlerMethod)) {
            return operation;
        }
        if (operation.getResponses() == null) {
            return operation;
        }

        for (ApiResponse response : operation.getResponses().values()) {
            Content content = response.getContent();
            if (content == null) {
                continue;
            }
            // 컨트롤러가 produces를 명시하지 않아 springdoc이 "*/*" 등으로 생성하므로
            // 미디어 타입 키에 의존하지 않고 본문이 있는 모든 응답 스키마를 봉투로 감싼다.
            for (MediaType mediaType : content.values()) {
                if (mediaType.getSchema() != null) {
                    mediaType.setSchema(wrap(mediaType.getSchema()));
                }
            }
        }
        return operation;
    }

    private boolean isWrapped(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(WrapResponse.class)
                || handlerMethod.getBeanType().isAnnotationPresent(WrapResponse.class);
    }

    private Schema<?> wrap(Schema<?> dataSchema) {
        return new ObjectSchema()
                .description("공통 응답 봉투 (ResponseWrapperAdvice)")
                .addProperty("success", new BooleanSchema().example(true))
                .addProperty("data", dataSchema)
                .addProperty("message", new StringSchema().nullable(true))
                .addProperty("error", new StringSchema().nullable(true));
    }
}
