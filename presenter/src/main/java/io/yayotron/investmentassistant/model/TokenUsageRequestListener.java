package io.yayotron.investmentassistant.model;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TokenUsageRequestListener implements ChatModelListener {

    private static final Logger logger = LoggerFactory.getLogger(TokenUsageRequestListener.class);

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        logger.info("Response token usage {}", responseContext.chatResponse().tokenUsage());
    }
}
