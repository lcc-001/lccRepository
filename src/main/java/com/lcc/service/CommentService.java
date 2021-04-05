package com.lcc.service;

import com.lcc.dao.*;
import com.lcc.dto.CommentDTO;
import com.lcc.enmus.CommentTypeEnum;
import com.lcc.enmus.NotificationStatusEnum;
import com.lcc.enmus.NotificationTypeEnum;
import com.lcc.exception.CustomizeErrorCode;
import com.lcc.exception.CustomizeException;
import com.lcc.mapper.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionExtMapper questionExtMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CommentExtMapper commentExtMapper;
    @Autowired
    private NotificationMapper notificationMapper;
//事务
@Transactional
public void insert(Comment comment, User commentator) {
    if (comment.getParentId() == null || comment.getParentId() == 0) {
        throw new CustomizeException(CustomizeErrorCode.TARGET_PARAM_NOT_FOUND);
    }
    if (comment.getType() == null || !CommentTypeEnum.isExist(comment.getType())) {
        throw new CustomizeException(CustomizeErrorCode.TYPE_PARAM_WRONG);
    }
    if (comment.getType() == CommentTypeEnum.COMMENT.getType()) {
        // 回复评论
        Comment dbComment = commentMapper.selectByPrimaryKey(comment.getParentId());
        if (dbComment == null) {
            throw new CustomizeException(CustomizeErrorCode.COMMENT_NOT_FOUND);
        }

        // 回复问题
        Question question = questionMapper.selectByPrimaryKey(dbComment.getParentId());
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }

        commentMapper.insert(comment);

        // 增加评论数
        Comment parentComment = new Comment();
        parentComment.setId(comment.getParentId());
        parentComment.setCommentCount(1);
        commentExtMapper.incCommentCount(parentComment);

        // 创建通知
        createNotify(comment, dbComment.getCommentator(), commentator.getName(), question.getTitle(), NotificationTypeEnum.REPLY_COMMENT, question.getId());
    } else {
        // 回复问题
        Question question = questionMapper.selectByPrimaryKey(comment.getParentId());
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        comment.setCommentCount(0);
        commentMapper.insert(comment);
        question.setCommentCount(1);
        questionExtMapper.incCommentCount(question);

        // 创建通知
        createNotify(comment, question.getCreator(), commentator.getName(), question.getTitle(), NotificationTypeEnum.REPLY_QUESTION, question.getId());
    }
}

    private void createNotify(Comment comment, Integer receiver, String notifierName, String outerTitle, NotificationTypeEnum notificationType, Integer outerId) {
        if (receiver == comment.getCommentator()) {
            return;
        }
        Notification notification = new Notification();
        notification.setGmtCreate(System.currentTimeMillis());
        notification.setType(notificationType.getType());
        notification.setOuterid(outerId);
        notification.setNotifier(comment.getCommentator());
        notification.setStatus(NotificationStatusEnum.UNREAD.getStatus());
        notification.setReceiver(receiver);
        notification.setNotifierName(notifierName);
        notification.setOuterTitle(outerTitle);
        notificationMapper.insert(notification);
    }

    public List<CommentDTO> listByTargetId(Integer id, CommentTypeEnum type) {
        CommentExample commentExample = new CommentExample();
        commentExample.createCriteria()
                .andParentIdEqualTo(id)
                .andTypeEqualTo(type.getType());
        commentExample.setOrderByClause("gmt_create desc");
        List<Comment> comments = commentMapper.selectByExample(commentExample);
        if (comments.size() == 0) {
            return new ArrayList<>();
        }
        // 获取去重的评论人
        Set<Integer> commentators = comments.stream().map(comment -> comment.getCommentator()).collect(Collectors.toSet());
        List<Integer> userIds = new ArrayList();
        userIds.addAll(commentators);


        // 获取评论人并转换为 Map
        UserExample userExample = new UserExample();
        userExample.createCriteria()
                .andIdIn(userIds);
        List<User> users = userMapper.selectByExample(userExample);
        Map<Integer, User> userMap = users.stream().collect(Collectors.toMap(user -> user.getId(), user -> user));


        // 转换 comment 为 commentDTO
        List<CommentDTO> commentDTOS = comments.stream().map(comment -> {
            CommentDTO commentDTO = new CommentDTO();
            BeanUtils.copyProperties(comment, commentDTO);
            commentDTO.setUser(userMap.get(comment.getCommentator()));
            return commentDTO;
        }).collect(Collectors.toList());

        return commentDTOS;
    }
}
