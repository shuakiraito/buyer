package com.study.r4a122.webportal.chat;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;

import com.study.r4a122.webportal.user.UserData;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  // セッションにユーザー情報を格納する際のキー（LoginServiceと同じ）
  private static final String SESSION_USER_DATA_KEY = "userData";

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .addInterceptors(new HandshakeInterceptor() {
          @Override
          public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                         WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            if (request instanceof ServletServerHttpRequest) {
              ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
              // 現在のHTTPセッションを取得
              HttpSession session = servletRequest.getServletRequest().getSession(false);
              
              if (session != null) {
                // HTTPセッションからユーザー情報を取得
                UserData userData = (UserData) session.getAttribute(SESSION_USER_DATA_KEY);
                
                if (userData != null) {
                  // ユーザーIDをPrincipalとして設定
                  UserPrincipal principal = new UserPrincipal(userData.userId());
                  attributes.put("principal", principal);
                  
                  // HTTPセッションの属性もコピー（必要に応じて）
                  attributes.put("httpSession", session);
                }
              }
            }
            return true;
          }

          @Override
          public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                     WebSocketHandler wsHandler, Exception exception) {
            // 処理なし
          }
        })
        .setHandshakeHandler(new DefaultHandshakeHandler() {
          @Override
          protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
            // HandshakeInterceptorで設定したprincipalを取得
            Principal principal = (Principal) attributes.get("principal");
            return principal;
          }
        })
        .withSockJS();
  }
}

