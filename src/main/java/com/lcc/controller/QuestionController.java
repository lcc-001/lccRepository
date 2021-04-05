package com.lcc.controller;

import com.lcc.dao.User;
import com.lcc.dto.CommentDTO;
import com.lcc.dto.QuestionDTO;
import com.lcc.enmus.CommentTypeEnum;
import com.lcc.service.CommentService;
import com.lcc.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class QuestionController {
      @Autowired
      private QuestionService questionService;
      @Autowired
      private CommentService commentService;
    @GetMapping("/question/{id}")
   public String question(@PathVariable(name="id") Integer id, HttpServletRequest request,
                          Model model){
        QuestionDTO questionDTO=questionService.getById(id);
        List<QuestionDTO> relatedQuestions=questionService.selectRelated(questionDTO);
        List<CommentDTO> comments=commentService.listByTargetId(id, CommentTypeEnum.QUESTION);
        //增加阅读数
        questionService.incView(id);
        model.addAttribute("question",questionDTO);
        model.addAttribute("comments",comments);
        model.addAttribute("relatedQuestions",relatedQuestions);
        return "question";
}

}
