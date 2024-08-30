package kr.granblue.dcscraper.domain.enums;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public enum SourceType {
    INTERNAL("internal", "내부"),
    EXTERNAL("external", "외부"),
    DC("dc", "디시"),
    NONE("none", "none"),
    ALL("all", "전체")

    ;

    // 앱이 실행될때  읽기전용 맵으로 캐싱.
    private static final Map<String, String> SOURCE_TYPE_MAP = Collections.unmodifiableMap(
            // Category.values() 로 모아 key = code, value = Category.name 의 맵으로
            Stream.of(values()).collect(Collectors.toMap(SourceType::getCode, SourceType::name))
    );

    @Getter
    String code;

    @Getter
    String sourceTypeName;

    SourceType(String code, String sourceTypeName) {this.code = code; this.sourceTypeName = sourceTypeName;}

    /**
     * String sourceTypeName 를 받아 this 리턴
     */
    public static SourceType of(String sourceTypeName) {
        return SourceType.valueOf(SOURCE_TYPE_MAP.get(sourceTypeName));
    }

    public boolean isExternal() {
        return this == EXTERNAL;
    }

}
