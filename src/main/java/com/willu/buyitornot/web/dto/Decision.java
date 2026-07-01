package com.willu.buyitornot.web.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 투표 결정. JSON 직렬화/역직렬화는 소문자(buy/skip/maybe)로 매핑된다.
 */
@Schema(
        description = "투표 결정. buy=구매(오른쪽) / skip=패스(왼쪽) / maybe=고민중(위).",
        example = "buy"
)
public enum Decision {
    BUY("buy"),
    SKIP("skip"),
    MAYBE("maybe");

    private final String value;

    Decision(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Decision from(String raw) {
        if (raw != null) {
            for (Decision d : values()) {
                if (d.value.equalsIgnoreCase(raw)) {
                    return d;
                }
            }
        }
        throw new IllegalArgumentException("Invalid decision: " + raw);
    }
}
