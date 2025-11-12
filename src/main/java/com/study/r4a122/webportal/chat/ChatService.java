package com.study.r4a122.webportal.chat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

  @Autowired
  private ChannelRepository channelRepository;

  @Autowired
  private MessageRepository messageRepository;

  @Autowired
  private ThreadRepository threadRepository;

  @Autowired
  private ReactionRepository reactionRepository;

  @Autowired
  private MessageReadRepository messageReadRepository;

  @Autowired
  private ChannelRequestRepository channelRequestRepository;

  @Autowired
  private ChannelMemberRepository channelMemberRepository;

  @Autowired
  private DirectMessageRepository directMessageRepository;

  @Autowired
  private ActivityRepository activityRepository;

  @Autowired
  private ChannelStarRepository channelStarRepository;

  @Autowired
  private MessageFileRepository messageFileRepository;

  @Autowired
  private ChannelInvitationRepository channelInvitationRepository;

  public List<ChannelData> getAllChannels() {
    return channelRepository.findAll();
  }

  public ChannelData getChannelById(int channelId) {
    return channelRepository.findById(channelId);
  }

  public List<MessageData> getMessagesByChannelId(int channelId) {
    return messageRepository.findByChannelId(channelId);
  }

  public MessageData saveMessage(int channelId, String userId, String messageText) {
    return saveMessage(channelId, userId, messageText, null, "NORMAL");
  }

  public MessageData saveMessage(int channelId, String userId, String messageText, Integer parentMessageId) {
    return saveMessage(channelId, userId, messageText, parentMessageId, "NORMAL");
  }

  public MessageData saveMessage(int channelId, String userId, String messageText, Integer parentMessageId, String importance) {
    String normalizedImportance = normalizeImportance(importance, "NORMAL");
    MessageData messageData = new MessageData(0, channelId, userId, null, messageText, parentMessageId, normalizedImportance, false, false, LocalDateTime.now(), null);
    messageRepository.save(messageData);
    
    // 保存後に最新のメッセージを取得（IDとユーザー名を含む）
    List<MessageData> messages = messageRepository.findByChannelId(channelId);
    return messages.isEmpty() ? null : messages.get(messages.size() - 1);
  }
  
  public boolean updateMessage(int messageId, String messageText, String importance, String userId, String userRole) {
    MessageData target = messageRepository.findById(messageId);
    if (target == null) {
      return false;
    }
    String normalizedImportance = normalizeImportance(importance, target.importance());
    if (target.userId().equals(userId)) {
      return messageRepository.updateMessage(messageId, messageText, normalizedImportance, userId);
    }
    if (isAdmin(userRole)) {
      return messageRepository.updateMessageByAdmin(messageId, messageText, normalizedImportance);
    }
    return false;
  }
  
  public boolean deleteMessage(int messageId, String userId, String userRole) {
    MessageData target = messageRepository.findById(messageId);
    if (target == null) {
      return false;
    }
    if (target.userId().equals(userId)) {
      return messageRepository.deleteMessage(messageId, userId);
    }
    if (isAdmin(userRole)) {
      return messageRepository.deleteMessageByAdmin(messageId);
    }
    return false;
  }
  
  public MessageData getMessageById(int messageId) {
    return messageRepository.findById(messageId);
  }

  public ChannelData createChannel(String channelName, String description, boolean isPublic, String createdBy, List<String> initialMemberIds) {
    int channelId = channelRepository.nextId();
    ChannelData channelData = new ChannelData(channelId, channelName, description, isPublic, "ACTIVE", LocalDateTime.now(), createdBy);
    channelRepository.save(channelData);
    
    ChannelData created = channelRepository.findById(channelId);
    if (created != null) {
      channelMemberRepository.addCreatorAsMember(created.id(), createdBy);
      if (initialMemberIds != null) {
        for (String memberId : initialMemberIds) {
          if (memberId == null || memberId.isBlank() || memberId.equals(createdBy)) {
            continue;
          }
          channelMemberRepository.joinChannel(created.id(), memberId);
        }
      }
    }
    return created;
  }

  // スレッド機能
  public List<ThreadData> getThreadsByMessageId(int messageId) {
    return threadRepository.findByMessageId(messageId);
  }

  public ThreadData saveThread(int messageId, String userId, String threadText) {
    ThreadData threadData = new ThreadData(0, messageId, userId, null, threadText, false, false, LocalDateTime.now(), null);
    threadRepository.save(threadData);
    
    List<ThreadData> threads = threadRepository.findByMessageId(messageId);
    return threads.isEmpty() ? null : threads.get(threads.size() - 1);
  }

  public boolean updateThread(int threadId, String threadText, String userId, String userRole) {
    ThreadData target = threadRepository.findById(threadId);
    if (target == null) {
      return false;
    }
    if (target.userId().equals(userId)) {
      return threadRepository.updateThread(threadId, threadText, userId);
    }
    if (isAdmin(userRole)) {
      return threadRepository.updateThreadByAdmin(threadId, threadText);
    }
    return false;
  }

  public boolean deleteThread(int threadId, String userId, String userRole) {
    ThreadData target = threadRepository.findById(threadId);
    if (target == null) {
      return false;
    }
    if (target.userId().equals(userId)) {
      return threadRepository.deleteThread(threadId, userId);
    }
    if (isAdmin(userRole)) {
      return threadRepository.deleteThreadByAdmin(threadId);
    }
    return false;
  }

  public ThreadData getThreadById(int threadId) {
    return threadRepository.findById(threadId);
  }

  // リアクション機能
  public List<ReactionData> getReactionsByMessageId(int messageId) {
    return reactionRepository.findByMessageId(messageId);
  }

  public List<ReactionData> getReactionsByThreadId(int threadId) {
    return reactionRepository.findByThreadId(threadId);
  }

  public boolean addReaction(Integer messageId, Integer threadId, String userId, String emoji) {
    return reactionRepository.addReaction(messageId, threadId, userId, emoji);
  }

  public boolean removeReaction(Integer messageId, Integer threadId, String userId, String emoji) {
    return reactionRepository.removeReaction(messageId, threadId, userId, emoji);
  }

  // 未読管理機能
  public void markMessageAsRead(int messageId, String userId) {
    messageReadRepository.markAsRead(messageId, userId);
  }

  public List<MessageReadData> getUnreadUsers(int messageId) {
    return messageReadRepository.findUnreadUsers(messageId);
  }

  // メッセージ検索機能
  public List<MessageData> searchMessages(
      String currentUserId,
      String userRole,
      String keyword,
      String userId,
      Integer channelId,
      LocalDate dateFrom,
      LocalDate dateTo,
      String importance) {

    List<ChannelData> accessibleChannels = getAccessibleChannels(currentUserId, userRole);
    if (accessibleChannels.isEmpty()) {
      return List.of();
    }

    List<Integer> accessibleChannelIds = accessibleChannels.stream()
        .map(ChannelData::id)
        .toList();

    if (channelId != null && !accessibleChannelIds.contains(channelId)) {
      // 指定されたチャンネルにアクセスできない場合は結果なし
      return List.of();
    }

    LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
    LocalDateTime to = null;
    if (dateTo != null) {
      // 終日の終わりまで含める
      to = dateTo.atTime(LocalTime.MAX);
    }

    return messageRepository.searchMessages(
        accessibleChannelIds,
        keyword,
        userId,
        channelId,
        from,
        to,
        importance,
        200);
  }

  // チャンネル申請機能
  public List<ChannelRequestData> getAllPendingRequests() {
    return channelRequestRepository.findAllPending();
  }

  public ChannelRequestData createChannelRequest(String channelName, String description, boolean isPublic, String requestedBy) {
    ChannelRequestData requestData = new ChannelRequestData(0, channelName, description, isPublic, requestedBy, null, "PENDING", LocalDateTime.now(), null, null);
    channelRequestRepository.save(requestData);
    
    List<ChannelRequestData> requests = channelRequestRepository.findAllPending();
    return requests.isEmpty() ? null : requests.get(0);
  }

  public boolean approveChannelRequest(int requestId, String reviewedBy) {
    ChannelRequestData request = channelRequestRepository.findById(requestId);
    if (request == null || !"PENDING".equals(request.status())) {
      return false;
    }

    boolean updated = channelRequestRepository.updateStatus(requestId, "APPROVED", reviewedBy);
    if (updated) {
      // チャンネルを作成
      ChannelData channel = createChannel(request.channelName(), request.description(), request.isPublic(), request.requestedBy(), List.of());
      if (channel != null) {
        channelMemberRepository.addCreatorAsMember(channel.id(), request.requestedBy());
      }
    }
    return updated;
  }

  public boolean rejectChannelRequest(int requestId, String reviewedBy) {
    return channelRequestRepository.updateStatus(requestId, "REJECTED", reviewedBy);
  }

  // チャンネル参加/退出機能
  public boolean joinChannel(int channelId, String userId) {
    return channelMemberRepository.joinChannel(channelId, userId);
  }

  public boolean leaveChannel(int channelId, String userId) {
    return channelMemberRepository.leaveChannel(channelId, userId);
  }

  public boolean isChannelMember(int channelId, String userId) {
    return channelMemberRepository.isMember(channelId, userId);
  }

  public List<String> getChannelMembers(int channelId) {
    return channelMemberRepository.getChannelMembers(channelId);
  }

  public List<ChannelData> getAccessibleChannels(String userId, String userRole) {
    List<ChannelData> allChannels = channelRepository.findAll();
    
    // 講師はすべてのチャンネルにアクセス可能
    if ("ROLE_TEACHER".equals(userRole) || "ROLE_ADMIN".equals(userRole)) {
      return allChannels;
    }
    
    // 学生は公開チャンネルまたは参加しているチャンネルのみ
    return allChannels.stream()
        .filter(ch -> ch.isPublic() || channelMemberRepository.isMember(ch.id(), userId))
        .toList();
  }

  // DM機能
  public List<DirectMessageData> getDirectMessages(String userId1, String userId2) {
    return directMessageRepository.findConversation(userId1, userId2);
  }

  public List<String> getDirectMessagePartners(String userId) {
    return directMessageRepository.findConversationPartners(userId);
  }

  public DirectMessageData sendDirectMessage(String senderId, String receiverId, String messageText) {
    DirectMessageData messageData = new DirectMessageData(0, senderId, null, receiverId, null, messageText, false, false, LocalDateTime.now(), null);
    directMessageRepository.save(messageData);
    
    List<DirectMessageData> messages = directMessageRepository.findConversation(senderId, receiverId);
    return messages.isEmpty() ? null : messages.get(messages.size() - 1);
  }

  public boolean updateDirectMessage(int messageId, String messageText, String userId, String userRole) {
    DirectMessageData target = directMessageRepository.findById(messageId);
    if (target == null) {
      return false;
    }
    if (target.senderId().equals(userId)) {
      return directMessageRepository.updateMessage(messageId, messageText, userId);
    }
    if (isAdmin(userRole)) {
      return directMessageRepository.updateMessageByAdmin(messageId, messageText);
    }
    return false;
  }

  public boolean deleteDirectMessage(int messageId, String userId, String userRole) {
    DirectMessageData target = directMessageRepository.findById(messageId);
    if (target == null) {
      return false;
    }
    if (target.senderId().equals(userId) || target.receiverId().equals(userId)) {
      return directMessageRepository.deleteMessage(messageId, userId);
    }
    if (isAdmin(userRole)) {
      return directMessageRepository.deleteMessageByAdmin(messageId);
    }
    return false;
  }

  public DirectMessageData getDirectMessageById(int messageId) {
    return directMessageRepository.findById(messageId);
  }

  // アクティビティ機能
  public List<ActivityData> getActivities(String userId) {
    return activityRepository.findByUserId(userId);
  }

  public void logActivity(String userId, String activityType, String activityMessage, Integer relatedId, String relatedType) {
    ActivityData activityData = new ActivityData(0, userId, null, activityType, activityMessage, relatedId, relatedType, LocalDateTime.now());
    activityRepository.save(activityData);
  }

  // チャンネルスター機能
  public boolean toggleChannelStar(int channelId, String userId) {
    return channelStarRepository.toggleStar(channelId, userId);
  }

  public boolean isChannelStarred(int channelId, String userId) {
    return channelStarRepository.isStarred(channelId, userId);
  }

  public List<Integer> getStarredChannels(String userId) {
    return channelStarRepository.findStarredChannels(userId);
  }

  // ファイル機能
  public List<MessageFileData> getMessageFiles(int messageId) {
    return messageFileRepository.findByMessageId(messageId);
  }

  public MessageFileData saveMessageFile(int messageId, String fileName, String filePath, Long fileSize, String fileType, String uploadedBy) {
    MessageFileData fileData = new MessageFileData(0, messageId, fileName, filePath, fileSize, fileType, uploadedBy, LocalDateTime.now());
    messageFileRepository.save(fileData);
    
    List<MessageFileData> files = messageFileRepository.findByMessageId(messageId);
    return files.isEmpty() ? null : files.get(files.size() - 1);
  }

  // チャンネル更新機能
  public boolean updateChannel(int channelId, String channelName, String description, boolean isPublic, String requesterId, String requesterRole) {
    ChannelData channel = channelRepository.findById(channelId);
    if (channel == null) {
      return false;
    }
    if (!canManageChannel(channel, requesterId, requesterRole)) {
      return false;
    }
    return channelRepository.update(channelId, channelName, description, isPublic);
  }

  public boolean deleteChannel(int channelId, String requesterId, String requesterRole) {
    ChannelData channel = channelRepository.findById(channelId);
    if (channel == null) {
      return false;
    }
    if (!canManageChannel(channel, requesterId, requesterRole)) {
      return false;
    }
    return channelRepository.softDelete(channelId);
  }

  public ChannelInvitationData inviteToChannel(int channelId, String inviterId, String inviteeId, String userRole) {
    if (inviterId.equals(inviteeId)) {
      return null;
    }
    ChannelData channel = channelRepository.findById(channelId);
    if (channel == null || !"ACTIVE".equals(channel.status())) {
      return null;
    }
    boolean inviterCanManage = canManageChannel(channel, inviterId, userRole) || channelMemberRepository.isMember(channelId, inviterId);
    if (!inviterCanManage && !isAdmin(userRole)) {
      return null;
    }
    if (channelMemberRepository.isMember(channelId, inviteeId)) {
      return null;
    }
    if (channelInvitationRepository.existsPending(channelId, inviteeId)) {
      return null;
    }
    return channelInvitationRepository.createInvitation(channelId, inviterId, inviteeId);
  }

  public boolean respondChannelInvitation(int invitationId, String userId, boolean accept) {
    ChannelInvitationData invitation = channelInvitationRepository.findById(invitationId);
    if (invitation == null || invitation.status() == null || !"PENDING".equals(invitation.status())) {
      return false;
    }
    if (!invitation.inviteeId().equals(userId)) {
      return false;
    }
    boolean updated = channelInvitationRepository.updateStatus(invitationId, accept ? "ACCEPTED" : "DECLINED");
    if (updated && accept) {
      channelMemberRepository.joinChannel(invitation.channelId(), userId);
    }
    return updated;
  }

  public List<ChannelInvitationData> getPendingInvitations(String userId) {
    return channelInvitationRepository.findPendingByInvitee(userId);
  }

  public List<ChannelInvitationData> getChannelInvitations(int channelId, String requesterId, String requesterRole) {
    ChannelData channel = channelRepository.findById(channelId);
    if (channel == null) {
      return List.of();
    }
    if (!canManageChannel(channel, requesterId, requesterRole) && !isAdmin(requesterRole)) {
      return List.of();
    }
    return channelInvitationRepository.findByChannel(channelId);
  }

  public boolean removeChannelMember(int channelId, String targetUserId, String requesterId, String requesterRole) {
    ChannelData channel = channelRepository.findById(channelId);
    if (channel == null) {
      return false;
    }
    if (!canManageChannel(channel, requesterId, requesterRole) && !isAdmin(requesterRole)) {
      return false;
    }
    if (channel.createdBy() != null && channel.createdBy().equals(targetUserId) && !isAdmin(requesterRole)) {
      // 作成者は管理者のみ削除可能
      return false;
    }
    if (!channelMemberRepository.isMember(channelId, targetUserId)) {
      return false;
    }
    return channelMemberRepository.leaveChannel(channelId, targetUserId);
  }

  private boolean canManageChannel(ChannelData channel, String requesterId, String requesterRole) {
    if (isAdmin(requesterRole)) {
      return true;
    }
    return channel.createdBy() != null && channel.createdBy().equals(requesterId);
  }

  private boolean isAdmin(String role) {
    return role != null && Set.of("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_MANAGER", "ROLE_TOP").contains(role);
  }

  private String normalizeImportance(String requested, String current) {
    if (requested == null || requested.isBlank()) {
      return current != null ? current : "NORMAL";
    }
    String upper = requested.trim().toUpperCase();
    return switch (upper) {
      case "IMPORTANT", "URGENT" -> upper;
      default -> "NORMAL";
    };
  }
}

