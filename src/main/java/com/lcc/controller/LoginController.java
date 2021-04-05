package com.lcc.controller;

import com.lcc.dao.User;
import com.lcc.dao.UserExample;
import com.lcc.dto.PaginationDTO;
import com.lcc.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;

@Controller
public class LoginController {

    @Autowired
    private UserMapper userMapper;
    @GetMapping("/{action}")
    public String tologin(@PathVariable(name = "action") String action, Model model){
        if("login".equals(action)){
            model.addAttribute("section","login");

        }else if("regist".equals(action)){
            model.addAttribute("section","regist");
        }
        return "login";
    }
    @PostMapping("/login")
    public String login(User user, HttpServletRequest request, HttpServletResponse response, Model model){

        String code= UUID.randomUUID().toString();
        user.setCode(code);
        UserExample userExample=new UserExample();
         UserExample.Criteria criteria=userExample.createCriteria();
         criteria.andNameEqualTo(user.getName());
         criteria.andPasswordEqualTo(user.getPassword());
         List<User> userList=userMapper.selectByExample(userExample);
         System.out.println(userList.get(0).getId());
        System.out.println(userList.get(0).getName());
        System.out.println(userList.get(0).getPassword());
        System.out.println(userList.get(0).getPhoto());
         response.addCookie(new Cookie("code",code));
        userMapper.updateByExampleSelective(user,userExample);
        if(userList.size()==0){
            model.addAttribute("error","用户名或密码错误");
            model.addAttribute("section","login");
            return "login";
        }
        return "redirect:/";
    }
       //退出登录
      @GetMapping("/logout")
     public String logout(HttpServletRequest request,HttpServletResponse response){
         request.getSession().removeAttribute("user");
         Cookie cookie=new Cookie("code",null);
         cookie.setMaxAge(0);
         response.addCookie(cookie);
        return "redirect:/";
     }
     //注册
    @PostMapping("/regist")
      public String regist( @RequestParam(name = "name") String name,
                            @RequestParam(name = "password") String password,
                            @RequestParam(name = "password2") String password2,
                            Model model){
        if(password.equals(password2)){
            User user=new User();
            user.setName(name);
            user.setPassword(password);
            userMapper.insert(user);
            model.addAttribute("section","login");
            return "login";
        }else {
            System.out.println(password);
            System.out.println(password2);
            model.addAttribute("message","两次密码不一致，请再次输入");
            model.addAttribute("section","regist");
            return "login";
        }

      }
}
