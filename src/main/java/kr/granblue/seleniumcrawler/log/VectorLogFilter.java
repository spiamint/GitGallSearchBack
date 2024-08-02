package kr.granblue.seleniumcrawler.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.LoggerFactory;

public class VectorLogFilter extends Filter<ILoggingEvent> {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(VectorLogFilter.class);

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String message = event.getMessage();
        if (message.contains("INSERT") && message.contains("::v")) {
            return FilterReply.DENY; // INSERT문 파라미터에 ::v(vector) 값이 들어가면 기존 로거 무시
        }
        return FilterReply.NEUTRAL;
    }
}
