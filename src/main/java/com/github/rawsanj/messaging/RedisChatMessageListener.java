package com.github.rawsanj.messaging;

import com.github.rawsanj.handler.ChatWebSocketHandler;
import com.github.rawsanj.model.ChatMessage;
import com.github.rawsanj.util.ObjectStringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.stereotype.Component;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static com.github.rawsanj.config.ChatConstants.MESSAGE_TOPIC;

@Component
@Slf4j
public class RedisChatMessageListener {

	private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
	private final ChatWebSocketHandler chatWebSocketHandler;

	public RedisChatMessageListener(ReactiveStringRedisTemplate reactiveStringRedisTemplate, ChatWebSocketHandler chatWebSocketHandler) {
		this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
		this.chatWebSocketHandler = chatWebSocketHandler;
	}

	/**
	 * 监听redis topic
	 */
	public Mono<Void> subscribeMessageChannelAndPublishOnWebSocket() {
		return reactiveStringRedisTemplate.listenTo(new PatternTopic(MESSAGE_TOPIC))
			//接收消息
			.map(ReactiveSubscription.Message::getMessage)
			//消息反序列化
			.flatMap(message -> ObjectStringConverter.stringToObject(message, ChatMessage.class))
			//判断是否为空
			.filter(chatMessage -> !chatMessage.getMessage().isEmpty())
			//通过webSocket发送消息
			.flatMap(chatWebSocketHandler::sendMessage)
			.then();
	}

	public static void main(String[] args) {
	}


}
