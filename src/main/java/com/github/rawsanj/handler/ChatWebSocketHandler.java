package com.github.rawsanj.handler;

import com.github.rawsanj.messaging.RedisChatMessagePublisher;
import com.github.rawsanj.model.ChatMessage;
import com.github.rawsanj.util.ObjectStringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

	private final DirectProcessor<ChatMessage> messageDirectProcessor;
	private final FluxSink<ChatMessage> chatMessageFluxSink;
	private final RedisChatMessagePublisher redisChatMessagePublisher;
	private final RedisAtomicLong activeUserCounter;

	public ChatWebSocketHandler(DirectProcessor<ChatMessage> messageDirectProcessor, RedisChatMessagePublisher redisChatMessagePublisher, RedisAtomicLong activeUserCounter) {
		this.messageDirectProcessor = messageDirectProcessor;
		this.chatMessageFluxSink = messageDirectProcessor.sink();
		this.redisChatMessagePublisher = redisChatMessagePublisher;
		this.activeUserCounter = activeUserCounter;
	}

	//用于建立连接 和 构建, 这个方法只会在第一次接收消息时执行
	@Override
	public Mono<Void> handle(WebSocketSession webSocketSession) {
		Flux<WebSocketMessage> sendMessageFlux = messageDirectProcessor
			.flatMap(ObjectStringConverter::objectToString)
			.map(webSocketSession::textMessage)
			.doOnError(throwable -> log.info("Error Occurred while sending message to WebSocket.", throwable));
		//消息直接返回客户端
		Mono<Void> outputMessage = webSocketSession.send(sendMessageFlux);

		//发布到redis
		Mono<Void> inputMessage = webSocketSession.receive()
			.flatMap(webSocketMessage -> redisChatMessagePublisher.publishChatMessage(webSocketMessage.getPayloadAsText()))
			.doOnSubscribe(subscription -> {
				long activeUserCount = activeUserCounter.incrementAndGet();
				log.debug("User '{}' Connected. Total Active Users: {}", webSocketSession.getId(), activeUserCount);
				chatMessageFluxSink.next(new ChatMessage(0, "CONNECTED", "CONNECTED", activeUserCount));
			})
			.doOnError(throwable -> log.info("Error Occurred while sending message to Redis.", throwable))
			.doFinally(signalType -> {
				long activeUserCount = activeUserCounter.decrementAndGet();
				log.debug("User '{}' Disconnected. Total Active Users: {}", webSocketSession.getId(), activeUserCount);
				chatMessageFluxSink.next(new ChatMessage(0, "DISCONNECTED", "DISCONNECTED", activeUserCount));
			})
			.then();

		return Mono.zip(inputMessage, outputMessage).then();
	}

	//提交数据到流 messageDirectProcessor
	public Mono<Void> sendMessage(ChatMessage chatMessage) {
		return Mono.fromSupplier(() -> chatMessageFluxSink.next(chatMessage)).then();
	}

}
