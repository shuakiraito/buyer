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
            System.out.println("--- HandshakeInterceptor: beforeHandshake ---");
            
            if (request instanceof ServletServerHttpRequest) {
              ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
              // 現在のHTTPセッションを取得
              HttpSession session = servletRequest.getServletRequest().getSession(false);
              
              if (session == null) {
                System.out.println("! 警告: HTTPセッションが 'null' です。ログインしていません。");
              } else {
                System.out.println("  セッションID: " + session.getId());
                System.out.println("  セッションキー名: " + SESSION_USER_DATA_KEY);
                
                // セッション内のすべての属性名を確認（デバッグ用）
                java.util.Enumeration<String> attributeNames = session.getAttributeNames();
                System.out.println("  セッション内の属性一覧:");
                while (attributeNames.hasMoreElements()) {
                  String attrName = attributeNames.nextElement();
                  System.out.println("    - " + attrName + " (型: " + 
                    (session.getAttribute(attrName) != null ? session.getAttribute(attrName).getClass().getName() : "null") + ")");
                }
                
                // HTTPセッションからユーザー情報を取得
                Object sessionData = session.getAttribute(SESSION_USER_DATA_KEY);
                
                if (sessionData == null) {
                  System.out.println("! 警告: " + SESSION_USER_DATA_KEY + " というキーでセッションからデータを取得しましたが 'null' でした。");
                  System.out.println("  (キー名が間違っているか、ログイン処理が完了していません)");
                } else if (!(sessionData instanceof UserData)) {
                  System.out.println("! エラー: セッションのデータ型が UserData ではありません。型: " + sessionData.getClass().getName());
                } else {
                  UserData userData = (UserData) sessionData;
                  System.out.println("  成功: ユーザー " + userData.userId() + " をセッションから取得しました。");
                  
                  // ユーザーIDをPrincipalとして設定
                  UserPrincipal principal = new UserPrincipal(userData.userId());
                  attributes.put("principal", principal);
                  System.out.println("  Principalを設定しました: " + principal.getName());
                  
                  // HTTPセッションの属性もコピー（必要に応じて）
                  attributes.put("httpSession", session);
                }
              }
            } else {
              System.out.println("! 警告: リクエストがServletServerHttpRequestではありません。");
            }
            System.out.println("-------------------------------------------------");
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
            if (principal == null) {
              System.out.println("! 警告: HandshakeHandler: attributesからprincipalが取得できませんでした。");
            } else {
              System.out.println("  HandshakeHandler: Principalを取得しました: " + principal.getName());
            }
            return principal;
          }
        })
        .withSockJS();
  }
}

