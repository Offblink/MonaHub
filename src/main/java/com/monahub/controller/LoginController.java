package com.monahub.controller;

import com.monahub.entity.SysUser;
import com.monahub.interceptor.LoginInterceptor;
import com.monahub.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 登录 / 退出。对应任务书 F1 的"登录与拦截器"。
 */
@Controller
public class LoginController {

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    /** 根路径：已登录看列表，未登录被拦截器送回 /login。 */
    @GetMapping("/")
    public String index() {
        return "redirect:/files";
    }

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LoginInterceptor.SESSION_USER) != null) {
            return "redirect:/files";
        }
        return "login";
    }

    /**
     * 登录提交：查 sys_user → 加盐散列比对 → 脱敏用户进 session。
     * 失败时不区分"账号不存在"与"密码错误"，避免账号枚举。
     */
    @PostMapping("/login")
    public String doLogin(@RequestParam(value = "username", required = false) String username,
                          @RequestParam(value = "password", required = false) String password,
                          HttpServletRequest request,
                          Model model) {
        SysUser user = userService.login(username, password);
        if (user == null) {
            model.addAttribute("error", "账号或密码错误");
            model.addAttribute("username", username == null ? "" : username);
            return "login";
        }

        // 防会话固定：登录成功丢弃旧会话，换一个新的 session id 再放用户
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(LoginInterceptor.SESSION_USER, user);
        session.setMaxInactiveInterval(30 * 60);
        return "redirect:/files";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}
