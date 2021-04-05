package com.lcc.service;

import com.lcc.dao.Question;
import com.lcc.dao.QuestionExample;
import com.lcc.dao.User;
import com.lcc.dto.PaginationDTO;
import com.lcc.dto.QuestionDTO;
import com.lcc.dto.QuestionQueryDTO;
import com.lcc.exception.CustomizeErrorCode;
import com.lcc.exception.CustomizeException;
import com.lcc.mapper.QuestionExtMapper;
import com.lcc.mapper.QuestionMapper;
import com.lcc.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {
  @Autowired
  private QuestionMapper questionMapper;
  @Autowired
  private UserMapper userMapper;
  @Autowired
    QuestionExtMapper questionExtMapper;

    public PaginationDTO list(String search, Integer page, Integer size) {

        if (StringUtils.isNotBlank(search)) {
            String[] tags = StringUtils.split(search, " ");
            search=Arrays.stream(tags).collect(Collectors.joining("|"));
        }
        //分页
        PaginationDTO paginationDTO= new PaginationDTO();
        Integer totalPage;
        QuestionQueryDTO questionQueryDTO=new QuestionQueryDTO();
        questionQueryDTO.setSearch(search);
        Integer totalCount= questionExtMapper.countBySearch(new QuestionQueryDTO());
        if (totalCount % size == 0) {
            totalPage = totalCount / size;
        } else {
            totalPage = totalCount / size + 1;
        }

        if (page < 1) {
            page = 1;
        }
        if (page > totalPage) {
            page = totalPage;
        }
        paginationDTO.setPagination(totalPage,page);
        Integer offset = size * (page - 1);
        questionQueryDTO.setSize(size);
        questionQueryDTO.setPage(offset);
        List<Question> questions= questionExtMapper.selectBySearch(questionQueryDTO);
        List<QuestionDTO> questionDTOList=new ArrayList<>();

   for(Question question:questions){
     User user=userMapper.selectByPrimaryKey(question.getCreator());
     QuestionDTO questionDTO=new QuestionDTO();
     BeanUtils.copyProperties(question,questionDTO);
     questionDTO.setUser(user);
     questionDTOList.add(questionDTO);
   }
   paginationDTO.setData(questionDTOList);
   return paginationDTO;
    }

    public PaginationDTO list(Integer userId, Integer page, Integer size) {
        PaginationDTO paginationDTO = new PaginationDTO();

        Integer totalPage;

        QuestionExample questionExample = new QuestionExample();
        questionExample.createCriteria()
                .andCreatorEqualTo(userId);
        Integer totalCount = (int) questionMapper.countByExample(questionExample);

        if (totalCount % size == 0) {
            totalPage = totalCount / size;
        } else {
            totalPage = totalCount / size + 1;
        }

        if (page < 1) {
            page = 1;
        }
        if (page > totalPage) {
            page = totalPage;
        }

        paginationDTO.setPagination(totalPage, page);

        //size*(page-1)
        Integer offset = size * (page - 1);
        QuestionExample example = new QuestionExample();
        example.createCriteria()
                .andCreatorEqualTo(userId);
        List<Question> questions = questionMapper.selectByExampleWithBLOBs(example, new RowBounds(offset, size));
        List<QuestionDTO> questionDTOList = new ArrayList<>();

        for (Question question : questions) {
            User user = userMapper.selectByPrimaryKey(question.getCreator());
            QuestionDTO questionDTO = new QuestionDTO();
            BeanUtils.copyProperties(question, questionDTO);
            questionDTO.setUser(user);
            questionDTOList.add(questionDTO);
        }

        paginationDTO.setData(questionDTOList);
        return paginationDTO;
    }

    public QuestionDTO getById(Integer id) {
        Question question= questionMapper.selectByPrimaryKey(id);
        if(question==null)
        {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }

        QuestionDTO questionDTO=new QuestionDTO();
        BeanUtils.copyProperties(question,questionDTO);
        User user=userMapper.selectByPrimaryKey(question.getCreator());
        questionDTO.setUser(user);
        return questionDTO;
    }
    //判断是新发布帖子还是更新编辑之前的帖子
    public void createOrUpdate(Question question) {
         if(question.getId()==null){
             question.setGmtCreate(System.currentTimeMillis());
             question.setGmtModified(question.getGmtCreate());
             question.setViewCount(0);
             question.setLikeCount(0);
             question.setCommentCount(0);
             questionMapper.insert(question);
         }else {
             Question updateQuestion=new Question();
             updateQuestion.setGmtModified(System.currentTimeMillis());
             updateQuestion.setTitle(question.getTitle());
             updateQuestion.setDescription(question.getDescription());
             updateQuestion.setTag(question.getTag());
             QuestionExample questionExample=new QuestionExample();
             questionExample.createCriteria().andIdEqualTo(question.getId());
             int updated= questionMapper.updateByExampleSelective(updateQuestion,questionExample);
             if(updated!=1){
                 throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
             }
         }
    }
//增加阅读数
    public void incView(Integer id) {
       Question question=new Question();
       question.setId(id);
       question.setViewCount(1);
       questionExtMapper.incView(question);
    }
//标签相关
    public List<QuestionDTO> selectRelated(QuestionDTO queryDTO) {
           if(StringUtils.isBlank(queryDTO.getTag())){
               return new ArrayList<>();
           }
        String[] tags=StringUtils.split(queryDTO.getTag(),",");
           String regexpTag= Arrays.stream(tags).collect(Collectors.joining("|"));
           Question question=new Question();
           question.setId(queryDTO.getId());
           question.setTag(queryDTO.getTag());

           List<Question> questions=questionExtMapper.selectRelated(question);
           List<QuestionDTO> questionDTOS=questions.stream().map(q->{
               QuestionDTO questionDTO=new QuestionDTO();
               BeanUtils.copyProperties(q,questionDTO);
               return questionDTO;
           }).collect(Collectors.toList());
           return questionDTOS;
    }
}

