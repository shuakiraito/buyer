package com.study.r4a122.webportal.chat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.study.r4a122.webportal.user.LoginService;
import com.study.r4a122.webportal.user.UserRepository;
import com.study.r4a122.webportal.user.UserData;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class ChatController {

  private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ChatService chatService;
  private final LoginService loginService;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;

  public ChatController(
      ChatService chatService,
      LoginService loginService,
      UserRepository userRepository,
      SimpMessagingTemplate messagingTemplate) {
    this.chatService = chatService;
    this.loginService = loginService;
    this.userRepository = userRepository;
    this.messagingTemplate = messagingTemplate;
  }

  @GetMapping("/chat")
  public String chatIndex(Model model) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return "redirect:/login";
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";
    List<ChannelData> channels = chatService.getAccessibleChannels(userId, userRole);
    model.addAttribute("channels", channels);
    model.addAttribute("userId", userId);
    model.addAttribute("userName", userData != null ? userData.userName() : "Unknown");
    model.addAttribute("userRole", userRole);
    
    // 各チャンネルの参加状況を確認
    Map<Integer, Boolean> channelMembership = new HashMap<>();
    for (ChannelData channel : channels) {
      channelMembership.put(channel.id(), chatService.isChannelMember(channel.id(), userId));
    }
    model.addAttribute("channelMembership", channelMembership);
    
    // 講師の場合、保留中の申請を表示
    if ("ROLE_TEACHER".equals(userRole) || "ROLE_ADMIN".equals(userRole)) {
      List<ChannelRequestData> pendingRequests = chatService.getAllPendingRequests();
      model.addAttribute("pendingRequests", pendingRequests);
    }
    
    List<UserData> allUsers = userRepository.findAll().stream()
        .filter(u -> !u.userId().equals(userId))
        .toList();
    model.addAttribute("allUsers", allUsers);
    
    List<ChannelInvitationData> pendingInvitations = chatService.getPendingInvitations(userId);
    model.addAttribute("pendingInvitations", pendingInvitations);
    
    return "chat/index";
  }

  @GetMapping("/chat/channel/{channelId}")
  public String chatChannel(@PathVariable(name = "channelId") int channelId, Model model) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return "redirect:/login";
    }

    ChannelData channel = chatService.getChannelById(channelId);
    if (channel == null) {
      return "redirect:/chat";
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";
    
    // チャンネルへのアクセス権限チェック
    if (!channel.isPublic() && !chatService.isChannelMember(channelId, userId) 
        && !"ROLE_TEACHER".equals(userRole) && !"ROLE_ADMIN".equals(userRole)) {
      return "redirect:/chat";
    }

    List<MessageData> messages = chatService.getMessagesByChannelId(channelId);
    List<ChannelData> channels = chatService.getAccessibleChannels(userId, userRole);

    model.addAttribute("channel", channel);
    model.addAttribute("messages", messages);
    model.addAttribute("channels", channels);
    model.addAttribute("userId", userId);
    model.addAttribute("userName", userData != null ? userData.userName() : "Unknown");
    model.addAttribute("userRole", userRole);
    return "chat/channel";
  }

  @PostMapping("/chat/channel/create")
  public String createChannel(
      @RequestParam(name = "channelName") String channelName,
      @RequestParam(name = "description", required = false) String description,
      @RequestParam(name = "isPublic", required = false, defaultValue = "true") boolean isPublic,
      @RequestParam(name = "invitees", required = false) List<String> invitees) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return "redirect:/login";
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";
    
    // 講師は即時作成、学生は申請
    if ("ROLE_TEACHER".equals(userRole) || "ROLE_ADMIN".equals(userRole) || "ROLE_MANAGER".equals(userRole) || "ROLE_TOP".equals(userRole)) {
      chatService.createChannel(channelName, description != null ? description : "", isPublic, userId, invitees);
    } else {
      chatService.createChannelRequest(channelName, description != null ? description : "", isPublic, userId);
    }
    return "redirect:/chat";
  }

  // メッセージ編集・削除
  @PostMapping("/chat/message/update")
  public ResponseEntity<String> updateMessage(
      @RequestParam(name = "messageId") int messageId,
      @RequestParam(name = "messageText") String messageText,
      @RequestParam(name = "importance", required = false) String importance) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";

    boolean success = chatService.updateMessage(messageId, messageText, importance, userId, userRole);
    if (!success) {
      return ResponseEntity.badRequest().body("Failed");
    }

    MessageData updatedMessage = chatService.getMessageById(messageId);
    if (updatedMessage != null) {
      ChatMessage event = buildChatMessage(updatedMessage, "UPDATE");
      messagingTemplate.convertAndSend(channelTopic(updatedMessage.channelId()), event);
    }
    return ResponseEntity.ok("Updated");
  }

  @PostMapping("/chat/message/delete")
  public ResponseEntity<String> deleteMessage(@RequestParam(name = "messageId") int messageId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";

    MessageData targetMessage = chatService.getMessageById(messageId);
    if (targetMessage == null) {
      return ResponseEntity.badRequest().body("Failed");
    }

    boolean success = chatService.deleteMessage(messageId, userId, userRole);
    if (!success) {
      return ResponseEntity.badRequest().body("Failed");
    }

    ChatMessage event = buildChatMessage(targetMessage, "DELETE");
    if (event != null) {
      event.setDeleted(true);
      event.setMessageText(null);
      messagingTemplate.convertAndSend(channelTopic(targetMessage.channelId()), event);
    }
    return ResponseEntity.ok("Deleted");
  }

  // スレッド返信
  @PostMapping("/chat/thread/create")
  public ResponseEntity<ThreadMessage> createThread(
      @RequestParam(name = "messageId") int messageId,
      @RequestParam(name = "threadText") String threadText) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    ThreadData thread = chatService.saveThread(messageId, userId, threadText);
    if (thread == null) {
      return ResponseEntity.badRequest().build();
    }

    ThreadMessage event = buildThreadMessage(thread, "NEW");
    if (event != null) {
      messagingTemplate.convertAndSend(threadTopic(event.getChannelId()), event);
    }
    return event != null ? ResponseEntity.ok(event) : ResponseEntity.status(500).build();
  }

  @GetMapping("/chat/thread/{messageId}")
  public ResponseEntity<List<ThreadMessage>> getThreads(@PathVariable(name = "messageId") int messageId) {
    List<ThreadData> threads = chatService.getThreadsByMessageId(messageId);
    List<ThreadMessage> response = threads.stream()
        .map(thread -> buildThreadMessage(thread, null))
        .filter(Objects::nonNull)
        .toList();
    return ResponseEntity.ok(response);
  }

  @PostMapping("/chat/thread/update")
  public ResponseEntity<String> updateThread(
      @RequestParam(name = "threadId") int threadId,
      @RequestParam(name = "threadText") String threadText) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";

    boolean success = chatService.updateThread(threadId, threadText, userId, userRole);
    if (!success) {
      return ResponseEntity.badRequest().body("Failed");
    }

    ThreadData updatedThread = chatService.getThreadById(threadId);
    if (updatedThread != null) {
      ThreadMessage event = buildThreadMessage(updatedThread, "UPDATE");
      if (event != null) {
        messagingTemplate.convertAndSend(threadTopic(event.getChannelId()), event);
      }
    }
    return ResponseEntity.ok("Updated");
  }

  @PostMapping("/chat/thread/delete")
  public ResponseEntity<String> deleteThread(@RequestParam(name = "threadId") int threadId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";

    ThreadData targetThread = chatService.getThreadById(threadId);
    if (targetThread == null) {
      return ResponseEntity.badRequest().body("Failed");
    }

    boolean success = chatService.deleteThread(threadId, userId, userRole);
    if (!success) {
      return ResponseEntity.badRequest().body("Failed");
    }

    ThreadMessage event = buildThreadMessage(targetThread, "DELETE");
    if (event != null) {
      event.setDeleted(true);
      messagingTemplate.convertAndSend(threadTopic(event.getChannelId()), event);
    }
    return ResponseEntity.ok("Deleted");
  }

  @GetMapping("/chat/message/{messageId}/reactions")
  public ResponseEntity<List<ReactionMessage>> getReactions(@PathVariable(name = "messageId") int messageId) {
    List<ReactionData> reactions = chatService.getReactionsByMessageId(messageId);
    List<ReactionMessage> response = reactions.stream()
        .map(this::buildReactionMessage)
        .filter(Objects::nonNull)
        .toList();
    return ResponseEntity.ok(response);
  }

  // リアクション
  @PostMapping("/chat/reaction/add")
  public ResponseEntity<String> addReaction(
      @RequestParam(name = "messageId", required = false) Integer messageId,
      @RequestParam(name = "threadId", required = false) Integer threadId,
      @RequestParam(name = "emoji") String emoji) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    boolean success = chatService.addReaction(messageId, threadId, userId, emoji);
    return success ? ResponseEntity.ok("Added") : ResponseEntity.badRequest().body("Failed");
  }

  @PostMapping("/chat/reaction/remove")
  public ResponseEntity<String> removeReaction(
      @RequestParam(name = "messageId", required = false) Integer messageId,
      @RequestParam(name = "threadId", required = false) Integer threadId,
      @RequestParam(name = "emoji") String emoji) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    boolean success = chatService.removeReaction(messageId, threadId, userId, emoji);
    return success ? ResponseEntity.ok("Removed") : ResponseEntity.badRequest().body("Failed");
  }

  // 未読者管理
  @GetMapping("/chat/message/{messageId}/unread")
  public ResponseEntity<List<MessageReadData>> getUnreadUsers(@PathVariable(name = "messageId") int messageId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";
    
    // 講師のみアクセス可能
    if (!"ROLE_TEACHER".equals(userRole) && !"ROLE_ADMIN".equals(userRole)) {
      return ResponseEntity.status(403).build();
    }

    List<MessageReadData> unreadUsers = chatService.getUnreadUsers(messageId);
    return ResponseEntity.ok(unreadUsers);
  }

  @PostMapping("/chat/message/{messageId}/read")
  public ResponseEntity<String> markAsRead(@PathVariable(name = "messageId") int messageId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    chatService.markMessageAsRead(messageId, userId);
    return ResponseEntity.ok("Marked as read");
  }

  // リマインド送信機能
  @PostMapping("/chat/message/{messageId}/remind")
  public ResponseEntity<String> sendReminder(@PathVariable(name = "messageId") int messageId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";
    
    // 講師のみアクセス可能
    if (!"ROLE_TEACHER".equals(userRole) && !"ROLE_ADMIN".equals(userRole)) {
      return ResponseEntity.status(403).body("Forbidden");
    }

    List<MessageReadData> unreadUsers = chatService.getUnreadUsers(messageId);
    // リマインド通知を送信（簡易実装：未読者リストを返す）
    // 実際の実装では、メール通知やプッシュ通知などを送信
    return ResponseEntity.ok("Reminder sent to " + unreadUsers.size() + " users");
  }

  // メッセージ検索
  @GetMapping("/chat/search")
  public String searchMessages(
      @RequestParam(name = "keyword", required = false) String keyword,
      @RequestParam(name = "userId", required = false) String userId,
      @RequestParam(name = "channelId", required = false) Integer channelId,
      @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
      @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
      @RequestParam(name = "importance", required = false) String importance,
      Model model) {
    String currentUserId = loginService.getLoginUserId();
    if (currentUserId == null) {
      return "redirect:/login";
    }

    var currentUserData = loginService.getUserData();
    String userRole = currentUserData != null ? currentUserData.role() : "ROLE_STUDENT";

    List<ChannelData> accessibleChannels = chatService.getAccessibleChannels(currentUserId, userRole);
    List<UserData> users = userRepository.findAll();
    List<MessageData> results = chatService.searchMessages(
        currentUserId, userRole, keyword, userId, channelId, dateFrom, dateTo, importance);

    Map<Integer, String> channelNameMap = accessibleChannels.stream()
        .collect(Collectors.toMap(ChannelData::id, ChannelData::channelName));

    model.addAttribute("results", results);
    model.addAttribute("keyword", keyword);
    model.addAttribute("selectedUserId", userId);
    model.addAttribute("selectedChannelId", channelId);
    model.addAttribute("dateFrom", dateFrom);
    model.addAttribute("dateTo", dateTo);
    model.addAttribute("importance", importance);
    model.addAttribute("channels", accessibleChannels);
    model.addAttribute("users", users);
    model.addAttribute("channelNameMap", channelNameMap);
    return "chat/search";
  }

  // チャンネル申請承認/却下
  @PostMapping("/chat/request/{requestId}/approve")
  public String approveRequest(@PathVariable(name = "requestId") int requestId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return "redirect:/login";
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";
    
    if (!"ROLE_TEACHER".equals(userRole) && !"ROLE_ADMIN".equals(userRole)) {
      return "redirect:/chat";
    }

    chatService.approveChannelRequest(requestId, userId);
    return "redirect:/chat";
  }

  @PostMapping("/chat/request/{requestId}/reject")
  public String rejectRequest(@PathVariable(name = "requestId") int requestId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return "redirect:/login";
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";
    
    if (!"ROLE_TEACHER".equals(userRole) && !"ROLE_ADMIN".equals(userRole)) {
      return "redirect:/chat";
    }

    chatService.rejectChannelRequest(requestId, userId);
    return "redirect:/chat";
  }

  // チャンネル参加/退出
  @PostMapping("/chat/channel/{channelId}/join")
  public String joinChannel(@PathVariable(name = "channelId") int channelId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return "redirect:/login";
    }

    chatService.joinChannel(channelId, userId);
    return "redirect:/chat/channel/" + channelId;
  }

  @PostMapping("/chat/channel/{channelId}/leave")
  public String leaveChannel(@PathVariable(name = "channelId") int channelId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return "redirect:/login";
    }

    chatService.leaveChannel(channelId, userId);
    return "redirect:/chat";
  }

  @MessageMapping("/chat.send")
  public void sendMessage(ChatMessage chatMessage, Principal principal) {
    // セキュリティ: Principalがnullの場合は認証エラーとして処理
    if (principal == null) {
      System.err.println("! エラー: 認証されていないユーザーからのチャンネル送信要求を拒否しました。");
      return;
    }
    
    // 認証済みのIDを取得
    String actualSenderId = principal.getName();
    
    // クライアントから送信されたメッセージを使用
    MessageData savedMessage = chatService.saveMessage(
        chatMessage.getChannelId(),
        actualSenderId, // クライアント申告のIDではなく、認証済みのIDを使う
        chatMessage.getMessageText(),
        null,
        chatMessage.getImportance());

    if (savedMessage != null) {
      // 保存したメッセージをすべてのチャンネル参加者にブロードキャスト
      // （savedMessage にはDBがJOINした正しいユーザー名が含まれる）
      ChatMessage event = buildChatMessage(savedMessage, "NEW");
      messagingTemplate.convertAndSend(channelTopic(savedMessage.channelId()), event);
    }
  }

  @MessageMapping("/dm.send")
  public void sendDirectMessage(DirectMessageEvent dmEvent, Principal principal) {
    // セキュリティ: Principalがnullの場合は認証エラーとして処理
    if (principal == null) {
      System.err.println("! エラー: 認証されていないユーザーからのDM送信要求を拒否しました。");
      System.err.println("  (Principalがnullです。WebSocket接続時に認証情報が設定されていません)");
      return;
    }
    
    System.out.println("DM送信: Principal = " + principal.getName() + ", クライアントのsenderId = " + dmEvent.getSenderId());
    
    // principal.getName()には、現在ログインしているユーザーのIDが自動的に入る
    String actualSenderId = principal.getName();
    
    // 入力値の検証
    if (dmEvent.getReceiverId() == null || dmEvent.getMessageText() == null) {
      return;
    }
    
    // 最低限の検証: senderIdとreceiverIdが異なること（自分自身へのDMは許可しない）
    if (actualSenderId.equals(dmEvent.getReceiverId())) {
      return;
    }
    
    // ★★★ここが最重要★★★
    // クライアントが何を申告してきても（dmEvent.getSenderId()）、
    // それを無視し、認証済みのID (actualSenderId) を強制的に使う
    DirectMessageData message = chatService.sendDirectMessage(
        actualSenderId, // ← これで「なりすまし」が不可能になる
        dmEvent.getReceiverId(),
        dmEvent.getMessageText());
    chatService.logActivity(actualSenderId, "DM_SENT", "ダイレクトメッセージを送信しました", null, "DM");

    if (message != null) {
      DirectMessageEvent event = buildDirectMessageEvent(message, "NEW");
      if (event != null) {
        // dmTopicの計算も、信頼できるactualSenderIdを使う
        messagingTemplate.convertAndSend(dmTopic(actualSenderId, dmEvent.getReceiverId()), event);
      }
    }
  }

  // DM機能
  @GetMapping("/chat/dm")
  public String dmIndex(Model model) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return "redirect:/login";
    }

    List<String> partners = chatService.getDirectMessagePartners(userId);
    List<UserData> allUsers = userRepository.findAll().stream()
        .filter(u -> !u.userId().equals(userId))
        .toList();
    
    model.addAttribute("partners", partners);
    model.addAttribute("allUsers", allUsers);
    model.addAttribute("userId", userId);
    var userData = loginService.getUserData();
    model.addAttribute("userName", userData != null ? userData.userName() : "Unknown");
    return "chat/dm";
  }

  @GetMapping("/chat/dm/{partnerId}")
  public String dmConversation(@PathVariable(name = "partnerId") String partnerId, Model model) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return "redirect:/login";
    }

    List<DirectMessageData> messages = chatService.getDirectMessages(userId, partnerId);
    List<String> partners = chatService.getDirectMessagePartners(userId);
    
    var partnerData = userRepository.findByUserId(partnerId);
    String partnerName = partnerData != null ? partnerData.userName() : partnerId;
    
    model.addAttribute("messages", messages);
    model.addAttribute("partners", partners);
    model.addAttribute("partnerId", partnerId);
    model.addAttribute("partnerName", partnerName);
    model.addAttribute("userId", userId);
    var userData = loginService.getUserData();
    model.addAttribute("userName", userData != null ? userData.userName() : "Unknown");
    return "chat/dm-conversation";
  }

  @PostMapping("/chat/dm/send")
  public ResponseEntity<DirectMessageData> sendDirectMessage(
      @RequestParam(name = "receiverId") String receiverId,
      @RequestParam(name = "messageText") String messageText) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    DirectMessageData message = chatService.sendDirectMessage(userId, receiverId, messageText);
    chatService.logActivity(userId, "DM_SENT", "ダイレクトメッセージを送信しました", null, "DM");

    if (message != null) {
      DirectMessageEvent event = buildDirectMessageEvent(message, "NEW");
      if (event != null) {
        messagingTemplate.convertAndSend(dmTopic(userId, receiverId), event);
      }
    }
    return message != null ? ResponseEntity.ok(message) : ResponseEntity.badRequest().build();
  }

  @PostMapping("/chat/dm/update")
  public ResponseEntity<DirectMessageData> updateDirectMessage(
      @RequestParam(name = "messageId") int messageId,
      @RequestParam(name = "messageText") String messageText) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";

    boolean success = chatService.updateDirectMessage(messageId, messageText, userId, userRole);
    if (!success) {
      return ResponseEntity.badRequest().build();
    }
    DirectMessageData updated = chatService.getDirectMessageById(messageId);
    if (updated != null) {
      DirectMessageEvent event = buildDirectMessageEvent(updated, "UPDATE");
      if (event != null) {
        messagingTemplate.convertAndSend(dmTopic(updated.senderId(), updated.receiverId()), event);
      }
    }
    return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.ok().build();
  }

  @PostMapping("/chat/dm/delete")
  public ResponseEntity<String> deleteDirectMessage(
      @RequestParam(name = "messageId") int messageId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";

    boolean success = chatService.deleteDirectMessage(messageId, userId, userRole);
    if (!success) {
      return ResponseEntity.badRequest().body("Failed");
    }

    DirectMessageData deletedMessage = chatService.getDirectMessageById(messageId);
    if (deletedMessage != null) {
      DirectMessageEvent event = buildDirectMessageEvent(deletedMessage, "DELETE");
      if (event != null) {
        event.setDeleted(true);
        event.setMessageText(null);
        messagingTemplate.convertAndSend(dmTopic(deletedMessage.senderId(), deletedMessage.receiverId()), event);
      }
    }
    return ResponseEntity.ok("Deleted");
  }

  // アクティビティ機能
  @GetMapping("/chat/activity")
  public String activityIndex(Model model) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return "redirect:/login";
    }

    List<ActivityData> activities = chatService.getActivities(userId);
    model.addAttribute("activities", activities);
    model.addAttribute("userId", userId);
    return "chat/activity";
  }

  // チャンネルスター機能
  @PostMapping("/chat/channel/{channelId}/star")
  public ResponseEntity<String> toggleChannelStar(@PathVariable(name = "channelId") int channelId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    boolean success = chatService.toggleChannelStar(channelId, userId);
    return success ? ResponseEntity.ok("Toggled") : ResponseEntity.badRequest().body("Failed");
  }

  @GetMapping("/chat/channel/{channelId}/star")
  public ResponseEntity<Boolean> isChannelStarred(@PathVariable(name = "channelId") int channelId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    boolean isStarred = chatService.isChannelStarred(channelId, userId);
    return ResponseEntity.ok(isStarred);
  }

  // チャンネルメンバー追加
  @PostMapping("/chat/channel/{channelId}/add-member")
  public ResponseEntity<String> addChannelMember(
      @PathVariable(name = "channelId") int channelId,
      @RequestParam(name = "userId") String memberUserId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    boolean success = chatService.joinChannel(channelId, memberUserId);
    if (success) {
      chatService.logActivity(userId, "MEMBER_ADDED", "チャンネルにメンバーを追加しました", channelId, "CHANNEL");
    }
    return success ? ResponseEntity.ok("Added") : ResponseEntity.badRequest().body("Failed");
  }

  @GetMapping("/chat/channel/{channelId}/members")
  public ResponseEntity<List<UserData>> getChannelMembers(@PathVariable(name = "channelId") int channelId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    List<String> memberIds = chatService.getChannelMembers(channelId);
    List<UserData> members = userRepository.findAll().stream()
        .filter(u -> memberIds.contains(u.userId()))
        .toList();
    return ResponseEntity.ok(members);
  }

  // チャンネル設定
  @PostMapping("/chat/channel/{channelId}/update")
  public ResponseEntity<String> updateChannel(
      @PathVariable(name = "channelId") int channelId,
      @RequestParam(name = "channelName") String channelName,
      @RequestParam(name = "description") String description,
      @RequestParam(name = "isPublic") boolean isPublic) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";

    boolean success = chatService.updateChannel(channelId, channelName, description, isPublic, userId, userRole);
    if (success) {
      chatService.logActivity(userId, "CHANNEL_UPDATED", "チャンネルを更新しました", channelId, "CHANNEL");
    }
    return success ? ResponseEntity.ok("Updated") : ResponseEntity.badRequest().body("Failed");
  }

  @PostMapping("/chat/channel/{channelId}/invite")
  public ResponseEntity<ChannelInvitationData> inviteChannelMember(
      @PathVariable(name = "channelId") int channelId,
      @RequestParam(name = "userId") String inviteeId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";

    ChannelInvitationData invitation = chatService.inviteToChannel(channelId, userId, inviteeId, userRole);
    if (invitation == null) {
      return ResponseEntity.badRequest().build();
    }
    chatService.logActivity(userId, "CHANNEL_INVITE", "チャンネルに招待しました", channelId, "CHANNEL");
    return ResponseEntity.ok(invitation);
  }

  @PostMapping("/chat/invitation/{invitationId}/respond")
  public ResponseEntity<String> respondInvitation(
      @PathVariable(name = "invitationId") int invitationId,
      @RequestParam(name = "accept") boolean accept) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    boolean success = chatService.respondChannelInvitation(invitationId, userId, accept);
    return success ? ResponseEntity.ok("Updated") : ResponseEntity.badRequest().body("Failed");
  }

  @GetMapping("/chat/invitations/pending")
  public ResponseEntity<List<ChannelInvitationData>> getPendingInvitations() {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }
    List<ChannelInvitationData> invitations = chatService.getPendingInvitations(userId);
    return ResponseEntity.ok(invitations);
  }

  @PostMapping("/chat/channel/{channelId}/remove-member")
  public ResponseEntity<String> removeChannelMember(
      @PathVariable(name = "channelId") int channelId,
      @RequestParam(name = "userId") String targetUserId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    var userData = loginService.getUserData();
    String userRole = userData != null ? userData.role() : "ROLE_STUDENT";

    boolean success = chatService.removeChannelMember(channelId, targetUserId, userId, userRole);
    if (success) {
      chatService.logActivity(userId, "MEMBER_REMOVED", "チャンネルからメンバーを除外しました", channelId, "CHANNEL");
    }
    return success ? ResponseEntity.ok("Removed") : ResponseEntity.badRequest().body("Failed");
  }

  // ファイルアップロード
  @PostMapping("/chat/message/{messageId}/file")
  public ResponseEntity<String> uploadFile(
      @PathVariable(name = "messageId") int messageId,
      @RequestParam("file") MultipartFile file) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).body("Unauthorized");
    }

    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body("File is empty");
    }

    try {
      String uploadDir = "src/main/resources/static/uploads/";
      File dir = new File(uploadDir);
      if (!dir.exists()) {
        dir.mkdirs();
      }

      String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
      Path filePath = Paths.get(uploadDir + fileName);
      Files.write(filePath, file.getBytes());
      
      // 静的リソースパスとして保存
      String staticPath = "/uploads/" + fileName;

      MessageFileData fileData = chatService.saveMessageFile(
          messageId, file.getOriginalFilename(), staticPath,
          file.getSize(), file.getContentType(), userId);

      return fileData != null ? ResponseEntity.ok("Uploaded") : ResponseEntity.badRequest().body("Failed");
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
    }
  }

  // ユーザー一覧取得（メンション用）
  @GetMapping("/chat/users")
  public ResponseEntity<List<UserData>> getUsers() {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    List<UserData> users = userRepository.findAll();
    return ResponseEntity.ok(users);
  }

  // 最新メッセージ取得（ファイルアップロード用）
  @GetMapping("/chat/message/latest")
  public ResponseEntity<MessageData> getLatestMessage(@RequestParam(name = "channelId") int channelId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    List<MessageData> messages = chatService.getMessagesByChannelId(channelId);
    MessageData latest = messages.isEmpty() ? null : messages.get(messages.size() - 1);
    return latest != null ? ResponseEntity.ok(latest) : ResponseEntity.notFound().build();
  }

  // メッセージファイル取得
  @GetMapping("/chat/message/{messageId}/files")
  public ResponseEntity<List<MessageFileData>> getMessageFiles(@PathVariable(name = "messageId") int messageId) {
    String userId = loginService.getLoginUserId();
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }

    List<MessageFileData> files = chatService.getMessageFiles(messageId);
    return ResponseEntity.ok(files);
  }

  private ChatMessage buildChatMessage(MessageData messageData, String eventType) {
    if (messageData == null) {
      return null;
    }
    ChatMessage response = new ChatMessage();
    response.setId(messageData.id());
    response.setChannelId(messageData.channelId());
    response.setUserId(messageData.userId());
    response.setUserName(messageData.userName());
    response.setMessageText(messageData.messageText());
    response.setImportance(messageData.importance());
    response.setEventType(eventType);
    response.setEdited(messageData.isEdited());
    response.setDeleted(messageData.isDeleted());
    if (messageData.createdAt() != null) {
      response.setCreatedAt(ISO_FORMATTER.format(messageData.createdAt()));
    }
    if (messageData.updatedAt() != null) {
      response.setUpdatedAt(ISO_FORMATTER.format(messageData.updatedAt()));
    }
    return response;
  }

  private ThreadMessage buildThreadMessage(ThreadData threadData, String eventType) {
    if (threadData == null) {
      return null;
    }
    ThreadMessage response = new ThreadMessage();
    response.setId(threadData.id());
    response.setMessageId(threadData.messageId());
    response.setUserId(threadData.userId());
    response.setUserName(threadData.userName());
    response.setThreadText(threadData.threadText());
    response.setEventType(eventType);
    response.setEdited(threadData.isEdited());
    response.setDeleted(threadData.isDeleted());
    if (threadData.createdAt() != null) {
      response.setCreatedAt(ISO_FORMATTER.format(threadData.createdAt()));
    }
    if (threadData.updatedAt() != null) {
      response.setUpdatedAt(ISO_FORMATTER.format(threadData.updatedAt()));
    }

    MessageData parent = chatService.getMessageById(threadData.messageId());
    if (parent != null) {
      response.setChannelId(parent.channelId());
    }
    return response;
  }

  private ReactionMessage buildReactionMessage(ReactionData reactionData) {
    if (reactionData == null) {
      return null;
    }
    ReactionMessage response = new ReactionMessage();
    response.setId(reactionData.id());
    response.setMessageId(reactionData.messageId());
    response.setThreadId(reactionData.threadId());
    response.setUserId(reactionData.userId());
    response.setUserName(reactionData.userName());
    response.setEmoji(reactionData.emoji());
    if (reactionData.createdAt() != null) {
      response.setCreatedAt(ISO_FORMATTER.format(reactionData.createdAt()));
    }
    return response;
  }

  private String dmTopic(String userId1, String userId2) {
    if (userId1 == null || userId2 == null) {
      return "/topic/dm.unknown";
    }
    if (userId1.compareTo(userId2) <= 0) {
      return "/topic/dm." + userId1 + "_" + userId2;
    }
    return "/topic/dm." + userId2 + "_" + userId1;
  }

  private String channelTopic(int channelId) {
    return "/topic/channel." + channelId;
  }

  private String threadTopic(Integer channelId) {
    if (channelId == null) {
      return "/topic/channel.unknown.threads";
    }
    return "/topic/channel." + channelId + ".threads";
  }

  private DirectMessageEvent buildDirectMessageEvent(DirectMessageData data, String eventType) {
    if (data == null) {
      return null;
    }
    DirectMessageEvent event = new DirectMessageEvent();
    event.setId(data.id());
    event.setSenderId(data.senderId());
    event.setSenderName(data.senderName());
    event.setReceiverId(data.receiverId());
    event.setReceiverName(data.receiverName());
    if (!data.isDeleted()) {
      event.setMessageText(data.messageText());
    }
    event.setEdited(data.isEdited());
    event.setDeleted(data.isDeleted());
    if (data.createdAt() != null) {
      event.setCreatedAt(ISO_FORMATTER.format(data.createdAt()));
    }
    if (data.updatedAt() != null) {
      event.setUpdatedAt(ISO_FORMATTER.format(data.updatedAt()));
    }
    event.setEventType(eventType);
    return event;
  }
}

