package kr.granblue.gbfsearchback.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * sql문에 들어간 vector 값을 축약하는 터보필터 (logback.xml 에 등록)
 */
//@Slf4j
public class VectorLogFilter extends TurboFilter {
    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (format != null &&
                logger.getName().contains("sqltiming") &&
                format.contains("::v")) { // sqltiming 에 ::vector 붙은 로그면
            String modifiedFormat = format; // 로거 메시지
            int i = 0; // 무한루프 방지
            do {
                int headIndex = modifiedFormat.indexOf("[") + 14; // 벡터값 시작 [ 부터 첫 값까지 
                int tailIndex = modifiedFormat.indexOf("]"); // 벡터값 끝
                // 로그메시지의 [벡터값] 을 {0.1234566, ... } 로 변경
                String vector = modifiedFormat.substring(modifiedFormat.indexOf("["), modifiedFormat.indexOf("]") + 1);
                String head;
                if (vector.length() > 10) {
                    // vector = [0.123456789, ... ]
                    head = modifiedFormat.substring(0, modifiedFormat.indexOf("[") + 14);
                } else {
                    // vector = [0.0]
                    head = modifiedFormat.substring(0, modifiedFormat.indexOf("[") + 4);
                }
                String tail = modifiedFormat.substring(modifiedFormat.indexOf("]"));
                modifiedFormat = head + " ... " + tail;
                modifiedFormat = modifiedFormat.replaceFirst("\\[", "{").replaceFirst("]", "}");
                i++;
            } while (modifiedFormat.contains("[") && i < 1001);
            // 벡터값 축약한 로그 찍기
            logger.info("{}", modifiedFormat);
            return FilterReply.DENY; // 기존로거 무시
        }
        return FilterReply.NEUTRAL;
    }
}
